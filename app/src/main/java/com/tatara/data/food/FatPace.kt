package com.tatara.data.food

/**
 * §3.6 — the metric is the fat density of the calories remaining, not a fixed gram
 * threshold. Colour mapping (§2.5): FAT_LIGHT = cool, ON_PACE = primary, FAT_LOADED =
 * warm at 60% opacity (the one amber in the app), SPENT = warm, OVER = muted.
 */
enum class FatPaceState(val message: String?) {
    FAT_LIGHT("Room for fat — add oil or nuts"),
    ON_PACE(null),
    FAT_LOADED("Lean choices from here"),
    SPENT("Chicken and rice territory"),
    OVER("Over on fat — tomorrow's problem"),
}

object FatPace {

    fun state(remainingFatG: Float, remainingKcal: Float): FatPaceState = when {
        remainingFatG < 0f -> FatPaceState.OVER
        remainingKcal <= 0f -> FatPaceState.SPENT
        else -> {
            val pace = remainingFatG / remainingKcal
            when {
                pace > 0.045f -> FatPaceState.FAT_LIGHT
                pace >= 0.020f -> FatPaceState.ON_PACE
                pace >= 0.012f -> FatPaceState.FAT_LOADED
                else -> FatPaceState.SPENT
            }
        }
    }

    /**
     * §3.6 — the protein mirror, inverted: flag when the calories left demand a
     * markedly higher protein density than the day's overall target ratio.
     */
    fun proteinBehind(
        remainingProteinG: Float,
        remainingKcal: Float,
        targetProteinG: Float,
        targetKcal: Float,
    ): Boolean {
        if (remainingProteinG <= 0f || targetKcal <= 0f) return false
        if (remainingKcal <= 0f) return true
        val neededDensity = remainingProteinG / remainingKcal
        val targetDensity = targetProteinG / targetKcal
        return neededDensity > targetDensity * 1.5f
    }
}
