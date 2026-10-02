package com.example.data.quran

import com.example.data.model.Ayah
import java.security.MessageDigest
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * The bundled corpus, loaded and verified exactly once.
 *
 * Tanzil Uthmani 1.1 Arabic text plus the Saheeh International English
 * translation, with the Tanzil partition metadata. See
 * `app/src/main/resources/quran/SOURCES.md` for provenance, licensing and
 * attribution.
 *
 * ### What "verified" means here
 *
 * Each file's SHA-256 is checked at load, and before anything is served the
 * corpus asserts that it holds 114 surahs, 6,236 verses in canonical order, and
 * complete 604-page / 30-juz' / 240-hizb-quarter partitions. A truncated or
 * reordered resource fails loudly at startup instead of producing a mushaf with
 * a missing page in the middle of it.
 *
 * ### Why the partitions are ranges and not lookups per verse
 *
 * The partition of a verse is "the last partition that starts at or before it",
 * which reads as a linear scan - and doing that scan 6,236 times, three times
 * over, is 4.4 million comparisons on the main thread while the reader is
 * opening. Instead each partition becomes a **half-open range of verse
 * indices**, and a verse's page falls out of a precomputed lookup table.
 *
 * This also gives the rest of the app O(1) navigation: "which verses are on page
 * 285" is two array reads and a `subList`, not a 6,236-element `filter`.
 */
internal object QuranCorpus {

    const val TEXT_HASH = "6933e133dd56db778c801bf738848454e43648105a151e8d84d86a7cae39ec5f"
    const val METADATA_HASH = "8867c1d88191472adec9db694b3cd9f135b1a2ef580574d32cf888dcb22c5c7a"
    const val EN_SAHIH_HASH = "f090ed258e647a393ddaa0b48f1e81ab7a5ccd5ad7d72d0da498e151bc0dbc18"

    /**
     * The one translation this app ships.
     *
     * `en_sahihintl.txt` is the only translation in the bundle, and [EN_SAHIH_HASH]
     * is checked against it at load. So this is a *description of the resource
     * above*, and it lives next to the resource rather than in a list of editions
     * in a settings screen - which is where it used to be, offering twelve choices
     * when there was one, because a list of editions is somewhere a second edition
     * can be added without anyone noticing that the code which would read it does
     * not exist.
     *
     * Shown to the reader so they know what they are being shown. Not a preference,
     * and there is nothing to persist.
     */
    const val TRANSLATION_EDITION = "English (Saheeh International)"

    const val SURA_COUNT = 114
    const val VERSE_COUNT = 6236
    const val PAGE_COUNT = 604
    const val JUZ_COUNT = 30
    const val QUARTER_COUNT = 240
    const val HIZB_COUNT = 60

    /** A verse as one integer, `surah * 1000 + ayah`. Order-preserving and exact. */
    fun reference(surah: Int, ayah: Int): Int = surah * 1000 + ayah

    private fun resource(name: String, hash: String): ByteArray {
        val bytes = requireNotNull(javaClass.getResourceAsStream("/quran/$name")) {
            "Missing Quran resource: $name"
        }.use { it.readBytes() }
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        require(digest == hash) { "Quran resource integrity check failed: $name" }
        return bytes
    }

    // --- Metadata ---------------------------------------------------------

    private class Partition(val index: Int, val reference: Int)

    private val metadata by lazy {
        DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(resource("metadata.xml", METADATA_HASH).inputStream())
    }

    private fun partitions(tag: String): List<Partition> {
        val nodes = metadata.getElementsByTagName(tag)
        return (0 until nodes.length).map { index ->
            val node = nodes.item(index) as Element
            Partition(
                node.getAttribute("index").toInt(),
                reference(node.getAttribute("sura").toInt(), node.getAttribute("aya").toInt())
            )
        }
    }

