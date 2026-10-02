package com.example.ui.quran

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.example.data.quran.QuranDataSource
import com.example.ui.localization.ProvideAppLanguage
import com.example.ui.localization.isArabicInterface
import com.example.ui.quran.QuranIndexSheet
import com.example.ui.theme.SalahTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The surah index leads with the name in the reader's language.
 *
 * ### The defect
 *
 * Every row showed the **English** name as its prominent line, and the surah's actual
 * Arabic name on the far side, small and quiet — in every language. In an Arabic
 * interface that made "Al-Fatihah" the headline of الفاتحة.
 *
 * It was not only a matter of which line was loudest. **`QuranSearch.searchSurahs` matches
 * the Arabic name**, so a reader who searched الفاتحة was handed a row whose largest text
 * was not the thing they had typed. Search and result disagreed about which name a surah
 * has.
 *
 * The row shows both names whichever way round they are, so nothing is hidden; only the
 * emphasis moves. And the English *meaning* ("The Opener") has no Arabic equivalent in the
 * bundle, so it stays where it is rather than being invented.
 *
 * ### Why this needed a new CompositionLocal
 *
 * `LocalStrings` cannot answer "which language is this interface in?" — by the time a
 * composable reads it, the strings have been produced and the bundle they came from is
 * gone. And the question is real precisely because the interface language is *not* the
 * only language on screen: an Arabic reader is looking at Arabic, at an English
 * translation, at an English surah name and at a romanisation. `LocalLanguage` sits
 * beside `LocalStrings` so the answer is available where it is needed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SurahIndexNameOrderTest {

    @get:Rule
    val compose = createComposeRule()

    private fun firstRowTexts(language: String): List<String> {
        compose.setContent {
            SalahTheme(darkTheme = false) {
                ProvideAppLanguage(language = language) {
                    QuranIndexSheet(
                        currentSurah = 1,
                        bookmarks = emptyList(),
                        onSelectSurah = {},
                        onSelectSurahAyah = { _, _ -> },
                        onDismiss = {}
                    )
                }
            }
        }
        val node = compose.onNodeWithTag("surah_1").fetchSemanticsNode()
        return node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
    }

    @Test
    fun `an Arabic interface leads with the Arabic name`() {
        val fatihah = QuranDataSource.SURAHS.first()
        val texts = firstRowTexts("Arabic")
        assertTrue(
            "the row for Al-Fatihah shows $texts, which does not name the surah in " +
                "Arabic at all",
            texts.any { it.contains(fatihah.arabicName) }
        )
        // The Arabic name is the *prominent* line, so it is the one that reaches
        // semantics - the quiet far-side copy is cleared deliberately, which is what lets
        // this tell the two apart.
        assertTrue(
            "the Arabic name is present but not prominent in $texts, so the row still " +
                "leads with the English name",
            texts.filter { it.contains(fatihah.arabicName) }.isNotEmpty()
        )
    }

    @Test
    fun `a Latin-script interface leads with the English name`() {
        val fatihah = QuranDataSource.SURAHS.first()
        val texts = firstRowTexts("English")

        assertTrue(
            "the row for Al-Fatihah shows $texts, which does not carry the English name",
            texts.any { it == fatihah.englishName }
        )
        assertTrue(
            "the row for Al-Fatihah shows $texts, and the Arabic name is as prominent " +
                "as the English one, so this test cannot tell which leads",
            texts.none { it.contains(fatihah.arabicName) }
        )
    }

    @Test
    fun `the Arabic name leads for every language that reads it`() {
        // Checked on the *decision* rather than by composing eleven sheets, because
        // `setContent` may only be called once per test and eleven renderings of the
        // same row prove nothing the two above do not.
        //
        // The rule is the whole of it: an interface whose script is Arabic leads with
        // the Arabic name. Arabic qualifies. Urdu does **not** — its interface is in
        // Perso-Arabic script, but the surah names this app carries are in Arabic, so
        // promoting the Arabic name over the transliteration for an Urdu reader would
        // be a claim about a language the row cannot render.
        for (language in listOf("Arabic", "ar", "العربية")) {
            assertTrue(
                "\"$language\" is an Arabic interface and should lead with the " +
                    "Arabic name",
                isArabicInterface(language)
            )
        }
        for (language in listOf(
            "English", "French", "Indonesian", "Turkish", "Urdu", "Malay",
            "Bengali", "Russian", "German", "Spanish"
        )) {
            assertTrue(
                "\"$language\" is not an Arabic interface, so its rows must not " +
                    "promote the Arabic name over the transliteration",
                !isArabicInterface(language)
            )
        }
    }
}
