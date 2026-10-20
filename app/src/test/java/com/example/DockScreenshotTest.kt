package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.components.DockItem
import com.example.ui.components.GlassChromeScaffold
import com.example.ui.components.SalahDock
import com.example.ui.theme.LocalGlassEnabled
import com.example.ui.theme.SalahTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the floating dock over a deliberately busy backdrop.
 *
 * The bars are there so the two paths are visually distinguishable: glass
 * smears them through the blur and tint, while the opaque path covers them. The
 * large-text case proves the icon-only fallback does not clip.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class DockScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val items = listOf(
        DockItem("today", "Today", Icons.Filled.WbSunny, Icons.Outlined.WbSunny),
        DockItem("qibla", "Qibla", Icons.Filled.Explore, Icons.Outlined.Explore),
        DockItem("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    private fun render(
        dark: Boolean,
        glass: Boolean,
        largeText: Boolean = false,
        rtl: Boolean = false,
        name: String
    ) {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalGlassEnabled provides glass,
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalDensity provides Density(
                    density = density.density,
                    fontScale = if (largeText) 1.6f else 1f
                )
            ) {
                SalahTheme(darkTheme = dark) {
                    GlassChromeScaffold(
                        dock = { modifier ->
                            SalahDock(
                                items = items,
                                selectedRoute = "today",
                                onSelect = {},
                                modifier = modifier
                            )
                        }
                    ) {
                        DockBackdrop()
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun dock_glass_light() = render(dark = false, glass = true, name = "dock_glass_light")

    @Test
    fun dock_glass_dark() = render(dark = true, glass = true, name = "dock_glass_dark")

    @Test
    fun dock_opaque_light() = render(dark = false, glass = false, name = "dock_opaque_light")

    @Test
    fun dock_opaque_dark() = render(dark = true, glass = false, name = "dock_opaque_dark")

    @Test
    fun dock_glass_large_text() =
        render(dark = false, glass = true, largeText = true, name = "dock_glass_large_text")

    @Test
    fun dock_glass_rtl() =
        render(dark = false, glass = true, rtl = true, name = "dock_glass_rtl")
}

@Composable
private fun DockBackdrop() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFBFD8EA),
                        Color(0xFF53748A),
                        Color(0xFF0B1220)
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(220.dp)
        ) {
            repeat(12) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (index % 2 == 0) Color(0xFF0B1220) else Color(0xFFF8FAFC)
                        )
                )
            }
        }
    }
}