    private val pagePartitions by lazy {
        partitions("page").also { require(it.size == PAGE_COUNT) { "expected $PAGE_COUNT pages" } }
    }
    private val juzPartitions by lazy {
        partitions("juz").also { require(it.size == JUZ_COUNT) { "expected $JUZ_COUNT juz'" } }
    }
    private val quarterPartitions by lazy {
        partitions("quarter").also { require(it.size == QUARTER_COUNT) { "expected $QUARTER_COUNT quarters" } }
    }

    /**
     * Half-open verse-index ranges for a partition list.
     *
     * `bounds[i]` is the index of the first verse of partition `i`, and
     * `bounds[n + 1]` is the end of the last one, so partition `i` is exactly
     * `bounds[i] until bounds[i + 1]`. `bounds[0]` is a sentinel: nothing is
     * "in partition zero", and reading it must not throw.
     *
     * Partitions are written at their own index rather than one past it. The
     * earlier version offset every write by one and then read back at the
     * un-offset index, so the first partition was never written at all and
     * partition 33 came out as -1 - "page 33 has no verses", on a partition that
     * is perfectly well formed. Offsets that are *derived from each other* have
     * to be written in exactly one convention and read in the same one.
     */
    private fun boundsOf(parts: List<Partition>): IntArray {
        val bounds = IntArray(parts.size + 2)
        for (part in parts) {
            val at = indexOfReference(part.reference)
            require(at >= 0) {
                "partition ${part.index} starts at a verse that does not exist"
            }
            bounds[part.index] = at
        }
        // A partition starting before the previous one ends would make an
        // overlapping range, and one starting after it would make an empty one.
        // Both mean the partition data is not what this app thinks it is, and
        // both would show a reader as duplicated or blank pages.
        for (i in 1..parts.size) {
            require(bounds[i] >= bounds[i - 1]) {
                "partition $i starts at verse ${bounds[i]}, " +
                    "before partition ${i - 1} at ${bounds[i - 1]}"
            }
        }
        bounds[parts.size + 1] = VERSE_COUNT
        return bounds
    }

    // --- Verses -----------------------------------------------------------

    /**
     * The verse texts, joined once, in canonical order.
     *
     * Index `i` of this list is the same `i` every partition range is expressed
     * in, which is what makes the ranges and the page table agree.
     */
    private val verseTexts: List<Ayah> by lazy {
        // The translation is loaded first so that a corrupt translation fails
        // fast, with a clear message, rather than halfway through building the
        // Arabic half.
        val translations = englishByReference
        // The BOM is stripped because the bundled file has one, and the first verse
        // would otherwise begin with U+FEFF - an invisible character that is not
        // Arabic, sits inside the ayah's own text, and is copied out with the verse.
        //
        // Written as an escape rather than as the character itself: a literal U+FEFF
        // in the middle of a source file is invisible to read and is flagged by lint
        // as a stray byte-order mark, so the one place that handles a BOM is the one
        // place that cannot contain one.
        val text = resource("uthmani.txt", TEXT_HASH)
            .toString(Charsets.UTF_8)
            .removePrefix("\uFEFF")
        val verses = text.lineSequence()
            .filter { it.firstOrNull()?.isDigit() == true }
            .map { line ->
                val fields = line.split('|', limit = 3)
                require(fields.size == 3 && fields[2].isNotBlank()) { "Bad verse line: $line" }
                val surah = fields[0].toInt()
                val ayah = fields[1].toInt()
                Ayah(
                    surahNumber = surah,
                    ayahNumber = ayah,
                    textArabic = fields[2],
                    textEnglish = translations[reference(surah, ayah)].orEmpty()
                )
            }
            .toList()

        require(verses.size == VERSE_COUNT) {
            "Expected $VERSE_COUNT verses, got ${verses.size}"
        }
        verifyCanonicalOrder(verses)
        verses
    }

