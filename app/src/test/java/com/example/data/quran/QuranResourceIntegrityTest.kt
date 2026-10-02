package com.example.data.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.util.Locale

/**
 * The declared digests match the files that ship.
 *
 * ### Why this exists
 *
 * `QuranCorpus.resource` verifies every bundled resource's SHA-256 on load and throws on
 * a mismatch — which is right, and is the only place in the app that refuses to run
 * rather than guess. A swapped or truncated `uthmani.txt` would put words in a reader's
 * mouth that are not in the Quran, and correct pagination and correct rendering do not
 * make that acceptable.
 *
 * **But the check ran only on a user's device.** The digests are `const val`s next to the
 * code, and nothing compared them to the assets, so editing an asset and forgetting the
 * constant left the build green and made the reader open empty for everyone who
 * installed it — a failure discovered in the wild, on the one surface that must not fail.
 *
 * This moves it to CI, where it is a red build instead.
 *
 * ### Why the hex is locale-independent
 *
 * A digest is ASCII by definition. The formatting is pinned to [Locale.ROOT] and this test
 * recomputes it the same way, so the check depends on the bytes and nothing else. Java's
 * `%x` emits ASCII hex in every locale today; relying on that would make the whole
 * corpus's availability depend on a formatting decision nobody was looking for.
 */
class QuranResourceIntegrityTest {

    /** The assets under test, and the constant each one is checked against. */
    private val resources = listOf(
        "uthmani.txt" to QuranCorpus.TEXT_HASH,
        "en_sahihintl.txt" to QuranCorpus.EN_SAHIH_HASH,
        "metadata.xml" to QuranCorpus.METADATA_HASH
    )

    private fun bytesOf(name: String): ByteArray =
        checkNotNull(javaClass.getResourceAsStream("/quran/$name")) {
            "the bundled resource $name is not on the classpath"
        }.use { it.readBytes() }

    private fun digestOf(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { String.format(Locale.ROOT, "%02x", it.toInt() and 255) }

    @Test
    fun `every bundled resource matches the digest declared beside it`() {
        for ((name, declared) in resources) {
            assertEquals(
                "the bundled $name does not match its declared SHA-256. The reader " +
                    "will refuse to open on every device. Update the constant, or " +
                    "restore the asset - do not weaken the check.",
                declared,
                digestOf(bytesOf(name))
            )
        }
    }

    @Test
    fun `a digest is 64 lowercase hex characters`() {
        // A constant typed with an uppercase letter, a truncated paste, or a stray
        // newline would fail the comparison above at runtime rather than here, with a
        // message about integrity rather than about the constant being malformed.
        for ((name, declared) in resources) {
            assertEquals(
                "$name's declared digest is not 64 characters",
                64,
                declared.length
            )
            assertTrue(
                "$name's declared digest is not lowercase hex: \"$declared\"",
                declared.all { it in '0'..'9' || it in 'a'..'f' }
            )
        }
    }

    @Test
    fun `a changed byte changes the digest`() {
        // The check is only worth anything if it can fail. Corrupt one byte of a real
        // asset and the digest must not still match.
        val original = bytesOf("uthmani.txt")
        val corrupted = original.copyOf()
        corrupted[corrupted.size / 2] = (corrupted[corrupted.size / 2].toInt() xor 0x01).toByte()

        assertNotEquals(
            "flipping a single bit of the corpus left the digest unchanged, so the " +
                "integrity check cannot detect a tampered resource",
            digestOf(original),
            digestOf(corrupted)
        )
        assertNotEquals(
            "a corrupted resource still matched its declared digest",
            QuranCorpus.TEXT_HASH,
            digestOf(corrupted)
        )
    }

    @Test
    fun `the corpus actually loads, so the check is reachable`() {
        // Every other test in the suite reads the corpus, so this is nearly redundant —
        // but it is the assertion that says *why* they can: the integrity gate is on the
        // path to the first verse, not on a code path nothing takes.
        assertEquals(6236, QuranCorpus.VERSE_COUNT)
        assertTrue(
            "the corpus loaded but is empty",
            QuranCorpus.ayahs.isNotEmpty()
        )
        assertEquals(
            "the corpus holds a different number of verses than it declares",
            QuranCorpus.VERSE_COUNT,
            QuranCorpus.ayahs.size
        )
    }
}