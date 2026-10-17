package com.example.data.quran

import com.example.data.model.RevelationType
import com.example.data.model.Surah

/**
 * The 114 surahs: curated names over facts read out of the bundled corpus.
 *
 * Two kinds of column hide behind this one list, and their relationship to the
 * truth is deliberately different:
 *
 * - The **names** are the app's own writing. Translated names are judgement,
 *   not fact, and even the Arabic spelling is a choice - Ibrahim is
 *   ابراهيم in the corpus's raw text and إبراهيم here, a hamza on the
 *   alif, and neither is wrong.
 * - The **facts** - how many verses, where revealed, which page the surah
 *   opens on - belong to [QuranCorpus]. The row literal this object used to be
 *   typed each count, each place and each page by hand as well, and every one
 *   of those was a second copy of a fact the corpus had already decided - the
 *   one list could announce values the book this very app ships contradicts.
 *   The facts are now taken at construction, so the table cannot say otherwise.
 *
 * The one check that survives as an exception to "names are ours": the Arabic
 * name is compared against the corpus's, folded, at construction. Folding
 * forgives the hamza spellings and does not turn a different *word* into
 * agreement - a genuinely different name still throws.
 */
object QuranDataSource {

    /**
     * The names, per surah, in surah order.
     *
     * Positional: a row's number is its position plus one, so "numbered 1..114
     * without a gap" is one fact rather than one hundred fourteen - the count
     * is pinned against [QuranCorpus.SURA_COUNT] when the rows are compiled,
     * and a names table that lost or shuffled a row fails there instead of
     * showing a reader a shifted book.
     */
    private val NAMES: List<SurahNames> = listOf(
        SurahNames("الفاتحة", "Al-Fatihah", "The Opener"),
        SurahNames("البقرة", "Al-Baqarah", "The Cow"),
        SurahNames("آل عمران", "Ali 'Imran", "Family of Imran"),
        SurahNames("النساء", "An-Nisa", "The Women"),
        SurahNames("المائدة", "Al-Ma'idah", "The Table Spread"),
        SurahNames("الأنعام", "Al-An'am", "The Cattle"),
        SurahNames("الأعراف", "Al-A'raf", "The Heights"),
        SurahNames("الأنفال", "Al-Anfal", "The Spoils of War"),
        SurahNames("التوبة", "At-Tawbah", "The Repentance"),
        SurahNames("يونس", "Yunus", "Jonah"),
        SurahNames("هود", "Hud", "Hud"),
        SurahNames("يوسف", "Yusuf", "Joseph"),
        SurahNames("الرعد", "Ar-Ra'd", "The Thunder"),
        SurahNames("إبراهيم", "Ibrahim", "Abraham"),
        SurahNames("الحجر", "Al-Hijr", "The Rocky Tract"),
        SurahNames("النحل", "An-Nahl", "The Bee"),
        SurahNames("الإسراء", "Al-Isra", "The Night Journey"),
        SurahNames("الكهف", "Al-Kahf", "The Cave"),
        SurahNames("مريم", "Maryam", "Mary"),
        SurahNames("طه", "Taha", "Ta-Ha"),
        SurahNames("الأنبياء", "Al-Anbiya", "The Prophets"),
        SurahNames("الحج", "Al-Hajj", "The Pilgrimage"),
        SurahNames("المؤمنون", "Al-Mu'minun", "The Believers"),
        SurahNames("النور", "An-Nur", "The Light"),
        SurahNames("الفرقان", "Al-Furqan", "The Criterion"),
        SurahNames("الشعراء", "Ash-Shu'ara", "The Poets"),
        SurahNames("النمل", "An-Naml", "The Ant"),
        SurahNames("القصص", "Al-Qasas", "The Stories"),
        SurahNames("العنكبوت", "Al-'Ankabut", "The Spider"),
        SurahNames("الروم", "Ar-Rum", "The Romans"),
        SurahNames("لقمان", "Luqman", "Luqman"),
        SurahNames("السجدة", "As-Sajdah", "The Prostration"),
        SurahNames("الأحزاب", "Al-Ahzab", "The Combined Forces"),
        SurahNames("سبأ", "Saba", "Sheba"),
        SurahNames("فاطر", "Fatir", "Originator"),
        SurahNames("يس", "Ya-Sin", "Ya-Sin"),
        SurahNames("الصافات", "As-Saffat", "Those Who Set The Ranks"),
        SurahNames("ص", "Sad", "The Letter Sad"),
        SurahNames("الزمر", "Az-Zumar", "The Troops"),
        SurahNames("غافر", "Ghafir", "The Forgiver"),
        SurahNames("فصلت", "Fussilat", "Explained in Detail"),
        SurahNames("الشورى", "Ash-Shura", "The Consultation"),
        SurahNames("الزخرف", "Az-Zukhruf", "The Ornaments of Gold"),
        SurahNames("الدخان", "Ad-Dukhan", "The Smoke"),
        SurahNames("الجاثية", "Al-Jathiyah", "The Crouching"),
        SurahNames("الأحقاف", "Al-Ahqaf", "The Wind-Curved Sandhills"),
        SurahNames("محمد", "Muhammad", "Muhammad"),
        SurahNames("الفتح", "Al-Fath", "The Victory"),
        SurahNames("الحجرات", "Al-Hujurat", "The Rooms"),
        SurahNames("ق", "Qaf", "The Letter Qaf"),
        SurahNames("الذاريات", "Adh-Dhariyat", "The Winnowing Winds"),
        SurahNames("الطور", "At-Tur", "The Mount"),
        SurahNames("النجم", "An-Najm", "The Star"),
        SurahNames("القمر", "Al-Qamar", "The Moon"),
        SurahNames("الرحمن", "Ar-Rahman", "The Beneficent"),
        SurahNames("الواقعة", "Al-Waqi'ah", "The Inevitable"),
        SurahNames("الحديد", "Al-Hadid", "The Iron"),
        SurahNames("المجادلة", "Al-Mujadila", "The Pleading Woman"),
        SurahNames("الحشر", "Al-Hashr", "The Exile"),
        SurahNames("الممتحنة", "Al-Mumtahanah", "She That Is To Be Examined"),
        SurahNames("الصف", "As-Saff", "The Ranks"),
        SurahNames("الجمعة", "Al-Jumu'ah", "Friday"),
        SurahNames("المنافقون", "Al-Munafiqun", "The Hypocrites"),
        SurahNames("التغابن", "At-Taghabun", "Mutual Disillusion"),
        SurahNames("الطلاق", "At-Talaq", "The Divorce"),
        SurahNames("التحريم", "At-Tahrim", "The Prohibition"),
        SurahNames("الملك", "Al-Mulk", "The Sovereignty"),
        SurahNames("القلم", "Al-Qalam", "The Pen"),
        SurahNames("الحاقة", "Al-Haqqah", "The Reality"),
        SurahNames("المعارج", "Al-Ma'arij", "The Ascending Stairways"),
        SurahNames("نوح", "Nuh", "Noah"),
        SurahNames("الجن", "Al-Jinn", "The Jinn"),
        SurahNames("المزمل", "Al-Muzzammil", "The Enshrouded One"),
        SurahNames("المدثر", "Al-Muddaththir", "The Cloaked One"),
        SurahNames("القيامة", "Al-Qiyamah", "The Resurrection"),
        SurahNames("الإنسان", "Al-Insan", "Man"),
        SurahNames("المرسلات", "Al-Mursalat", "The Emissaries"),
        SurahNames("النبأ", "An-Naba", "The Tidings"),
        SurahNames("النازعات", "An-Nazi'at", "Those Who Drag Forth"),
        SurahNames("عبس", "'Abasa", "He Frowned"),
        SurahNames("التكوير", "At-Takwir", "The Overthrowing"),
        SurahNames("الانفطار", "Al-Infitar", "The Cleaving"),
        SurahNames("المطففين", "Al-Mutaffifin", "Defrauding"),
        SurahNames("الانشقاق", "Al-Inshiqaq", "The Splitting Open"),
        SurahNames("البروج", "Al-Buruj", "The Mansions of the Stars"),
        SurahNames("الطارق", "At-Tariq", "The Morning Star"),
        SurahNames("الأعلى", "Al-A'la", "The Most High"),
        SurahNames("الغاشية", "Al-Ghashiyah", "The Overwhelming"),
        SurahNames("الفجر", "Al-Fajr", "The Dawn"),
        SurahNames("البلد", "Al-Balad", "The City"),
        SurahNames("الشمس", "Ash-Shams", "The Sun"),
        SurahNames("الليل", "Al-Layl", "The Night"),
        SurahNames("الضحى", "Ad-Duha", "The Morning Hours"),
        SurahNames("الشرح", "Ash-Sharh", "The Relief"),
        SurahNames("التين", "At-Tin", "The Fig"),
        SurahNames("العلق", "Al-'Alaq", "The Clot"),
        SurahNames("القدر", "Al-Qadr", "The Power"),
        SurahNames("البينة", "Al-Bayyinah", "The Clear Proof"),
        SurahNames("الزلزلة", "Az-Zalzalah", "The Earthquake"),
        SurahNames("العاديات", "Al-'Adiyat", "The Courser"),
        SurahNames("القارعة", "Al-Qari'ah", "The Calamity"),
        SurahNames("التكاثر", "At-Takathur", "The Rivalry in World Increase"),
        SurahNames("العصر", "Al-'Asr", "The Declining Day"),
        SurahNames("الهمزة", "Al-Humazah", "The Traducer"),
        SurahNames("الفيل", "Al-Fil", "The Elephant"),
        SurahNames("قريش", "Quraysh", "Quraysh"),
        SurahNames("الماعون", "Al-Ma'un", "The Small Kindnesses"),
        SurahNames("الكوثر", "Al-Kawthar", "Abundance"),
        SurahNames("الكافرون", "Al-Kafirun", "The Disbelievers"),
        SurahNames("النصر", "An-Nasr", "The Divine Support"),
        SurahNames("المسد", "Al-Masad", "The Palm Fiber"),
        SurahNames("الإخلاص", "Al-Ikhlas", "The Sincerity"),
        SurahNames("الفلق", "Al-Falaq", "The Daybreak"),
        SurahNames("الناس", "An-Nas", "Mankind"),
    )

