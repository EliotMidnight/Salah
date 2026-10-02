package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The reader's own invariants.
 *
 * These are pure functions over the options value, and they are the rules the
 * UI depends on being true:
 *
 * - a pinch cannot leave a size the slider cannot express;
 * - every combination of layout, axis and per-verse is reachable and none of
 *   them repairs another;
 * - the retired `per_ayah` layout survives a reinstall as the thing it was
 *   actually drawing.
 */
class QuranReadingOptionsTest {

    // --- Independence ---------------------------------------------------
    //
    // The whole point of the current model is that these four are independent
    // answers. Each test below changes one and asserts the other three are
    // untouched, because the failure mode being guarded against is a change
    // that quietly drags a second control with it.

    @Test
    fun `every layout keeps the axis it was given`() {
        QuranReadingLayout.entries.forEach { layout ->
            QuranScrollDirection.entries.forEach { scroll ->
                val options = QuranReadingOptions(layout = layout, scroll = scroll)
                assertEquals(layout, options.layout)
                assertEquals(scroll, options.scroll)
            }
        }
    }

    @Test
    fun `continuous text can scroll sideways`() {
        // This combination is the one the old model refused to allow, on the
        // grounds that text with no page breaks cannot scroll sideways. It can:
        // it is one wide column you pan across. If this assertion ever fails,
        // something has reintroduced a rule that rejects a real reading.
        val options = QuranReadingOptions(
            layout = QuranReadingLayout.CONTINUOUS,
            scroll = QuranScrollDirection.HORIZONTAL
        )

        assertEquals(QuranScrollDirection.HORIZONTAL, options.scroll)
        assertEquals(QuranReadingLayout.CONTINUOUS, options.layout)
    }

    @Test
    fun `per verse applies to both layouts independently of the axis`() {
        listOf(QuranReadingLayout.PER_PAGE, QuranReadingLayout.CONTINUOUS).forEach { layout ->
            QuranScrollDirection.entries.forEach { scroll ->
                val options = QuranReadingOptions(
                    layout = layout,
                    perVerse = true,
                    scroll = scroll
                )
                assertEquals(
                    "$layout/$scroll must be able to break verses out",
                    layout,
                    options.layout
                )
                assertTrue(options.perVerse)
                assertEquals(scroll, options.scroll)
            }
        }
    }

    @Test
    fun `all four layout axis and verse combinations are distinct and reachable`() {
        // Guards against the shape of the old model, where some pairs could be
        // expressed but not all, and the ones that could not were repaired into
        // each other by normalise().
        val reachable = QuranReadingLayout.entries.flatMap { layout ->
            QuranScrollDirection.entries.map { scroll -> layout to scroll }
        }

        assertEquals(4, reachable.size)
        assertEquals(
            "each layout must offer both axes exactly once",
            4,
            reachable.toSet().size
        )
        reachable.forEach { (layout, scroll) ->
            val options = QuranReadingOptions.normalise(
                QuranReadingOptions(layout = layout, scroll = scroll)
            )
            assertEquals(
                "normalise must not rewrite a legal combination",
                layout,
                options.layout
            )
            assertEquals(scroll, options.scroll)
        }
    }

    // --- Defaults -------------------------------------------------------

    @Test
    fun `the default is the canonical mushaf, unbroken text, vertical`() {
        val options = QuranReadingOptions()

        assertEquals(QuranReadingLayout.PER_PAGE, options.layout)
        assertFalse(options.perVerse)
        assertEquals(QuranScrollDirection.VERTICAL, options.scroll)
    }

    // --- Legacy migration -----------------------------------------------

    @Test
    fun `the retired per ayah layout migrates to per page with verses broken out`() {
        // This is what `per_ayah` was drawing: the per-page mushaf with each
        // verse as its own unit. Migrating it to per-page + perVerse keeps the
        // reader's appearance rather than resetting the reader to a default.
        val migrated = QuranReadingOptions.normalise(
            QuranReadingOptions(),
            legacyLayoutKey = "per_ayah"
        )

        assertEquals(QuranReadingLayout.PER_PAGE, migrated.layout)
        assertTrue(
            "per_ayah meant broken-out verses, so the flag must come with it",
            migrated.perVerse
        )
    }

