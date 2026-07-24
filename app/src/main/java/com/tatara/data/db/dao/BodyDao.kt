package com.tatara.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tatara.data.db.entity.Settings
import com.tatara.data.db.entity.TargetAdjustment
import com.tatara.data.db.entity.WeightEntry
import java.time.LocalDate

@Dao
interface BodyDao {
    @Insert suspend fun insertWeight(entry: WeightEntry): Long
    @Insert suspend fun insertWeights(entries: List<WeightEntry>)
    @Query("SELECT * FROM weight_entry WHERE date BETWEEN :from AND :to ORDER BY date")
    suspend fun weightsBetween(from: LocalDate, to: LocalDate): List<WeightEntry>
    @Query("SELECT * FROM weight_entry ORDER BY id") suspend fun getAllWeights(): List<WeightEntry>
    @Query("SELECT MIN(date) FROM weight_entry") suspend fun firstWeightDate(): LocalDate?
    @Query("DELETE FROM weight_entry") suspend fun deleteAllWeights()

    @Insert suspend fun insertAdjustment(adjustment: TargetAdjustment): Long
    @Insert suspend fun insertAdjustments(adjustments: List<TargetAdjustment>)
    @Query("SELECT * FROM target_adjustment ORDER BY effectiveFrom DESC LIMIT 1")
    suspend fun latestAdjustment(): TargetAdjustment?
    @Query("SELECT * FROM target_adjustment ORDER BY id")
    suspend fun getAllAdjustments(): List<TargetAdjustment>
    @Query("DELETE FROM target_adjustment") suspend fun deleteAllAdjustments()

    @Query("SELECT * FROM settings WHERE id = 1") suspend fun getSettings(): Settings?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertSettings(settings: Settings)
    @Query("DELETE FROM settings") suspend fun deleteSettings()
}
