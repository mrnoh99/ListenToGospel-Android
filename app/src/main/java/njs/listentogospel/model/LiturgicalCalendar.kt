package njs.listentogospel.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.TemporalAdjusters

enum class SundayCycle { A, B, C }
enum class WeekdayCycle { I, II }

enum class LiturgicalSeason {
    ADVENT,
    CHRISTMAS,
    ORDINARY_BEFORE_LENT,
    LENT,
    EASTER,
    ORDINARY_AFTER_PENTECOST
}

data class LiturgicalPosition(
    val season: LiturgicalSeason,
    val week: Int,
    val dayOfWeek: DayOfWeek,
    val sundayCycle: SundayCycle,
    val weekdayCycle: WeekdayCycle
)

object LiturgicalCalendar {

    // Anonymous Gregorian algorithm (Meeus/Jones/Butcher)
    fun easterDate(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }

    // First Sunday of Advent = 4th Sunday before Christmas (incl. Christmas itself)
    fun firstSundayOfAdvent(year: Int): LocalDate {
        val dec25 = LocalDate.of(year, Month.DECEMBER, 25)
        val sundayBeforeOrOnDec25 = dec25.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        return sundayBeforeOrOnDec25.minusWeeks(3)
    }

    // Sunday after January 6 (Epiphany fixed on Jan 6 per Korean Catholic practice)
    private fun baptismOfLord(year: Int): LocalDate {
        val jan6 = LocalDate.of(year, Month.JANUARY, 6)
        return jan6.with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
    }

    // Liturgical year that contains the given date.
    // A liturgical year is named by its ending calendar year (the year Advent ends).
    // E.g. Advent 2025 → Liturgical Year 2026 (ends Nov 2026).
    private fun liturgicalYear(date: LocalDate): Int {
        val advent = firstSundayOfAdvent(date.year)
        return if (date >= advent) date.year + 1 else date.year
    }

    // Sunday cycle: Year A = Matthew, B = Mark, C = Luke
    // Reference: Liturgical Year 2026 = A, 2027 = B, 2028 = C
    fun sundayCycle(date: LocalDate): SundayCycle {
        val ly = liturgicalYear(date)
        return when (((ly - 2026) % 3 + 3) % 3) {
            0 -> SundayCycle.A
            1 -> SundayCycle.B
            else -> SundayCycle.C
        }
    }

    // Weekday cycle: odd liturgical year = I, even = II
    fun weekdayCycle(date: LocalDate): WeekdayCycle {
        val ly = liturgicalYear(date)
        return if (ly % 2 != 0) WeekdayCycle.I else WeekdayCycle.II
    }

    fun liturgicalPosition(date: LocalDate): LiturgicalPosition {
        val year = date.year
        val sc = sundayCycle(date)
        val wc = weekdayCycle(date)
        val dow = date.dayOfWeek

        // Determine which liturgical year bracket this date falls in
        val advent = firstSundayOfAdvent(year)
        val inCurrentAdvent = date >= advent

        // Relevant boundary dates for this liturgical year
        val christmasYear = if (inCurrentAdvent) year else year - 1
        val easter = easterDate(if (inCurrentAdvent) year + 1 else year)
        val baptism = baptismOfLord(christmasYear + 1)
        val christmas = LocalDate.of(christmasYear, Month.DECEMBER, 25)
        val lYearStart = if (inCurrentAdvent) advent else firstSundayOfAdvent(year - 1)

        val ashWednesday = easter.minusDays(46)
        val pentecost = easter.plusDays(49)

        // Christ the King = last Sunday before next Advent
        val nextAdvent = if (inCurrentAdvent) firstSundayOfAdvent(year + 1) else advent
        val christTheKing = nextAdvent.minusWeeks(1)

        val season = when {
            date >= lYearStart && date < christmas -> LiturgicalSeason.ADVENT
            date >= christmas && date < baptism -> LiturgicalSeason.CHRISTMAS
            date >= baptism && date < ashWednesday -> LiturgicalSeason.ORDINARY_BEFORE_LENT
            date >= ashWednesday && date < easter -> LiturgicalSeason.LENT
            date >= easter && date < pentecost -> LiturgicalSeason.EASTER
            else -> LiturgicalSeason.ORDINARY_AFTER_PENTECOST
        }

        val week = when (season) {
            LiturgicalSeason.ADVENT -> {
                val days = (date.toEpochDay() - lYearStart.toEpochDay()).toInt()
                (days / 7 + 1).coerceIn(1, 4)
            }
            LiturgicalSeason.CHRISTMAS -> {
                val days = (date.toEpochDay() - christmas.toEpochDay()).toInt()
                (days / 7 + 1).coerceIn(1, 3)
            }
            LiturgicalSeason.ORDINARY_BEFORE_LENT -> {
                // OT week number: Baptism of Lord Sunday = end of Christmas / start of OT
                // Week 2 starts the Monday after Baptism of Lord Sunday
                val mondayWeek2 = baptism.plusDays(1)
                val days = (date.toEpochDay() - mondayWeek2.toEpochDay()).toInt()
                (days / 7 + 2).coerceIn(2, 9)
            }
            LiturgicalSeason.LENT -> {
                val days = (date.toEpochDay() - ashWednesday.toEpochDay()).toInt()
                (days / 7 + 1).coerceIn(1, 6)
            }
            LiturgicalSeason.EASTER -> {
                val days = (date.toEpochDay() - easter.toEpochDay()).toInt()
                (days / 7 + 1).coerceIn(1, 7)
            }
            LiturgicalSeason.ORDINARY_AFTER_PENTECOST -> {
                // Week number counted backward from Christ the King (week 34).
                // Anchor to the Sunday of the current week so Mon-Sat get the
                // same week number as their Sunday (not the following Sunday's).
                val sundayOfWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                val days = (christTheKing.toEpochDay() - sundayOfWeek.toEpochDay()).toInt()
                (34 - days / 7).coerceIn(10, 34)
            }
        }

        return LiturgicalPosition(season, week, dow, sc, wc)
    }

