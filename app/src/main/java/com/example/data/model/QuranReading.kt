package com.example.data.model

/**
 * How the text of a passage is presented.
 *
 * Three layouts, because they are three different tasks rather than three
 * preferences:
 *
 * - [PER_AYAH] gives every verse its own block with its actions attached. This
 *   is the working mode: you can read one verse, play it, save it, copy it.
 * - [PER_PAGE] is the real mushaf. It follows the canonical 604-page
 *   partition, so a page boundary is where the printed page actually breaks,
 *   and a page can begin or end mid-surah.
 * - [CONTINUOUS] renders a whole surah as one flowing surface with no breaks
 *   at all, which is what reading straight through actually feels like.
 *
 * [CONTINUOUS] only exists in one scroll axis - see [QuranScrollDirection].
 */
enum class QuranReadingLayout(val key: String) {
    PER_AYAH("per_ayah"),
    PER_PAGE("per_page"),
    CONTINUOUS("continuous");

    companion object {
        fun fromKey(key: String?): QuranReadingLayout =
            entries.firstOrNull { it.key == key } ?: PER_AYAH
    }
}

/**
 * Which way the passage moves under the finger.
 *
 * Horizontal is the mushaf gesture - turn the page. Vertical is the phone
 * gesture - scroll the text. Both are legitimate; neither is the default for
 * everybody, so neither is assumed.
 */
enum class QuranScrollDirection(val key: String) {
    VERTICAL("vertical"),
    HORIZONTAL("horizontal");

    companion object {
        fun fromKey(key: String?): QuranScrollDirection =
            entries.firstOrNull { it.key == key } ?: VERTICAL
    }
}

/**
 * What a two-finger pinch is allowed to change.
 *
 * Scaling the view and resizing the text look identical on screen and are not:
 * one changes the type and the line breaks reflow, the other magnifies
 * everything including the margins and the controls. Conflating them is why
 * pinch in a reading app so often feels broken, so the reader asks.
 */
enum class QuranPinchTarget(val key: String) {
    /** Pinch magnifies the reading surface. Text size is untouched. */
    VIEW_SCALE("view_scale"),

    /** Pinch changes the Quranic text size, and the line rewraps. */
    TEXT_SIZE("text_size");

    companion object {
        fun fromKey(key: String?): QuranPinchTarget =
            entries.firstOrNull { it.key == key } ?: TEXT_SIZE
    }
}

/**
 * The paper the mushaf is read on.
 *
 * Seven hues - the traditional rainbow wheel - held at very low chroma. A
 * saturated background behind 24sp Arabic is a reading hazard, not a theme, so
 * every wash here is a tint close to the page colour and the ink on top is
 * chosen per wash rather than inherited from the app scheme.
 *
 * [DEFAULT] means "no paper of my own": the reader uses the app's own
 * background, so the page still reads as part of the product.
 */
enum class QuranPaperTone(val key: String) {
    DEFAULT("default"),
    RED("red"),
    ORANGE("orange"),
    YELLOW("yellow"),
    GREEN("green"),
    BLUE("blue"),
    INDIGO("indigo"),
    VIOLET("violet");

    val isCustom: Boolean get() = this != DEFAULT

