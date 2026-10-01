package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * The guidance beside a compass.
 *
 * ### The bug this type exists to prevent
 *
 * The reference Qibla screen this app took its dial from checks alignment with
 * `(heading.abs() - qiblaAngle.abs()).abs() < tolerance` - a **linear** difference.
 * Heading 359 and a Qibla of 2 are two degrees apart in the world and 357 apart
 * under that expression, so at the moment of alignment the dial's own banner said
 * "turn right 357°" while the needle pointed straight at the Kaaba.
 *
 * Two fixes were needed and only one is obvious. The fold belongs in the engine -
 * [QiblaEngine.calculateRelativeAngle] does it - but the *result* also has to be
 * something a sentence can be built from, because "not aligned" is not guidance.
 * This is that result.
 */
class QiblaGuidanceTest {

    private fun guidance(heading: Float, qibla: Float) =
        QiblaGuidance.fromRelative(QiblaEngine.calculateRelativeAngle(heading, qibla))

    // --- Folding, the half that was broken --------------------------------

    @Test
    fun `two degrees apart across north is two degrees, not 358`() {
        // The exact case from the reference. 359 and 2 are neighbours going
        // forwards; a linear difference makes them the furthest apart thing on the
        // dial.
        val g = guidance(heading = 359f, qibla = 2f)
        assertTrue("two degrees apart must be aligned", g.isAligned)
    }

    @Test
    fun `the same case the other way round`() {
        assertTrue(guidance(heading = 2f, qibla = 359f).isAligned)
    }

    @Test
    fun `a real turn across north still reports the short way`() {
        // Heading 355, Qibla 5: ten degrees clockwise the short way. Not aligned,
        // and to the right - not "357 degrees to the left".
        val g = guidance(heading = 355f, qibla = 5f)
        assertFalse(g.isAligned)
        assertEquals(QiblaGuidance.Direction.RIGHT, g.direction)
        assertEquals(10, g.degrees)
    }

    @Test
    fun `a real turn across north the other way`() {
        val g = guidance(heading = 5f, qibla = 355f)
        assertEquals(QiblaGuidance.Direction.LEFT, g.direction)
        assertEquals(10, g.degrees)
    }

    // --- Direction -------------------------------------------------------

    @Test
    fun `a target to the right asks for a right turn`() {
        val g = guidance(heading = 0f, qibla = 45f)
        assertEquals(QiblaGuidance.Direction.RIGHT, g.direction)
        assertEquals(45, g.degrees)
    }

    @Test
    fun `a target to the left asks for a left turn`() {
        val g = guidance(heading = 0f, qibla = 315f)
        assertEquals(QiblaGuidance.Direction.LEFT, g.direction)
        assertEquals(45, g.degrees)
    }

    @Test
    fun `a target behind is a turn, not a shrug`() {
        // Directly behind: 180 either way, and the sign is what picks the side.
        // A reader who has to guess which way to turn from a needle pointing down
        // the screen is doing the work the app should be doing.
        val g = guidance(heading = 0f, qibla = 180f)
        assertFalse(g.isAligned)
        assertEquals(QiblaGuidance.Direction.RIGHT, g.direction)
        assertEquals(180, g.degrees)
    }

    // --- Magnitude -------------------------------------------------------

    @Test
    fun `the magnitude is the whole number of degrees, rounded`() {
        // A reader turns in whole degrees; a fraction of a degree of instruction is
        // noise, and rounding the *same* way here and in the sentence it builds is
        // what stops the banner and the needle disagreeing by one.
        assertEquals(12, guidance(0f, 12.4f).degrees)
        assertEquals(13, guidance(0f, 12.6f).degrees)
    }

    @Test
    fun `the magnitude grows monotonically as the reader turns away`() {
        // Stepped in whole degrees on purpose: `var heading = 90f; heading += 3f`
        // accumulates float error and a sweep whose endpoints do not land on the
        // values it asserts is a sweep that fails for reasons that have nothing to
        // do with the folding.
        //
        // Heading starts on the target and walks past it: at 90 the two are the
        // same bearing and the instruction is zero, and every step after that is a
        // reader further from facing the Kaaba and owed a bigger turn.
        var previous = 0
        for (heading in 90..180 step 3) {
            val degrees = guidance(heading.toFloat(), 90f).degrees
            assertTrue(
                "turning away to $heading reported $degrees, not more than $previous",
                degrees >= previous
            )
            previous = degrees
        }
        assertEquals("a quarter turn away should read as a quarter turn", 90, previous)
    }

    @Test
    fun `the magnitude falls as the reader turns towards the target`() {
        // The other half of the monotonic claim, and the one a reader actually
        // experiences: correcting a 90-degree error in 3-degree steps must produce a
        // decreasing series, ending at zero. A fold that got the sign wrong would
        // make this climb, and a fold that got the direction wrong would make it
        // jump to near 360 on the way in.
        var previous = Int.MAX_VALUE
        for (heading in 0..90 step 3) {
            val degrees = guidance(heading.toFloat(), 90f).degrees
            assertTrue(
                "turning towards $heading reported $degrees, not less than $previous",
                degrees <= previous
            )
            previous = degrees
        }
        assertEquals("arriving at the target should read as no turn", 0, previous)
    }

