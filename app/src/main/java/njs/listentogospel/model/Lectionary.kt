package njs.listentogospel.model

import java.time.DayOfWeek
import java.time.LocalDate

// Returns today's Mass gospel as a BibleChapter, or null if today's gospel
// is not from one of the 4 gospels in this app (should not occur in practice).
object Lectionary {

    fun getTodayGospelChapter(date: LocalDate = LocalDate.now()): BibleChapter? {
        val pos = LiturgicalCalendar.liturgicalPosition(date)
        val (gospel, chapter) = when (pos.season) {
            LiturgicalSeason.ADVENT -> adventGospel(pos)
            LiturgicalSeason.CHRISTMAS -> christmasGospel(pos)
            LiturgicalSeason.ORDINARY_BEFORE_LENT -> ordinaryGospel(pos)
            LiturgicalSeason.LENT -> lentGospel(pos)
            LiturgicalSeason.EASTER -> easterGospel(pos)
            LiturgicalSeason.ORDINARY_AFTER_PENTECOST -> ordinaryGospel(pos)
        } ?: return null
        val maxChapter = gospel.chapterCount
        if (chapter < 1 || chapter > maxChapter) return null
        return BibleChapter(gospel, chapter)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private val M = Gospel.MATTHEW
    private val Mk = Gospel.MARK
    private val L = Gospel.LUKE
    private val J = Gospel.JOHN

    private fun gc(g: Gospel, c: Int) = Pair(g, c)

    // ── ADVENT ───────────────────────────────────────────────────────────────

    private fun adventGospel(pos: LiturgicalPosition): Pair<Gospel, Int>? {
        // Dec 17-24: fixed O-Antiphon weekday readings regardless of week
        if (pos.dayOfWeek != DayOfWeek.SUNDAY) {
            // We approximate: Dec 17-23 use Mt 1 / Lk 1 readings
            // The week 4 weekdays (mid-Dec) map to Mt 1 & Lk 1
            if (pos.week == 4 && pos.dayOfWeek != DayOfWeek.SUNDAY) {
                return when (pos.dayOfWeek) {
                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY -> gc(M, 1)
                    else -> gc(L, 1)
                }
            }
        }

        return if (pos.dayOfWeek == DayOfWeek.SUNDAY) {
            adventSundayGospel(pos.week, pos.sundayCycle)
        } else {
            adventWeekdayGospel(pos.week, pos.dayOfWeek)
        }
    }

    // Sunday readings for Advent weeks 1-4, Years A/B/C
    private fun adventSundayGospel(week: Int, cycle: SundayCycle): Pair<Gospel, Int> = when (cycle) {
        SundayCycle.A -> when (week) {
            1 -> gc(M, 24); 2 -> gc(M, 3); 3 -> gc(M, 11); else -> gc(M, 1)
        }
        SundayCycle.B -> when (week) {
            1 -> gc(Mk, 13); 2 -> gc(Mk, 1); 3 -> gc(J, 1); else -> gc(L, 1)
        }
        SundayCycle.C -> when (week) {
            1 -> gc(L, 21); 2 -> gc(L, 3); 3 -> gc(L, 3); else -> gc(L, 1)
        }
    }

    // Advent weekday gospels (weeks 1-3; week 4 handled above)
    private fun adventWeekdayGospel(week: Int, dow: DayOfWeek): Pair<Gospel, Int> {
        // Rows: week 1-3, Columns: Mon-Sat (index 0-5)
        val table = arrayOf(
            // Week 1
            arrayOf(gc(L, 10), gc(M, 15), gc(L, 10), gc(M, 15), gc(M, 9),  gc(L, 20)),
            // Week 2
            arrayOf(gc(L, 5),  gc(M, 17), gc(M, 11), gc(M, 11), gc(L, 7),  gc(M, 17)),
            // Week 3
            arrayOf(gc(M, 21), gc(J, 1),  gc(M, 21), gc(L, 7),  gc(L, 7),  gc(J, 1))
        )
        val wIdx = (week - 1).coerceIn(0, 2)
        val dIdx = (dow.value - 1).coerceIn(0, 5) // Mon=0..Sat=5
        return table[wIdx][dIdx]
    }

    // ── CHRISTMAS ─────────────────────────────────────────────────────────────

    private fun christmasGospel(pos: LiturgicalPosition): Pair<Gospel, Int>? {
        return if (pos.dayOfWeek == DayOfWeek.SUNDAY) {
            christmasSundayGospel(pos.week, pos.sundayCycle)
        } else {
            christmasWeekdayGospel(pos.week, pos.dayOfWeek)
        }
    }

    private fun christmasSundayGospel(week: Int, cycle: SundayCycle): Pair<Gospel, Int> = when (week) {
        1 -> gc(J, 1)  // Christmas Day (Dec 25)
        2 -> when (cycle) {  // Holy Family
            SundayCycle.A -> gc(M, 2)
            else -> gc(L, 2)
        }
        else -> gc(J, 1)  // 2nd Sunday of Christmas
    }

    // Christmas weekday: Dec 26 onward → Jn 1, then Jn 1-2, back to synoptics
    private fun christmasWeekdayGospel(week: Int, dow: DayOfWeek): Pair<Gospel, Int> {
        val table = arrayOf(
            // Dec 26-31 (Christmas week 1, Mon-Sat)
            arrayOf(gc(J, 1), gc(J, 1), gc(J, 1), gc(J, 1), gc(J, 1), gc(J, 1)),
            // Jan 2-6 (week 2, Mon-Sat)
            arrayOf(gc(J, 1), gc(J, 1), gc(J, 2), gc(J, 2), gc(M, 2), gc(J, 1))
        )
        val wIdx = (week - 1).coerceIn(0, 1)
        val dIdx = (dow.value - 1).coerceIn(0, 5)
        return table[wIdx][dIdx]
    }

    // ── ORDINARY TIME (both before Lent and after Pentecost) ─────────────────

    private fun ordinaryGospel(pos: LiturgicalPosition): Pair<Gospel, Int>? {
        return if (pos.dayOfWeek == DayOfWeek.SUNDAY) {
            ordinarySundayGospel(pos.week, pos.sundayCycle)
        } else {
            ordinaryWeekdayGospel(pos.week, pos.dayOfWeek)
        }
    }

    // Sunday readings for Ordinary Time weeks 2-34 (the week after Baptism of Lord = week 2)
    private fun ordinarySundayGospel(week: Int, cycle: SundayCycle): Pair<Gospel, Int>? {
        return when (cycle) {
            SundayCycle.A -> ordinarySundayA(week)
            SundayCycle.B -> ordinarySundayB(week)
            SundayCycle.C -> ordinarySundayC(week)
        }
    }

    private fun ordinarySundayA(week: Int): Pair<Gospel, Int>? = when (week) {
        2  -> gc(J, 1)
        3  -> gc(M, 4)
        4  -> gc(M, 5)
        5  -> gc(M, 5)
        6  -> gc(M, 5)
        7  -> gc(M, 5)
        8  -> gc(M, 6)
        9  -> gc(M, 7)
        10 -> gc(M, 9)
        11 -> gc(M, 9)
        12 -> gc(M, 10)
        13 -> gc(M, 10)
        14 -> gc(M, 11)
        15 -> gc(M, 13)
        16 -> gc(M, 13)
        17 -> gc(M, 13)
        18 -> gc(M, 14)
        19 -> gc(M, 14)
        20 -> gc(M, 15)
        21 -> gc(M, 16)
        22 -> gc(M, 16)
        23 -> gc(M, 18)
        24 -> gc(M, 18)
        25 -> gc(M, 20)
        26 -> gc(M, 21)
        27 -> gc(M, 21)
        28 -> gc(M, 22)
        29 -> gc(M, 22)
        30 -> gc(M, 22)
        31 -> gc(M, 23)
        32 -> gc(M, 25)
        33 -> gc(M, 25)
        34 -> gc(M, 25) // Christ the King
        else -> null
    }

    private fun ordinarySundayB(week: Int): Pair<Gospel, Int>? = when (week) {
        2  -> gc(J, 1)
        3  -> gc(Mk, 1)
        4  -> gc(Mk, 1)
        5  -> gc(Mk, 1)
        6  -> gc(Mk, 1)
        7  -> gc(Mk, 2)
        8  -> gc(Mk, 2)
        9  -> gc(Mk, 2)
        10 -> gc(Mk, 3)
        11 -> gc(Mk, 4)
        12 -> gc(Mk, 4)
        13 -> gc(Mk, 5)
        14 -> gc(Mk, 6)
        15 -> gc(Mk, 6)
        16 -> gc(Mk, 6)
        17 -> gc(J, 6)  // John supplement (Bread of Life)
        18 -> gc(J, 6)
        19 -> gc(J, 6)
        20 -> gc(J, 6)
        21 -> gc(J, 6)
        22 -> gc(Mk, 7)
        23 -> gc(Mk, 7)
        24 -> gc(Mk, 8)
        25 -> gc(Mk, 9)
        26 -> gc(Mk, 9)
        27 -> gc(Mk, 10)
        28 -> gc(Mk, 10)
        29 -> gc(Mk, 10)
        30 -> gc(Mk, 10)
        31 -> gc(Mk, 12)
        32 -> gc(Mk, 12)
        33 -> gc(Mk, 13)
        34 -> gc(J, 18) // Christ the King
        else -> null
    }

    private fun ordinarySundayC(week: Int): Pair<Gospel, Int>? = when (week) {
        2  -> gc(J, 2)
        3  -> gc(L, 4)
        4  -> gc(L, 4)
        5  -> gc(L, 5)
        6  -> gc(L, 6)
        7  -> gc(L, 6)
        8  -> gc(L, 6)
        9  -> gc(L, 7)
        10 -> gc(L, 7)
        11 -> gc(L, 7)
        12 -> gc(L, 9)
        13 -> gc(L, 9)
        14 -> gc(L, 10)
        15 -> gc(L, 10)
        16 -> gc(L, 10)
        17 -> gc(L, 11)
        18 -> gc(L, 12)
        19 -> gc(L, 12)
        20 -> gc(L, 12)
        21 -> gc(L, 13)
        22 -> gc(L, 14)
        23 -> gc(L, 14)
        24 -> gc(L, 15)
        25 -> gc(L, 16)
        26 -> gc(L, 16)
        27 -> gc(L, 17)
        28 -> gc(L, 17)
        29 -> gc(L, 18)
        30 -> gc(L, 18)
        31 -> gc(L, 19)
        32 -> gc(L, 20)
        33 -> gc(L, 21)
        34 -> gc(L, 23) // Christ the King
        else -> null
    }

    // Ordinary Time weekday gospels [week 1-34][Mon-Sat]
    // Weeks 1-9: Mark; Weeks 10-21: Matthew; Weeks 22-34: Luke
    // (Same gospel readings for both Year I and II; only the first reading differs)
    private fun ordinaryWeekdayGospel(week: Int, dow: DayOfWeek): Pair<Gospel, Int>? {
        val table: Array<Array<Pair<Gospel, Int>>> = arrayOf(
            // Week 1
            arrayOf(gc(Mk,1), gc(Mk,1), gc(Mk,1), gc(Mk,1), gc(Mk,2), gc(Mk,2)),
            // Week 2
            arrayOf(gc(Mk,2), gc(Mk,2), gc(Mk,2), gc(Mk,3), gc(Mk,3), gc(Mk,3)),
            // Week 3
            arrayOf(gc(Mk,3), gc(Mk,3), gc(Mk,4), gc(Mk,4), gc(Mk,4), gc(Mk,4)),
            // Week 4
            arrayOf(gc(Mk,4), gc(Mk,5), gc(Mk,5), gc(Mk,5), gc(Mk,5), gc(Mk,5)),
            // Week 5
            arrayOf(gc(Mk,6), gc(Mk,6), gc(Mk,6), gc(Mk,6), gc(Mk,6), gc(Mk,7)),
            // Week 6
            arrayOf(gc(Mk,7), gc(Mk,7), gc(Mk,7), gc(Mk,7), gc(Mk,8), gc(Mk,8)),
            // Week 7
            arrayOf(gc(Mk,8), gc(Mk,8), gc(Mk,9), gc(Mk,9), gc(Mk,9), gc(Mk,9)),
            // Week 8
            arrayOf(gc(Mk,9), gc(Mk,9), gc(Mk,10), gc(Mk,10), gc(Mk,10), gc(Mk,10)),
            // Week 9
            arrayOf(gc(Mk,10), gc(Mk,10), gc(Mk,10), gc(Mk,12), gc(Mk,12), gc(Mk,12)),
            // Week 10 — Matthew (Sermon on the Mount)
            arrayOf(gc(M,5), gc(M,5), gc(M,5), gc(M,5), gc(M,5), gc(M,6)),
            // Week 11
            arrayOf(gc(M,5), gc(M,6), gc(M,6), gc(M,6), gc(M,6), gc(M,7)),
            // Week 12
            arrayOf(gc(M,7), gc(M,7), gc(M,7), gc(M,8), gc(M,9), gc(M,9)),
            // Week 13
            arrayOf(gc(M,9), gc(M,9), gc(M,9), gc(M,10), gc(M,10), gc(M,10)),
            // Week 14
            arrayOf(gc(M,10), gc(M,10), gc(M,10), gc(M,10), gc(M,10), gc(M,11)),
            // Week 15
            arrayOf(gc(M,10), gc(M,11), gc(M,11), gc(M,11), gc(M,12), gc(M,12)),
            // Week 16
            arrayOf(gc(M,12), gc(M,12), gc(M,12), gc(M,12), gc(M,13), gc(M,13)),
            // Week 17
            arrayOf(gc(M,13), gc(M,13), gc(M,13), gc(M,13), gc(M,13), gc(M,14)),
            // Week 18
            arrayOf(gc(M,14), gc(M,14), gc(M,15), gc(M,16), gc(M,17), gc(M,17)),
            // Week 19
            arrayOf(gc(M,17), gc(M,18), gc(M,18), gc(M,18), gc(M,18), gc(M,18)),
            // Week 20
            arrayOf(gc(M,19), gc(M,19), gc(M,19), gc(M,20), gc(M,20), gc(M,20)),
            // Week 21
            arrayOf(gc(M,21), gc(M,21), gc(M,21), gc(M,22), gc(M,22), gc(M,22)),
            // Week 22 — Luke
            arrayOf(gc(L,1),  gc(L,4),  gc(L,4),  gc(L,4),  gc(L,4),  gc(L,5)),
            // Week 23
            arrayOf(gc(L,6),  gc(L,6),  gc(L,6),  gc(L,6),  gc(L,6),  gc(L,6)),
            // Week 24
            arrayOf(gc(L,7),  gc(L,7),  gc(L,7),  gc(L,7),  gc(L,8),  gc(L,8)),
            // Week 25
            arrayOf(gc(L,8),  gc(L,9),  gc(L,9),  gc(L,9),  gc(L,9),  gc(L,9)),
            // Week 26
            arrayOf(gc(L,9),  gc(L,9),  gc(L,10), gc(L,10), gc(L,10), gc(L,10)),
            // Week 27
            arrayOf(gc(L,10), gc(L,10), gc(L,11), gc(L,11), gc(L,11), gc(L,11)),
            // Week 28
            arrayOf(gc(L,11), gc(L,11), gc(L,11), gc(L,12), gc(L,12), gc(L,12)),
            // Week 29
            arrayOf(gc(L,12), gc(L,12), gc(L,12), gc(L,12), gc(L,13), gc(L,13)),
            // Week 30
            arrayOf(gc(L,13), gc(L,13), gc(L,13), gc(L,14), gc(L,14), gc(L,14)),
            // Week 31
            arrayOf(gc(L,14), gc(L,15), gc(L,15), gc(L,15), gc(L,16), gc(L,16)),
            // Week 32
            arrayOf(gc(L,17), gc(L,17), gc(L,17), gc(L,17), gc(L,18), gc(L,18)),
            // Week 33
            arrayOf(gc(L,18), gc(L,19), gc(L,19), gc(L,19), gc(L,19), gc(L,19)),
            // Week 34
            arrayOf(gc(L,19), gc(L,20), gc(L,20), gc(L,21), gc(L,21), gc(L,21))
        )
        val wIdx = (week - 1).coerceIn(0, 33)
        val dIdx = (dow.value - 1).coerceIn(0, 5) // Mon=0..Sat=5
        return table[wIdx][dIdx]
    }

    // ── LENT ─────────────────────────────────────────────────────────────────

    private fun lentGospel(pos: LiturgicalPosition): Pair<Gospel, Int>? {
        return if (pos.dayOfWeek == DayOfWeek.SUNDAY) {
            lentSundayGospel(pos.week, pos.sundayCycle)
        } else {
            lentWeekdayGospel(pos.week, pos.dayOfWeek)
        }
    }

    private fun lentSundayGospel(week: Int, cycle: SundayCycle): Pair<Gospel, Int>? = when (cycle) {
        SundayCycle.A -> when (week) {
            1 -> gc(M, 4); 2 -> gc(M, 17); 3 -> gc(J, 4); 4 -> gc(J, 9); 5 -> gc(J, 11)
            6 -> gc(M, 26) // Palm Sunday (Passion)
            else -> null
        }
        SundayCycle.B -> when (week) {
            1 -> gc(Mk, 1); 2 -> gc(Mk, 9); 3 -> gc(J, 2); 4 -> gc(J, 3); 5 -> gc(J, 12)
            6 -> gc(Mk, 14) // Palm Sunday
            else -> null
        }
        SundayCycle.C -> when (week) {
            1 -> gc(L, 4); 2 -> gc(L, 9); 3 -> gc(J, 4); 4 -> gc(L, 15); 5 -> gc(J, 8)
            6 -> gc(L, 22) // Palm Sunday
            else -> null
        }
    }

    // Lent weekday gospels [week 1-6 incl. Holy Week Mon-Wed][Mon-Sat]
    // Week 1: starts Ash Wednesday (Wed); Mon/Tue before = end of previous OT
    private fun lentWeekdayGospel(week: Int, dow: DayOfWeek): Pair<Gospel, Int>? {
        val table: Array<Array<Pair<Gospel, Int>>> = arrayOf(
            // Week 1 (Ash Wed = Wed of this week)
            arrayOf(gc(M,25), gc(M,6), gc(M,6), gc(L,11), gc(M,7), gc(M,5)),
            // Week 2
            arrayOf(gc(L,6), gc(M,23), gc(L,13), gc(M,20), gc(L,15), gc(L,15)),
            // Week 3
            arrayOf(gc(L,4), gc(M,5), gc(L,11), gc(J,4), gc(Mk,12), gc(L,18)),
            // Week 4
            arrayOf(gc(J,4), gc(J,5), gc(J,5), gc(J,5), gc(J,7), gc(J,7)),
            // Week 5
            arrayOf(gc(J,8), gc(J,8), gc(J,8), gc(J,8), gc(J,10), gc(J,11)),
            // Holy Week (Mon-Wed; Thu-Sat = Triduum, no Mass gospel in same sense)
            arrayOf(gc(J,12), gc(J,13), gc(M,26), gc(J,13), gc(J,18), gc(L,23))
        )
        val wIdx = (week - 1).coerceIn(0, 5)
        val dIdx = (dow.value - 1).coerceIn(0, 5)
        return table[wIdx][dIdx]
    }

    // ── EASTER ───────────────────────────────────────────────────────────────

    private fun easterGospel(pos: LiturgicalPosition): Pair<Gospel, Int>? {
        return if (pos.dayOfWeek == DayOfWeek.SUNDAY) {
            easterSundayGospel(pos.week, pos.sundayCycle)
        } else {
            easterWeekdayGospel(pos.week, pos.dayOfWeek)
        }
    }

    // Sunday readings for Easter weeks 1-7, Years A/B/C
    private fun easterSundayGospel(week: Int, cycle: SundayCycle): Pair<Gospel, Int>? = when (week) {
        1 -> gc(J, 20) // Easter Sunday (same all years)
        2 -> gc(J, 20) // Divine Mercy Sunday (same all years)
        3 -> when (cycle) {
            SundayCycle.A -> gc(L, 24); SundayCycle.B -> gc(L, 24); SundayCycle.C -> gc(J, 21)
        }
        4 -> gc(J, 10) // Good Shepherd Sunday (same all years, different verses)
        5 -> when (cycle) {
            SundayCycle.A -> gc(J, 14); SundayCycle.B -> gc(J, 15); SundayCycle.C -> gc(J, 13)
        }
        6 -> when (cycle) {
            SundayCycle.A -> gc(J, 14); SundayCycle.B -> gc(J, 15); SundayCycle.C -> gc(J, 14)
        }
        7 -> when (cycle) {
            SundayCycle.A -> gc(J, 17); SundayCycle.B -> gc(J, 17); SundayCycle.C -> gc(J, 17)
        }
        else -> null
    }

    // Easter weekday gospels [week 1-7][Mon-Sat]
    private fun easterWeekdayGospel(week: Int, dow: DayOfWeek): Pair<Gospel, Int>? {
        val table: Array<Array<Pair<Gospel, Int>>> = arrayOf(
            // Easter Octave (week 1)
            arrayOf(gc(M,28), gc(J,20), gc(L,24), gc(L,24), gc(J,21), gc(J,21)),
            // Week 2
            arrayOf(gc(J,3),  gc(J,3),  gc(J,3),  gc(J,3),  gc(J,6),  gc(J,6)),
            // Week 3
            arrayOf(gc(J,6),  gc(J,6),  gc(J,6),  gc(J,6),  gc(J,6),  gc(J,6)),
            // Week 4
            arrayOf(gc(J,10), gc(J,10), gc(J,12), gc(J,13), gc(J,14), gc(J,14)),
            // Week 5
            arrayOf(gc(J,14), gc(J,14), gc(J,15), gc(J,15), gc(J,15), gc(J,15)),
            // Week 6
            arrayOf(gc(J,15), gc(J,16), gc(J,16), gc(J,16), gc(J,16), gc(J,16)),
            // Week 7 (before Pentecost)
            arrayOf(gc(J,16), gc(J,17), gc(J,17), gc(J,17), gc(J,21), gc(J,21))
        )
        val wIdx = (week - 1).coerceIn(0, 6)
        val dIdx = (dow.value - 1).coerceIn(0, 5)
        return table[wIdx][dIdx]
    }
}
