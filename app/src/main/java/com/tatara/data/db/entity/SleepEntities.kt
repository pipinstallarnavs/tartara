@file:UseSerializers(LocalDateSerializer::class, LocalTimeSerializer::class, InstantSerializer::class)

package com.tatara.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tatara.data.db.InstantSerializer
import com.tatara.data.db.LocalDateSerializer
import com.tatara.data.db.LocalTimeSerializer
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

/**
 * §6.1 — targets are a dated history, never a single mutable row. A night is always
 * evaluated against the target in effect on that date; edits insert a new row.
 */
@Serializable
@Entity(
    tableName = "sleep_target",
    indices = [Index("effectiveFrom")],
)
data class SleepTarget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val effectiveFrom: LocalDate,
    val targetBedTime: LocalTime,
    val targetWakeTime: LocalTime,
    val curfewMinutes: Int = 90,
)

@Serializable
@Entity(
    tableName = "sleep_log",
    indices = [Index("date", unique = true)],
)
data class SleepLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val bedTime: LocalTime,
    val wakeTime: LocalTime,
    val timeToFallAsleepMin: Int,
    val wakeCount: Int,
    val quality: Int,
)

/** §6.4 — curfewStart is a snapshot of the target in effect that night. Earns no XP. */
@Serializable
@Entity(
    tableName = "curfew_log",
    indices = [Index("date", unique = true)],
)
data class CurfewLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val curfewStart: LocalTime,
    val lastScreenAt: LocalTime? = null,
    val held: Boolean,
)

/** §6.5 — screen curfew is not duplicated here; it lives in curfew_log. */
@Serializable
@Entity(
    tableName = "sleep_checklist",
    indices = [Index("date", unique = true)],
)
data class SleepChecklist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val caffeineCutoffMet: Boolean,
    val roomDark: Boolean,
    val roomCool: Boolean,
    val noLateLargeMeal: Boolean,
)