    /** One names row: the app's Arabic spelling, its English name, its meaning. */
    private data class SurahNames(
        val arabic: String,
        val english: String,
        val translation: String,
    )

    /**
     * The surah rows, compiled once: names from [NAMES], facts from the
     * corpus.
     *
     * Compilation is the price of first access, and first access is where the
     * corpus is parsed - no surface pays it that never opens the Quran.
     */
    val SURAHS: List<Surah> by lazy {
        require(NAMES.size == QuranCorpus.SURA_COUNT) {
            "the names table has " + NAMES.size + " rows, but the corpus has " +
                QuranCorpus.SURA_COUNT + " surahs: a row is missing or one too many"
        }
        NAMES.mapIndexed { index, names ->
            val number = index + 1
            val corpusArabic = QuranCorpus.surahArabicName(number)
            require(
                QuranText.normalise(names.arabic) ==
                    QuranText.normalise(corpusArabic)
            ) {
                "Surah " + number + " is named '" + names.arabic +
                    "', but the corpus's Arabic is '" + corpusArabic + "'"
            }
            Surah(
                number = number,
                arabicName = names.arabic,
                englishName = names.english,
                englishTranslation = names.translation,
                totalVerses = QuranCorpus.surahVerseCount[number],
                revelationType = if (QuranCorpus.surahIsMedinan[number]) {
                    RevelationType.MEDINAN
                } else {
                    RevelationType.MECCAN
                },
                startPage = QuranCorpus.surahStartPage(number),
            )
        }
    }

