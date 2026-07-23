@file:UseSerializers(LocalDateSerializer::class, LocalTimeSerializer::class, InstantSerializer::class)

package com.tatara.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tatara.data.db.InstantSerializer
import com.tatara.data.db.LocalDateSerializer
import com.tatara.data.db.LocalTimeSerializer
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

/** §7.2 — XP is a ledger; level and tier are derived from the sum. Amounts may be negative. */
enum class XpEventType {
    RING_CLOSED,
    HABIT_COMPLETED,
    WORKOUT_LOGGED,
    SLEEP_LOGGED,
    WEIGHT_LOGGED,
    REVIEW_OPENED,
    HABIT_MISS_PENALTY,
}

@Serializable
@Entity(
    tableName = "xp_event",
    indices = [Index("date")],
)
data class XpEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val type: XpEventType,
    val amount: Int,
)

/** §7.3 — tier crossings are permanent; this table only ever grows. */
@Serializable
@Entity(tableName = "tier_crossing")
data class TierCrossing(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tier: String,
    val level: Int,
    val date: LocalDate,
)

/** §7.5 — reviews are archived and browsable; opening one awards XP once. */
@Serializable
@Entity(
    tableName = "weekly_review",
    indices = [Index("weekStart", unique = true)],
)
data class WeeklyReview(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekStart: LocalDate,
    val generatedAt: Instant,
    val openedAt: Instant? = null,
)

/**
 * §2.4 — cache of per-day aggregates, recomputed only for days that changed.
 * Not @Serializable on purpose: it is derivable, so it is excluded from export
 * and cleared on import.
 */
@Entity(tableName = "daily_rollup")
data class DailyRollup(
    @PrimaryKey val date: LocalDate,
    val kcal: Float = 0f,
    val proteinG: Float = 0f,
    val carbsG: Float = 0f,
    val fatG: Float = 0f,
    val satFatG: Float = 0f,
    val habitsCompleted: Int = 0,
    val habitsTotal: Int = 0,
    val ringClosed: Boolean = false,
)
