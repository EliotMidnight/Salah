package com.example.data.model

/**
 * How much of the text is on screen at once.
 *
 * - [PER_PAGE] is the real mushaf: the canonical 604-page partition, one page at
 *   a time. A page may begin or end mid-surah, exactly as the printed one does.
 * - [CONTINUOUS] is a whole surah as one unbroken flow, with no page breaks at
 *   all - which is what reading straight through actually feels like.
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
 */
enum class QuranFontFace(
    val key: String,
    val fontRes: Int,
    val lineHeightFactor: Float
) {
    /**
     * Amiri - a Naskh revival by Khaled Hosny, and the app's default.
     */
    AMIRI("amiri", com.example.R.font.quran_amiri, 2.00f),

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
        1.90f
    ),

    /**
     * Lateef - a compact Naskh designed for legibility at small sizes.
     */
    LATEEF("lateef", com.example.R.font.quran_lateef, 1.90f),

    /**
     * Scheherazade New - the modern revival of Scheherazade, SIL's classical Naskh.
     */
    SCHEHERAZADE_NEW(
        "scheherazade_new",
        com.example.R.font.quran_scheherazade_new,
        2.05f
    ),

    /**
     * Harmattan - SIL's Naskh, with the deepest descender in the set.
     */
    HARMATTAN("harmattan", com.example.R.font.quran_harmattan, 2.35f);

    companion object {
        fun fromKey(key: String?): QuranFontFace =
            entries.firstOrNull { it.key == key } ?: AMIRI
    }
}

/**
 * The reader's preferences, as one value.
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


        /** The scale a pinch on the *view* is allowed to reach. */
        val ViewScaleRange = 1.0f..2.2f

        /**
         * Builds options from stored values, migrating anything an older build
         * wrote.
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
