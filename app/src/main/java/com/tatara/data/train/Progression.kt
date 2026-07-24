package com.tatara.data.train

import com.tatara.data.db.entity.RoutineItem
import com.tatara.data.db.entity.SetEntry

/** §4.3 — deterministic progression. No AI. */
object Progression {

    const val DELOAD_FACTOR = 0.9f

    /** Epley: e1RM = weight × (1 + reps/30). */
    fun e1rm(weightKg: Float, reps: Int): Float = weightKg * (1 + reps / 30f)

    /**
     * Double progression: increment only when the slot's full set count was done
     * and every working set hit the top of the rep range.
     */
    fun shouldIncrement(item: RoutineItem, workingSets: List<SetEntry>): Boolean =
        workingSets.size >= item.targetSets &&
            workingSets.isNotEmpty() &&
            workingSets.all { it.reps >= item.repRangeHigh }

    /**
     * §4.3 — stalled when the best e1RM has not improved across 3 consecutive
     * sessions: neither of the last two sessions beat the one three back.
     * bests are chronological, one per session.
     */
    fun stalled(sessionBests: List<Float>): Boolean {
        if (sessionBests.size < 3) return false
        val reference = sessionBests[sessionBests.size - 3]
        return sessionBests.takeLast(2).max() <= reference
    }

    /**
     * §4.2 — per-side plate stack for a target weight. Null when the target is
     * below the bar or not representable with the available plates.
     */
    fun plateStack(
        targetKg: Float,
        barKg: Float = 20f,
        plates: List<Float> = listOf(25f, 20f, 15f, 10f, 5f, 2.5f, 1.25f),
    ): List<Float>? {
        var remaining = (targetKg - barKg) / 2f
        if (remaining < -0.01f) return null
        val stack = mutableListOf<Float>()
        for (plate in plates.sortedDescending()) {
            while (remaining >= plate - 0.001f) {
                stack.add(plate)
                remaining -= plate
            }
        }
        return if (remaining > 0.01f) null else stack
    }
}
