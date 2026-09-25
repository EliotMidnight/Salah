package com.example.data.quran

import com.example.data.model.Ayah
import com.example.data.model.RevelationType
import com.example.data.model.Surah

object QuranDataSource {

    val SURAHS: List<Surah> = listOf(
        Surah(1, "الفاتحة", "Al-Fatihah", "The Opener", 7, RevelationType.MECCAN, 1),
        Surah(2, "البقرة", "Al-Baqarah", "The Cow", 286, RevelationType.MEDINAN, 2),
        Surah(3, "آل عمران", "Ali 'Imran", "Family of Imran", 200, RevelationType.MEDINAN, 50),
        Surah(4, "النساء", "An-Nisa", "The Women", 176, RevelationType.MEDINAN, 77),
        Surah(5, "المائدة", "Al-Ma'idah", "The Table Spread", 120, RevelationType.MEDINAN, 106),
        Surah(6, "الأنعام", "Al-An'am", "The Cattle", 165, RevelationType.MECCAN, 128),
        Surah(7, "الأعراف", "Al-A'raf", "The Heights", 206, RevelationType.MECCAN, 151),
        Surah(8, "الأنفال", "Al-Anfal", "The Spoils of War", 75, RevelationType.MEDINAN, 177),
        Surah(9, "التوبة", "At-Tawbah", "The Repentance", 129, RevelationType.MEDINAN, 187),
        Surah(10, "يونس", "Yunus", "Jonah", 109, RevelationType.MECCAN, 208),
        Surah(11, "هود", "Hud", "Hud", 123, RevelationType.MECCAN, 221),
        Surah(12, "يوسف", "Yusuf", "Joseph", 111, RevelationType.MECCAN, 235),
        Surah(13, "الرعد", "Ar-Ra'd", "The Thunder", 43, RevelationType.MEDINAN, 249),
        Surah(14, "إبراهيم", "Ibrahim", "Abraham", 52, RevelationType.MECCAN, 255),
        Surah(15, "الحجر", "Al-Hijr", "The Rocky Tract", 99, RevelationType.MECCAN, 262),
        Surah(16, "النحل", "An-Nahl", "The Bee", 128, RevelationType.MECCAN, 267),
        Surah(17, "الإسراء", "Al-Isra", "The Night Journey", 111, RevelationType.MECCAN, 282),
        Surah(18, "الكهف", "Al-Kahf", "The Cave", 110, RevelationType.MECCAN, 293),
        Surah(19, "مريم", "Maryam", "Mary", 98, RevelationType.MECCAN, 305),
        Surah(20, "طه", "Taha", "Ta-Ha", 135, RevelationType.MECCAN, 312),
        Surah(21, "الأنبياء", "Al-Anbiya", "The Prophets", 112, RevelationType.MECCAN, 322),
        Surah(22, "الحج", "Al-Hajj", "The Pilgrimage", 78, RevelationType.MEDINAN, 332),
        Surah(23, "المؤمنون", "Al-Mu'minun", "The Believers", 118, RevelationType.MECCAN, 342),
        Surah(24, "النور", "An-Nur", "The Light", 64, RevelationType.MEDINAN, 350),
        Surah(25, "الفرقان", "Al-Furqan", "The Criterion", 77, RevelationType.MECCAN, 359),
        Surah(26, "الشعراء", "Ash-Shu'ara", "The Poets", 227, RevelationType.MECCAN, 367),
        Surah(27, "النمل", "An-Naml", "The Ant", 93, RevelationType.MECCAN, 377),
        Surah(28, "القصص", "Al-Qasas", "The Stories", 88, RevelationType.MECCAN, 385),
        Surah(29, "العنكبوت", "Al-'Ankabut", "The Spider", 69, RevelationType.MECCAN, 396),
        Surah(30, "الروم", "Ar-Rum", "The Romans", 60, RevelationType.MECCAN, 404),
        Surah(31, "لقمان", "Luqman", "Luqman", 34, RevelationType.MECCAN, 411),
        Surah(32, "السجدة", "As-Sajdah", "The Prostration", 30, RevelationType.MECCAN, 415),
        Surah(33, "الأحزاب", "Al-Ahzab", "The Combined Forces", 73, RevelationType.MEDINAN, 418),
        Surah(34, "سبأ", "Saba", "Sheba", 54, RevelationType.MECCAN, 428),
        Surah(35, "فاطر", "Fatir", "Originator", 45, RevelationType.MECCAN, 434),
        Surah(36, "يس", "Ya-Sin", "Ya-Sin", 83, RevelationType.MECCAN, 440),
        Surah(37, "الصافات", "As-Saffat", "Those Who Set The Ranks", 182, RevelationType.MECCAN, 446),
        Surah(38, "ص", "Sad", "The Letter Sad", 88, RevelationType.MECCAN, 453),
        Surah(39, "الزمر", "Az-Zumar", "The Troops", 75, RevelationType.MECCAN, 458),
        Surah(40, "غافر", "Ghafir", "The Forgiver", 85, RevelationType.MECCAN, 467),
        Surah(41, "فصلت", "Fussilat", "Explained in Detail", 54, RevelationType.MECCAN, 477),
        Surah(42, "الشورى", "Ash-Shura", "The Consultation", 53, RevelationType.MECCAN, 483),
        Surah(43, "الزخرف", "Az-Zukhruf", "The Ornaments of Gold", 89, RevelationType.MECCAN, 489),
        Surah(44, "الدخان", "Ad-Dukhan", "The Smoke", 59, RevelationType.MECCAN, 496),
        Surah(45, "الجاثية", "Al-Jathiyah", "The Crouching", 37, RevelationType.MECCAN, 499),
        Surah(46, "الأحقاف", "Al-Ahqaf", "The Wind-Curved Sandhills", 35, RevelationType.MECCAN, 502),
        Surah(47, "محمد", "Muhammad", "Muhammad", 38, RevelationType.MEDINAN, 507),
        Surah(48, "الفتح", "Al-Fath", "The Victory", 29, RevelationType.MEDINAN, 511),
        Surah(49, "الحجرات", "Al-Hujurat", "The Rooms", 18, RevelationType.MEDINAN, 515),
        Surah(50, "ق", "Qaf", "The Letter Qaf", 45, RevelationType.MECCAN, 518),
        Surah(51, "الذاريات", "Adh-Dhariyat", "The Winnowing Winds", 60, RevelationType.MECCAN, 520),
        Surah(52, "الطور", "At-Tur", "The Mount", 49, RevelationType.MECCAN, 523),
        Surah(53, "النجم", "An-Najm", "The Star", 62, RevelationType.MECCAN, 526),
        Surah(54, "القمر", "Al-Qamar", "The Moon", 55, RevelationType.MECCAN, 528),
        Surah(55, "الرحمن", "Ar-Rahman", "The Beneficent", 78, RevelationType.MEDINAN, 531),
        Surah(56, "الواقعة", "Al-Waqi'ah", "The Inevitable", 96, RevelationType.MECCAN, 534),
        Surah(57, "الحديد", "Al-Hadid", "The Iron", 29, RevelationType.MEDINAN, 537),
        Surah(58, "المجادلة", "Al-Mujadila", "The Pleading Woman", 22, RevelationType.MEDINAN, 542),
        Surah(59, "الحشر", "Al-Hashr", "The Exile", 24, RevelationType.MEDINAN, 545),
        Surah(60, "الممتحنة", "Al-Mumtahanah", "She That Is To Be Examined", 13, RevelationType.MEDINAN, 549),
        Surah(61, "الصف", "As-Saff", "The Ranks", 14, RevelationType.MEDINAN, 551),
        Surah(62, "الجمعة", "Al-Jumu'ah", "Friday", 11, RevelationType.MEDINAN, 553),
        Surah(63, "المنافقون", "Al-Munafiqun", "The Hypocrites", 11, RevelationType.MEDINAN, 554),
        Surah(64, "التغابن", "At-Taghabun", "Mutual Disillusion", 18, RevelationType.MEDINAN, 556),
        Surah(65, "الطلاق", "At-Talaq", "The Divorce", 12, RevelationType.MEDINAN, 558),
        Surah(66, "التحريم", "At-Tahrim", "The Prohibition", 12, RevelationType.MEDINAN, 560),
        Surah(67, "الملك", "Al-Mulk", "The Sovereignty", 30, RevelationType.MECCAN, 562),
        Surah(68, "القلم", "Al-Qalam", "The Pen", 52, RevelationType.MECCAN, 564),
        Surah(69, "الحاقة", "Al-Haqqah", "The Reality", 52, RevelationType.MECCAN, 566),
        Surah(70, "المعارج", "Al-Ma'arij", "The Ascending Stairways", 44, RevelationType.MECCAN, 568),
        Surah(71, "نوح", "Nuh", "Noah", 28, RevelationType.MECCAN, 570),
        Surah(72, "الجن", "Al-Jinn", "The Jinn", 28, RevelationType.MECCAN, 572),
        Surah(73, "المزمل", "Al-Muzzammil", "The Enshrouded One", 20, RevelationType.MECCAN, 574),
        Surah(74, "المدثر", "Al-Muddaththir", "The Cloaked One", 56, RevelationType.MECCAN, 575),
        Surah(75, "القيامة", "Al-Qiyamah", "The Resurrection", 40, RevelationType.MECCAN, 577),
        Surah(76, "الإنسان", "Al-Insan", "Man", 31, RevelationType.MEDINAN, 578),
        Surah(77, "المرسلات", "Al-Mursalat", "The Emissaries", 50, RevelationType.MECCAN, 580),
        Surah(78, "النبأ", "An-Naba", "The Tidings", 40, RevelationType.MECCAN, 582),
        Surah(79, "النازعات", "An-Nazi'at", "Those Who Drag Forth", 46, RevelationType.MECCAN, 583),
        Surah(80, "عبس", "'Abasa", "He Frowned", 42, RevelationType.MECCAN, 585),
        Surah(81, "التكوير", "At-Takwir", "The Overthrowing", 29, RevelationType.MECCAN, 586),
        Surah(82, "الانفطار", "Al-Infitar", "The Cleaving", 19, RevelationType.MECCAN, 587),
        Surah(83, "المطففين", "Al-Mutaffifin", "Defrauding", 36, RevelationType.MECCAN, 587),
        Surah(84, "الانشقاق", "Al-Inshiqaq", "The Splitting Open", 25, RevelationType.MECCAN, 589),
        Surah(85, "البروج", "Al-Buruj", "The Mansions of the Stars", 22, RevelationType.MECCAN, 590),
        Surah(86, "الطارق", "At-Tariq", "The Morning Star", 17, RevelationType.MECCAN, 591),
        Surah(87, "الأعلى", "Al-A'la", "The Most High", 19, RevelationType.MECCAN, 591),
        Surah(88, "الغاشية", "Al-Ghashiyah", "The Overwhelming", 26, RevelationType.MECCAN, 592),
        Surah(89, "الفجر", "Al-Fajr", "The Dawn", 30, RevelationType.MECCAN, 593),
        Surah(90, "البلد", "Al-Balad", "The City", 20, RevelationType.MECCAN, 594),
        Surah(91, "الشمس", "Ash-Shams", "The Sun", 15, RevelationType.MECCAN, 595),
        Surah(92, "الليل", "Al-Layl", "The Night", 21, RevelationType.MECCAN, 595),
        Surah(93, "الضحى", "Ad-Duha", "The Morning Hours", 11, RevelationType.MECCAN, 596),
        Surah(94, "الشرح", "Ash-Sharh", "The Relief", 8, RevelationType.MECCAN, 596),
        Surah(95, "التين", "At-Tin", "The Fig", 8, RevelationType.MECCAN, 597),
        Surah(96, "العلق", "Al-'Alaq", "The Clot", 19, RevelationType.MECCAN, 597),
        Surah(97, "القدر", "Al-Qadr", "The Power", 5, RevelationType.MECCAN, 598),
        Surah(98, "البينة", "Al-Bayyinah", "The Clear Proof", 8, RevelationType.MEDINAN, 598),
        Surah(99, "الزلزلة", "Az-Zalzalah", "The Earthquake", 8, RevelationType.MEDINAN, 599),
        Surah(100, "العاديات", "Al-'Adiyat", "The Courser", 11, RevelationType.MECCAN, 599),
        Surah(101, "القارعة", "Al-Qari'ah", "The Calamity", 11, RevelationType.MECCAN, 600),
        Surah(102, "التكاثر", "At-Takathur", "The Rivalry in World Increase", 8, RevelationType.MECCAN, 600),
        Surah(103, "العصر", "Al-'Asr", "The Declining Day", 3, RevelationType.MECCAN, 601),
        Surah(104, "الهمزة", "Al-Humazah", "The Traducer", 9, RevelationType.MECCAN, 601),
        Surah(105, "الفيل", "Al-Fil", "The Elephant", 5, RevelationType.MECCAN, 601),
        Surah(106, "قريش", "Quraysh", "Quraysh", 4, RevelationType.MECCAN, 602),
        Surah(107, "الماعون", "Al-Ma'un", "The Small Kindnesses", 7, RevelationType.MECCAN, 602),
        Surah(108, "الكوثر", "Al-Kawthar", "Abundance", 3, RevelationType.MECCAN, 602),
        Surah(109, "الكافرون", "Al-Kafirun", "The Disbelievers", 6, RevelationType.MECCAN, 603),
        Surah(110, "النصر", "An-Nasr", "The Divine Support", 3, RevelationType.MEDINAN, 603),
        Surah(111, "المسد", "Al-Masad", "The Palm Fiber", 5, RevelationType.MECCAN, 603),
        Surah(112, "الإخلاص", "Al-Ikhlas", "The Sincerity", 4, RevelationType.MECCAN, 604),
        Surah(113, "الفلق", "Al-Falaq", "The Daybreak", 5, RevelationType.MECCAN, 604),
        Surah(114, "الناس", "An-Nas", "Mankind", 6, RevelationType.MECCAN, 604)
    )

