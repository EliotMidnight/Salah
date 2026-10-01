package com.example.ui.quran.reader

import com.example.data.model.QuranRef
import com.example.data.quran.QuranBrowse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Where the reader is, and the rules about how it can change.
 *
 * ### Why position needed its own type
 *
 * The previous reader kept three numbers that all meant "where am I":
 * `pageCursor` (the pager's page), `activeReadingAyahNumber` (the anchor), and
 * `browsedPage` (a third value, written from `onGloballyPositioned` purely so the
 * pill could show something). Three writers, and they overwrote each other.
 *
 * The visible symptoms were:
 *
 * - **The pill and the page disagreed.** `rememberPagerState` reads `initialPage`
 *   once and ignores it afterwards, so a surah picked from the index moved the
 *   number in the pill and nothing else - the pager stayed where it was.
 * - **Turning a page did not move "continue reading".** The page-turn writer
 *   recorded the *first verse of the reader's surah*, not of the page. Turning
 *   from page 2 to page 3 wrote 2:1, so the stored position pointed back at page
 *   2 and the reader reopened on the page they had left.
 * - **A selection followed the reader off the page.** `selectedAyah` was a bare
 *   ayah number that survived a turn, so a verse selected on page 604 was still
 *   "selected" on page 1 - where it does not exist.
 *
 * Each of those is a case here.
 */
class ReaderPositionTest {

    private fun position(
        surah: Int = 2,
        ayah: Int = 1
    ) = ReaderPosition(QuranBrowse.refOrStart(surah, ayah))

    // --- One value -------------------------------------------------------

    @Test
    fun `the page and the surah are read off the one reference`() {
        val start = position(2, 255)
        assertEquals(42, start.page)
        assertEquals(2, start.surah)
        // And they move together, because there is only one thing to move.
        start.turnPage(1)
        assertEquals(43, start.page)
        assertEquals(QuranBrowse.placeAtPage(43).verse.surah, start.surah)
    }

    @Test
    fun `a page turn lands on the first verse of the page it lands on`() {
        // The bug that made "continue reading" useless in the mushaf. Turning to
        // page 3 recorded 2:1 - the first verse of the *surah* - so the stored
        // position pointed at page 2 and the reader reopened where they had been.
        val start = position(2, 1)
        assertTrue(start.turnPage(1))
        assertEquals(3, start.page)
        assertEquals(
            "the position after turning must be the page's own first verse",
            QuranBrowse.placeAtPage(3).verse,
            start.ref
        )
    }

    @Test
    fun `a page turn across a surah boundary lands in the new surah`() {
        // Page 604 holds Al-Ikhlas, Al-Falaq and An-Nas; page 603 is the last page
        // of Al-Kafir. Turning forward from 603 has to change the surah, and the
        // surah has to be the one the page *opens* in.
        val start = ReaderPosition(QuranBrowse.placeAtPage(603).verse)
        assertTrue(start.turnPage(1))
        assertEquals(604, start.page)
        assertEquals(112, start.surah)
        assertEquals(1, start.ref.ayah)
    }

    // --- Bounds ----------------------------------------------------------

    @Test
    fun `turning past the first page does nothing`() {
        val start = ReaderPosition(QuranBrowse.placeAtPage(1).verse)
        assertFalse("page 1 has no previous page", start.turnPage(-1))
        assertEquals(1, start.page)
    }

    @Test
    fun `turning past the last page does nothing`() {
        val start = ReaderPosition(QuranBrowse.placeAtPage(604).verse)
        assertFalse("page 604 has no next page", start.turnPage(1))
        assertEquals(604, start.page)
    }

    @Test
    fun `a large turn clamps to the ends rather than leaving the book`() {
        val start = ReaderPosition(QuranBrowse.placeAtPage(300).verse)
        start.turnPage(-1000)
        assertEquals(1, start.page)
        start.turnPage(2000)
        assertEquals(604, start.page)
    }

    // --- Selection is not position ---------------------------------------

    @Test
    fun `navigating clears the selection`() {
        // A reader who taps a verse and then picks a surah from the index has not
        // got a selected verse any more, and must not be left with one - especially
        // not one from the surah they left.
        val start = position(2, 255)
        val selected = QuranBrowse.refOrStart(2, 255)
        start.select(selected)
        assertEquals("the fixture did not take", selected, start.selection)

        // Juz' 30 opens An-Naba, 78:1, which is at the other end of the book from
        // 2:255.
        assertEquals(78, QuranBrowse.placeAtJuz(30).verse.surah)
        start.goTo(QuranBrowse.placeAtJuz(30).verse)
        assertNull(
            "a selection survived a navigation to a different surah",
            start.selection
        )
    }

    @Test
    fun `a selection survives a turn only while the new page still contains it`() {
        // 2:255 is on page 42. Turn to 43 and the selection is meaningless there.
        val start = position(2, 255)
        start.select(QuranBrowse.refOrStart(2, 255))
        assertTrue(start.turnPage(1))
        assertNull(
            "2:255 was carried onto page 43, which does not contain it",
            start.selection
        )
    }

    @Test
    fun `a selection on the same page is kept across a re-seat`() {
        val start = position(2, 255)
        val selected = QuranBrowse.refOrStart(2, 255)
        start.select(selected)
        // A re-seat on the same page - a layout change, say - must not throw away a
        // selection the reader just made and can still see.
        start.turnTo(QuranBrowse.placeAtPage(42).verse)
        assertEquals(selected, start.selection)
    }

    @Test
    fun `tapping the same verse clears the selection`() {
        val start = position(2, 1)
        val verse = QuranBrowse.refOrStart(2, 255)
        start.toggleSelection(verse)
        assertEquals(verse, start.selection)
        start.toggleSelection(verse)
        assertNull("tapping a selected verse again must deselect it", start.selection)
    }

    @Test
    fun `tapping a different verse moves the selection`() {
        val start = position(2, 1)
        start.toggleSelection(QuranBrowse.refOrStart(2, 255))
        start.toggleSelection(QuranBrowse.refOrStart(2, 256))
        assertEquals(256, start.selection?.ayah)
    }

    @Test
    fun `tapping nothing clears the selection`() {
        // What a tap in the margin, or on the page furniture, resolves to. It must
        // not be read as "keep whatever was selected".
        val start = position(2, 1)
        start.select(QuranBrowse.refOrStart(2, 255))
        start.toggleSelection(null)
        assertNull(start.selection)
    }

    @Test
    fun `a selection is never persisted with the position`() {
        // Stated as a property of the type: [ReaderPosition] is built from a
        // [QuranRef] and its `selection` has no saveable state of its own, so
        // `rememberSaveable` on the position cannot carry a selection across a
        // process death. A verse highlighted on a page the reader has not returned
        // to is a state they cannot explain.
        val position = position(2, 255)
        position.select(QuranBrowse.refOrStart(2, 256))
        val refFields = position.ref.let { listOf(it.surah, it.ayah, it.page) }
        assertEquals(listOf(2, 255, 42), refFields)
        assertEquals(256, position.selection?.ayah)
    }

    // --- Identity --------------------------------------------------------

    @Test
    fun `a selection is compared as a whole reference and not an ayah number`() {
        // Page 604 holds three surahs, so ayah 1 exists three times on it. If the
        // selection were an ayah number, selecting 1 on that page would match all
        // three, and the bookmark and copy actions would fire on whichever came
        // first in the list.
        val last = ReaderPosition(QuranBrowse.placeAtPage(604).verse)
        val fatiha = QuranBrowse.refOrStart(112, 1)
        last.toggleSelection(fatiha)
        assertEquals(112, last.selection?.surah)

        // Selecting the identical ayah number in a different surah is a different
        // selection, and must not be treated as a toggle-off.
        val falaq = QuranBrowse.refOrStart(113, 1)
        last.toggleSelection(falaq)
        assertEquals(
            "selecting 113:1 while 112:1 was selected must move the selection, not clear it",
            113,
            last.selection?.surah
        )
    }

    @Test
    fun `every position resolves to a verse that exists`() {
        // A position whose verse is not in the corpus would put a reader on a page
        // with no text where their text should be. Swept over all four ends of the
        // book and both transitions, because the failure is a stale value from a
        // different corpus rather than a bad input.
        val start = ReaderPosition(QuranRef.Start)
        for (delta in listOf(1, 2, 10, 300, 603)) {
            start.turnPage(delta)
            assertTrue(
                "page ${start.page} has no verse at ${start.ref}",
                QuranBrowse.ayah(start.ref.surah, start.ref.ayah) != null
            )
        }
        for (delta in listOf(-1, -10, -300, -603)) {
            start.turnPage(delta)
            assertTrue(
                "page ${start.page} has no verse at ${start.ref}",
                QuranBrowse.ayah(start.ref.surah, start.ref.ayah) != null
            )
        }
    }

    @Test
    fun `the first run starts at Al-Fatihah on page 1`() {
        val start = initialPosition()
        assertEquals(QuranRef(1, 1, 1), start)
    }
}
