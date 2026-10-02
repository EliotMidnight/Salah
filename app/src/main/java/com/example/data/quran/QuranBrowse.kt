package com.example.data.quran

import com.example.data.model.Ayah
import com.example.data.model.QuranPlace
import com.example.data.model.QuranRef
import com.example.data.model.Surah

/**
 * Everything the reader and the browse sheet ask the corpus for, in one place.
 *
 * ### Why this is a facade and not just a bigger [QuranDataSource]
 *
 * 1. The corpus now holds its partitions as **ranges of verse indices**, so
 *    "which verses are on page 285" is two array reads and a `subList`.
 * 2. Everything that is asked *per verse* inside a loop - the surah lookup, the
 *    partition, the normalisation - is hoisted into a precomputed table or a
 *    lazily built index, so the loop body is arithmetic.
 */
object QuranBrowse {

    /**
     * Whether a prostration after a verse is obligatory or recommended.
     */
    enum class SajdaKind { OBLIGATORY, RECOMMENDED }

    const val TOTAL_PAGES = QuranCorpus.PAGE_COUNT
    const val TOTAL_JUZ = QuranCorpus.JUZ_COUNT
    const val TOTAL_HIZB = QuranCorpus.HIZB_COUNT
    const val TOTAL_SURAHS = QuranCorpus.SURA_COUNT
    const val TOTAL_VERSES = QuranCorpus.VERSE_COUNT

    /**
     * The translation every verse's English text comes from.
     */
    const val TRANSLATION_EDITION = QuranCorpus.TRANSLATION_EDITION

    /** Every verse, in canonical order. */
    val ayahs: List<Ayah> get() = QuranCorpus.ayahs

    val surahs: List<Surah> get() = QuranDataSource.SURAHS

    // --- Verses ------------------------------------------------------------

    fun ayahsOnPage(page: Int): List<Ayah> = QuranCorpus.ayahsOnPage(page)

    fun ayahsInSurah(surah: Int): List<Ayah> = QuranCorpus.ayahsInSurah(surah)

    fun ayahsInJuz(juz: Int): List<Ayah> = QuranCorpus.ayahsInJuz(juz)

    fun ayahsInHizb(hizb: Int): List<Ayah> = QuranCorpus.ayahsInHizb(hizb)

    /** The corpus position of a verse, or -1 when the reference does not exist. */
    fun indexOf(surah: Int, ayah: Int): Int = QuranCorpus.indexOf(surah, ayah)

    /** A verse, or null when the reference does not exist. */
    fun ayah(surah: Int, ayah: Int): Ayah? =
        QuranCorpus.ayahAt(QuranCorpus.indexOf(surah, ayah))

    fun surah(number: Int): Surah? = QuranDataSource.getSurahByNumber(number)

    // --- One canonical reference type -------------------------------------

    /**
     * A verse as a [QuranRef], which is what the reader, the chrome and the
     * persistence all speak.
     */
    fun ref(surah: Int, ayah: Int): QuranRef? {
        val verse = ayah(surah, ayah) ?: return null
        return QuranRef(surah = surah, ayah = ayah, page = verse.pageNumber)
    }

    /**
     * A verse as a reference, never null.
     */
    fun refOrStart(surah: Int, ayah: Int): QuranRef =
        ref(surah, ayah) ?: QuranRef.Start

    // --- Places -----------------------------------------------------------

    /** The first verse of a page - the verse a reader arrives on. */
    fun placeAtPage(page: Int): QuranPlace {
        val p = page.coerceIn(1, TOTAL_PAGES)
        val first = QuranCorpus.ayahsOnPage(p).first()
        return QuranPlace(
            kind = QuranPlace.Kind.PAGE,
            number = p,
            verse = ref(first.surahNumber, first.ayahNumber) ?: QuranRef.Start
        )
    }

    fun placeAtJuz(juz: Int): QuranPlace {
        val j = juz.coerceIn(1, TOTAL_JUZ)
        val first = QuranCorpus.ayahsInJuz(j).first()
        return QuranPlace(
            kind = QuranPlace.Kind.JUZ,
            number = j,
            verse = ref(first.surahNumber, first.ayahNumber) ?: QuranRef.Start
        )
    }

    fun placeAtHizb(hizb: Int): QuranPlace {
        val h = hizb.coerceIn(1, TOTAL_HIZB)
        val first = QuranCorpus.ayahsInHizb(h).first()
        return QuranPlace(
            kind = QuranPlace.Kind.HIZB,
            number = h,
            verse = ref(first.surahNumber, first.ayahNumber) ?: QuranRef.Start
        )
    }

    // There was a `surahOfPage` here, documented as "the surah a page opens in, for the
    // running head", and it answered "the surah a page opens in" — which is exactly the
    // question [surahsOnPage] exists to refuse, on the 51 pages that cross a boundary.
    // It was read by nothing, because there is no running head; the pill beside the page
    // number names a *range* instead, which is the honest answer on all 604.

    /**
     * Every surah a page touches, in reading order.
     *
     * A page is not required to be inside one surah: page 604 holds the last
     * three, and page 285 crosses Al-Isra into Al-Kahf. A running head that
     * named only the surah the page *opens* in would be misleading on exactly
     * the pages where a reader most wants to know they have crossed over.
     */
    fun surahsOnPage(page: Int): List<Surah> =
        QuranCorpus.ayahsOnPage(page)
            .map { it.surahNumber }
            .distinct()
            .mapNotNull { surah(it) }

    // --- Verse furniture ---------------------------------------------------

    /**
     * The page a verse falls on, or 0 when there is no such verse.
     */
    fun pageOf(surah: Int, ayah: Int): Int {
        val index = indexOf(surah, ayah)
        if (index < 0) return 0
        return QuranCorpus.ayahs[index].pageNumber
    }

    /**
     * Whether [ref] falls on [page].
     */
    fun ayahOnPage(ref: QuranRef, page: Int): Boolean {
        val index = indexOf(ref.surah, ref.ayah)
        if (index < 0) return false
        return QuranCorpus.ayahs[index].pageNumber == page
    }

    /**
     * The prostration after a verse, if there is one.
     */
    fun sajdaAfter(surah: Int, ayah: Int): SajdaKind? =
        when (QuranCorpus.sajdaAfterVerse(surah, ayah)) {
            QuranCorpus.SajdaKind.OBLIGATORY -> SajdaKind.OBLIGATORY
            QuranCorpus.SajdaKind.RECOMMENDED -> SajdaKind.RECOMMENDED
            null -> null
        }

    /** The hizb-quarter (rub') a verse falls in, 1..240. */
    fun quarterOf(surah: Int, ayah: Int): Int = QuranCorpus.quarterOf(surah, ayah)

    /** The juz' a verse falls in, 1..30. */
    fun juzOf(surah: Int, ayah: Int): Int = QuranCorpus.juzOf(surah, ayah)

    /** The hizb a verse falls in, 1..60. */
    fun hizbOf(surah: Int, ayah: Int): Int = QuranCorpus.hizbOf(surah, ayah)
}