    /**
     * Confirms the corpus is 114 surahs of exactly the metadata's verse counts,
     * in order, with nothing missing and nothing repeated.
     *
     * This is the assertion that makes every arithmetic index below safe. If
     * this holds, a verse's position in [verseTexts] is
     * `surahOffset[surah] + ayah - 1` with no table and no search.
     */
    private fun verifyCanonicalOrder(verses: List<Ayah>) {
        val suraNodes = metadata.getElementsByTagName("sura")
        require(suraNodes.length == SURA_COUNT) {
            "Expected $SURA_COUNT surahs, got ${suraNodes.length}"
        }
        val expected = ArrayList<Ayah>(VERSE_COUNT)
        for (i in 0 until suraNodes.length) {
            val node = suraNodes.item(i) as Element
            val surah = node.getAttribute("index").toInt()
            require(surah == i + 1) { "Surah out of order at $i: index $surah" }
            for (ayah in 1..node.getAttribute("ayas").toInt()) {
                // Only the reference matters here; the text is checked by count
                // and by the ordering assertion below.
                expected += Ayah(
                    surahNumber = surah,
                    ayahNumber = ayah,
                    textArabic = ""
                )
            }
        }
        require(expected.size == verses.size) {
            "Metadata describes ${expected.size} verses, the text file has ${verses.size}"
        }
        for (i in expected.indices) {
            require(
                expected[i].surahNumber == verses[i].surahNumber &&
                    expected[i].ayahNumber == verses[i].ayahNumber
            ) { "Verse $i is ${verses[i].surahNumber}:${verses[i].ayahNumber}, " +
                "expected ${expected[i].surahNumber}:${expected[i].ayahNumber}" }
        }
    }

    /**
     * `surahOffset[s]` is the index of surah `s`'s first verse, and
     * `surahOffset[s + 1]` is the end of it, so surah `s` is exactly
     * `surahOffset[s] until surahOffset[s + 1]`.
     *
     * Sized `SURA_COUNT + 2` so the entry one past the last surah is a real slot
     * and not an out-of-bounds read on the final surah.
     */
    private val surahOffset: IntArray by lazy {
        val offsets = IntArray(SURA_COUNT + 2)
        // One pass, not a `count` per surah: the earlier version asked 114
        // questions of a 6,236-element list, which is 711k comparisons for an
        // answer a single pass gives for free.
        //
        // The offset of a surah is the index of its *own* first verse, so it is
        // written at the moment that verse is reached - which for surah 1 is
        // index 0, and is why the array is seeded with a zero rather than a
        // sentinel that would have to be special-cased at every read.
        val loaded = verseTexts
        var previous = 0
        loaded.forEachIndexed { index, ayah ->
            if (ayah.surahNumber != previous) {
                offsets[ayah.surahNumber] = index
                previous = ayah.surahNumber
            }
        }
        // `offsets[surah + 1]` is read as the *end* of surah `surah`, so the
        // entry past the last surah has to be the corpus size rather than a
        // default zero - otherwise surah 114 would appear to run to the end of a
        // list it already is the end of, and 115 would read as empty by luck.
        offsets[SURA_COUNT + 1] = loaded.size
        require(loaded.size == VERSE_COUNT) {
            "Corpus has ${loaded.size} verses, expected $VERSE_COUNT"
        }
        offsets
    }

    /** `surahOffset`-based position of a verse, or -1 when it does not exist. */
    private fun indexOfReference(ref: Int): Int {
        val surah = ref / 1000
        val ayah = ref % 1000
        if (surah !in 1..SURA_COUNT || ayah < 1) return -1
        val start = surahOffset[surah]
        val end = surahOffset[surah + 1]
        if (ayah > end - start) return -1
        return start + ayah - 1
    }

