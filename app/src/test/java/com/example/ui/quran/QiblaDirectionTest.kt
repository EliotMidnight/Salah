package com.example.ui.quran

import com.example.ui.localization.ArabicStrings
import com.example.ui.localization.BengaliStrings
import com.example.ui.localization.EnglishStrings
import com.example.ui.localization.FrenchStrings
import com.example.ui.localization.GermanStrings
import com.example.ui.localization.IndonesianStrings
import com.example.ui.localization.MalayStrings
import com.example.ui.localization.ReaderStrings
import com.example.ui.localization.RussianStrings
import com.example.ui.localization.SpanishStrings
import com.example.ui.localization.TurkishStrings
import com.example.ui.localization.UiStrings
import com.example.ui.localization.UrduStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The compass dial names its directions in the reader's language.
 *
 * ### What was wrong
 *
 * `getCardinalDirection` was a private function in `QiblaDirectionFinder` returning
 * `"N"`, `"NE"`, `"E"`, `"SE"`, `"S"`, `"SW"`, `"W"`, `"NW"`, used in two visible
 * places — the caption under the heading readout, and the text beside the azimuth. A
 * reader in any of the ten shipped languages saw an English abbreviation, in an app
 * that translates everything else it says about the compass.
 *
 * It was also **wrong in a language with a Latin script**, which is the part that makes
 * it a defect rather than a gap: German abbreviates *Nordost* to "NO", not "NE". So
 * this was not borrowing, and a shared abbreviation would have been wrong even for a
 * German reader who knows English.
 *
 * ### The boundary bug the old code hid
 *
 * The sectors were closed ranges — `22.5f..67.5f` and `67.5f..112.5f` both contain
 * 67.5 — so every boundary belonged to two sectors and the answer depended on which arm
 * of the `when` ran first. It came out right, by luck, and nothing tested it. The new
 * sectors are half-open and every boundary is checked below.
 */
class QiblaDirectionTest {

    private val languages: List<Pair<String, UiStrings>> = listOf(
        "English" to EnglishStrings,
        "Arabic" to ArabicStrings,
        "French" to FrenchStrings,
        "Indonesian" to IndonesianStrings,
        "Turkish" to TurkishStrings,
        "Urdu" to UrduStrings,
        "Malay" to MalayStrings,
        "Bengali" to BengaliStrings,
        "Russian" to RussianStrings,
        "German" to GermanStrings,
        "Spanish" to SpanishStrings
    )

    private fun reader(): ReaderStrings = EnglishStrings.more.reader

