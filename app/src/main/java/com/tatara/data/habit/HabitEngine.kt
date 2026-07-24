package com.tatara.data.habit

enum class HabitStage(val label: String) {
    INITIATION("Initiation"),
    LEARNING("Learning"),
    STABILISING("Stabilising"),
    AUTOMATIC("Automatic"),
}

/**
 * §5.4 — growth is asymptotic, not linear; misses are asymmetric. This yields ≈50%
 * automaticity at 15 completed days, ≈80% at 35, ≈95% at 65, and never reaches 100.
 */
object Automaticity {

    const val K = 0.045f

    /** §5.1 — reaching 95% graduates a HABIT to Hygiene and frees its slot. */
    const val GRADUATION = 95f

    fun afterCompletion(a: Float): Float = a + K * (100f - a)

    /**
     * Penalty by *consecutive* misses: a bad Tuesday is free, a bad week costs
     * real ground. The counter resets to zero on any completion.
     */
    fun penaltyFor(consecutiveMisses: Int): Float = when {
        consecutiveMisses <= 1 -> 0f
        consecutiveMisses == 2 -> 0.02f
        consecutiveMisses == 3 -> 0.05f
        else -> 0.08f
    }

    fun afterMiss(a: Float, consecutiveMisses: Int): Float = a * (1f - penaltyFor(consecutiveMisses))

    /** §5.5 — a display label; never regressed by a single miss (it just follows the number). */
    fun stage(a: Float): HabitStage = when {
        a < 30f -> HabitStage.INITIATION
        a < 60f -> HabitStage.LEARNING
        a < 85f -> HabitStage.STABILISING
        else -> HabitStage.AUTOMATIC
    }
}