    companion object {
        /** The seven hues, in wheel order, for the colour row in the sheet. */
        val wheel: List<QuranPaperTone> = listOf(
            RED, ORANGE, YELLOW, GREEN, BLUE, INDIGO, VIOLET
        )

        fun fromKey(key: String?): QuranPaperTone =
            entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

/**
 * A Quranic typeface.
 *
 * The five faces the reader offers by name, plus the two that can actually be
 * shipped today. That split is deliberate and is visible in the picker rather
 * than hidden: [fontRes] is null for any face whose own licence does not allow
 * redistribution, and the reader falls back to the app's Arabic serif for those
 * instead of drawing nothing.
 *
 * See `res/font/OFL.txt` for the licence each bundled face ships under and how
 * to enable one you hold permission for.
 *
 * [lineHeightFactor] is per face and not a style constant. Nastaliq descenders
 * alone can eat a third of the line box, and a Naskh face set with a Nastaliq
 * leading looks like a mistake - so each face carries its own multiplier and
 * the reader never shares one value across all of them.
 */
enum class QuranFontFace(
    val key: String,
    val fontRes: Int?,
    val lineHeightFactor: Float,
    /** Baseline nudge, in sp at 100% size, for faces whose marks sit low. */
    val baselineShiftSp: Float = 0f
) {
    /** King Fahd Glorious Quran Printing Complex Uthmanic script. */
    KFGQ("kfgq", null, 1.85f),

    /**
     * MeQuran. Marks sit noticeably below the baseline and need the extra
     * leading to keep neighbouring lines off each other.
     */
    ME_QURAN("me_quran", null, 2.05f, baselineShiftSp = -1f),

    /** Digital Khatt v2. */
    DIGITAL_KHATT("digital_khatt", null, 1.95f),

    /** Naskh Nastaleeq. Nastaliq needs by far the tallest line box of any of these. */
    NASKH_NASTALEEQ("naskh_nastaleeq", null, 2.60f, baselineShiftSp = -2f),

    /** Noorani Quran. */
    NOORANI("noorani", null, 1.95f),

    /** Amiri - Naskh revival, SIL OFL. Bundled. */
    AMIRI("amiri", com.example.R.font.quran_amiri, 2.00f, baselineShiftSp = -0.5f),

    /** Lateef - SIL OFL, compact and highly legible. Bundled. */
    LATEEF("lateef", com.example.R.font.quran_lateef, 1.90f);

    /** True when the face is on disk and can actually be rendered. */
    val isBundled: Boolean get() = fontRes != null

    companion object {
        /** Ships with the app and works immediately. */
        val bundled: List<QuranFontFace> = entries.filter { it.isBundled }

        fun fromKey(key: String?): QuranFontFace =
            entries.firstOrNull { it.key == key } ?: AMIRI
    }
}

/**
 * The reader's preferences, as one value.
 *
 * Grouped so that a change to any of them is a single write and a single
 * recomposition, and so that the invariants below can be enforced in exactly
 * one place instead of at every call site.
 *
 * [arabicScale] and [translationScale] are multiples, not point sizes, and they
 * are clamped to [ArabicScaleRange] / [TranslationScaleRange] on the way in -
 * a preference that can hold a value no slider can express is a preference the
 * user cannot undo.
 */
data class QuranReadingOptions(
    val layout: QuranReadingLayout = QuranReadingLayout.CONTINUOUS,
    val scroll: QuranScrollDirection = QuranScrollDirection.VERTICAL,
    val pinchTarget: QuranPinchTarget = QuranPinchTarget.TEXT_SIZE,
    val paper: QuranPaperTone = QuranPaperTone.DEFAULT,
    val font: QuranFontFace = QuranFontFace.AMIRI,
    val arabicScale: Float = 1f,
    val translationScale: Float = 1f,
    val showTranslation: Boolean = false
) {
    /** True when the current layout is drawn as discrete screens, not a scroll. */
    val isPaged: Boolean
        get() = layout == QuranReadingLayout.PER_PAGE || scroll == QuranScrollDirection.HORIZONTAL

    /**
     * Applies a layout change, repairing anything it makes impossible.
     *
     * Continuous text has no page boundaries and no page turns, so it cannot
     * scroll sideways. Asking for it while horizontal would otherwise leave the
     * reader with a gesture that does nothing - so the axis is pulled back to
     * vertical, and the scroll setting is reported back to the caller so the
     * segmented control shows what actually happened rather than what was asked.
     */
    fun withLayout(next: QuranReadingLayout): QuranReadingOptions {
        if (next != QuranReadingLayout.CONTINUOUS) return copy(layout = next)
        return copy(layout = next, scroll = QuranScrollDirection.VERTICAL)
    }

    /** Applies a scroll change, repairing anything it makes impossible. */
    fun withScroll(next: QuranScrollDirection): QuranReadingOptions = when {
        next == QuranScrollDirection.HORIZONTAL &&
            layout == QuranReadingLayout.CONTINUOUS -> copy(
            // Reached from an illegal state rather than through the controls.
            // Continuous survives by demoting to per-ayah, which is the layout
            // closest to it that can turn pages.
            layout = QuranReadingLayout.PER_AYAH,
            scroll = next
        )

        else -> copy(scroll = next)
    }

    companion object {
        val ArabicScaleRange = 0.7f..2.0f
        val TranslationScaleRange = 0.8f..1.8f

        /** Base size of the Arabic, before [arabicScale]. */
        const val ARABIC_BASE_SP = 24f

        /** Base size of the translation, before [translationScale]. */
        const val TRANSLATION_BASE_SP = 16f

        /** The scale a pinch on the *view* is allowed to reach. */
        val ViewScaleRange = 1.0f..2.2f

        fun normalise(options: QuranReadingOptions): QuranReadingOptions {
            val base = QuranReadingOptions()
            return options.copy(
                arabicScale = options.arabicScale.coerceIn(ArabicScaleRange),
                translationScale = options.translationScale.coerceIn(TranslationScaleRange),
                // Run both repairs so a stored value from an older build cannot
                // restore an illegal combination on launch.
                layout = options.layout.takeIf { it != QuranReadingLayout.CONTINUOUS } ?: base.layout,
                scroll = options.scroll.takeIf {
                    it == QuranScrollDirection.VERTICAL || options.layout != QuranReadingLayout.CONTINUOUS
                } ?: QuranScrollDirection.VERTICAL
            )
        }
    }
}
