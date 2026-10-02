package com.example.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.data.model.UserLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

sealed class LocationFetchResult {
    data class Success(val location: UserLocation, val isFresh: Boolean) : LocationFetchResult()
    data class Failure(
        val reason: LocationFailure,
        val cachedFallback: UserLocation?
    ) : LocationFetchResult()
}

class AppLocationService(private val context: Context) {

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    /**
     * Checks if either coarse or fine location permission is granted.
     */
    fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    /**
     * Checks if any location provider (GPS or Network) is enabled on the device.
     */
    fun isLocationEnabled(): Boolean {
        val lm = locationManager ?: return false
        val gpsEnabled = try { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (e: Exception) { false }
        val netEnabled = try { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (e: Exception) { false }
        return gpsEnabled || netEnabled
    }

    /**
     * Fetches the user's coordinates:
     * 1. Attempts to get a fresh GPS/Network fix with a 6-second timeout.
     * 2. Falls back immediately to the OS last known location across all providers.
     * 3. Falls back to cached fallback if provided.
     * Works seamlessly offline.
     */
    suspend fun fetchCurrentCoordinates(
        fallbackLocation: UserLocation? = null
    ): LocationFetchResult = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) {
            return@withContext LocationFetchResult.Failure(
                reason = LocationFailure.PERMISSION_NOT_GRANTED,
                cachedFallback = fallbackLocation
            )
        }

        val lm = locationManager
        if (lm == null || !isLocationEnabled()) {
            val lastKnown = getBestLastKnownLocation()
            if (lastKnown != null) {
                val resolved = buildUserLocationFromAndroidLocation(lastKnown)
                return@withContext LocationFetchResult.Success(resolved, isFresh = false)
            }
            return@withContext LocationFetchResult.Failure(
                reason = LocationFailure.SERVICES_DISABLED,
                cachedFallback = fallbackLocation
            )
        }

        // Try getting fresh location fix with timeout
        val freshLocation: Location? = withTimeoutOrNull(6500L) {
            requestSingleUpdate()
        }

        if (freshLocation != null) {
            val resolved = buildUserLocationFromAndroidLocation(freshLocation)
            LocationFetchResult.Success(resolved, isFresh = true)
        } else {
            // Timeout -> Fallback to best last known location cached by the OS
            val lastKnown = getBestLastKnownLocation()
            if (lastKnown != null) {
                val resolved = buildUserLocationFromAndroidLocation(lastKnown)
                LocationFetchResult.Success(resolved, isFresh = false)
            } else if (fallbackLocation != null) {
                LocationFetchResult.Success(fallbackLocation, isFresh = false)
            } else {
                LocationFetchResult.Failure(
                    reason = LocationFailure.NO_SIGNAL,
                    cachedFallback = fallbackLocation
                )
            }
        }
    }

    /**
     * Retrieves the most accurate and recent last-known location across providers.
     */
    fun getBestLastKnownLocation(): Location? {
        if (!hasLocationPermission()) return null
        val lm = locationManager ?: return null

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        var bestLocation: Location? = null
        for (provider in providers) {
            try {
                if (lm.isProviderEnabled(provider)) {
                    val loc = lm.getLastKnownLocation(provider) ?: continue
                    if (bestLocation == null || loc.accuracy < bestLocation.accuracy || loc.time > bestLocation.time) {
                        bestLocation = loc
                    }
                }
            } catch (e: SecurityException) {
                // Ignore security exception if permission changed
            } catch (e: Exception) {
                // Ignore provider error
            }
        }
        return bestLocation
    }

    private suspend fun requestSingleUpdate(): Location? = suspendCancellableCoroutine { cont ->
        val lm = locationManager
        if (lm == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                try {
                    lm.removeUpdates(this)
                } catch (e: Exception) {
                    // Ignore
                }
                if (cont.isActive) {
                    cont.resume(location)
                }
            }

            override fun onProviderDisabled(provider: String) {}
            override fun onProviderEnabled(provider: String) {}
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        var registered = false
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

        for (provider in providers) {
            try {
                if (lm.isProviderEnabled(provider)) {
                    lm.requestLocationUpdates(
                        provider,
                        0L,
                        0f,
                        listener,
                        Looper.getMainLooper()
                    )
                    registered = true
                }
            } catch (e: SecurityException) {
                // Permission not granted
            } catch (e: Exception) {
                // Ignore provider failures
            }
        }

        if (!registered) {
            if (cont.isActive) {
                cont.resume(null)
            }
        }

        cont.invokeOnCancellation {
            try {
                lm.removeUpdates(listener)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Converts Android Location to UserLocation with safe offline Geocoder reverse-lookup.
     */
    suspend fun buildUserLocationFromAndroidLocation(location: Location): UserLocation =
        withContext(Dispatchers.IO) {
            val lat = location.latitude
            val lng = location.longitude

            var cityName: String? = null
            var countryName: String? = null

            // Safe geocoding with offline fallback
            try {
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val addresses = suspendCancellableCoroutine<List<Address>> { cont ->
                            geocoder.getFromLocation(lat, lng, 1) { addrs ->
                                cont.resume(addrs ?: emptyList())
                            }
                        }
                        if (addresses.isNotEmpty()) {
                            val addr = addresses[0]
                            cityName = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                            countryName = addr.countryName
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(lat, lng, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            cityName = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                            countryName = addr.countryName
                        }
                    }
                }
            } catch (e: Exception) {
                // Geocoder fails gracefully when offline
            }

            val finalCity = cityName ?: String.format(Locale.US, "GPS (%.2f°, %.2f°)", lat, lng)
            val finalCountry = countryName ?: "Current Coordinates"

            UserLocation(
                name = finalCity,
                country = finalCountry,
                latitude = lat,
                longitude = lng,
                isGps = true
            )
        }
}

/**
 * Why a location could not be determined.
 *
 * ### An enum, not a sentence
 *
 * This was `reason: String` on [LocationFetchResult.Failure], and there were exactly
 * three possible values, each a fixed English sentence written in the service layer:
 * "Location permission not granted", "Location services disabled on device", "Unable
 * to acquire GPS signal. Using cached location." They were handed to the UI as a
 * `StatusBanner(message = ...)`, so a reader in any of the ten shipped languages saw
 * English, and there was nowhere in the UI that could have changed it.
 *
 * An enum says *which* of three things happened. The sentence is a presentation
 * concern and now lives in the strings, which is also the only place it can be
 * translated - see `UiStringsMore.locationError*`.
 *
 * The names are about the *cause*, not the remedy, because the remedy differs by
 * device and is the reader's to choose: `PERMISSION_NOT_GRANTED` may be answered by
 * granting the permission or by picking a city, and the app does not presume which.
 */
enum class LocationFailure {
    /** The location permission was not granted, so nothing was asked for. */
    PERMISSION_NOT_GRANTED,

    /** Location services are switched off, and there was no last known fix either. */
    SERVICES_DISABLED,

    /**
     * Services are on and a fix was asked for, and none arrived before the timeout,
     * and there was nothing cached to fall back to.
     */
    NO_SIGNAL
}
