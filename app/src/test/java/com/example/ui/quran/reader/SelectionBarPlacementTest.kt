package com.example.ui.quran.reader

import com.example.data.quran.QuranBrowse
import com.example.data.quran.QuranCorpus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A selected verse's actions are drawn under the block that holds the verse.
 *
 * ### The defect this file exists for
 *
 * The bar was the **last item in the flowing list**, so it was a bar under whatever the
 * reader had scrolled to and not under the verse they tapped. In Al-Baqarah a tap on
 * 2:1 put it after 2:286 - off screen, hundreds of verses away. The highlight appeared
 * and nothing else did: no bookmark, copy, share or play from the verse in front of
 * them, and the only way to find the bar was to scroll to the end of the surah.
 *
 * The fix draws it as an item directly under the block containing the verse. The
 * horizontal axis had no bar at all and draws one at the foot of the surface, because
 * that axis turns viewport-sized blocks like pages and there is no list to hang it under.
 *
 * ### Why this is a test and not a screenshot
 *
 * Where an item sits in a lazy list is not in the picture of one screenful, and the
 * screenshot suite is seeded at the top of the surah on purpose. The decision is one
 * number - the block the bar goes under - and it is a function of the verse and the
 * list, so the sweep below is 6,236 verses' worth of it.
 *
 * The lookup this replaced has a shape worth pinning while it is written down: a block
 * searched with `verseIndex in block.indices` answers 0 for *every* verse, because the
 * first block's indices contain 0. A bar "under the verse" would then have been under
 * the first block of the surah for all of them - the same failure, with a different seat.
 */
class SelectionBarPlacementTest {

    @Test
    fun `every verse is located in the block its own list was built from`() {
        var checked = 0
        for (surah in 1..QuranCorpus.SURA_COUNT) {
            val ayahs = QuranBrowse.ayahsInSurah(surah)
            val blocks = ayahs.chunked(FLOW_BLOCK_VERSES)

            ayahs.forEachIndexed { index, ayah ->
                val found = selectionBlockIndex(blocks, ayah.ref)

                assertEquals(
                    "verse ${ayah.surahNumber}:${ayah.ayahNumber} was located in block " +
                        "$found, but the list built it into block " +
                        "${index / FLOW_BLOCK_VERSES}",
                    index / FLOW_BLOCK_VERSES,
                    found
                )
                assertTrue(
                    "block $found, chosen for ${ayah.surahNumber}:${ayah.ayahNumber}, " +
                        "does not hold that verse",
                    blocks[found].any { it.ref == ayah.ref }
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
    fun `most of the book sits in a block that is not the first, so the seat matters`() {
        // The guard against this file passing vacuously. If every verse were in its
        // surah's first block, "under the containing block" and "under the first block"
        // would draw the same picture and the sweep above would be checking nothing.
        var outsideTheFirstBlock = 0
        for (surah in 1..QuranCorpus.SURA_COUNT) {
            val ayahs = QuranBrowse.ayahsInSurah(surah)
            outsideTheFirstBlock += (ayahs.size - FLOW_BLOCK_VERSES).coerceAtLeast(0)
        }

        assertTrue(
            "only $outsideTheFirstBlock of the book's verses have a bar seat other " +
                "than the surah's first block, so a lookup that answered 0 could pass " +
                "for most of the book",
            outsideTheFirstBlock * 4 > QuranCorpus.VERSE_COUNT * 3
        )
    }

    @Test
    fun `a late verse is located in a late block, not at the top of the surah`() {
        // The reported case. 2:255 is in Al-Baqarah's twenty-second block; a lookup
        // answering 0 would put the bar under 2:1 while the reader is a screenful of
        // text further down.
        val ayahs = QuranBrowse.ayahsInSurah(2)
        val blocks = ayahs.chunked(FLOW_BLOCK_VERSES)
        val ref = requireNotNull(QuranBrowse.ref(2, 255)) { "2:255 is not in the corpus" }

        val found = selectionBlockIndex(blocks, ref)

        assertTrue(
            "the block located for 2:255 was $found; the bar would be drawn at the top " +
                "of the surah and not under the verse",
            found > 0
        )
        assertTrue("block $found does not hold 2:255", blocks[found].any { it.ayahNumber == 255 })
    }

    @Test
    fun `no selection is no block, so no bar is drawn anywhere`() {
        // -1, not 0: 0 is a real block, and the list draws the bar wherever this number
        // points. A selection cleared by a page turn must take the bar with it.
        assertEquals(
            "an empty selection resolved to a block",
            -1,
            selectionBlockIndex(QuranBrowse.ayahsInSurah(1).chunked(FLOW_BLOCK_VERSES), null)
        )
    }

    @Test
    fun `a selection from another surah is no block`() {
        // A selection survives a position that is still on its page, so turning into
        // another surah can leave one behind - and a bar under a block of *this* surah
        // would be acting on a verse that is not on screen.
        val blocks = QuranBrowse.ayahsInSurah(1).chunked(FLOW_BLOCK_VERSES)
        val elsewhere = requireNotNull(QuranBrowse.ref(2, 1)) { "2:1 is not in the corpus" }

        assertEquals(
            "a verse of Al-Baqarah was located in Al-Fatiha's list",
            -1,
            selectionBlockIndex(blocks, elsewhere)
        )
    }

    @Test
    fun `an ayah number is not an identity across surahs`() {
        // Ayah numbers repeat, and the list is one surah long by design. Matching on the
        // number alone would put Al-Fatiha's verse 1 in Al-Baqarah's first block, whose
        // verse 1 is a different verse with a different page.
        val blocks = QuranBrowse.ayahsInSurah(2).chunked(FLOW_BLOCK_VERSES)
        val oneOne = requireNotNull(QuranBrowse.ref(1, 1)) { "1:1 is not in the corpus" }

        assertEquals(
            "1:1 was located in Al-Baqarah's blocks, where an ayah 1 also exists",
            -1,
            selectionBlockIndex(blocks, oneOne)
        )
    }
}
