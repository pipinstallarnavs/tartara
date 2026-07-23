package com.tatara.data

import java.time.LocalDate

/**
 * §2.1 — entries may be created or edited for today, yesterday, and the day before.
 * Body weight alone may be back-filled up to 7 days.
 */
object EditWindow {
    const val ENTRY_DAYS = 2L
    const val WEIGHT_DAYS = 7L

    fun isEditable(date: LocalDate, today: LocalDate): Boolean =
        !date.isAfter(today) && !date.isBefore(today.minusDays(ENTRY_DAYS))

    fun isWeightEditable(date: LocalDate, today: LocalDate): Boolean =
        !date.isAfter(today) && !date.isBefore(today.minusDays(WEIGHT_DAYS))
}
