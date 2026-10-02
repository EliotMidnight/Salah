package com.example.data.quran

import com.example.data.model.RevelationType
import com.example.data.model.Surah

/**
 * The 114 surahs, curated.
 *
 * ### This is the only place surah names are written
 *
 * The reader, the index, the search and the sheets all read this list. It used
 * to be duplicated in spirit: the corpus metadata carries its own transliteration
 * and meaning, and the two were never compared, so a fix applied to one did not
 * reach the other. [assertMatchesCorpus] now checks the four facts the metadata
 * is authoritative for - verse count, start page, revelation place, and the
 * Arabic name - so the curated half can be curated and the factual half cannot
 * quietly go stale.
 *
 * Everything else the reader needs from the corpus is on [QuranBrowse].
 */
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

    fun getSurahByNumber(number: Int): Surah? =
        if (number in 1..SURAHS.size) SURAHS[number - 1] else null

    // -----------------------------------------------------------------------
    // The bridge that used to be here is gone.
    //
    // Three `@Deprecated` forwarders lived here — `normalizeArabic`,
    // `toArabicDigits`, `searchAyahs` — and the block above them said they were
    // "removed when the reader and the index sheet are on the new surfaces". Both
    // surfaces are on them, and the last caller had gone, so they were kept by
    // nobody.
    //
    // Worth recording why they were there at all: each was a forwarding call to the
    // rebuilt implementation, never to the old one. The linear filters and the
    // regex-in-a-loop search they replaced are *gone*, not wrapped, because a wrapper
    // around the slow version is a way of continuing to ship it. So while a shim
    // exists, behaviour is the rebuilt behaviour and only the call sites are old —
    // which is what made them safe to delete one at a time rather than all at once.
    // -----------------------------------------------------------------------

    /**
     * The curated surah list, checked against the bundled metadata.
     *
     * The names, meanings and revelation places are *curated*, not derived: the
     * metadata spells them `Al-Baqara` and `The Cow`, which is not what a reader
     * types or expects, and transliterating them properly is a human judgement
     * about Arabic orthography rather than a transformation of a file.
     *
     * So the human data stays, and the machine-checkable facts in it are
     * verified rather than trusted. [assertMatchesCorpus] is called from the
     * corpus test, which means a wrong verse count or a wrong start page fails
     * the build instead of producing a surah index that lies.
     */
    internal fun assertMatchesCorpus() {
        SURAHS.forEachIndexed { index, surah ->
            val number = index + 1
            require(surah.number == number) {
                "Surah list is out of order at $number: found ${surah.number}"
            }
            val expectedVerses = QuranCorpus.surahVerseCount[number]
            require(surah.totalVerses == expectedVerses) {
                "Surah $number is listed as ${surah.totalVerses} verses, " +
                    "metadata says $expectedVerses"
            }
            val expectedPage = QuranCorpus.surahStartPage(number)
            require(surah.startPage == expectedPage) {
                "Surah $number is listed as starting on page ${surah.startPage}, " +
                    "metadata says $expectedPage"
            }
            val expectedMedinan = QuranCorpus.surahIsMedinan[number]
            require(
                (surah.revelationType == RevelationType.MEDINAN) == expectedMedinan
            ) {
                "Surah $number is listed as ${surah.revelationType}, " +
                    "metadata says ${if (expectedMedinan) "Medinan" else "Meccan"}"
            }

            // Compared folded, not byte for byte. The two spellings of a surah
            // name in wide use differ only in the hamza on the alif - Ibrahim is
            // both ابراهيم and إبراهيم, and the app writes the latter while
            // Tanzil writes the former. That is not a disagreement about a fact,
            // so this check must not be one; a *different* name still is, because
            // folding does not turn بقرة into the ladder.
            require(
                QuranText.normalise(surah.arabicName) ==
                    QuranText.normalise(QuranCorpus.surahArabicName(number))
            ) {
                "Surah $number is listed as '${surah.arabicName}', " +
                    "metadata says '${QuranCorpus.surahArabicName(number)}'"
            }
        }
    }
}
