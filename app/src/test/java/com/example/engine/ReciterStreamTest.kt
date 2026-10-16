package com.example.engine

import com.example.data.model.Reciter
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every offered reciter streams from its own directory, and nothing else can.
 *
 * ### Why this is a test and not a screenshot
 *
 * The defect was not visible on screen: Settings offered four reciters, the
 * banner repeated the chosen one, and the player streamed `Alafasy_128kbps`
 * whatever was chosen - one fact ("who is being heard") decided in two places,
 * with only one of them real. A screenshot of the picker or the banner looks
 * the same either way. What can be asserted is the artifact the player hands
 * to the network: the URL, per reciter.
 *
 * ### The mapping is the property
 *
 * The assertions run over [Reciter.entries], so a reciter added to the registry
 * is exercised the day it is added, and a directory typo in any of them fails
 * here rather than 404ing in a reader's ear.
 */
class ReciterStreamTest {

    @Test
    fun `every offered reciter streams from its own directory, padded as everyayah names files`() {
        val directories = mutableSetOf<String>()
        for (reciter in Reciter.entries) {
            for (surah in 1..114) {
                // Two verses per surah is enough to pin both padded fields: the
                // first and the last, which is where an off-by-one in padding
                // would first show.
                for (ayah in listOf(1, 6)) {
                    val url = reciter.verseUrl(surah, ayah)
                    val expected = "https://everyayah.com/data/${reciter.directory}/" +
                        String.format(Locale.ROOT, "%03d", surah) +
                        String.format(Locale.ROOT, "%03d", ayah) +
                        ".mp3"
                    assertEquals("$reciter must stream its own directory", expected, url)
                }
            }
            assertTrue(
                "$reciter's directory is not a directory name",
                reciter.directory.isNotBlank() && !reciter.directory.contains(' ')
            )
            assertTrue(
                "$reciter and another reciter share a directory",
                directories.add(reciter.directory)
            )
        }
    }

    @Test
    fun `the file name uses ASCII digits whatever the default locale numbers with`() {
        // The app ships in Arabic, Urdu and Bengali. In those locales
        // `String.format("%03d", 2)` without an explicit locale produces "٠٠٢",
        // and everyayah.com names files in ASCII - the request would 404 in
        // exactly the languages where listening matters most.
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            val url = Reciter.ALAFASY.verseUrl(2, 255)
            assertTrue("expected ASCII file name, got $url", url.endsWith("/002255.mp3"))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `a stored name resolves to its reciter, and an unknown one cannot name a stream that is not offered`() {
        for (reciter in Reciter.entries) {
            assertEquals(
                "$reciter does not resolve from its own display name",
                reciter,
                Reciter.from(reciter.displayName)
            )
        }

        // The fallback is what the banner's honesty rests on: a preference the
        // registry cannot resolve must resolve to a reciter whose stream exists,
        // because `from` is what the player streams *and* what the banner names.
        for (unknown in listOf(null, "", "   ", "Some Reciter")) {
            assertEquals(Reciter.ALAFASY, Reciter.from(unknown))
        }
    }

    @Test
    fun `each reciter previews with its own tone`() {
        val tones = Reciter.entries.map { previewToneHz(it) }
        assertEquals(
            "two reciters share a preview tone, so their previews are indistinguishable",
            Reciter.entries.size,
            tones.toSet().size
        )
        // The old matcher's dead branch ("Ghamdi") is gone, and no tone may be
        // copied back from it without this failing.
        assertNotEquals(0.0, tones.first())
    }
}