    @Test
    fun `the magnitude is the same from either side`() {
        assertEquals(
            "twenty degrees left and twenty degrees right should read the same",
            guidance(0f, 20f).degrees,
            guidance(0f, 340f).degrees
        )
    }

    // --- The window ------------------------------------------------------

    @Test
    fun `the alignment window is as wide as it says it is`() {
        val tolerance = QiblaGuidance.TOLERANCE_DEGREES
        // Just outside is not aligned, and by exactly one degree.
        val outside = guidance(0f, tolerance + 0.6f)
        assertFalse(outside.isAligned)
        assertEquals(tolerance + 1, outside.degrees)

        // Just inside is aligned.
        assertTrue(guidance(0f, tolerance - 0.4f).isAligned)
        assertEquals(0, guidance(0f, tolerance - 0.4f).degrees)
    }

    @Test
    fun `an aligned reading reports no degrees to turn`() {
        // "Turn left 0°" is a sentence nobody should ever read.
        val g = guidance(heading = 120f, qibla = 121f)
        assertTrue(g.isAligned)
        assertEquals(0, g.degrees)
        assertEquals(QiblaGuidance.Direction.ON_TARGET, g.direction)
    }

    @Test
    fun `the window is wide enough to survive a standing-still jitter`() {
        // A compass reading jitters by a degree or two with nobody moving. If the
        // window were tighter than that, the aligned state would flicker and a
        // reader would correct past the target and back.
        assertTrue(
            "a ${QiblaGuidance.TOLERANCE_DEGREES}° window is narrower than 3° of jitter",
            QiblaGuidance.TOLERANCE_DEGREES >= 3
        )
    }

    // --- Every heading ----------------------------------------------------

    @Test
    fun `every heading produces a definite answer`() {
        // Swept rather than asserted case by case: a heading that produced no
        // direction at all, or a direction with no magnitude, would leave the
        // banner with a hole in it for one specific bearing - and the bearings a
        // reader actually stands at are the ones nobody thought to test.
        var heading = 0f
        while (heading < 360f) {
            listOf(0f, 1.5f, 90f, 178f, 181f, 270f, 359f).forEach { qibla ->
                val g = guidance(heading, qibla)
                assertTrue(
                    "heading=$heading qibla=$qibla produced no direction",
                    g.degrees >= 0
                )
                if (g.isAligned) {
                    assertEquals(
                        "heading=$heading qibla=$qibla is aligned but reports a turn",
                        0,
                        g.degrees
                    )
                } else {
                    assertTrue(
                        "heading=$heading qibla=$qibla is not aligned but reports 0°",
                        g.degrees > 0
                    )
                }
            }
            heading += 7f
        }
    }

    @Test
    fun `the guidance agrees with the signed angle it came from`() {
        // The banner and the needle are drawn from the same number, and this is the
        // check that they stay that way: a right turn must correspond to a positive
        // relative angle, or the screen tells a reader to turn the way they are
        // already turning.
        // Stepped with a `for` over whole degrees, and the aligned cases filtered
        // out of the value rather than skipped with `continue`.
        //
        // That is not a style preference. `if (g.isAligned) continue` jumps to the
        // loop condition, which skips the increment at the bottom of the body - so
        // the sweep hung forever on the first aligned reading and the test suite
        // burned fifteen minutes and was then killed. Ten whole degrees apart is
        // also coarser than the three-degree step used elsewhere, and no matter:
        // the sweep is about direction agreeing with sign, not about resolving the
        // exact edge of the window.
        for (step in -17..17) {
            val relative = step * 10f
            val g = QiblaGuidance.fromRelative(relative)
            if (g.isAligned) {
                assertEquals(
                    "relative $relative is inside the window, so it should be aligned",
                    QiblaGuidance.Direction.ON_TARGET,
                    g.direction
                )
                continue
            }
            if (relative > 0f) {
                assertEquals(
                    "relative $relative reported ${g.direction}",
                    QiblaGuidance.Direction.RIGHT,
                    g.direction
                )
            } else {
                assertEquals(
                    "relative $relative reported ${g.direction}",
                    QiblaGuidance.Direction.LEFT,
                    g.direction
                )
            }
        }
    }

    @Test
    fun `the window edges are the only place the two answers could part`() {
        // The sweep above steps in tens, which never lands on the boundary. This is
        // the boundary case, stated once: a relative angle one degree outside the
        // window is a turn, one degree inside is not, and there is no value in
        // between where the answer is neither. A tolerance implemented as `<=` in
        // one place and `<` in another puts exactly such a value in the gap.
        val tolerance = QiblaGuidance.TOLERANCE_DEGREES
        val justOutside = tolerance + 1f
        val justInside = tolerance - 1f

        listOf(justOutside, -justOutside).forEach { angle ->
            assertFalse(
                "$angle should be a turn",
                QiblaGuidance.fromRelative(angle).isAligned
            )
        }
        listOf(justInside, -justInside, 0f).forEach { angle ->
            assertTrue(
                "$angle should be aligned",
                QiblaGuidance.fromRelative(angle).isAligned
            )
        }

        // Every whole degree across the whole window and one past each side, which
        // is the sweep that would have caught a boundary hole.
        for (degrees in -tolerance - 2..tolerance + 2) {
            val inside = abs(degrees) <= tolerance
            assertEquals(
                "$degrees° alignment",
                inside,
                QiblaGuidance.fromRelative(degrees.toFloat()).isAligned
            )
        }
    }
}