    fun getSurahByNumber(number: Int): Surah? = SURAHS.find { it.number == number }

    /**
     * Full verified Arabic text (Tanzil Uthmani) with Saheeh International
     * English for every verse. Loaded once from bundled resources.
     */
    fun getAyahsForSurah(surahNumber: Int): List<Ayah> {
        return QuranCorpus.ayahs.filter { it.surahNumber == surahNumber }
    }

    fun getAyahsForPage(pageNumber: Int): List<Ayah> {
        val page = pageNumber.coerceIn(1, 604)
        return QuranCorpus.ayahs.filter { it.pageNumber == page }
    }

    /** First verse on each mushaf page (1..604), built once from corpus order. */
    private val pageStarts: Map<Int, Ayah> by lazy {
        val starts = HashMap<Int, Ayah>(604)
        for (ayah in QuranCorpus.ayahs) {
            starts.putIfAbsent(ayah.pageNumber, ayah)
        }
        starts
    }

    /** First verse on a mushaf page (canonical page start). */
    fun firstAyahOnPage(pageNumber: Int): Ayah? =
        pageStarts[pageNumber.coerceIn(1, 604)]

    /** Surah that contains the start of a mushaf page. */
    fun surahForPage(pageNumber: Int): Surah? =
        firstAyahOnPage(pageNumber)?.let { getSurahByNumber(it.surahNumber) }