    private val englishByReference: Map<Int, String> by lazy {
        val text = resource("en_sahihintl.txt", EN_SAHIH_HASH).toString(Charsets.UTF_8)
        val map = HashMap<Int, String>(VERSE_COUNT * 2)
        text.lineSequence().forEach { line ->
            if (line.isBlank()) return@forEach
            val fields = line.split('|', limit = 3)
            require(fields.size == 3 && fields[2].isNotBlank()) { "Bad translation line: $line" }
            map[reference(fields[0].toInt(), fields[1].toInt())] = fields[2]
        }
        require(map.size == VERSE_COUNT) {
            "Expected $VERSE_COUNT translated verses, got ${map.size}"
        }
        map
    }

    // --- Partition ranges and the per-verse lookup table ------------------

    val pageBounds: IntArray by lazy { boundsOf(pagePartitions) }
    val juzBounds: IntArray by lazy { boundsOf(juzPartitions) }
    val quarterBounds: IntArray by lazy { boundsOf(quarterPartitions) }

    /**
     * Hizb boundaries, taken from the quarter table.
     *
     * The corpus stores quarters (1..240) rather than hizb (1..60) because that
     * is the finer partition, and four quarters make one hizb. Deriving the
     * coarser boundary from the finer one means a reader can never land in two
     * different hizb numbers for one verse, which is what a second hand-kept
     * table would eventually do.
     */
    val hizbBounds: IntArray by lazy {
        val bounds = IntArray(HIZB_COUNT + 2)
        for (hizb in 1..HIZB_COUNT) {
            val firstQuarter = (hizb - 1) * 4 + 1
            bounds[hizb] = quarterBounds[firstQuarter]
        }
        bounds[HIZB_COUNT + 1] = VERSE_COUNT
        bounds
    }

    /** Verse index to page / juz' / quarter, filled once by walking the ranges. */
    private val pageOfIndex: IntArray by lazy { lookupTable(pageBounds, PAGE_COUNT) }
    private val juzOfIndex: IntArray by lazy { lookupTable(juzBounds, JUZ_COUNT) }
    private val quarterOfIndex: IntArray by lazy { lookupTable(quarterBounds, QUARTER_COUNT) }

    private fun lookupTable(bounds: IntArray, count: Int): IntArray {
        val table = IntArray(VERSE_COUNT)
        for (part in 1..count) {
            val from = bounds[part]
            val to = bounds[part + 1]
            for (i in from until to) table[i] = part
        }
        // A gap would leave a zero in the table, and zero is not a valid
        // partition, so its presence is a corrupt-resource signal rather than a
        // silent wrong answer.
        require(table.all { it in 1..count }) { "A partition does not cover every verse" }
        return table
    }

    // --- The public surface ----------------------------------------------

    /** Every verse, in canonical order. Index-stable for the life of the process. */
    val ayahs: List<Ayah> by lazy {
        val pages = pageOfIndex
        val juzs = juzOfIndex
        val quarters = quarterOfIndex
        verseTexts.mapIndexed { index, ayah ->
            ayah.copy(
                pageNumber = pages[index],
                juzNumber = juzs[index],
                hizbQuarter = quarters[index]
            )
        }
    }

    /** Verses on a mushaf page, as a range of the canonical list. */
    fun ayahsOnPage(page: Int): List<Ayah> {
        val p = page.coerceIn(1, PAGE_COUNT)
        return ayahs.subList(pageBounds[p], pageBounds[p + 1])
    }

    /** Verses of a surah, as a range of the canonical list. */
    fun ayahsInSurah(surah: Int): List<Ayah> {
        if (surah !in 1..SURA_COUNT) return emptyList()
        return ayahs.subList(surahOffset[surah], surahOffset[surah + 1])
    }

    /** Verses of a juz'. */
    fun ayahsInJuz(juz: Int): List<Ayah> {
        val j = juz.coerceIn(1, JUZ_COUNT)
        return ayahs.subList(juzBounds[j], juzBounds[j + 1])
    }

    /** Verses of a hizb. */
    fun ayahsInHizb(hizb: Int): List<Ayah> {
        val h = hizb.coerceIn(1, HIZB_COUNT)
        return ayahs.subList(hizbBounds[h], hizbBounds[h + 1])
    }

