package com.tatara.data.habit

import com.tatara.data.db.entity.HabitLog
import com.tatara.data.db.entity.HabitLogStatus
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HabitMetricsTest {

    private val today = LocalDate.of(2026, 7, 23)

    private fun log(daysAgo: Long, status: HabitLogStatus) =
        HabitLog(habitId = 1, date = today.minusDays(daysAgo), status = status)

    @Test
    fun consistencyCountsUnloggedDaysAsMisses() {
        // Created 10 days ago; completed 4 of the 9 closed-or-open past days.
        val logs = listOf(log(1, HabitLogStatus.COMPLETED), log(2, HabitLogStatus.COMPLETED),
            log(4, HabitLogStatus.COMPLETED), log(7, HabitLogStatus.COMPLETED))
        val c = HabitMetrics.consistency(logs, createdOn = today.minusDays(9), today = today)
        assertEquals(4f / 9f * 100f, c!!, 0.01f)
    }

    @Test
    fun frozenDaysAreNeutralInConsistency() {
        // 3 eligible days (frozen excluded), 2 completed.
        val logs = listOf(
            log(1, HabitLogStatus.COMPLETED),
            log(2, HabitLogStatus.FROZEN),
            log(3, HabitLogStatus.COMPLETED),
            log(4, HabitLogStatus.MISSED),
        )
        val c = HabitMetrics.consistency(logs, createdOn = today.minusDays(4), today = today)
        assertEquals(2f / 3f * 100f, c!!, 0.01f)
    }

    @Test
    fun consistencyWindowIs28Days() {
        // Habit far older than the window: only the trailing 28 count.
        val logs = (1..28L).map { log(it, HabitLogStatus.COMPLETED) } +
            (29..60L).map { log(it, HabitLogStatus.MISSED) }
        val c = HabitMetrics.consistency(logs, createdOn = today.minusDays(100), today = today)
        assertEquals(100f, c!!, 0.01f)
    }

    @Test
    fun consistencyNullOnDayOne() {
        assertNull(HabitMetrics.consistency(emptyList(), createdOn = today, today = today))
    }

    @Test
    fun streakPassesThroughFrozenDays() {
        val logs = listOf(
            log(1, HabitLogStatus.COMPLETED),
            log(2, HabitLogStatus.FROZEN),
            log(3, HabitLogStatus.COMPLETED),
            log(4, HabitLogStatus.COMPLETED),
            log(5, HabitLogStatus.MISSED),
        )
        assertEquals(3, HabitMetrics.currentStreak(logs, today))
    }

    @Test
    fun streakIncludesTodayOnlyWhenCompleted() {
        val base = listOf(log(1, HabitLogStatus.COMPLETED), log(2, HabitLogStatus.COMPLETED))
        assertEquals(2, HabitMetrics.currentStreak(base, today))
        assertEquals(3, HabitMetrics.currentStreak(base + log(0, HabitLogStatus.COMPLETED), today))
    }

    @Test
    fun streakZeroAfterMissedYesterday() {
        val logs = listOf(log(1, HabitLogStatus.MISSED), log(2, HabitLogStatus.COMPLETED))
        assertEquals(0, HabitMetrics.currentStreak(logs, today))
    }

    @Test
    fun longestStreakSurvivesFrozenBridgesAndBreaksOnGaps() {
        val logs = listOf(
            log(10, HabitLogStatus.COMPLETED),
            log(9, HabitLogStatus.COMPLETED),
            log(8, HabitLogStatus.FROZEN),
            log(7, HabitLogStatus.COMPLETED),
            // unlogged day 6 — gap breaks the chain
            log(5, HabitLogStatus.COMPLETED),
            log(4, HabitLogStatus.COMPLETED),
        )
        assertEquals(3, HabitMetrics.longestStreak(logs))
    }

    @Test
    fun contextStabilityBands() {
        fun times(vararg minuteOffsets: Int) =
            minuteOffsets.map { LocalTime.of(7, 0).plusMinutes(it.toLong()) }

        assertEquals(StabilityBand.TIGHT, HabitMetrics.contextStability(times(0, 5, 10, 3, 7)))
        assertEquals(
            StabilityBand.VARIABLE,
            HabitMetrics.contextStability(times(0, 60, 120, 30, 90)),
        )
        assertEquals(
            StabilityBand.SCATTERED,
            HabitMetrics.contextStability(times(0, 240, 480, 120, 360)),
        )
        assertNull(HabitMetrics.contextStability(times(0, 5, 10, 3)))
    }
}