    @Test
    fun `a legacy per ayah layout does not disturb the other preferences`() {
        // Only the layout and the verse flag are implied by the old key. A
        // reader who had chosen horizontal continuous text before upgrading had
        // those repaired onto them by the old model; migrating must not silently
        // change their text size, font or paper.
        val before = QuranReadingOptions(
            layout = QuranReadingLayout.CONTINUOUS,
            scroll = QuranScrollDirection.HORIZONTAL,
            arabicScale = 1.4f,
            translationScale = 1.2f,
            paper = QuranPaperTone.GREEN,
            font = QuranFontFace.LATEEF,
            showTranslation = true
        )

        val migrated = QuranReadingOptions.normalise(before, legacyLayoutKey = "per_ayah")

        assertEquals(QuranReadingLayout.PER_PAGE, migrated.layout)
        assertTrue(migrated.perVerse)
        assertEquals(before.scroll, migrated.scroll)
        assertEquals(before.arabicScale, migrated.arabicScale, 0.001f)
        assertEquals(before.translationScale, migrated.translationScale, 0.001f)
        assertEquals(before.paper, migrated.paper)
        assertEquals(before.font, migrated.font)
        assertEquals(before.showTranslation, migrated.showTranslation)
    }

    @Test
    fun `a current layout key leaves the verse flag alone`() {
        // The migration is one-way. Per-page with verses already broken out must
        // not be re-migrated into something else, and a current key must not
        // turn the flag on.
        val off = QuranReadingOptions.normalise(
            QuranReadingOptions(layout = QuranReadingLayout.PER_PAGE, perVerse = false),
            legacyLayoutKey = "per_page"
        )
        assertFalse(off.perVerse)

        // Already migrated in an earlier session: the flag is on and stays on.
        val on = QuranReadingOptions.normalise(
            QuranReadingOptions(layout = QuranReadingLayout.PER_PAGE, perVerse = true),
            legacyLayoutKey = "per_page"
        )
        assertTrue(on.perVerse)
        assertEquals(QuranReadingLayout.PER_PAGE, on.layout)
    }

    @Test
    fun `per verse already true survives the legacy migration idempotently`() {
        val once = QuranReadingOptions.normalise(
            QuranReadingOptions(perVerse = true),
            legacyLayoutKey = "per_ayah"
        )
        val twice = QuranReadingOptions.normalise(once, legacyLayoutKey = "per_ayah")

        assertEquals(once, twice)
    }

    // --- Scale clamping -------------------------------------------------

    @Test
    fun `normalise clamps scales into the range the sliders express`() {
        val wild = QuranReadingOptions(
            arabicScale = 9f,
            translationScale = -4f
        ).let(QuranReadingOptions::normalise)

        assertTrue(wild.arabicScale <= QuranReadingOptions.ArabicScaleRange.endInclusive)
        assertTrue(wild.arabicScale >= QuranReadingOptions.ArabicScaleRange.start)
        assertTrue(wild.translationScale <= QuranReadingOptions.TranslationScaleRange.endInclusive)
        assertTrue(wild.translationScale >= QuranReadingOptions.TranslationScaleRange.start)
    }

    @Test
    fun `normalise does not repair a legal stored combination`() {
        val stored = QuranReadingOptions(
            layout = QuranReadingLayout.CONTINUOUS,
            scroll = QuranScrollDirection.HORIZONTAL,
            perVerse = true
        )

        assertEquals(stored, QuranReadingOptions.normalise(stored))
    }

    // --- Keys -----------------------------------------------------------

    @Test
    fun `every layout key round trips`() {
        QuranReadingLayout.entries.forEach { layout ->
            assertEquals(layout, QuranReadingLayout.fromKey(layout.key))
        }
        assertEquals(
            "an unknown key must not throw",
            QuranReadingLayout.PER_PAGE,
            QuranReadingLayout.fromKey("something-from-the-future")
        )
        assertEquals(QuranReadingLayout.PER_PAGE, QuranReadingLayout.fromKey(null))
    }

    @Test
    fun `the retired per ayah key is not selectable as a layout`() {
        // It must migrate rather than resolve. If `fromKey("per_ayah")` ever
        // returned a real layout, the flag migration in normalise would stop
        // running and the reader would silently lose its per-verse setting.
        assertFalse(QuranReadingLayout.entries.any { it.key == "per_ayah" })
        assertEquals(
            QuranReadingLayout.PER_PAGE,
            QuranReadingLayout.fromKey("per_ayah")
        )
    }

    @Test
    fun `every scroll pinch paper and font key round trips`() {
        QuranScrollDirection.entries.forEach { assertEquals(it, QuranScrollDirection.fromKey(it.key)) }
        QuranPinchTarget.entries.forEach { assertEquals(it, QuranPinchTarget.fromKey(it.key)) }
        QuranPaperTone.entries.forEach { assertEquals(it, QuranPaperTone.fromKey(it.key)) }
        QuranFontFace.entries.forEach { assertEquals(it, QuranFontFace.fromKey(it.key)) }
    }

