package com.tatara.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tatara.data.db.entity.CurfewLog
import com.tatara.data.db.entity.SleepChecklist
import com.tatara.data.db.entity.SleepLog
import com.tatara.data.db.entity.SleepTarget
import java.time.LocalDate

@Dao
interface SleepDao {
    @Insert suspend fun insertTarget(target: SleepTarget): Long
    @Insert suspend fun insertTargets(targets: List<SleepTarget>)
    @Query("SELECT * FROM sleep_target WHERE effectiveFrom <= :date ORDER BY effectiveFrom DESC, id DESC LIMIT 1")
    suspend fun targetInEffectOn(date: LocalDate): SleepTarget?
    @Query("SELECT * FROM sleep_target ORDER BY id") suspend fun getAllTargets(): List<SleepTarget>
    @Query("DELETE FROM sleep_target") suspend fun deleteAllTargets()

    @Insert suspend fun insertLog(log: SleepLog): Long
    @Insert suspend fun insertLogs(logs: List<SleepLog>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertLog(log: SleepLog)
    @Query("SELECT * FROM sleep_log WHERE date = :date LIMIT 1") suspend fun logOn(date: LocalDate): SleepLog?
    @Query("SELECT * FROM sleep_log WHERE date BETWEEN :from AND :to ORDER BY date")
    suspend fun logsBetween(from: LocalDate, to: LocalDate): List<SleepLog>
    @Query("SELECT * FROM sleep_log ORDER BY id") suspend fun getAllLogs(): List<SleepLog>
    @Query("DELETE FROM sleep_log") suspend fun deleteAllLogs()

    @Insert suspend fun insertCurfewLog(log: CurfewLog): Long
    @Insert suspend fun insertCurfewLogs(logs: List<CurfewLog>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertCurfewLog(log: CurfewLog)
    @Query("SELECT * FROM curfew_log WHERE date = :date LIMIT 1") suspend fun curfewOn(date: LocalDate): CurfewLog?
    @Query("SELECT * FROM curfew_log WHERE date BETWEEN :from AND :to ORDER BY date")
    suspend fun curfewsBetween(from: LocalDate, to: LocalDate): List<CurfewLog>
    @Query("SELECT * FROM curfew_log ORDER BY id") suspend fun getAllCurfewLogs(): List<CurfewLog>
    @Query("DELETE FROM curfew_log") suspend fun deleteAllCurfewLogs()

    @Insert suspend fun insertChecklist(checklist: SleepChecklist): Long
    @Insert suspend fun insertChecklists(checklists: List<SleepChecklist>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertChecklist(checklist: SleepChecklist)
    @Query("SELECT * FROM sleep_checklist WHERE date = :date LIMIT 1")
    suspend fun checklistOn(date: LocalDate): SleepChecklist?
    @Query("SELECT * FROM sleep_checklist WHERE date BETWEEN :from AND :to ORDER BY date")
    suspend fun checklistsBetween(from: LocalDate, to: LocalDate): List<SleepChecklist>
    @Query("SELECT * FROM sleep_checklist ORDER BY id") suspend fun getAllChecklists(): List<SleepChecklist>
    @Query("DELETE FROM sleep_checklist") suspend fun deleteAllChecklists()
}
