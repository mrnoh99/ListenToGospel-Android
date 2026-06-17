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
}