    /**
     * The azimuth each direction is **centred** on, in order from north.
     *
     * Centres, not starts, and that is the shape of an eight-point compass: north
     * covers everything from 337.5 through zero to 22.5, so its sector straddles the
     * wrap rather than starting at it. The *starts* are 22.5, 67.5, 112.5 and so on,
     * which is the boundary list below.
     */
    private val sectorCentres = listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f)

    /** Every sector boundary: eight of them, starting at north-east's lower edge. */
    private val sectorBoundaries =
        listOf(22.5f, 67.5f, 112.5f, 157.5f, 202.5f, 247.5f, 292.5f, 337.5f)

    @Test
    fun `every degree resolves to one of the eight directions, and only those`() {
        val r = reader()
        // Whole degrees across the full circle, plus the half-degrees either side of
        // each boundary, which is where a `when` goes wrong.
        val samples = buildList {
            for (degree in 0..359) {
                add(degree.toFloat())
                add(degree + 0.5f)
            }
            // Either side of every boundary, which is where a `when` goes wrong.
            for (boundary in sectorBoundaries) {
                for (offset in listOf(-0.5f, -0.001f, 0f, 0.001f, 0.5f)) {
                    val value = boundary + offset
                    if (value >= 0f && value < 360f) add(value)
                }
            }
        }

        val allowed = setOf(
            r.cardinalNorth, r.cardinalNorthEast, r.cardinalEast, r.cardinalSouthEast,
            r.cardinalSouth, r.cardinalSouthWest, r.cardinalWest, r.cardinalNorthWest
        )
        assertEquals(
            "the eight direction names collapse into fewer than eight words, so two " +
                "sectors are indistinguishable",
            8,
            allowed.size
        )
        for (sample in samples) {
            val said = r.cardinal(sample)
            assertTrue(
                "\"$sample\" resolved to \"$said\", which is not one of the eight " +
                    "directions",
                said in allowed
            )
            assertTrue("\"$said\" is blank", said.isNotBlank())
        }
    }

    @Test
    fun `a sector boundary belongs to exactly one direction`() {
        // The specific bug: the old sectors were closed ranges, so 67.5 was in both
        // north-east and east and the answer depended on which arm of the `when` ran
        // first. Half-open means a boundary belongs to the sector **above** it.
        //
        // This is what makes the old `in 22.5f..67.5f` form checkable at all: with it,
        // `cardinal(22.5f)` would be whatever the first matching arm said and there
        // would be nothing to compare it against.
        val r = reader()
        for (boundary in sectorBoundaries) {
            val below = r.cardinal(boundary - 0.001f)
            val at = r.cardinal(boundary)
            val above = r.cardinal(boundary + 0.001f)

            assertEquals(
                "the boundary at $boundary reads as \"$at\" but the sector above it " +
                    "reads as \"$above\" - so the boundary belongs to both",
                above,
                at
            )
            assertNotEquals(
                "the boundary at $boundary reads as \"$at\", which is the same as the " +
                    "sector below it (\"$below\") - so the boundary belongs to both",
                at,
                below
            )
            assertNotEquals(
                "the sectors either side of $boundary are the same (\"$below\")",
                below,
                above
            )
        }
    }

    @Test
    fun `north wraps, so either side of zero is north`() {
        val r = reader()
        assertEquals(r.cardinalNorth, r.cardinal(0f))
        assertEquals(r.cardinalNorth, r.cardinal(359.9f))
        assertEquals(r.cardinalNorth, r.cardinal(360f))
        assertEquals(r.cardinalNorth, r.cardinal(-0.1f))
        // A negative azimuth and its 360-complement are the same direction.
        for (degree in listOf(-45f, -90f, -180f, -315f, -359f)) {
            assertEquals(
                "\"$degree\" and its 360-complement resolved differently",
                r.cardinal(degree),
                r.cardinal(degree + 360f)
            )
        }
    }

    @Test
    fun `each direction reads as itself on its own centre`() {
        // Every 45-degree step must give a *different* name from the step before, or
        // two of the eight directions would be one direction.
        val r = reader()
        val spoken = sectorCentres.map { r.cardinal(it) }
        assertEquals(
            "the eight centres do not give eight distinct directions: $spoken",
            8,
            spoken.toSet().size
        )
        // And a point offset from the centre but well inside the sector still reads
        // the same - a sector is 45 degrees wide, so 10 degrees off centre is inside.
        for (centre in sectorCentres) {
            for (offset in listOf(-10f, -5f, 5f, 10f)) {
                val value = ((centre + offset) % 360f + 360f) % 360f
                assertEquals(
                    "${centre}°+${offset}° reads differently from ${centre}°",
                    r.cardinal(centre),
                    r.cardinal(value)
                )
            }
        }
    }

    @Test
    fun `German does not use English's abbreviation for north-east`() {
        // The reason this is a defect and not a gap: German abbreviates *Nordost* to
        // "NO" and *Südost* to "SO", where English writes "NE" and "SE". A shared
        // abbreviation would be wrong for a German reader who reads English.
        val german = GermanStrings.more.reader
        assertNotEquals(
            "German spells north-east the English way",
            englishReader().cardinalNorthEast,
            german.cardinal(45f)
        )
        assertNotEquals(
            "German spells south-east the English way",
            englishReader().cardinalSouthEast,
            german.cardinal(135f)
        )
        // And the things that genuinely agree, so this is not just "everything differs".
        assertEquals(
            "German spells north the same way",
            englishReader().cardinalNorth,
            german.cardinal(0f)
        )
    }

    @Test
    fun `the dial is translated in every language that ships`() {
        for ((language, strings) in languages) {
            val r = strings.more.reader
            for (degree in 0 until 360 step 15) {
                assertTrue(
                    "the $language name for $degree° is blank",
                    r.cardinal(degree.toFloat()).isNotBlank()
                )
            }
            // All eight present and not collapsing into fewer words than exist.
            val all = (0 until 360 step 5).map { r.cardinal(it.toFloat()) }.toSet()
            assertEquals(
                "the $language compass has ${all.size} distinct names for 8 directions",
                8,
                all.size
            )
        }
    }

    private fun englishReader(): ReaderStrings = EnglishStrings.more.reader
}