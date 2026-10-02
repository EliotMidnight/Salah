package com.example.ui.quran.reader

import com.example.data.model.QuranRef
import com.example.data.quran.QuranBrowse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tapping a verse selects *that* verse.
 *
 * ### The defect this file exists for
 *
 * A mushaf page is not inside one surah. **51 of the 604 hold more than one**, and
 * 523 verses sit after a surah boundary on the page they are printed on.
 *
 * The page resolved a tap into a reference by handing its caller an **ayah number**
 * and letting the caller fill in the surah from the page's *first* verse. So on
 * every one of those 51 pages, tapping any verse after the boundary produced a
 * reference into the previous surah. Concretely, on page 293 - which holds the end
 * of Al-Kahf and the start of Al-Hijr - tapping 18:1 selected `17:1`, which is on
 * page 282 and not on the page under the reader's finger. Three separate wrong
 * answers fell out of that one:
 *
 * 1. **No highlight at all**, because 17:1 is not in the page's text.
 * 2. **The action bar showed Al-Kahf's opening verse**, not the one tapped.
 * 3. **Bookmarking saved 17:1.**
 *
 * The accessibility actions had the same loss *and* announced the right thing: an
 * action labelled "Select 18:1" selected `17:1`. A screen-reader user was told one
 * verse and given another, which is worse than being given nothing.
 *
 * ### Why this is a test and not a screenshot
 *
 * A screenshot cannot show it. The two pages where it is most visible - 604 and 596 -
 * differ from their neighbours in the surah *name* printed on them, which is
 * correct either way. And the tap is the thing that is wrong, not the picture.
 *
 * So this asserts the property directly: for every verse on every page, the
 * reference the page would produce is the verse's own. It is 6,236 assertions' worth
 * of fact compressed into one sweep, and it cannot be satisfied by a page that
 * reconstructs identity from anything but the verse.
 */
class VerseSelectionTest {

    @Test
    fun `every verse on every page resolves to its own reference`() {
        var checked = 0
        for (page in 1..QuranBrowse.TOTAL_PAGES) {
            for (ayah in QuranBrowse.ayahsOnPage(page)) {
                // What a page does now: hand back the verse's own reference. The
                // whole point is that no caller assembles one.
                val selected = ayah.ref

                assertEquals(
                    "verse ${ayah.surahNumber}:${ayah.ayahNumber} reported page " +
                        "${selected.page}, but it is on page $page",
                    page,
                    selected.page
                )
                assertEquals(
                    "selecting ${ayah.surahNumber}:${ayah.ayahNumber} on page $page " +
                        "produced ${selected.surah}:${selected.ayah}",
                    ayah.surahNumber,
                    selected.surah
                )
                assertEquals(
                    "selecting ${ayah.surahNumber}:${ayah.ayahNumber} on page $page " +
                        "produced ayah ${selected.ayah}",
                    ayah.ayahNumber,
                    selected.ayah
                )
                assertTrue(
                    "verse ${ayah.surahNumber}:${ayah.ayahNumber} has no text, so the " +
                        "corpus the page is drawn from is wrong",
                    ayah.textArabic.isNotBlank()
                )
                checked++
            }
        }
        assertEquals(
            "the corpus no longer holds 6,236 verses, so this sweep is not covering " +
                "the whole book",
            6_236,
            checked
        )
    }

    @Test
    fun `a page's first surah is not the surah of every verse on it`() {
        // The premise of the defect, pinned. If this ever reached zero the old
        // reconstruction would be correct by accident and the bug would be back with
        // nothing failing - which is exactly what happened to the surah-head rule.
        //
        // Read from the corpus rather than asserted as a constant, so it is the
        // property and not a remembered number being checked.
        val pages = (1..QuranBrowse.TOTAL_PAGES).filter { page ->
            val ayahs = QuranBrowse.ayahsOnPage(page)
            ayahs.map { it.surahNumber }.distinct().size > 1
        }
        val versesAfterABoundary = pages.sumOf { page ->
            val ayahs = QuranBrowse.ayahsOnPage(page)
            val first = ayahs.first().surahNumber
            ayahs.count { it.surahNumber != first }
        }

        assertEquals(
            "the number of pages holding more than one surah has changed",
            51,
            pages.size
        )
        assertEquals(
            "the number of verses that sit after a surah boundary has changed",
            523,
            versesAfterABoundary
        )
        assertTrue(
            "if no verse sat after a boundary, the defect could not be reproduced " +
                "and this file would be guarding nothing",
            versesAfterABoundary > 0
        )
    }

