package com.tatara.data.habit

import com.tatara.data.db.entity.HabitLog
import com.tatara.data.db.entity.HabitLogStatus
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.sqrt

/** §5.6 — Tight ≤30 min, Variable ≤90 min, Scattered beyond (std dev of completion time). */
enum class StabilityBand(val label: String) {
    TIGHT("Tight"),
    VARIABLE("Variable"),
    SCATTERED("Scattered"),
}

object HabitMetrics {

    /**
     * §5.6 — the headline number: completed ÷ eligible days over the trailing 28,
     * as a percentage. Frozen days are neutral (out of the denominator), days before
     * the habit existed don't count, today is still in progress and excluded.
     * Unlogged past days count as misses — that is what makes it degrade gracefully.
     */
    fun consistency(logs: List<HabitLog>, createdOn: LocalDate, today: LocalDate): Float? {
        val from = maxOf(createdOn, today.minusDays(27))
        val to = today.minusDays(1)
        if (to.isBefore(from)) return null
        val byDate = logs.associateBy { it.date }
        var eligible = 0
        var completed = 0
        var d = from
        while (!d.isAfter(to)) {
            val status = byDate[d]?.status
            if (status != HabitLogStatus.FROZEN) {
                eligible++
                if (status == HabitLogStatus.COMPLETED) completed++
            }
            d = d.plusDays(1)
        }
        return if (eligible == 0) null else completed * 100f / eligible
    }

    /** §5.6 — decoration only. Frozen days preserve the streak without extending it. */
    fun currentStreak(logs: List<HabitLog>, today: LocalDate): Int {
        val byDate = logs.associateBy { it.date }
        var streak = 0
        var d = if (byDate[today]?.status == HabitLogStatus.COMPLETED) today else today.minusDays(1)
        while (true) {
            when (byDate[d]?.status) {
                HabitLogStatus.COMPLETED -> streak++
                HabitLogStatus.FROZEN -> Unit
                else -> return streak
            }
            d = d.minusDays(1)
        }
    }

    fun longestStreak(logs: List<HabitLog>): Int {
        var longest = 0
        var current = 0
        var prev: LocalDate? = null
        for (log in logs.sortedBy { it.date }) {
            val contiguous = prev != null && log.date == prev.plusDays(1)
            when (log.status) {
                HabitLogStatus.COMPLETED -> {
                    current = if (contiguous || prev == null) current + 1 else 1
                    longest = maxOf(longest, current)
                }
                HabitLogStatus.FROZEN -> if (!contiguous && prev != null) current = 0
                HabitLogStatus.MISSED -> current = 0
            }
            prev = log.date
        }
        return longest
    }

    /**
     * §5.6 — context stability: std dev of completion time-of-day, banded. Needs at
     * least 5 timed completions; times near midnight are not wrapped (documented
     * simplification — these habits anchor to waking hours).
     */
    fun contextStability(times: List<LocalTime>): StabilityBand? {
        if (times.size < 5) return null
        val minutes = times.map { it.toSecondOfDay() / 60f }
        val mean = minutes.sum() / minutes.size
        val sd = sqrt(minutes.sumOf { ((it - mean) * (it - mean)).toDouble() } / minutes.size).toFloat()
        return when {
            sd <= 30f -> StabilityBand.TIGHT
            sd <= 90f -> StabilityBand.VARIABLE
            else -> StabilityBand.SCATTERED
        }
    }
}
