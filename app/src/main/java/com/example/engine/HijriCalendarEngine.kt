package com.example.engine

import com.example.data.model.HijriDate
import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

data class IslamicEvent(
    val titleEn: String,
    val titleAr: String,
    val hijriMonth: Int,
    val hijriDay: Int,
    val description: String
)

object HijriCalendarEngine {

    val ISLAMIC_MONTHS_EN = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
    )

    val ISLAMIC_MONTHS_AR = listOf(
        "محرّم", "صفر", "ربيع الأول", "ربيع الثاني",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوّال", "ذو القعدة", "ذو الحجة"
    )

    val KEY_EVENTS = listOf(
        IslamicEvent("Islamic New Year", "رأس السنة الهجرية", 1, 1, "Beginning of the Islamic lunar calendar year"),
        IslamicEvent("Day of Ashura", "يوم عاشوراء", 1, 10, "Day Moses was saved; day of fasting and remembrance"),
        IslamicEvent("Mawlid an-Nabi", "المولد النبوي الشريف", 3, 12, "Birth of the Prophet Muhammad ﷺ"),
        IslamicEvent("Isra and Mi'raj", "الإسراء والمعراج", 7, 27, "The miraculous Night Journey and Heavenly Ascension"),
        IslamicEvent("Mid-Sha'ban (Laylat al-Bara'ah)", "ليلة النصف من شعبان", 8, 15, "Night of forgiveness and records"),
        IslamicEvent("First Day of Ramadan", "أول أيام شهر رمضان", 9, 1, "Beginning of the blessed month of fasting and Quran"),
        IslamicEvent("Laylat al-Qadr", "ليلة القدر", 9, 27, "The Night of Power, better than a thousand months"),
        IslamicEvent("Eid al-Fitr", "عيد الفطر المبارك", 10, 1, "Festival marking the joyous completion of Ramadan fasting"),
        IslamicEvent("Day of Arafah", "يوم عرفة", 12, 9, "The greatest day of Hajj pilgrimage, standing at Mount Arafat"),
        IslamicEvent("Eid al-Adha", "عيد الأضحى المبارك", 12, 10, "Feast of the Sacrifice and days of Tashreeq")
    )

    fun getHijriDate(gregorianDate: LocalDate): HijriDate {
        return try {
            val hijrahDate = HijrahDate.from(gregorianDate)
            val year = hijrahDate.get(ChronoField.YEAR)
            val month = hijrahDate.get(ChronoField.MONTH_OF_YEAR)
            val day = hijrahDate.get(ChronoField.DAY_OF_MONTH)

            val monthIndex = (month - 1).coerceIn(0, 11)
            HijriDate(
                day = day,
                monthNumber = month,
                monthNameEn = ISLAMIC_MONTHS_EN[monthIndex],
                monthNameAr = ISLAMIC_MONTHS_AR[monthIndex],
                year = year
            )
        } catch (e: Exception) {
            // Fallback calculation algorithm
            calculateHijriFallback(gregorianDate)
        }
    }

    private fun calculateHijriFallback(date: LocalDate): HijriDate {
        val jd = toJulianDay(date)
        val l = jd - 1948440 + 10632
        val n = ((l - 1) / 10631).toInt()
        val lPrime = l - 10631 * n + 354
        val j = (((10985 - lPrime) / 5316).toInt()) * (((50 * lPrime) / 17719).toInt()) +
                ((lPrime / 5670).toInt()) * (((43 * lPrime) / 15238).toInt())
        val lDoublePrime = lPrime - (((30 - j) / 15).toInt()) * (((17719 * j) / 50).toInt()) -
                ((j / 16).toInt()) * (((15238 * j) / 43).toInt()) + 29
        val m = ((24 * lDoublePrime) / 709).toInt()
        val d = lDoublePrime - ((709 * m) / 24).toInt()
        val y = 30 * n + j - 30

        val monthIndex = (m - 1).coerceIn(0, 11)
        return HijriDate(
            day = d.toInt().coerceIn(1, 30),
            monthNumber = m.coerceIn(1, 12),
            monthNameEn = ISLAMIC_MONTHS_EN[monthIndex],
            monthNameAr = ISLAMIC_MONTHS_AR[monthIndex],
            year = y
        )
    }

    private fun toJulianDay(date: LocalDate): Long {
        val a = (14 - date.monthValue) / 12
        val y = date.year + 4800 - a
        val m = date.monthValue + 12 * a - 3
        return (date.dayOfMonth + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045).toLong()
    }

    fun getEventForDate(hijriDate: HijriDate): IslamicEvent? {
        return KEY_EVENTS.find { it.hijriMonth == hijriDate.monthNumber && it.hijriDay == hijriDate.day }
    }
}
