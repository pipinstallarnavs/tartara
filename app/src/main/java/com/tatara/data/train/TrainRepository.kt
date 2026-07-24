package com.tatara.data.train

import com.tatara.data.db.TataraDatabase
import com.tatara.data.db.entity.Exercise
import com.tatara.data.db.entity.Routine
import com.tatara.data.db.entity.RoutineItem
import com.tatara.data.db.entity.Session
import com.tatara.data.db.entity.SetEntry
import java.time.LocalDate

/**
 * §4.2.1 — a pre-filled set: rendered dimmed, editable, and NOT persisted, counted,
 * or fed into progression until confirmed with a tap.
 */
data class PrefillSet(
    val exerciseId: Long,
    val routineItemId: Long?,
    val setIndex: Int,
    val weightKg: Float,
    val reps: Int,
    val isWarmup: Boolean = false,
    /** §4.2.1 — the app moved this weight; render the small warm dot. */
    val incremented: Boolean = false,
    /** §4.2 — the previous session's numbers, as ghost text ("100×5"). */
    val ghost: String? = null,
)

data class SessionStart(
    val session: Session,
    val routine: Routine?,
    val prefill: List<PrefillSet>,
)

sealed interface FinishResult {
    data class Finished(val session: Session, val incrementedItems: List<RoutineItem>) : FinishResult
    /** §4.2.1 — bail out with nothing confirmed and the session never happened. */
    data object Discarded : FinishResult
}