    fun getAyahsForJuz(juzNumber: Int): List<Ayah> {
        val juz = juzNumber.coerceIn(1, 30)
        return QuranCorpus.ayahs.filter { it.juzNumber == juz }
    }

    private val juzStarts: Map<Int, Ayah> by lazy {
        val starts = HashMap<Int, Ayah>(30)
        for (ayah in QuranCorpus.ayahs) {
            starts.putIfAbsent(ayah.juzNumber, ayah)
        }
        starts
    }

    /** First verse of a Juz'. */
    fun firstAyahForJuz(juzNumber: Int): Ayah? =
        juzStarts[juzNumber.coerceIn(1, 30)]

    private val hizbStarts: Map<Int, Ayah> by lazy {
        val starts = HashMap<Int, Ayah>(60)
        for (ayah in QuranCorpus.ayahs) {
            val hizb = ((ayah.hizbQuarter - 1) / 4) + 1
            starts.putIfAbsent(hizb, ayah)
        }
        starts
    }

    /**
     * First verse of a Hizb (1..60). Corpus stores hizb-quarters (1..240);
     * four quarters make one hizb.
     */
    fun firstAyahForHizb(hizbNumber: Int): Ayah? =
        hizbStarts[hizbNumber.coerceIn(1, 60)]

    /** Resolves a mushaf page to the surah + ayah the reader should open at. */
    fun resolvePage(pageNumber: Int): Pair<Surah, Int>? {
        val start = firstAyahOnPage(pageNumber) ?: return null
        val surah = getSurahByNumber(start.surahNumber) ?: return null
        return surah to start.ayahNumber
    }