    fun liturgicalDayName(date: LocalDate = LocalDate.now()): String {
        val md = date.monthValue * 100 + date.dayOfMonth
        val fixed = when (md) {
            101  -> "천주의 성모 마리아 대축일"
            202  -> "주님 봉헌 축일"
            319  -> "복되신 동정 마리아의 배필 성 요셉 대축일"
            325  -> "주님 탄생 예고 대축일"
            624  -> "성 요한 세례자 탄생 대축일"
            629  -> "성 베드로와 성 바오로 사도 대축일"
            806  -> "주님의 거룩한 변모 축일"
            815  -> "성모 승천 대축일"
            1101 -> "모든 성인 대축일"
            1102 -> "죽은 모든 이를 기억하는 위령의 날"
            1208 -> "한국 교회의 수호자 원죄 없이 잉태되신 복되신 동정 마리아 대축일"
            1225 -> "주님 성탄 대축일"
            1226 -> "성 스테파노 첫 순교자 축일"
            1228 -> "죄 없는 아기 순교자들 축일"
            else -> null
        }
        if (fixed != null) return fixed

        if (date.monthValue == 12 && date.dayOfMonth in 17..24) {
            return "12월 ${date.dayOfMonth}일 대림"
        }

        // Korea: Epiphany on the Sunday between Jan 2–8
        if (date.monthValue == 1 && date.dayOfMonth in 2..8 && date.dayOfWeek == DayOfWeek.SUNDAY) {
            return "주님 공현 대축일"
        }

        val year = date.year
        val easter = easterDate(year)
        val ashWed = easter.minusDays(46)
        val pentecost = easter.plusDays(49)
        val baptism = baptismOfLord(year)
        val dec25 = LocalDate.of(year, Month.DECEMBER, 25)
        val holyFamily = if (dec25.dayOfWeek == DayOfWeek.SUNDAY)
            LocalDate.of(year, Month.DECEMBER, 30)
        else
            dec25.with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
        val christTheKing = firstSundayOfAdvent(year).minusWeeks(1)

        val movable = when (date) {
            baptism                -> "주님 세례 축일"
            ashWed                 -> "재의 수요일"
            ashWed.plusDays(1)     -> "재의 예식 다음 목요일"
            ashWed.plusDays(2)     -> "재의 예식 다음 금요일"
            ashWed.plusDays(3)     -> "재의 예식 다음 토요일"
            easter.minusDays(7)    -> "주님 수난 성지 주일"
            easter.minusDays(3)    -> "주님 만찬 성목요일"
            easter.minusDays(2)    -> "주님 수난 성금요일"
            easter.minusDays(1)    -> "성토요일"
            easter                 -> "주님 부활 대축일"
            easter.plusDays(1)     -> "부활 팔일 축제 월요일"
            easter.plusDays(2)     -> "부활 팔일 축제 화요일"
            easter.plusDays(3)     -> "부활 팔일 축제 수요일"
            easter.plusDays(4)     -> "부활 팔일 축제 목요일"
            easter.plusDays(5)     -> "부활 팔일 축제 금요일"
            easter.plusDays(6)     -> "부활 팔일 축제 토요일"
            easter.plusDays(42)    -> "주님 승천 대축일"   // Korea: 7th Sunday of Easter
            pentecost              -> "성령 강림 대축일"
            pentecost.plusDays(1)  -> "교회의 어머니 복되신 동정 마리아 기념일"
            pentecost.plusDays(7)  -> "지극히 거룩하신 삼위일체 대축일"
            pentecost.plusDays(14) -> "지극히 거룩하신 그리스도의 성체 성혈 대축일"
            pentecost.plusDays(19) -> "지극히 거룩하신 예수 성심 대축일"
            holyFamily             -> "예수, 마리아, 요셉의 성가정 축일"
            christTheKing          -> "온 누리의 임금이신 우리 주 예수 그리스도 왕 대축일"
            else                   -> null
        }
        if (movable != null) return movable

        val pos = liturgicalPosition(date)
        val s = when (pos.season) {
            LiturgicalSeason.ADVENT -> "대림"
            LiturgicalSeason.CHRISTMAS -> "성탄"
            LiturgicalSeason.ORDINARY_BEFORE_LENT,
            LiturgicalSeason.ORDINARY_AFTER_PENTECOST -> "연중"
            LiturgicalSeason.LENT -> "사순"
            LiturgicalSeason.EASTER -> "부활"
        }
        return when (pos.dayOfWeek) {
            DayOfWeek.SUNDAY    -> "$s 제${pos.week}주일"
            DayOfWeek.MONDAY    -> "$s 제${pos.week}주간 월요일"
            DayOfWeek.TUESDAY   -> "$s 제${pos.week}주간 화요일"
            DayOfWeek.WEDNESDAY -> "$s 제${pos.week}주간 수요일"
            DayOfWeek.THURSDAY  -> "$s 제${pos.week}주간 목요일"
            DayOfWeek.FRIDAY    -> "$s 제${pos.week}주간 금요일"
            DayOfWeek.SATURDAY  -> "$s 제${pos.week}주간 토요일"
            else                -> s
        }
    }
}
