package com.example.ui.quran.reader

import com.example.data.model.QuranReadingLayout
import com.example.data.model.QuranRef
import com.example.data.quran.MushafLayout
import com.example.data.quran.QuranBrowse

/**
 * The reference [this] names, page-attributed the way [layout] shows the
 * Quran.
 *
 * The Quran carries two paginations on purpose. The corpus's page table
 * serves the flowing layouts - all of them, whatever their scroll or span -
 * and the Madinah print's own line breaks serve the Madinah mushaf, which
 * opens 25 pages at a different verse for it. Where a request for a verse is
 * honoured, this is the point the attribution is re-answered, because the
 * page a surface shows a verse on is that surface's fact to state; every
 * consumer below builds on it:
 *
 * - `ReaderPosition.openRef` / `alignTo` - a request or a layout switch
 *   re-attributes an existing position without moving the verse,
 * - `ReaderPosition.turnToPage` - the page a turn lands on is the
 *   attribution's opener for that page.
 *
 * Keeping the conversion at this one boundary is what lets the reader carry
 * a single ref around: the position's page is always the page the surface on
 * screen actually shows, and no filter downstream has to know which table
 * produced it.
 */
internal fun QuranRef.onSurfacePage(layout: QuranReadingLayout): QuranRef = when (layout) {
    QuranReadingLayout.MADINAH -> MushafLayout.ref(surah, ayah)
    else -> QuranRef(surah = surah, ayah = ayah, page = QuranBrowse.pageOf(surah, ayah))
}
