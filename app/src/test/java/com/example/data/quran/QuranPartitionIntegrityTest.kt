package com.example.data.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every juz' and hizb is a real place in the book.
 *
 * ### The gap this fills
 *
 * `QuranPageIntegrityTest` walks **all 604 pages** and checks that each has verses, that a
 * turn from every page lands on that page, that the pages tile the book, and that the 51
 * pages which cross a surah boundary are the ones that do.
 *
 * **No such sweep existed for the 30 juz' or the 60 hizb.** Nothing checked that any of
 * them has a verse in it, that they cover the book, that they do not overlap, or that
 * `QuranCorpus.juzOf` agrees with the partition a verse actually lands in.
 *
 * That matters because of what depends on it:
 *
 * - `QuranBrowse.placeAtJuz` and `placeAtHizb` call `.first()` on the partition. An empty
 *   partition is a crash, and they are on the path the index sheet's "Juz'" and "Hizb"
 *   filters take — so an empty juz' would take the reader's sheet down rather than show an
 *   empty list.
 * - `ContinuousReader` draws a page rule from `pageNumber` changes, and `ReaderPosition`
 *   resolves stored positions through the same ranges, so a wrong bound resumes a reader
 *   in the wrong juz'.
 *
 * So this is the page sweep, applied to the two partitions that were never swept, plus the
 * per-verse agreement check that pages get for free from `QuranRef.page`.
 */
class QuranPartitionIntegrityTest {

    private val everySurah = 1..QuranCorpus.SURA_COUNT

    private fun versesOfJuz(juz: Int) = QuranCorpus.ayahsInJuz(juz)

    private fun versesOfHizb(hizb: Int) = QuranCorpus.ayahsInHizb(hizb)

    // --- The thing that would crash a reader ---------------------------------

    @Test
    fun `every juz' has at least one verse`() {
        for (juz in 1..QuranCorpus.JUZ_COUNT) {
            assertTrue(
                "juz' $juz has no verses, and placeAtJuz takes .first() of it",
                versesOfJuz(juz).isNotEmpty()
            )
        }
    }

    @Test
    fun `every hizb has at least one verse`() {
        for (hizb in 1..QuranCorpus.HIZB_COUNT) {
            assertTrue(
                "hizb $hizb has no verses, and placeAtHizb takes .first() of it",
                versesOfHizb(hizb).isNotEmpty()
            )
        }
    }

    // --- Do they tile the book? ----------------------------------------------

    @Test
    fun `the juz' partition covers every verse exactly once`() {
        val seen = HashMap<Int, Int>() // corpus index -> juz'
        for (juz in 1..QuranCorpus.JUZ_COUNT) {
            for (ayah in versesOfJuz(juz)) {
                val index = QuranCorpus.indexOf(ayah.surahNumber, ayah.ayahNumber)
                assertTrue(
                    "${ayah.surahNumber}:${ayah.ayahNumber} is not in the corpus at all",
                    index >= 0
                )
                val previous = seen.put(index, juz)
                assertEquals(
                    "${ayah.surahNumber}:${ayah.ayahNumber} is in juz' $previous and " +
                        "juz' $juz as well, so a reader would find it under two numbers",
                    null,
                    previous
                )
            }
        }
        assertEquals(
            "the ${QuranCorpus.JUZ_COUNT} juz' cover ${seen.size} verses, and the book " +
                "has ${QuranCorpus.VERSE_COUNT}",
            QuranCorpus.VERSE_COUNT,
            seen.size
        )
    }

    @Test
    fun `the hizb partition covers every verse exactly once`() {
        val seen = HashMap<Int, Int>()
        for (hizb in 1..QuranCorpus.HIZB_COUNT) {
            for (ayah in versesOfHizb(hizb)) {
                val index = QuranCorpus.indexOf(ayah.surahNumber, ayah.ayahNumber)
                assertTrue(
                    "${ayah.surahNumber}:${ayah.ayahNumber} is not in the corpus at all",
                    index >= 0
                )
                val previous = seen.put(index, hizb)
                assertEquals(
                    "${ayah.surahNumber}:${ayah.ayahNumber} is in hizb $previous and " +
                        "hizb $hizb as well",
                    null,
                    previous
                )
            }
        }
        assertEquals(
            "the ${QuranCorpus.HIZB_COUNT} hizb cover ${seen.size} verses, and the book " +
                "has ${QuranCorpus.VERSE_COUNT}",
            QuranCorpus.VERSE_COUNT,
            seen.size
        )
    }

    @Test
    fun `each juz' is contiguous in the corpus`() {
        // Contiguity is what makes "juz' 18" a range rather than a set. A partition with a
        // gap would still pass the coverage test above and would resume a reader into the
        // wrong place.
        for (juz in 1..QuranCorpus.JUZ_COUNT) {
            val indices = versesOfJuz(juz).map {
                QuranCorpus.indexOf(it.surahNumber, it.ayahNumber)
            }
            for (i in 1 until indices.size) {
                assertEquals(
                    "juz' $juz jumps from corpus index ${indices[i - 1]} to ${indices[i]}, " +
                        "so it is a set of verses rather than a range",
                    indices[i - 1] + 1,
                    indices[i]
                )
            }
        }
    }