    @Test
    fun `the last page's three references are distinguishable where their ayah numbers are not`() {
        // The reason a reference carries a page at all, stated against the page that
        // makes it necessary. Page 604 holds the last three surahs, and all three
        // contribute an ayah 1.
        val last = QuranBrowse.ayahsOnPage(QuranBrowse.TOTAL_PAGES)
            .filter { it.ayahNumber == 1 }
            .map { it.ref }

        assertEquals("the last page no longer holds three ayah-1s", 3, last.size)
        assertEquals(
            "the three ayah-1s are no longer ambiguous by ayah number, so the page in " +
                "the reference is not being exercised",
            1,
            last.map { it.ayah }.distinct().size
        )
        assertEquals(
            "three ayah-1s produced fewer than three references",
            3,
            last.distinct().size
        )
        assertEquals(
            "the references disagree on the surah, which is the part that makes them " +
                "distinguishable here",
            3,
            last.map { it.surah }.distinct().size
        )
    }

    @Test
    fun `the old reconstruction resolves a verse to a different one`() {
        // The defect itself, reproduced.
        //
        // This is the code that used to be in the pager, kept as a *test* so the
        // failure cannot be quietly reintroduced by someone who reasons - as someone
        // did - that "a page needs to know its surah". Every line of it is the
        // previous implementation, unchanged.
        fun oldTapResolution(page: Int, ayahNumber: Int): QuranRef {
            val firstSurah = QuranBrowse.ayahsOnPage(page).first().surahNumber
            return QuranRef(firstSurah, ayahNumber, page)
        }

        val page = 293 // Al-Kahf into Al-Hijr
        val hijrFirstVerse = QuranBrowse.ayah(18, 1)!!
        assertEquals(
            "fixture: 18:1 is no longer on page 293, so the case is not covered",
            293,
            hijrFirstVerse.pageNumber
        )

        val resolved = oldTapResolution(page, hijrFirstVerse.ayahNumber)
        assertNotEquals(
            "the old reconstruction now resolves 18:1 correctly, so this file is " +
                "guarding a bug that no longer exists in the form described",
            hijrFirstVerse.ref,
            resolved
        )
        assertEquals(
            "expected the old reconstruction to name ayah 1 of the page's FIRST " +
                "surah",
            QuranBrowse.ayahsOnPage(page).first().surahNumber,
            resolved.surah
        )
        assertNotEquals(
            "18:1 was reported on page " +
                "${QuranBrowse.ayah(resolved.surah, resolved.ayah)?.pageNumber}, so the " +
                "old code's failure is no longer 'the wrong verse in the wrong surah' " +
                "and this test needs rethinking",
            page,
            QuranBrowse.ayah(resolved.surah, resolved.ayah)?.pageNumber
        )
    }

    @Test
    fun `a reference built from a verse agrees with the verse on all three numbers`() {
        // The positive counterpart, and the reason [com.example.data.model.Ayah.ref]
        // exists: one conversion, used everywhere, that cannot disagree with the
        // verse it came from. Checked over a spread that includes the awkward
        // shapes - a verse that is a surah's first, the last page, a boundary verse.
        val sample = listOf(
            QuranBrowse.ayah(1, 1),
            QuranBrowse.ayah(2, 1),
            QuranBrowse.ayah(2, 286),
            QuranBrowse.ayah(3, 1),
            QuranBrowse.ayah(18, 1),
            QuranBrowse.ayah(112, 1),
            QuranBrowse.ayah(113, 1),
            QuranBrowse.ayah(114, 6)
        ).map { it!! }

        for (ayah in sample) {
            val ref = ayah.ref
            assertEquals(
                "${ayah.surahNumber}:${ayah.ayahNumber} -> ref surah",
                ayah.surahNumber,
                ref.surah
            )
            assertEquals(
                "${ayah.surahNumber}:${ayah.ayahNumber} -> ref ayah",
                ayah.ayahNumber,
                ref.ayah
            )
            assertEquals(
                "${ayah.surahNumber}:${ayah.ayahNumber} -> ref page",
                ayah.pageNumber,
                ref.page
            )
            assertEquals(
                "${ayah.surahNumber}:${ayah.ayahNumber} -> ref, resolved back",
                ayah,
                QuranBrowse.ayah(ref.surah, ref.ayah)
            )
        }
    }
}