class TrainRepository(
    private val db: TataraDatabase,
    private val today: () -> LocalDate = { LocalDate.now() },
) {

    suspend fun exercises(): List<Exercise> = db.trainDao().getAllExercises()
    suspend fun routines(): List<Routine> = db.trainDao().routinesInOrder()
    suspend fun itemsFor(routineId: Long): List<RoutineItem> = db.trainDao().itemsForRoutine(routineId)
    suspend fun history(): List<Session> = db.trainDao().sessionsNewestFirst()
    suspend fun setsFor(sessionId: Long): List<SetEntry> = db.trainDao().setsForSession(sessionId)

    suspend fun createRoutine(name: String): Routine {
        val order = (routines().maxOfOrNull { it.sortOrder } ?: -1) + 1
        val routine = Routine(name = name.trim(), sortOrder = order)
        return routine.copy(id = db.trainDao().insertRoutine(routine))
    }

    suspend fun addRoutineItem(
        routineId: Long,
        exerciseId: Long,
        targetSets: Int,
        repRangeLow: Int,
        repRangeHigh: Int,
        incrementKg: Float,
        startWeightKg: Float,
    ): RoutineItem {
        val order = (itemsFor(routineId).maxOfOrNull { it.sortOrder } ?: -1) + 1
        val item = RoutineItem(
            routineId = routineId, exerciseId = exerciseId, targetSets = targetSets,
            sortOrder = order, repRangeLow = repRangeLow, repRangeHigh = repRangeHigh,
            incrementKg = incrementKg, currentWeightKg = startWeightKg,
        )
        return item.copy(id = db.trainDao().insertRoutineItem(item))
    }

    /**
     * §4.1 — routines are a cycle, not a calendar: the next routine is the one
     * after the last session's, wrapping. Missed days just resume the cycle.
     */
    suspend fun nextRoutine(): Routine? {
        val ordered = routines()
        if (ordered.isEmpty()) return null
        val lastRoutineId = db.trainDao().lastRoutineSession()?.routineId ?: return ordered.first()
        val lastIndex = ordered.indexOfFirst { it.id == lastRoutineId }
        return if (lastIndex < 0) ordered.first() else ordered[(lastIndex + 1) % ordered.size]
    }

    /**
     * §4.2.1 — "Repeat last": every exercise and set pre-filled from the previous
     * time this routine's slots were done, with the weight already at the slot's
     * currentWeightKg (which the finish step incremented if progression fired).
     */
    suspend fun startSession(routine: Routine?): SessionStart {
        val prefill = mutableListOf<PrefillSet>()
        if (routine != null) {
            for (item in itemsFor(routine.id)) {
                val last = db.trainDao().lastSetsForItem(item.id)
                val lastWorking = last.filter { !it.isWarmup }
                val incremented = lastWorking.isNotEmpty() &&
                    item.currentWeightKg > lastWorking.maxOf { it.weightKg }
                if (last.isEmpty()) {
                    repeat(item.targetSets) { i ->
                        prefill.add(
                            PrefillSet(
                                exerciseId = item.exerciseId, routineItemId = item.id, setIndex = i,
                                weightKg = item.currentWeightKg, reps = item.repRangeLow,
                            )
                        )
                    }
                } else {
                    last.forEachIndexed { i, prev ->
                        prefill.add(
                            PrefillSet(
                                exerciseId = item.exerciseId, routineItemId = item.id, setIndex = i,
                                weightKg = if (prev.isWarmup) prev.weightKg else item.currentWeightKg,
                                reps = when {
                                    prev.isWarmup -> prev.reps
                                    incremented -> item.repRangeLow
                                    else -> prev.reps
                                },
                                isWarmup = prev.isWarmup,
                                incremented = incremented && !prev.isWarmup,
                                ghost = "${trim(prev.weightKg)}×${prev.reps}",
                            )
                        )
                    }
                }
            }
        }
        val session = Session(date = today(), routineId = routine?.id)
        val id = db.trainDao().insertSession(session)
        return SessionStart(session.copy(id = id), routine, prefill)
    }

    /** One tap: the set exists only from this moment (§4.2.1 guardrail). */
    suspend fun confirmSet(
        sessionId: Long,
        exerciseId: Long,
        routineItemId: Long?,
        setIndex: Int,
        weightKg: Float,
        reps: Int,
        isWarmup: Boolean = false,
    ): SetEntry {
        val entry = SetEntry(
            sessionId = sessionId, exerciseId = exerciseId, routineItemId = routineItemId,
            setIndex = setIndex, weightKg = weightKg, reps = reps, isWarmup = isWarmup,
        )
        return entry.copy(id = db.trainDao().insertSet(entry))
    }

    /**
     * §4.3 — apply double progression per slot at finish; a session with nothing
     * confirmed is deleted as if it never started.
     */
    suspend fun finishSession(sessionId: Long, durationMin: Int?): FinishResult {
        val session = db.trainDao().sessionById(sessionId) ?: return FinishResult.Discarded
        val sets = db.trainDao().setsForSession(sessionId)
        if (sets.isEmpty()) {
            db.trainDao().deleteSessionById(sessionId)
            return FinishResult.Discarded
        }
        db.trainDao().updateSession(session.copy(durationMin = durationMin))

        val incremented = mutableListOf<RoutineItem>()
        for (itemId in sets.mapNotNull { it.routineItemId }.distinct()) {
            val item = db.trainDao().routineItemById(itemId) ?: continue
            val working = sets.filter { it.routineItemId == itemId && !it.isWarmup }
            if (Progression.shouldIncrement(item, working)) {
                val updated = item.copy(currentWeightKg = item.currentWeightKg + item.incrementKg)
                db.trainDao().updateRoutineItem(updated)
                incremented.add(updated)
            }
        }
        return FinishResult.Finished(session.copy(durationMin = durationMin), incremented)
    }

    /**
     * §4.1 — e1RM is global per exercise: a 100kg×5 is the same evidence whichever
     * slot it happened on. Best per session, warmups excluded, chronological.
     */
    suspend fun e1rmHistory(exerciseId: Long): List<Pair<LocalDate, Float>> {
        val dates = db.trainDao().getAllSessions().associate { it.id to it.date }
        return db.trainDao().workingSetsForExercise(exerciseId)
            .groupBy { it.sessionId }
            .mapNotNull { (sessionId, sets) ->
                dates[sessionId]?.let { date -> date to sets.maxOf { Progression.e1rm(it.weightKg, it.reps) } }
            }
            .sortedBy { it.first }
    }

    /** §4.3 — flag only; deload (90%) is a suggestion, never auto-applied. */
    suspend fun isStalled(exerciseId: Long): Boolean =
        Progression.stalled(e1rmHistory(exerciseId).map { it.second })

    suspend fun stallFlags(): List<Exercise> =
        exercises().filter { isStalled(it.id) }

    /** §4.4 — weekly volume by muscle group, in working sets, not tonnage. */
    suspend fun weeklySetsByMuscleGroup(): Map<String, Int> {
        val end = today()
        val sessions = db.trainDao().sessionsBetween(end.minusDays(6), end)
        val groupOf = exercises().associate { it.id to it.muscleGroup }
        val counts = mutableMapOf<String, Int>()
        for (session in sessions) {
            for (set in db.trainDao().setsForSession(session.id)) {
                if (set.isWarmup) continue
                val group = groupOf[set.exerciseId] ?: continue
                counts[group] = (counts[group] ?: 0) + 1
            }
        }
        return counts
    }

    private fun trim(v: Float): String =
        if (v == v.toInt().toFloat()) v.toInt().toString() else v.toString()
}