    @Test
    fun `each hizb is contiguous in the corpus`() {
        for (hizb in 1..QuranCorpus.HIZB_COUNT) {
            val indices = versesOfHizb(hizb).map {
                QuranCorpus.indexOf(it.surahNumber, it.ayahNumber)
            }
            for (i in 1 until indices.size) {
                assertEquals(
                    "hizb $hizb jumps from corpus index ${indices[i - 1]} to " +
                        "${indices[i]}",
                    indices[i - 1] + 1,
                    indices[i]
                )
            }
        }
    }

    // --- Do the lookups agree with the partitions? ---------------------------

    /**
     * Corpus index -> the juz' and hizb that verse is in.
     *
     * Built once, because asking the question the other way round — for each verse,
     * searching all thirty partitions — is 6,236 x 30 list rebuilds, which is minutes
     * of work to answer a question a single pass answers.
     */
    private fun indexToPartition(
        partitions: Int,
        versesOf: (Int) -> List<com.example.data.model.Ayah>
    ): IntArray {
        val owner = IntArray(QuranCorpus.VERSE_COUNT) { -1 }
        for (part in 1..partitions) {
            for (ayah in versesOf(part)) {
                val index = QuranCorpus.indexOf(ayah.surahNumber, ayah.ayahNumber)
                if (index in owner.indices) owner[index] = part
            }
        }
        return owner
    }

    private val corpusIndexes: List<Int> by lazy {
        QuranCorpus.ayahs.map { QuranCorpus.indexOf(it.surahNumber, it.ayahNumber) }
    }

    private val juzOfEachVerse: IntArray by lazy { indexToPartition(QuranCorpus.JUZ_COUNT, ::versesOfJuz) }

    private val hizbOfEachVerse: IntArray by lazy {
        indexToPartition(QuranCorpus.HIZB_COUNT, ::versesOfHizb)
    }

    @Test
    fun `juzOf agrees with the partition every verse is in`() {
        // One check per verse, across the whole book. `juzOf` is what the reader's chrome
        // and the stored position resolve through; the partition is what the index sheet
        // lists. If they disagree, the sheet shows one juz' and the reader is told another.
        for (ayah in QuranCorpus.ayahs) {
            val index = QuranCorpus.indexOf(ayah.surahNumber, ayah.ayahNumber)
            assertEquals(
                "${ayah.surahNumber}:${ayah.ayahNumber} is in juz' " +
                    "${juzOfEachVerse[index]} but juzOf says " +
                    "${QuranCorpus.juzOf(ayah.surahNumber, ayah.ayahNumber)}",
                juzOfEachVerse[index],
                QuranCorpus.juzOf(ayah.surahNumber, ayah.ayahNumber)
            )
        }
    }

    @Test
    fun `hizbOf agrees with the partition every verse is in`() {
        for (ayah in QuranCorpus.ayahs) {
            val index = QuranCorpus.indexOf(ayah.surahNumber, ayah.ayahNumber)
            assertEquals(
                "${ayah.surahNumber}:${ayah.ayahNumber} is in hizb " +
                    "${hizbOfEachVerse[index]} but hizbOf says " +
                    "${QuranCorpus.hizbOf(ayah.surahNumber, ayah.ayahNumber)}",
                hizbOfEachVerse[index],
                QuranCorpus.hizbOf(ayah.surahNumber, ayah.ayahNumber)
            )
        }
    }

    @Test
    fun `each juz' is exactly two hizb`() {
        // The relationship the "Hizb half" labels in the index depend on: sixty hizb and
        // thirty juz' says each juz' is two consecutive hizb, which is a claim about the
        // *contents* and not just the counts.
        for (juz in 1..QuranCorpus.JUZ_COUNT) {
            val expected = versesOfHizb(juz * 2 - 1) + versesOfHizb(juz * 2)
            assertEquals(
                "juz' $juz is not hizb ${juz * 2 - 1} followed by hizb ${juz * 2}",
                expected.size,
                versesOfJuz(juz).size
            )
            assertEquals(
                "juz' $juz holds different verses from its two hizb",
                expected.map { it.surahNumber to it.ayahNumber },
                versesOfJuz(juz).map { it.surahNumber to it.ayahNumber }
            )
        }
        assertEquals(
            "there are not twice as many hizb as juz'",
            QuranCorpus.JUZ_COUNT * 2,
            QuranCorpus.HIZB_COUNT
        )
    }

    @Test
    fun `the first juz' starts the book and the last one ends it`() {
        // A partition that starts late loses verses at the front; one that ends early
        // loses them at the back. Both would pass the coverage test only if the lost
        // verses appeared in a *neighbouring* partition, so these are checked directly.
        val firstVerse = QuranCorpus.ayahs.first()
        assertTrue(
            "juz' 1 does not begin with the book's first verse, " +
                "${firstVerse.surahNumber}:${firstVerse.ayahNumber}",
            firstVerse in versesOfJuz(1)
        )
        val lastVerse = QuranCorpus.ayahs.last()
        assertTrue(
            "juz' ${QuranCorpus.JUZ_COUNT} does not end with the book's last verse, " +
                "${lastVerse.surahNumber}:${lastVerse.ayahNumber}",
            lastVerse in versesOfJuz(QuranCorpus.JUZ_COUNT)
        )
        assertTrue(
            "juz' 1 begins before the book does",
            versesOfJuz(1).first().surahNumber == 1 &&
                versesOfJuz(1).first().ayahNumber == 1
        )
        assertTrue(
            "the last juz' does not reach the end of the book",
            versesOfJuz(QuranCorpus.JUZ_COUNT).last().surahNumber == 114
        )
    }
}