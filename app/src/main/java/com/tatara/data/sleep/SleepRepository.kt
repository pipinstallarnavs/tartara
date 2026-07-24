package com.tatara.data.sleep

import com.tatara.data.EditWindow
import com.tatara.data.db.TataraDatabase
import com.tatara.data.db.entity.CurfewLog
import com.tatara.data.db.entity.SleepChecklist
import com.tatara.data.db.entity.SleepLog
import com.tatara.data.db.entity.SleepTarget
import java.time.LocalDate
import java.time.LocalTime

data class SleepStats(
    val regularity: Float?,
    val meanDurationMin: Float?,
)

class SleepRepository(
    private val db: TataraDatabase,
    private val today: () -> LocalDate = { LocalDate.now() },
) {

    /**
     * §6.1 — the target in effect on a date; a night is always judged against the
     * target of its own date. Falls back to the spec defaults when none is set.
     */
    suspend fun targetOn(date: LocalDate): SleepTarget =
        db.sleepDao().targetInEffectOn(date) ?: SleepTarget(
            effectiveFrom = date,
            targetBedTime = LocalTime.of(23, 0),
            targetWakeTime = LocalTime.of(7, 0),
            curfewMinutes = 90,
        )

    /** §6.1 — editing inserts a dated row; it never overwrites history. */
    suspend fun editTarget(bed: LocalTime, wake: LocalTime, curfewMinutes: Int): SleepTarget {
        val target = SleepTarget(
            effectiveFrom = today(),
            targetBedTime = bed,
            targetWakeTime = wake,
            curfewMinutes = curfewMinutes.coerceIn(30, 180),
        )
        return target.copy(id = db.sleepDao().insertTarget(target))
    }

    fun curfewStartFor(target: SleepTarget): LocalTime =
        target.targetBedTime.minusMinutes(target.curfewMinutes.toLong())

    suspend fun logOn(date: LocalDate): SleepLog? = db.sleepDao().logOn(date)

    /** §6.2 — one log per night; re-logging a date replaces it (within the §2.1 window). */
    suspend fun logSleep(
        date: LocalDate,
        bed: LocalTime,
        wake: LocalTime,
        timeToFallAsleepMin: Int,
        wakeCount: Int,
        quality: Int,
    ): Boolean {
        if (!EditWindow.isEditable(date, today())) return false
        if (quality !in 1..5) return false
        db.sleepDao().upsertLog(
            SleepLog(
                date = date, bedTime = bed, wakeTime = wake,
                timeToFallAsleepMin = timeToFallAsleepMin.coerceAtLeast(0),
                wakeCount = wakeCount.coerceAtLeast(0),
                quality = quality,
            )
        )
        return true
    }

    suspend fun curfewOn(date: LocalDate): CurfewLog? = db.sleepDao().curfewOn(date)

    /** §6.4 — the fast path: one tap, held or broke. curfewStart is snapshotted. */
    suspend fun logCurfew(date: LocalDate, held: Boolean): Boolean {
        if (!EditWindow.isEditable(date, today())) return false
        db.sleepDao().upsertCurfewLog(
            CurfewLog(date = date, curfewStart = curfewStartFor(targetOn(date)), held = held)
        )
        return true
    }

    /** §6.4 — the precise path: held is derived from the actual last-screen time. */
    suspend fun logCurfewPrecise(date: LocalDate, lastScreenAt: LocalTime): Boolean {
        if (!EditWindow.isEditable(date, today())) return false
        val start = curfewStartFor(targetOn(date))
        db.sleepDao().upsertCurfewLog(
            CurfewLog(
                date = date, curfewStart = start, lastScreenAt = lastScreenAt,
                held = SleepMath.heldCurfew(lastScreenAt, start),
            )
        )
        return true
    }

    suspend fun checklistOn(date: LocalDate): SleepChecklist? = db.sleepDao().checklistOn(date)

    suspend fun logChecklist(
        date: LocalDate,
        caffeineCutoffMet: Boolean,
        roomDark: Boolean,
        roomCool: Boolean,
        noLateLargeMeal: Boolean,
    ): Boolean {
        if (!EditWindow.isEditable(date, today())) return false
        db.sleepDao().upsertChecklist(
            SleepChecklist(
                date = date, caffeineCutoffMet = caffeineCutoffMet, roomDark = roomDark,
                roomCool = roomCool, noLateLargeMeal = noLateLargeMeal,
            )
        )
        return true
    }

    /** §6.4 — 28 marks, oldest first: held / broken / null for unlogged. */
    suspend fun curfewStrip(days: Int = 28): List<CurfewLog?> {
        val end = today()
        val byDate = db.sleepDao().curfewsBetween(end.minusDays(days - 1L), end).associateBy { it.date }
        return (days - 1 downTo 0).map { byDate[end.minusDays(it.toLong())] }
    }

    suspend fun heldOfLast(days: Int = 28): Int =
        curfewStrip(days).count { it?.held == true }

    /** §6.3 — regularity over the trailing 7 days (headline), mean duration (secondary). */
    suspend fun stats(): SleepStats {
        val end = today()
        val logs = db.sleepDao().logsBetween(end.minusDays(6), end)
        val durations = logs.map { SleepMath.durationMinutes(it.bedTime, it.wakeTime, it.timeToFallAsleepMin) }
        val midpoints = logs.mapIndexed { i, log -> SleepMath.midpointMinutes(log.bedTime, durations[i]) }
        return SleepStats(
            regularity = SleepMath.regularity(midpoints),
            meanDurationMin = if (durations.isEmpty()) null else durations.sum().toFloat() / durations.size,
        )
    }

    /** §6.5 — one comparison per checklist item plus the curfew, trailing 30 days. */
    suspend fun correlations(): List<SleepMath.Comparison> {
        val end = today()
        val from = end.minusDays(29)
        val quality = db.sleepDao().logsBetween(from, end).associate { it.date to it.quality }
        val checklists = db.sleepDao().checklistsBetween(from, end)
        val curfews = db.sleepDao().curfewsBetween(from, end)

        fun pairsOf(flags: List<Pair<LocalDate, Boolean>>): List<Pair<Boolean, Int>> =
            flags.mapNotNull { (date, flag) -> quality[date]?.let { flag to it } }

        return listOfNotNull(
            SleepMath.compare("Caffeine cutoff", pairsOf(checklists.map { it.date to it.caffeineCutoffMet })),
            SleepMath.compare("Screen curfew", pairsOf(curfews.map { it.date to it.held })),
            SleepMath.compare("Room dark", pairsOf(checklists.map { it.date to it.roomDark })),
            SleepMath.compare("Room cool", pairsOf(checklists.map { it.date to it.roomCool })),
            SleepMath.compare("No late meal", pairsOf(checklists.map { it.date to it.noLateLargeMeal })),
        )
    }
}
