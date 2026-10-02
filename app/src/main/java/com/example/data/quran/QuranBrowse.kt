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
 * The old data source was a singleton whose every method was a linear scan:
 * `getAyahsForPage` filtered 6,236 verses, `getAyahsForSurah` filtered 6,236
 * verses, `searchAyahs` normalised all 6,236 *and* recompiled three regexes per
 * verse, and `getSurahByNumber` searched a 114-element list per verse inside the
 * search loop. The reader calls some of these on every page turn and the index
 * calls them on every scroll, so the arithmetic was not a theoretical problem -
 * it was the reader being slow.
 *
 * Two things fix it and both belong here rather than in the call sites:
 *
 * 1. The corpus now holds its partitions as **ranges of verse indices**, so
 *    "which verses are on page 285" is two array reads and a `subList`.
 * 2. Everything that is asked *per verse* inside a loop - the surah lookup, the
 *    partition, the normalisation - is hoisted into a precomputed table or a
 *    lazily built index, so the loop body is arithmetic.
 *
 * ### What is deliberately not here
 *
 * No state, no caching of *results*, and no `Flow`. The corpus is immutable for
 * the life of the process and every value here is derived from it, so a cache
 * that could go stale cannot be written. The only lazily built things are the
 * two search indexes, and they are derived from immutable data.
 */
object QuranBrowse {

    /**
     * Whether a prostration after a verse is obligatory or recommended.
     *
     * Re-exported rather than reached through [QuranCorpus], which is internal
     * because the corpus is a loading detail. The kind is part of the reader's
     * vocabulary - a marker that says *which* prostration it is - so it belongs
     * on the public surface with the rest of the browse vocabulary.
     */
    enum class SajdaKind { OBLIGATORY, RECOMMENDED }

    const val TOTAL_PAGES = QuranCorpus.PAGE_COUNT
    const val TOTAL_JUZ = QuranCorpus.JUZ_COUNT
    const val TOTAL_HIZB = QuranCorpus.HIZB_COUNT
    const val TOTAL_SURAHS = QuranCorpus.SURA_COUNT
    const val TOTAL_VERSES = QuranCorpus.VERSE_COUNT

    /**
     * The translation every verse's English text comes from.
     *
     * Re-exported for the same reason as [SajdaKind]: the edition is part of what
     * the reader is reading, so it belongs on the public surface rather than
     * something a screen reaches past this facade to find.
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
     *
     * Resolving a reference to a page here, in one place, is what keeps the page
     * number in the reader's chrome, the page written to storage and the page a
     * bookmark opens at from ever disagreeing. There is no second answer to
     * "which page is this verse on" for them to drift apart from.
     */
    fun ref(surah: Int, ayah: Int): QuranRef? {
        val verse = ayah(surah, ayah) ?: return null
        return QuranRef(surah = surah, ayah = ayah, page = verse.pageNumber)
    }

    /**
     * A verse as a reference, never null.
     *
     * For the places that must always produce *something* to show - the reader's
     * starting position, a bookmark whose surah has since been removed from the
     * corpus. An unknown reference clamps into the corpus rather than throwing,
     * because a reader who opens the Quran and gets a crash has a worse week than
     * one who lands on page 1.
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

    fun placeAtSurah(surah: Int): QuranPlace {
        val s = surah.coerceIn(1, TOTAL_SURAHS)
        val first = QuranCorpus.ayahsInSurah(s).first()
        return QuranPlace(
            kind = QuranPlace.Kind.SURAH,
            number = s,
            verse = ref(first.surahNumber, first.ayahNumber) ?: QuranRef.Start
        )
    }

    /** The surah a page opens in, for the running head. */
    fun surahOfPage(page: Int): Surah? =
        QuranCorpus.ayahsOnPage(page).firstOrNull()?.let { surah(it.surahNumber) }

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
     *
     * The inverse of [ayahOnPage], and needed anywhere the answer is "which page
     * does this point to" rather than "is this point on that page" - the index
     * sheet's page filter, and the check that a stored position still resolves
     * after a corpus change.
     */
    fun pageOf(surah: Int, ayah: Int): Int {
        val index = indexOf(surah, ayah)
        if (index < 0) return 0
        return QuranCorpus.ayahs[index].pageNumber
    }

    /**
     * Whether [ref] falls on [page].
     *
     * Used to decide whether a selection survives a page turn. A selection is a
     * property of the *surface* it was made on, and page 604 holds three surahs -
     * so ayah 1 selected there is not ayah 1 anywhere else, and carrying it across
     * a turn would highlight a verse on a page that does not contain it.
     */
    fun ayahOnPage(ref: QuranRef, page: Int): Boolean {
        val index = indexOf(ref.surah, ref.ayah)
        if (index < 0) return false
        return QuranCorpus.ayahs[index].pageNumber == page
    }

    /**
     * The prostration after a verse, if there is one.
     *
     * Read from the bundled metadata, which has carried all fifteen positions
     * since the beginning and which the reader had never once asked - so a page
     * containing a prostration looked exactly like one that did not.
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