    fun getSurahByNumber(number: Int): Surah? =
        if (number in 1..SURAHS.size) SURAHS[number - 1] else null

    /**
     * A surah's name in an interface, from the names table, without compiling [SURAHS].
     *
     * [SURAHS] recomposes these rows with the corpus's facts - verse counts, revelation
     * places, start pages - and compiling one parses both text files and verifies their
     * digests. Today's Continue Reading card names a surah on the app's first screen, and
     * a name is not worth the corpus; the strings live in [NAMES], so this reads the same
     * rows the compiled list carries.
     *
     * Null for a number no surah has, so a display site can choose its own empty string
     * rather than a throw on a screen a reader is looking at.
     */
    fun surahName(number: Int, preferArabicName: Boolean): String? =
        NAMES.getOrNull(number - 1)?.let { if (preferArabicName) it.arabic else it.english }

    // -----------------------------------------------------------------------
    //
    // Three `@Deprecated` forwarders lived here - `normalizeArabic`,
    // `toArabicDigits`, `searchAyahs` - and the block above them said they were
    // "removed when the reader and the index sheet are on the new surfaces". Both
    // surfaces are on them, and the last caller had gone, so they were kept by
    // nobody.
    //
    // Worth recording why they were there at all: each was a forwarding call to the
    // rebuilt implementation, never to the old one. The linear filters and the
    // regex-in-a-loop search they replaced are *gone*, not wrapped, because a wrapper
    // around the slow version is a way of continuing to ship it. So while a shim
    // exists, behaviour is the rebuilt behaviour and only the call sites are old -
    // which is what made them safe to delete one at a time rather than all at once.
    // -----------------------------------------------------------------------
}
