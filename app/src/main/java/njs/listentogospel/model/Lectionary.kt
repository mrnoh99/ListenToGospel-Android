package njs.listentogospel.model

import java.time.DayOfWeek
import java.time.LocalDate

// Returns today's Mass gospel as a (BibleChapter, startVerse) pair,
// or null if not from one of the 4 gospels in this app.
object Lectionary {

    fun getTodayGospelChapter(date: LocalDate = LocalDate.now()): BibleChapter? =
        getTodayGospelReading(date)?.first

    fun getTodayGospelReading(date: LocalDate = LocalDate.now()): Pair<BibleChapter, Int>? {
        val pos = LiturgicalCalendar.liturgicalPosition(date)

        if (pos.season == LiturgicalSeason.ORDINARY_AFTER_PENTECOST) {
            val pentecost = LiturgicalCalendar.easterDate(date.year).plusDays(49)
            val override = when (date) {
                pentecost.plusDays(7) -> when (pos.sundayCycle) {
                    SundayCycle.A -> gc(J, 3, 16)
                    SundayCycle.B -> gc(M, 28, 16)
                    SundayCycle.C -> gc(J, 16, 12)
                }
                pentecost.plusDays(14) -> when (pos.sundayCycle) {
                    SundayCycle.A -> gc(J, 6, 51)
                    SundayCycle.B -> gc(Mk, 14, 12)
                    SundayCycle.C -> gc(L, 9, 11)
                }
                pentecost.plusDays(19) -> when (pos.sundayCycle) {
                    SundayCycle.A -> gc(M, 11, 25)
                    SundayCycle.B -> gc(J, 19, 31)
                    SundayCycle.C -> gc(L, 15, 3)
                }
                else -> null
            }
            if (override != null) return override.toReading()
        }

        val triple = when (pos.season) {
            LiturgicalSeason.ADVENT                  -> adventGospel(pos)
            LiturgicalSeason.CHRISTMAS               -> christmasGospel(pos)
            LiturgicalSeason.ORDINARY_BEFORE_LENT    -> ordinaryGospel(pos)
            LiturgicalSeason.LENT                    -> lentGospel(pos)
            LiturgicalSeason.EASTER                  -> easterGospel(pos)
            LiturgicalSeason.ORDINARY_AFTER_PENTECOST -> ordinaryGospel(pos)
        } ?: return null

        return triple.toReading()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private val M  = Gospel.MATTHEW
    private val Mk = Gospel.MARK
    private val L  = Gospel.LUKE
    private val J  = Gospel.JOHN

    private fun gc(g: Gospel, c: Int, v: Int = 1) = Triple(g, c, v)

    private fun Triple<Gospel, Int, Int>.toReading(): Pair<BibleChapter, Int>? {
        val (gospel, chapter, verse) = this
        if (chapter < 1 || chapter > gospel.chapterCount) return null
        return Pair(BibleChapter(gospel, chapter), verse)
    }

    // ── ADVENT ───────────────────────────────────────────────────────────────

    private fun adventGospel(pos: LiturgicalPosition): Triple<Gospel, Int, Int>? {
        if (pos.dayOfWeek != DayOfWeek.SUNDAY) {
            if (pos.week == 4) {
                return when (pos.dayOfWeek) {
                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY -> gc(M, 1, 18)
                    else -> gc(L, 1, 26)
                }
            }
        }
        return if (pos.dayOfWeek == DayOfWeek.SUNDAY) {
            adventSundayGospel(pos.week, pos.sundayCycle)
        } else {
            adventWeekdayGospel(pos.week, pos.dayOfWeek)
        }
    }

    private fun adventSundayGospel(week: Int, cycle: SundayCycle): Triple<Gospel, Int, Int> = when (cycle) {
        SundayCycle.A -> when (week) {
            1 -> gc(M, 24, 37); 2 -> gc(M, 3, 1); 3 -> gc(M, 11, 2); else -> gc(M, 1, 18)
        }
        SundayCycle.B -> when (week) {
            1 -> gc(Mk, 13, 33); 2 -> gc(Mk, 1, 1); 3 -> gc(J, 1, 6); else -> gc(L, 1, 26)
        }
        SundayCycle.C -> when (week) {
            1 -> gc(L, 21, 25); 2 -> gc(L, 3, 1); 3 -> gc(L, 3, 10); else -> gc(L, 1, 39)
        }
    }

    private fun adventWeekdayGospel(week: Int, dow: DayOfWeek): Triple<Gospel, Int, Int> {
        val table = arrayOf(
            // Week 1: Mon-Sat
            arrayOf(gc(L, 10, 25), gc(M, 15, 29), gc(L, 10, 21), gc(M, 15, 21), gc(M,  9, 27), gc(L, 20, 27)),
            // Week 2
            arrayOf(gc(L,  5, 17), gc(M, 17, 10), gc(M, 11, 28), gc(M, 11, 11), gc(L,  7, 24), gc(M, 17, 10)),
            // Week 3
            arrayOf(gc(M, 21, 28), gc(J,  1,  6), gc(M, 21, 28), gc(L,  7, 24), gc(L,  7, 31), gc(J,  1, 19))
        )
        val wIdx = (week - 1).coerceIn(0, 2)
        val dIdx = (dow.value - 1).coerceIn(0, 5)
        return table[wIdx][dIdx]
    }

    // ── CHRISTMAS ─────────────────────────────────────────────────────────────

    private fun christmasGospel(pos: LiturgicalPosition): Triple<Gospel, Int, Int>? {
        return if (pos.dayOfWeek == DayOfWeek.SUNDAY) {
            christmasSundayGospel(pos.week, pos.sundayCycle)
        } else {
            christmasWeekdayGospel(pos.week, pos.dayOfWeek)
        }
    }

    private fun christmasSundayGospel(week: Int, cycle: SundayCycle): Triple<Gospel, Int, Int> = when (week) {
        1 -> gc(J, 1, 1)
        2 -> when (cycle) {
            SundayCycle.A -> gc(M, 2, 13)
            SundayCycle.B -> gc(L, 2, 22)
            SundayCycle.C -> gc(L, 2, 41)
        }
        else -> gc(J, 1, 1)
    }

    private fun christmasWeekdayGospel(week: Int, dow: DayOfWeek): Triple<Gospel, Int, Int> {
        val table = arrayOf(
            // Dec 26-31 (week 1, Mon-Sat)
            arrayOf(gc(J, 1,  1), gc(J, 1,  1), gc(J, 1,  1), gc(J, 1,  1), gc(J, 1,  1), gc(J, 1,  1)),
            // Jan 2-6  (week 2, Mon-Sat)
            arrayOf(gc(J, 1, 19), gc(J, 1, 29), gc(J, 2,  1), gc(J, 2,  1), gc(M, 2, 13), gc(J, 1, 43))
        )
        val wIdx = (week - 1).coerceIn(0, 1)
        val dIdx = (dow.value - 1).coerceIn(0, 5)
        return table[wIdx][dIdx]
    }

    // ── ORDINARY TIME ─────────────────────────────────────────────────────────

    private fun ordinaryGospel(pos: LiturgicalPosition): Triple<Gospel, Int, Int>? {
        return if (pos.dayOfWeek == DayOfWeek.SUNDAY) {
            ordinarySundayGospel(pos.week, pos.sundayCycle)
        } else {
            ordinaryWeekdayGospel(pos.week, pos.dayOfWeek)
        }
    }

    private fun ordinarySundayGospel(week: Int, cycle: SundayCycle): Triple<Gospel, Int, Int>? {
        return when (cycle) {
            SundayCycle.A -> ordinarySundayA(week)
            SundayCycle.B -> ordinarySundayB(week)
            SundayCycle.C -> ordinarySundayC(week)
        }
    }

    private fun ordinarySundayA(week: Int): Triple<Gospel, Int, Int>? = when (week) {
        2  -> gc(J,  1, 29)
        3  -> gc(M,  4, 12)
        4  -> gc(M,  5,  1)
        5  -> gc(M,  5, 13)
        6  -> gc(M,  5, 17)
        7  -> gc(M,  5, 38)
        8  -> gc(M,  6, 24)
        9  -> gc(M,  7, 21)
        10 -> gc(M,  9,  9)
        11 -> gc(M,  9, 36)
        12 -> gc(M, 10, 26)
        13 -> gc(M, 10, 37)
        14 -> gc(M, 11, 25)
        15 -> gc(M, 13,  1)
        16 -> gc(M, 13, 24)
        17 -> gc(M, 13, 44)
        18 -> gc(M, 14, 13)
        19 -> gc(M, 14, 22)
        20 -> gc(M, 15, 21)
        21 -> gc(M, 16, 13)
        22 -> gc(M, 16, 21)
        23 -> gc(M, 18, 15)
        24 -> gc(M, 18, 21)
        25 -> gc(M, 20,  1)
        26 -> gc(M, 21, 28)
        27 -> gc(M, 21, 33)
        28 -> gc(M, 22,  1)
        29 -> gc(M, 22, 15)
        30 -> gc(M, 22, 34)
        31 -> gc(M, 23,  1)
        32 -> gc(M, 25,  1)
        33 -> gc(M, 25, 14)
        34 -> gc(M, 25, 31)
        else -> null
    }

    private fun ordinarySundayB(week: Int): Triple<Gospel, Int, Int>? = when (week) {
        2  -> gc(J,  1, 35)
        3  -> gc(Mk, 1, 14)
        4  -> gc(Mk, 1, 21)
        5  -> gc(Mk, 1, 29)
        6  -> gc(Mk, 1, 40)
        7  -> gc(Mk, 2,  1)
        8  -> gc(Mk, 2, 18)
        9  -> gc(Mk, 2, 23)
        10 -> gc(Mk, 3, 20)
        11 -> gc(Mk, 4, 26)
        12 -> gc(Mk, 4, 35)
        13 -> gc(Mk, 5, 21)
        14 -> gc(Mk, 6,  1)
        15 -> gc(Mk, 6,  7)
        16 -> gc(Mk, 6, 30)
        17 -> gc(J,  6,  1)
        18 -> gc(J,  6, 24)
        19 -> gc(J,  6, 41)
        20 -> gc(J,  6, 51)
        21 -> gc(J,  6, 60)
        22 -> gc(Mk, 7,  1)
        23 -> gc(Mk, 7, 31)
        24 -> gc(Mk, 8, 27)
        25 -> gc(Mk, 9, 30)
        26 -> gc(Mk, 9, 38)
        27 -> gc(Mk, 10,  2)
        28 -> gc(Mk, 10, 17)
        29 -> gc(Mk, 10, 35)
        30 -> gc(Mk, 10, 46)
        31 -> gc(Mk, 12, 28)
        32 -> gc(Mk, 12, 38)
        33 -> gc(Mk, 13, 24)
        34 -> gc(J,  18, 33)
        else -> null
    }

    private fun ordinarySundayC(week: Int): Triple<Gospel, Int, Int>? = when (week) {
        2  -> gc(J,  2,  1)
        3  -> gc(L,  4, 14)
        4  -> gc(L,  4, 21)
        5  -> gc(L,  5,  1)
        6  -> gc(L,  6, 17)
        7  -> gc(L,  6, 27)
        8  -> gc(L,  6, 39)
        9  -> gc(L,  7,  1)
        10 -> gc(L,  7, 11)
        11 -> gc(L,  7, 36)
        12 -> gc(L,  9, 18)
        13 -> gc(L,  9, 51)
        14 -> gc(L, 10,  1)
        15 -> gc(L, 10, 25)
        16 -> gc(L, 10, 38)
        17 -> gc(L, 11,  1)
        18 -> gc(L, 12, 13)
        19 -> gc(L, 12, 32)
        20 -> gc(L, 12, 49)
        21 -> gc(L, 13, 22)
        22 -> gc(L, 14,  1)
        23 -> gc(L, 14, 25)
        24 -> gc(L, 15,  1)
        25 -> gc(L, 16,  1)
        26 -> gc(L, 16, 19)
        27 -> gc(L, 17,  5)
        28 -> gc(L, 17, 11)
        29 -> gc(L, 18,  1)
        30 -> gc(L, 18,  9)
        31 -> gc(L, 19,  1)
        32 -> gc(L, 20, 27)
        33 -> gc(L, 21,  5)
        34 -> gc(L, 23, 35)
        else -> null
    }

    private fun ordinaryWeekdayGospel(week: Int, dow: DayOfWeek): Triple<Gospel, Int, Int>? {
        val table: Array<Array<Triple<Gospel, Int, Int>>> = arrayOf(
            // Week 1  — Mark 1
            arrayOf(gc(Mk, 1,14), gc(Mk, 1,21), gc(Mk, 1,29), gc(Mk, 1,40), gc(Mk, 2, 1), gc(Mk, 2,13)),
            // Week 2  — Mark 2-3
            arrayOf(gc(Mk, 2,18), gc(Mk, 2,23), gc(Mk, 3, 1), gc(Mk, 3, 7), gc(Mk, 3,13), gc(Mk, 3,20)),
            // Week 3  — Mark 3-4
            arrayOf(gc(Mk, 3,22), gc(Mk, 3,31), gc(Mk, 4, 1), gc(Mk, 4,21), gc(Mk, 4,26), gc(Mk, 4,35)),
            // Week 4  — Mark 4-5
            arrayOf(gc(Mk, 4,35), gc(Mk, 5, 1), gc(Mk, 5,21), gc(Mk, 5,21), gc(Mk, 5,35), gc(Mk, 5,35)),
            // Week 5  — Mark 6-7
            arrayOf(gc(Mk, 6, 1), gc(Mk, 6, 7), gc(Mk, 6,14), gc(Mk, 6,30), gc(Mk, 6,34), gc(Mk, 7, 1)),
            // Week 6  — Mark 7-8
            arrayOf(gc(Mk, 7, 1), gc(Mk, 7,14), gc(Mk, 7,24), gc(Mk, 7,31), gc(Mk, 8, 1), gc(Mk, 8,14)),
            // Week 7  — Mark 8-9
            arrayOf(gc(Mk, 8,22), gc(Mk, 8,22), gc(Mk, 9, 2), gc(Mk, 9,14), gc(Mk, 9,30), gc(Mk, 9,38)),
            // Week 8  — Mark 9-10
            arrayOf(gc(Mk, 9,38), gc(Mk, 9,41), gc(Mk,10, 1), gc(Mk,10,13), gc(Mk,10,17), gc(Mk,10,28)),
            // Week 9  — Mark 10, 12
            arrayOf(gc(Mk,10,32), gc(Mk,10,35), gc(Mk,10,46), gc(Mk,12,28), gc(Mk,12,35), gc(Mk,12,38)),
            // Week 10 — Matthew 5
            arrayOf(gc(M,  5, 1), gc(M,  5,13), gc(M,  5,17), gc(M,  5,20), gc(M,  5,27), gc(M,  5,33)),
            // Week 11 — Matthew 5-6
            arrayOf(gc(M,  5,38), gc(M,  5,43), gc(M,  6, 1), gc(M,  6, 7), gc(M,  6,19), gc(M,  6,24)),
            // Week 12 — Matthew 7-8
            arrayOf(gc(M,  7, 1), gc(M,  7, 6), gc(M,  7,15), gc(M,  7,21), gc(M,  8, 1), gc(M,  8, 5)),
            // Week 13 — Matthew 8-9
            arrayOf(gc(M,  8,18), gc(M,  8,23), gc(M,  8,28), gc(M,  9, 1), gc(M,  9, 9), gc(M,  9,14)),
            // Week 14 — Matthew 9-10
            arrayOf(gc(M,  9,18), gc(M,  9,32), gc(M, 10, 1), gc(M, 10, 7), gc(M, 10,16), gc(M, 10,24)),
            // Week 15 — Matthew 10-12
            arrayOf(gc(M, 10,34), gc(M, 11,20), gc(M, 11,25), gc(M, 11,28), gc(M, 12, 1), gc(M, 12,14)),
            // Week 16 — Matthew 12-13
            arrayOf(gc(M, 12,38), gc(M, 12,46), gc(M, 13, 1), gc(M, 13,10), gc(M, 13,18), gc(M, 13,24)),
            // Week 17 — Matthew 13-14
            arrayOf(gc(M, 13,31), gc(M, 13,36), gc(M, 13,44), gc(M, 13,47), gc(M, 13,54), gc(M, 14, 1)),
            // Week 18 — Matthew 14-17
            arrayOf(gc(M, 14,13), gc(M, 14,22), gc(M, 15,21), gc(M, 16,13), gc(M, 17, 1), gc(M, 17,14)),
            // Week 19 — Matthew 17-18
            arrayOf(gc(M, 17,22), gc(M, 18, 1), gc(M, 18,15), gc(M, 18,21), gc(M, 18,21), gc(M, 18,21)),
            // Week 20 — Matthew 19-20
            arrayOf(gc(M, 19, 3), gc(M, 19,13), gc(M, 19,23), gc(M, 20, 1), gc(M, 20,17), gc(M, 20,24)),
            // Week 21 — Matthew 23-25
            arrayOf(gc(M, 23, 1), gc(M, 23,13), gc(M, 23,27), gc(M, 24,37), gc(M, 25, 1), gc(M, 25,14)),
            // Week 22 — Luke 4-6
            arrayOf(gc(L,  4,16), gc(L,  4,31), gc(L,  4,38), gc(L,  5, 1), gc(L,  5,33), gc(L,  6, 1)),
            // Week 23 — Luke 6
            arrayOf(gc(L,  6, 6), gc(L,  6,12), gc(L,  6,20), gc(L,  6,27), gc(L,  6,39), gc(L,  6,43)),
            // Week 24 — Luke 7-8
            arrayOf(gc(L,  7, 1), gc(L,  7,11), gc(L,  7,31), gc(L,  7,36), gc(L,  8, 1), gc(L,  8, 4)),
            // Week 25 — Luke 8-9
            arrayOf(gc(L,  8,16), gc(L,  8,19), gc(L,  9, 1), gc(L,  9, 7), gc(L,  9,18), gc(L,  9,43)),
            // Week 26 — Luke 9-10
            arrayOf(gc(L,  9,46), gc(L,  9,51), gc(L,  9,57), gc(L, 10, 1), gc(L, 10,13), gc(L, 10,17)),
            // Week 27 — Luke 10-11
            arrayOf(gc(L, 10,25), gc(L, 10,38), gc(L, 11, 1), gc(L, 11, 5), gc(L, 11,15), gc(L, 11,27)),
            // Week 28 — Luke 11-12
            arrayOf(gc(L, 11,29), gc(L, 11,37), gc(L, 11,42), gc(L, 11,47), gc(L, 12, 1), gc(L, 12, 8)),
            // Week 29 — Luke 12-13
            arrayOf(gc(L, 12,13), gc(L, 12,35), gc(L, 12,39), gc(L, 12,49), gc(L, 12,54), gc(L, 13, 1)),
            // Week 30 — Luke 13-14
            arrayOf(gc(L, 13,10), gc(L, 13,18), gc(L, 13,22), gc(L, 13,31), gc(L, 14, 1), gc(L, 14, 7)),
            // Week 31 — Luke 14-16
            arrayOf(gc(L, 14,12), gc(L, 14,15), gc(L, 14,25), gc(L, 15, 1), gc(L, 16, 1), gc(L, 16, 9)),
            // Week 32 — Luke 17-18
            arrayOf(gc(L, 17, 1), gc(L, 17, 7), gc(L, 17,11), gc(L, 17,20), gc(L, 17,26), gc(L, 18, 1)),
            // Week 33 — Luke 18-20
            arrayOf(gc(L, 18,35), gc(L, 19, 1), gc(L, 19,11), gc(L, 19,41), gc(L, 19,45), gc(L, 20,27)),
            // Week 34 — Luke 21
            arrayOf(gc(L, 21, 1), gc(L, 21, 5), gc(L, 21,12), gc(L, 21,20), gc(L, 21,29), gc(L, 21,34))
        )
        val wIdx = (week - 1).coerceIn(0, 33)
        val dIdx = (dow.value - 1).coerceIn(0, 5)
        return table[wIdx][dIdx]
    }

    // ── LENT ─────────────────────────────────────────────────────────────────

    private fun lentGospel(pos: LiturgicalPosition): Triple<Gospel, Int, Int>? {
        return if (pos.dayOfWeek == DayOfWeek.SUNDAY) {
            lentSundayGospel(pos.week, pos.sundayCycle)
        } else {
            lentWeekdayGospel(pos.week, pos.dayOfWeek)
        }
    }

    private fun lentSundayGospel(week: Int, cycle: SundayCycle): Triple<Gospel, Int, Int>? = when (cycle) {
        SundayCycle.A -> when (week) {
            1 -> gc(M,  4,  1); 2 -> gc(M, 17,  1); 3 -> gc(J,  4,  5)
            4 -> gc(J,  9,  1); 5 -> gc(J, 11,  1); 6 -> gc(M, 26, 14)
            else -> null
        }
        SundayCycle.B -> when (week) {
            1 -> gc(Mk, 1, 12); 2 -> gc(Mk, 9,  2); 3 -> gc(J,  2, 13)
            4 -> gc(J,  3, 14); 5 -> gc(J, 12, 20); 6 -> gc(Mk,14,  1)
            else -> null
        }
        SundayCycle.C -> when (week) {
            1 -> gc(L,  4,  1); 2 -> gc(L,  9, 28); 3 -> gc(J,  4,  5)
            4 -> gc(L, 15,  1); 5 -> gc(J,  8,  1); 6 -> gc(L, 22, 14)
            else -> null
        }
    }

    private fun lentWeekdayGospel(week: Int, dow: DayOfWeek): Triple<Gospel, Int, Int>? {
        val table: Array<Array<Triple<Gospel, Int, Int>>> = arrayOf(
            // Week 1 (Ash Wed = Wed)
            arrayOf(gc(M, 25,31), gc(M,  6, 1), gc(M,  6, 7), gc(L, 11, 1), gc(M,  7, 7), gc(M,  5,43)),
            // Week 2
            arrayOf(gc(L,  6,36), gc(M, 23, 1), gc(L, 13,22), gc(M, 20,17), gc(L, 15, 1), gc(L, 15,11)),
            // Week 3
            arrayOf(gc(L,  4,24), gc(M,  5,17), gc(L, 11,29), gc(J,  4,43), gc(Mk,12,28), gc(L, 18, 9)),
            // Week 4
            arrayOf(gc(J,  4,43), gc(J,  5, 1), gc(J,  5,17), gc(J,  5,31), gc(J,  7, 1), gc(J,  7,40)),
            // Week 5
            arrayOf(gc(J,  8, 1), gc(J,  8,21), gc(J,  8,31), gc(J,  8,51), gc(J, 10,31), gc(J, 11,45)),
            // Holy Week
            arrayOf(gc(J, 12, 1), gc(J, 13,21), gc(M, 26,14), gc(J, 13, 1), gc(J, 18, 1), gc(L, 23,50))
        )
        val wIdx = (week - 1).coerceIn(0, 5)
        val dIdx = (dow.value - 1).coerceIn(0, 5)
        return table[wIdx][dIdx]
    }

    // ── EASTER ───────────────────────────────────────────────────────────────

    private fun easterGospel(pos: LiturgicalPosition): Triple<Gospel, Int, Int>? {
        return if (pos.dayOfWeek == DayOfWeek.SUNDAY) {
            easterSundayGospel(pos.week, pos.sundayCycle)
        } else {
            easterWeekdayGospel(pos.week, pos.dayOfWeek)
        }
    }

    private fun easterSundayGospel(week: Int, cycle: SundayCycle): Triple<Gospel, Int, Int>? = when (week) {
        1 -> gc(J, 20,  1)
        2 -> gc(J, 20, 19)
        3 -> when (cycle) {
            SundayCycle.A -> gc(L, 24, 13)
            SundayCycle.B -> gc(L, 24, 35)
            SundayCycle.C -> gc(J, 21,  1)
        }
        4 -> when (cycle) {
            SundayCycle.A -> gc(J, 10,  1)
            SundayCycle.B -> gc(J, 10, 11)
            SundayCycle.C -> gc(J, 10, 27)
        }
        5 -> when (cycle) {
            SundayCycle.A -> gc(J, 14,  1)
            SundayCycle.B -> gc(J, 15,  1)
            SundayCycle.C -> gc(J, 13, 31)
        }
        6 -> when (cycle) {
            SundayCycle.A -> gc(J, 14, 15)
            SundayCycle.B -> gc(J, 15,  9)
            SundayCycle.C -> gc(J, 14, 23)
        }
        7 -> when (cycle) {
            SundayCycle.A -> gc(J, 17,  1)
            SundayCycle.B -> gc(J, 17, 11)
            SundayCycle.C -> gc(J, 17, 20)
        }
        else -> null
    }

    private fun easterWeekdayGospel(week: Int, dow: DayOfWeek): Triple<Gospel, Int, Int>? {
        val table: Array<Array<Triple<Gospel, Int, Int>>> = arrayOf(
            // Easter Octave (week 1)
            arrayOf(gc(M, 28, 8), gc(J, 20,11), gc(L, 24,13), gc(L, 24,35), gc(J, 21, 1), gc(J, 21, 1)),
            // Week 2
            arrayOf(gc(J,  3, 1), gc(J,  3, 7), gc(J,  3,16), gc(J,  3,31), gc(J,  6, 1), gc(J,  6,16)),
            // Week 3
            arrayOf(gc(J,  6,22), gc(J,  6,30), gc(J,  6,35), gc(J,  6,44), gc(J,  6,52), gc(J,  6,60)),
            // Week 4
            arrayOf(gc(J, 10, 1), gc(J, 10,22), gc(J, 12,44), gc(J, 13,16), gc(J, 14, 1), gc(J, 14, 7)),
            // Week 5
            arrayOf(gc(J, 14,21), gc(J, 14,27), gc(J, 15, 1), gc(J, 15, 9), gc(J, 15,12), gc(J, 15,18)),
            // Week 6
            arrayOf(gc(J, 15,26), gc(J, 16, 5), gc(J, 16,12), gc(J, 16,16), gc(J, 16,20), gc(J, 16,23)),
            // Week 7
            arrayOf(gc(J, 16,29), gc(J, 17, 1), gc(J, 17,11), gc(J, 17,20), gc(J, 21,15), gc(J, 21,20))
        )
        val wIdx = (week - 1).coerceIn(0, 6)
        val dIdx = (dow.value - 1).coerceIn(0, 5)
        return table[wIdx][dIdx]
    }
}
