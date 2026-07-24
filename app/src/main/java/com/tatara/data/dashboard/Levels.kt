package com.tatara.data.dashboard

import kotlin.math.roundToInt

/** §7.2 / §7.3 — levels, XP costs, diminishing returns, and the tier ladder. */
object Levels {

    const val MAX_LEVEL = 100

    /** Canonical (spec §7.3) tier names, band index 0–8; themes re-skin the display name. */
    val TIER_NAMES = listOf(
        "Tamahagane", "Orikaeshi", "Tsuchioki", "Yaki-ire", "Hamon",
        "Togi", "Mei", "Meibutsu", "Kokuhō",
    )

    /** §7.2 — XP cost to reach level n. Σ over 1..100 = 83,750. */
    fun cost(n: Int): Int = 80 + 15 * n

    /** Total XP needed to hold [level]. */
    fun cumulative(level: Int): Long = (1..level).sumOf { cost(it).toLong() }

    /** Level held at [xp], 1–100. Levels can fall when XP is lost. */
    fun levelFor(xp: Long): Int {
        var cum = 0L
        for (n in 1..MAX_LEVEL) {
            cum += cost(n)
            if (xp < cum) return (n - 1).coerceAtLeast(1)
        }
        return MAX_LEVEL
    }

    /** §7.3 — bands are 11 levels wide: 1–11, 12–22, …, 89–100. */
    fun bandStartLevel(band: Int): Int = 1 + band * 11

    fun bandFor(level: Int): Int {
        require(level in 1..MAX_LEVEL) { "level $level outside 1..$MAX_LEVEL" }
        return ((level - 1) / 11).coerceAtMost(8)
    }

    /** §7.3 — a tier floor is permanent: level never displays below the highest band entered. */
    fun effectiveLevel(xp: Long, highestBandEntered: Int): Int {
        val floor = if (highestBandEntered < 0) 1 else bandStartLevel(highestBandEntered)
        return maxOf(levelFor(xp), floor).coerceAtMost(MAX_LEVEL)
    }

    /** §7.3 — show the cliff before it arrives: within 2 levels of the tier floor. */
    fun nearFloor(level: Int, highestBandEntered: Int): Boolean {
        if (highestBandEntered <= 0) return false
        return level - bandStartLevel(highestBandEntered) < 2
    }

    fun xpToNextTier(xp: Long, band: Int): Long? {
        if (band >= 8) return null
        return (cumulative(bandStartLevel(band + 1)) - xp).coerceAtLeast(0)
    }

    /** §7.2 — diminishing returns on workouts within a week: dr(n) = 1/(1+0.35(n−1)). */
    fun dr(n: Int): Float = 1f / (1f + 0.35f * (n - 1))

    // §7.2 earning table.
    const val XP_RING_CLOSED = 40
    const val XP_HABIT_COMPLETED = 12
    const val XP_WORKOUT_BASE = 30
    const val XP_SLEEP_LOGGED = 8
    const val XP_WEIGHT_LOGGED = 5
    const val XP_REVIEW_OPENED = 25

    fun workoutXp(nThisWeek: Int): Int = (XP_WORKOUT_BASE * dr(nThisWeek)).roundToInt()
}
