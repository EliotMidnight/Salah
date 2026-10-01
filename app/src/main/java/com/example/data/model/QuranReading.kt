package com.example.data.model

/**
 * How much of the text is on screen at once.
 *
 * Exactly two, because these are two genuinely different surfaces and not two
 * preferences:
 *
 * - [PER_PAGE] is the real mushaf: the canonical 604-page partition, one page at
 *   a time. A page may begin or end mid-surah, exactly as the printed one does.
 * - [CONTINUOUS] is a whole surah as one unbroken flow, with no page breaks at
 *   all - which is what reading straight through actually feels like.
 *
 * How a verse is *presented* - as its own selectable unit, or as part of the
 * running text - is not a layout. It is [QuranReadingOptions.perVerse], which
 * applies to either of these, because it is a separate question and answering it
 * with a third layout is what produced "per ayah", "horizontal per page" and
 * every other combination pretending to be its own mode.
 */
enum class QuranReadingLayout(val key: String) {
    PER_PAGE("per_page"),
    CONTINUOUS("continuous");

    companion object {
        fun fromKey(key: String?): QuranReadingLayout =
            entries.firstOrNull { it.key == key } ?: PER_PAGE
    }
}

/**
 * The retired `per_ayah` layout, stored by earlier builds.
 *
 * A per-ayah layout is not a third surface - it is the per-page mushaf with
 * verses broken out - so it migrates to exactly that rather than being dropped
 * on the floor or kept as an entry the reader can still select.
 */
internal const val LegacyPerAyahKey = "per_ayah"

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
 * recomposition, and so that [normalise] has exactly one place to enforce the
 * invariants.
 *
 * ### Four independent answers, not one mode
 *
 * [layout] and [perVerse] decide what is on screen; [scroll] decides which way it
 * moves; [pinchTarget] decides what a pinch means. None of them constrains any
 * other, and there is deliberately no code here that repairs one by changing
 * another. That repair is what made the reader offer "horizontal per page" and
 * "vertical continuous" as though they were modes a reader had to choose between:
 * choosing continuous used to silently drag the axis back to vertical, because
 * the model had decided continuous text *cannot* scroll sideways.
 *
 * It can. It is one wide column you pan across. So there are two layouts, two
 * axes, and every combination is reachable and behaves as itself.
 *
 * [arabicScale] and [translationScale] are multiples, not point sizes, and they
 * are clamped to [ArabicScaleRange] / [TranslationScaleRange] on the way in -
 * a preference that can hold a value no slider can express is a preference the
 * user cannot undo.
 */
data class QuranReadingOptions(
    val layout: QuranReadingLayout = QuranReadingLayout.PER_PAGE,
    /** Break every verse out as its own selectable unit. Works in either layout. */
    val perVerse: Boolean = false,
    val scroll: QuranScrollDirection = QuranScrollDirection.VERTICAL,
    val pinchTarget: QuranPinchTarget = QuranPinchTarget.TEXT_SIZE,
    val paper: QuranPaperTone = QuranPaperTone.DEFAULT,
    val font: QuranFontFace = QuranFontFace.AMIRI,
    val arabicScale: Float = 1f,
    val translationScale: Float = 1f,
    val showTranslation: Boolean = false
) {
    companion object {
        val ArabicScaleRange = 0.7f..2.0f
        val TranslationScaleRange = 0.8f..1.8f

        /** Base size of the Arabic, before [arabicScale]. */
        const val ARABIC_BASE_SP = 24f

        /** Base size of the translation, before [translationScale]. */
        const val TRANSLATION_BASE_SP = 16f

        /** The scale a pinch on the *view* is allowed to reach. */
        val ViewScaleRange = 1.0f..2.2f

        /**
         * Builds options from stored values, migrating anything an older build
         * wrote.
         *
         * Two migrations, both one-way:
         *
         * - a stored `per_ayah` layout becomes the per-page mushaf with
         *   [perVerse] on, which is what that layout was drawing;
         * - the scales are re-clamped, so a value that a previous build could
         *   write but this one cannot express cannot survive a reinstall.
         */
        fun normalise(
            options: QuranReadingOptions,
            legacyLayoutKey: String? = null
        ): QuranReadingOptions = options.copy(
            layout = if (legacyLayoutKey == LegacyPerAyahKey) {
                QuranReadingLayout.PER_PAGE
            } else {
                options.layout
            },
            perVerse = options.perVerse || legacyLayoutKey == LegacyPerAyahKey,
            arabicScale = options.arabicScale.coerceIn(ArabicScaleRange),
            translationScale = options.translationScale.coerceIn(TranslationScaleRange)
        )
    }
}
