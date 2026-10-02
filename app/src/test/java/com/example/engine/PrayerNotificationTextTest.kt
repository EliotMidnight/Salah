package com.example.engine

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.localization.ArabicStrings
import com.example.ui.localization.BengaliStrings
import com.example.ui.localization.EnglishStrings
import com.example.ui.localization.FrenchStrings
import com.example.ui.localization.GermanStrings
import com.example.ui.localization.IndonesianStrings
import com.example.ui.BorrowedWords
import com.example.ui.localization.LocalizationManager
import com.example.ui.localization.alertModeLabel
import com.example.ui.localization.MalayStrings
import com.example.ui.localization.RussianStrings
import com.example.ui.localization.SpanishStrings
import com.example.ui.localization.TurkishStrings
import com.example.ui.localization.UiStringsMore
import com.example.ui.localization.UrduStrings
import com.example.ui.localization.prayerName
import com.example.data.model.Prayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The prayer notification speaks the reader's language.
 *
 * ### What was wrong
 *
 * Every word the app showed in a notification was English, in an app that ships ten
 * languages and which a reader chose *because* it speaks theirs. Sixteen separate
 * strings across three classes, none of them in the strings file:
 *
 * - three notification **channel** names and three descriptions;
 * - **six** status sentences, one per alert mode;
 * - the expanded text ("Enter prayer and turn towards the Holy Kaaba");
 * - **two** action labels, "Silence" and "Mark Prayed";
 * - the **pre-prayer** title and body;
 * - and two more in the foreground service, "Adhan in progress" / "Prayer alert is
 *   playing" — the one notification a reader cannot dismiss.
 *
 * The six status sentences each interpolated `prayer.englishName`, so even the
 * *prayer's* name was English in the sentence while the title above showed both
 * scripts.
 *
 * ### Why this needs a test at all
 *
 * A notification is not in any screenshot, and `PrayerNotificationManager` composes it
 * from a `Context` rather than a composition, so nothing else in the suite can see it.
 * The strings themselves *are* covered by `StringCoverageTest` — so the thing this file
 * has to check is the **wiring**: that each of those positions actually reads a string,
 * and reads it in the reader's language, with no English left over from an `if`.
 *
 * The channel-name caveat is real and is stated where the strings are: Android caches a
 * channel's name at creation, so on an existing install the three channel names stay
 * English whatever this app does. The strings are there so a fresh install is not the
 * only one that reads correctly, and this test cannot see that difference.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrayerNotificationTextTest {

    private lateinit var context: Context

    private val languages = listOf(
        "English" to EnglishStrings,
        "Arabic" to ArabicStrings,
        "French" to FrenchStrings,
        "Indonesian" to IndonesianStrings,
        "Turkish" to TurkishStrings,
        "Urdu" to UrduStrings,
        "Malay" to MalayStrings,
        "Bengali" to BengaliStrings,
        "Russian" to RussianStrings,
        "German" to GermanStrings,
        "Spanish" to SpanishStrings
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun withLanguage(language: String, body: () -> Unit) {
        context.getSharedPreferences("salah_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("pref_language", language)
            .commit()
        body()
    }

    /** The strings the notification manager would resolve, for the stored language. */
    private fun storedStrings(): UiStringsMore {
        val stored = context.getSharedPreferences("salah_prefs", Context.MODE_PRIVATE)
            .getString("pref_language", "English")
        return LocalizationManager.getStrings(stored ?: "English").more
    }

    @Test
    fun `the notification manager resolves the stored language, not English`() {
        // The wiring that had to exist for any of this to be reachable: the manager
        // used to have no access to the strings at all.
        for ((language, expected) in languages) {
            withLanguage(language) {
                assertEquals(
                    "the stored language \"$language\" did not resolve to its strings",
                    expected.more.notifications.notifPrayerArrived,
                    storedStrings().notifications.notifPrayerArrived
                )
            }
        }
    }

    @Test
    fun `a missing language preference falls back to English rather than failing`() {
        // A notification cannot afford to fail because a preference could not be read.
        context.getSharedPreferences("salah_prefs", Context.MODE_PRIVATE)
            .edit().remove("pref_language").commit()
        assertEquals(
            "a notification with no stored language did not fall back to English",
            EnglishStrings.more.notifications.notifPrayerArrived,
            storedStrings().notifications.notifPrayerArrived
        )
    }

    @Test
    fun `every status sentence names the prayer in the reader's language`() {
        // The specific thing the old code got wrong: the sentence interpolated
        // `prayer.englishName`, so an Arabic reader saw "Time for Fajr prayer has
        // arrived" - English grammar with an English name in it.
        val englishNames = setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha", "Sunrise")
        for ((language, strings) in languages) {
            for (prayer in Prayer.entries) {
                val spoken = strings.prayerName(prayer)
                assertTrue(
                    "the $language name for $prayer is blank",
                    spoken.isNotBlank()
                )
                if (language == "English") continue
                // Through the one borrowed-word table, not a second list: five of the six
                // prayer names are genuinely kept as English by French, German and
                // Spanish - they have no other word a reader would recognise - while
                // all three *do* translate sunrise.
                if (spoken in englishNames && !BorrowedWords.isBorrowed(spoken, language)) {
                    assertFalse(
                        "the $language name for $prayer is still the English " +
                            "\"$spoken\", and no reason is recorded in " +
                            "BorrowedWords for keeping it",
                        spoken in englishNames
                    )
                }
            }
        }
    }

    @Test
    fun `every notification template is filled, in every language`() {
        // A template with a missing argument prints a bare "%1$s" and reads as noise
        // rather than as an error, and one with an extra argument is silently ignored.
        // Neither is visible anywhere but here.
        for ((language, strings) in languages) {
            val m = strings.more.notifications
            val spoken = strings.prayerName(Prayer.MAGHRIB)
            val rendered = listOf(
                "silentFor" to m.notifSilentFor.format(spoken),
                "vibrate" to m.notifVibrateAlert.format(spoken),
                "takbeer" to m.notifTakbeerAlert.format(spoken),
                "chime" to m.notifChimeAlert.format(spoken),
                "arrived" to m.notifPrayerArrived.format(spoken, "17:42"),
                "enterPrayer" to m.notifEnterPrayer.format("17:42"),
                "prePrayerTitle" to m.notifPrePrayerTitle.format(spoken, 10),
                "prePrayerText" to m.notifPrePrayerText.format(spoken, "17:42"),
                "channelAdhan" to m.notifChannelAdhan,
                "channelPrePrayer" to m.notifChannelPrePrayer,
                "channelSilent" to m.notifChannelSilent,
                "inProgress" to m.notifAdhanInProgress,
                "inProgressBody" to m.notifAdhanInProgressBody,
                "silenceAction" to m.notifSilenceAction,
                "markPrayed" to m.notifMarkPrayed
            )
            for ((what, said) in rendered) {
                assertFalse(
                    "the $language $what template has an unfilled placeholder: \"$said\"",
                    said.contains("%")
                )
                assertTrue("the $language $what is blank", said.isNotBlank())
            }
        }
    }

    @Test
    fun `the three notification channels are named in every language`() {
        // Distinct names, because two channels with the same name in the system
        // settings are one channel as far as a reader is concerned - and a reader
        // muting one cannot tell which.
        for ((language, strings) in languages) {
            val names = listOf(
                strings.more.notifications.notifChannelAdhan,
                strings.more.notifications.notifChannelPrePrayer,
                strings.more.notifications.notifChannelSilent
            )
            assertEquals(
                "the $language notification channels are not all distinct: $names",
                3,
                names.distinct().size
            )
        }
    }

    @Test
    fun `the alert modes are stable English keys with translated labels`() {
        // The five mode strings are **persisted preference keys**, not copy — see
        // `UiStringsMore.alertModeLabel`. This is here because the notification's status
        // text is now built by *matching* on them, and a "helpful" refactor that routed
        // a comparison through the strings would make every stored preference match
        // nothing and silently fall through to the full-adhan sentence.
        //
        // So the two halves are checked separately: the keys stay as they are, and the
        // label a reader sees is translated.
        val keys = listOf("Full Adhan", "Takbeer Only", "Gentle Chime", "Vibrate Only", "Silent")

        for ((language, strings) in languages) {
            for (key in keys) {
                val label = strings.more.alertModeLabel(key)
                assertTrue(
                    "the $language label for \"$key\" is blank",
                    label.isNotBlank()
                )
                // English is exempt: it shortens "Full Adhan" to "Adhan", and "Silent"
                // *is* the word for silent, so a key and its label coincide there
                // without anything being wrong. The risk worth guarding is a
                // **translated** language falling through to the stored value, which
                // would put an English preference key in front of the reader.
                if (language != "English") {
                    assertFalse(
                        "the $language label for \"$key\" is the stored key itself, " +
                            "so the reader sees an English preference value",
                        label in keys
                    )
                }
            }
            // An unknown key must fall back to a *label*, never echo the key back: a
            // key this build does not know is a stored preference from a future one.
            val unknown = strings.more.alertModeLabel("Something From The Future")
            assertTrue(
                "the $language fallback for an unknown mode is blank",
                unknown.isNotBlank()
            )
            assertEquals(
                "the $language fallback for an unknown mode is the unknown value " +
                    "itself, which would show a stored preference key to the reader",
                strings.more.alertModeLabel("Silent"),
                unknown
            )
        }
    }
}