    /** The verse list position of a reference, or -1. */
    fun indexOf(surah: Int, ayah: Int): Int = indexOfReference(reference(surah, ayah))

    /** The verse at a list position. */
    fun ayahAt(index: Int): Ayah? = ayahs.getOrNull(index)

    /** The canonical page a verse falls on, or 1 for a reference that has none. */
    fun pageOf(surah: Int, ayah: Int): Int {
        val index = indexOfReference(reference(surah, ayah))
        return if (index < 0) 1 else pageOfIndex[index]
    }

    fun juzOf(surah: Int, ayah: Int): Int {
        val index = indexOfReference(reference(surah, ayah))
        return if (index < 0) 1 else juzOfIndex[index]
    }

    fun quarterOf(surah: Int, ayah: Int): Int {
        val index = indexOfReference(reference(surah, ayah))
        return if (index < 0) 1 else quarterOfIndex[index]
    }

    fun hizbOf(surah: Int, ayah: Int): Int = (quarterOf(surah, ayah) - 1) / 4 + 1

    // --- Prostration ------------------------------------------------------

    /** Whether a prostration after a verse is obligatory or recommended. */
    enum class SajdaKind { OBLIGATORY, RECOMMENDED }

    /**
     * The fifteen prostrations, keyed by the verse they follow.
     *
     * Bundled in the metadata since the beginning and never read by the reader,
     * which is why a page containing a sajdah looked exactly like one that did
     * not. A prostration position is marked in the margin of every printed
     * mushaf, and a reader looking for it has no other way to know they are
     * standing on one.
     */
    val sajdaAfter: Map<Int, SajdaKind> by lazy {
        val nodes = metadata.getElementsByTagName("sajda")
        val map = HashMap<Int, SajdaKind>(16)
        for (i in 0 until nodes.length) {
            val node = nodes.item(i) as Node as Element
            val ref = reference(
                node.getAttribute("sura").toInt(),
                node.getAttribute("aya").toInt()
            )
            val kind = if (node.getAttribute("type") == "obligatory") {
                SajdaKind.OBLIGATORY
            } else {
                SajdaKind.RECOMMENDED
            }
            // Tanzil lists sura 22 twice (18 and 77) and 38 twice (24 and the
            // sajdah-count conventions differ between prints); first wins, so
            // the map is a function rather than a coin toss.
            map.putIfAbsent(ref, kind)
        }
        require(map.isNotEmpty()) { "No sajda positions in the metadata" }
        map
    }

    fun sajdaAfterVerse(surah: Int, ayah: Int): SajdaKind? =
        sajdaAfter[reference(surah, ayah)]

    // --- Surah metadata ---------------------------------------------------

    /** Verse count per surah, straight from the metadata. */
    val surahVerseCount: IntArray by lazy {
        val nodes = metadata.getElementsByTagName("sura")
        require(nodes.length == SURA_COUNT)
        IntArray(SURA_COUNT + 1) { i ->
            if (i == 0) 0 else (nodes.item(i - 1) as Element).getAttribute("ayas").toInt()
        }
    }

    /** Revelation place per surah, as the metadata's own spelling. */
    val surahIsMedinan: BooleanArray by lazy {
        val nodes = metadata.getElementsByTagName("sura")
        require(nodes.length == SURA_COUNT)
        BooleanArray(SURA_COUNT + 1) { i ->
            i != 0 && (nodes.item(i - 1) as Element).getAttribute("type") == "Medinan"
        }
    }

    /** The Arabic name the metadata carries for a surah. */
    fun surahArabicName(surah: Int): String {
        val node = metadata.getElementsByTagName("sura").item(surah - 1) as Element
        return node.getAttribute("name")
    }

    /** The page a surah begins on. */
    fun surahStartPage(surah: Int): Int = pageOf(surah, 1)
}