    @Test
    fun `the two pinch targets stay distinct`() {
        // Resizing text and magnifying the view look the same on screen and are
        // not: one reflows the line breaks, the other does not. A default that
        // collapses them makes the setting meaningless.
        assertFalse(QuranPinchTarget.TEXT_SIZE == QuranPinchTarget.VIEW_SCALE)
        assertEquals(QuranPinchTarget.TEXT_SIZE, QuranPinchTarget.fromKey(null))
    }

    @Test
    fun `magnification and text size are bounded separately`() {
        // Magnification is temporary and view-only; text size is the persisted
        // preference. They must not share a range, or one gesture would silently
        // start writing the other.
        assertFalse(
            QuranReadingOptions.ViewScaleRange == QuranReadingOptions.ArabicScaleRange
        )
        assertEquals(1f, QuranReadingOptions.ViewScaleRange.start, 0.001f)
        assertTrue(
            "magnification that cannot magnify is not a feature",
            QuranReadingOptions.ViewScaleRange.endInclusive > 1f
        )
    }

    // --- Paper and type -------------------------------------------------

    @Test
    fun `the wheel is exactly seven colours plus default`() {
        assertEquals(7, QuranPaperTone.wheel.size)
        assertEquals(7, QuranPaperTone.wheel.distinct().size)
        assertFalse(QuranPaperTone.wheel.contains(QuranPaperTone.DEFAULT))
        assertFalse(QuranPaperTone.DEFAULT.isCustom)
        assertTrue(QuranPaperTone.RED.isCustom)
    }

    @Test
    fun `every face the picker offers is one the app can actually draw`() {
        // The picker used to offer seven faces and ship two. The other five have
        // licences that do not permit redistribution, so they resolved to a fallback
        // and the reader saw five choices that all drew Amiri - a control that looks
        // like a choice and is not one, discoverable only by opening a settings
        // screen and choosing each in turn.
        //
        // So the invariant is now an equality: offering a face and being able to
        // draw it are the same fact. If a future face needs a licence check, the
        // honest answer is to leave it out of the enum rather than to ship a control
        // that lies.
        //
        // The check is that every face has a real font resource. There used to be a
        // `QuranFontFace.isBundled` property - always `true`, by construction - which
        // this test asserted; that assertion could not fail, and the property existed
        // only to be asserted. The resource id is the part that can actually be zero.
        assertTrue(QuranFontFace.entries.isNotEmpty())
        QuranFontFace.entries.forEach { face ->
            assertTrue(
                "${face.key} has no font resource",
                face.fontRes != 0
            )
        }
    }

    @Test
    fun `every face key round trips and no two faces share one`() {
        // A shared key is a silent bug: the last face to be written wins the
        // preference, and a reader who chose the other one gets it changed
        // underneath them by a later save.
        val keys = QuranFontFace.entries.map { it.key }
        assertEquals(
            "two faces share a key",
            keys.size,
            keys.distinct().size
        )
        QuranFontFace.entries.forEach { face ->
            assertEquals(face, QuranFontFace.fromKey(face.key))
        }
    }

    @Test
    fun `the default face is the one the app ships`() {
        assertEquals(QuranFontFace.AMIRI, QuranReadingOptions().font)
    }

    @Test
    fun `every face carries a line height that suits its script`() {
        QuranFontFace.entries.forEach { face ->
            assertTrue(
                "${face.key} needs more than a single line's leading",
                face.lineHeightFactor > 1f
            )
            assertTrue(
                "${face.key} would set lines on top of each other",
                face.lineHeightFactor <= 3f
            )
        }
        // Harmattan has the deepest descender in the set, and it is the reason the
        // multiplier is per face at all: a line box sized for Amiri puts the
        // neighbouring line on top of its descenders.
        assertTrue(
            "Harmattan's descenders need more room than Amiri's",
            QuranFontFace.HARMATTAN.lineHeightFactor > QuranFontFace.AMIRI.lineHeightFactor
        )
        // And Amiri Quran is the compact cut of Amiri, so it needs less.
        assertTrue(
            "Amiri Quran is more compact than Amiri",
            QuranFontFace.AMIRI_QURAN.lineHeightFactor <= QuranFontFace.AMIRI.lineHeightFactor
        )
    }
}
