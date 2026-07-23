@file:UseSerializers(LocalDateSerializer::class, LocalTimeSerializer::class, InstantSerializer::class)

package com.tatara.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tatara.data.db.InstantSerializer
import com.tatara.data.db.LocalDateSerializer
import com.tatara.data.db.LocalTimeSerializer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

/** §5.2 — the widget and Habits list render stacks as rows, not individual checkboxes. */
@Serializable
@Entity(tableName = "stack")
data class Stack(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int,
)

/** §5.1 — HYGIENE items cannot cost XP or levels; only HABIT items drive the engine. */
enum class HabitList { HYGIENE, HABIT }

@Serializable
@Entity(
    tableName = "habit",
    foreignKeys = [
        ForeignKey(
            entity = Stack::class,
            parentColumns = ["id"],
            childColumns = ["stackId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("stackId")],
)
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val list: HabitList,
    val stackId: Long? = null,
    val cue: String = "",
    val intendedTime: LocalTime? = null,
    val location: String? = null,
    val intention: String = "",
    val automaticity: Float = 0f,
    val consecutiveMisses: Int = 0,
    val createdAt: Instant,
    val graduatedAt: Instant? = null,
)

/** §5.6 — FROZEN is a freeze token applied to a missed day: no penalty, no XP, streak kept. */
enum class HabitLogStatus { COMPLETED, MISSED, FROZEN }

/** completedAt feeds the context stability score (§5.6). */
@Serializable
@Entity(
    tableName = "habit_log",
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("habitId", "date", unique = true), Index("date")],
)
data class HabitLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val date: LocalDate,
    val status: HabitLogStatus,
    val completedAt: Instant? = null,
)
