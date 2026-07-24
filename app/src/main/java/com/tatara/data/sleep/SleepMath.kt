package com.tatara.data.sleep

import java.time.LocalTime
import kotlin.math.sqrt

object SleepMath {

    /** §6.3 — duration = wake − bed − timeToFallAsleep, across midnight, floored at 0. */
    fun durationMinutes(bed: LocalTime, wake: LocalTime, timeToFallAsleepMin: Int): Int {
        val spanMin = ((wake.toSecondOfDay() - bed.toSecondOfDay() + 86400) % 86400) / 60
        return (spanMin - timeToFallAsleepMin).coerceAtLeast(0)
    }

    /**
     * Times before noon are treated as belonging to the next day (+24h), so a
     * 23:50 and a 00:10 bedtime sit 20 minutes apart, not 23 hours.
     */
    fun shiftedMinutes(t: LocalTime): Int {
        val m = t.toSecondOfDay() / 60
        return if (m < 720) m + 1440 else m
    }

    /** §6.3 — midpoint = bed + duration/2, on the shifted scale. */
    fun midpointMinutes(bed: LocalTime, durationMin: Int): Float =
        shiftedMinutes(bed) + durationMin / 2f

    /**
     * §6.3 — regularity = 100 − sd(midpoints, minutes) × 1.5, clamped 0–100.
     * The headline metric; timing consistency predicts outcomes better than
     * duration. Needs at least 2 nights.
     */
    fun regularity(midpoints: List<Float>): Float? {
        if (midpoints.size < 2) return null
        val mean = midpoints.sum() / midpoints.size
        val sd = sqrt(midpoints.sumOf { ((it - mean) * (it - mean)).toDouble() } / midpoints.size).toFloat()
        return (100f - sd * 1.5f).coerceIn(0f, 100f)
    }

    /** "Did the phone go down by curfew" on the shifted scale, so 00:30 breaks a 21:30 curfew. */
    fun heldCurfew(lastScreenAt: LocalTime, curfewStart: LocalTime): Boolean =
        shiftedMinutes(lastScreenAt) <= shiftedMinutes(curfewStart)

    data class Comparison(
        val label: String,
        val yesMean: Float,
        val yesN: Int,
        val noMean: Float,
        val noN: Int,
    )

    /**
     * §6.5 — plain arithmetic: mean quality on yes-nights vs no-nights. Suppressed
     * (null) below 5 observations on either side. No significance, no causation.
     */
    fun compare(label: String, pairs: List<Pair<Boolean, Int>>, minN: Int = 5): Comparison? {
        val yes = pairs.filter { it.first }.map { it.second }
        val no = pairs.filter { !it.first }.map { it.second }
        if (yes.size < minN || no.size < minN) return null
        return Comparison(
            label = label,
            yesMean = yes.sum().toFloat() / yes.size,
            yesN = yes.size,
            noMean = no.sum().toFloat() / no.size,
            noN = no.size,
        )
    }
}