    /**
     * Resolves a single verse reference from the verified corpus, or null when
     * the reference does not exist. Used to display bookmarks from live text.
     */
    fun resolveAyah(surahNumber: Int, ayahNumber: Int): Ayah? {
        val surah = getSurahByNumber(surahNumber) ?: return null
        if (ayahNumber < 1 || ayahNumber > surah.totalVerses) return null
        return QuranCorpus.ayahs.firstOrNull {
            it.surahNumber == surahNumber && it.ayahNumber == ayahNumber
        }
    }

    /**
     * Converts an integer to Eastern Arabic-Indic numerals (٠-٩) for
     * authentic Uthmani ayah-end markers.
     */
    fun toArabicDigits(number: Int): String {
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        return number.toString().map { arabicDigits[it - '0'] }.joinToString("")
    }

    /**
     * Normalizes Arabic text by removing tashkeel (diacritics), tatweel, and
     * normalizing alif/hamza for seamless full-text offline search.
     */
    fun normalizeArabic(text: String): String {
        return text
            // Remove Harakat (Tashkeel)
            .replace(Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]"), "")
            // Remove Tatweel / Kashida
            .replace("\u0640", "")
            // Normalize Alif forms (أ, إ, آ, ٱ -> ا)
            .replace(Regex("[\\u0622\\u0623\\u0625\\u0671]"), "\u0627")
            // Normalize Taa Marbuta (ة -> ه)
            .replace("\u0629", "\u0647")
            // Normalize Yaa (ى -> ي)
            .replace("\u0649", "\u064A")
            .trim()
    }

    /**
     * Offline search across surah names, Arabic text (diacritics-insensitive)
     * and the English translation.
     */
    fun searchAyahs(query: String): List<Ayah> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val normalizedQuery = normalizeArabic(trimmed).lowercase(java.util.Locale.ROOT)
        val lowerQuery = trimmed.lowercase(java.util.Locale.ROOT)
        return QuranCorpus.ayahs.filter { ayah ->
            val surah = getSurahByNumber(ayah.surahNumber)
            normalizeArabic(ayah.textArabic).lowercase(java.util.Locale.ROOT).contains(normalizedQuery) ||
                ayah.textEnglish.lowercase(java.util.Locale.ROOT).contains(lowerQuery) ||
                (surah != null && (
                    surah.englishName.contains(trimmed, ignoreCase = true) ||
                        surah.arabicName.contains(trimmed) ||
                        surah.englishTranslation.contains(trimmed, ignoreCase = true)
                    ))
        }
    }
}
