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
 * ### Every face here is bundled
 *
 * The previous list offered seven faces and shipped two. The other five - KFGQ,
 * MeQuran, Digital Khatt, Naskh Nastaleeq and Noorani - have licences that do not
 * permit redistribution, so they resolved to a fallback and the picker offered a
 * reader five choices that all drew the same face. A control that looks like a
 * choice and is not one is worse than no control, and it cost the reader a settings
 * screen to discover it.
 *
 * So the list is now **only faces the app actually ships**, all under the SIL Open
 * Font License, which permits redistribution inside an Apache-2.0 application. Five
 * working faces instead of seven with five dead: Amiri, Amiri Quran, Lateef,
 * Scheherazade New and Harmattan.
 *
 * Each was checked for **U+06DD**, the ayah-end ornament, before being added. That
 * codepoint is a standalone ornament rather than a numeric placeholder, and a mushaf
 * whose ayah markers are tofu boxes is not a mushaf. Reem Kufi was considered and
 * rejected on exactly that: a beautiful face that cannot draw the mark.
 *
 * Adding a face later is a two-step change - drop the file into `res/font`, point
 * the entry at it - and needs nothing else. Each face's licence is in
 * `app/src/main/assets/quran_fonts_OFL.txt`.
 *
 * ### [lineHeightFactor] is per face, and not a style constant
 *
 * Nastaliq descenders alone can eat a third of the line box, and a Naskh face set
 * with a Nastaliq leading looks like a mistake. Each face carries its own multiplier
 * and the reader never shares one value across all of them. Harmattan is the extreme
 * case here: it is a Naskh with a famously deep descender, and it needs more room
 * than any other face in the set - which is the reason for the whole mechanism.
 */
enum class QuranFontFace(
    val key: String,
    val fontRes: Int,
    val lineHeightFactor: Float,
    /** Baseline nudge, in sp at 100% size, for faces whose marks sit low. */
    val baselineShiftSp: Float = 0f
) {
    /**
     * Amiri - a Naskh revival by Khaled Hosny, and the app's default.
     *
     * The most traditional of the set and the most widely used for digital mushaf
     * work, which is why it is the default rather than the prettiest.
     */
    AMIRI("amiri", com.example.R.font.quran_amiri, 2.00f, baselineShiftSp = -0.5f),

    /**
     * Amiri Quran - the same revival cut specifically for Quranic text.
     *
     * A different design from plain Amiri rather than a variant of it: the
     * letterforms are drawn to sit correctly at Quranic sizes. It is noticeably more
     * compact vertically, so it needs less leading than plain Amiri.
     */
    AMIRI_QURAN(
        "amiri_quran",
        com.example.R.font.quran_amiri_quran,
        1.90f,
        baselineShiftSp = -0.5f
    ),

    /**
     * Lateef - a compact Naskh designed for legibility at small sizes.
     *
     * The most text-per-page of the set, which is what makes it the right face for
     * a dense page on a small screen.
     */
    LATEEF("lateef", com.example.R.font.quran_lateef, 1.90f),

    /**
     * Scheherazade New - the modern revival of Scheherazade, SIL's classical Naskh.
     *
     * Wider than Amiri at the same size, so it needs slightly more leading and
     * fits fewer words to a line.
     */
    SCHEHERAZADE_NEW(
        "scheherazade_new",
        com.example.R.font.quran_scheherazade_new,
        2.05f
    ),

    /**
     * Harmattan - SIL's Naskh, with the deepest descender in the set.
     *
     * Its marks and descenders reach well below the baseline, and a line box sized
     * for Amiri puts neighbouring lines on top of them. It is the reason
     * [lineHeightFactor] is per face at all.
     */
    HARMATTAN("harmattan", com.example.R.font.quran_harmattan, 2.35f, baselineShiftSp = -1f);

    /**
     * Always true, and that is the point: every face offered is one that can
     * actually be drawn.
     *
     * Kept as a property rather than deleted because a caller asking "can this be
     * rendered" is asking a real question, and the answer being constant is a change
     * worth having.
     */
    val isBundled: Boolean get() = true

    companion object {
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
