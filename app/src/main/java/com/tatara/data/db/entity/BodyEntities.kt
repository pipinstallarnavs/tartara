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

/**
 * §2.1 — weight may be back-filled up to 7 days; back-filled weights are flagged and
 * excluded from that week's TDEE calculation if entered after the Sunday recalculation.
 */
@Serializable
@Entity(
    tableName = "weight_entry",
    indices = [Index("date", unique = true)],
)
data class WeightEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val weightKg: Float,
    val isBackfilled: Boolean = false,
    val loggedAt: Instant,
)

/**
 * §3.5 — one row per Sunday recalculation. The intermediate arithmetic is stored so the
 * weekly review can show every number; nullable fields are null when the adjustment was
 * skipped (fewer than 5 logged days / fewer than 4 weigh-ins / first 14 days).
 */
@Serializable
@Entity(tableName = "target_adjustment")
data class TargetAdjustment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val effectiveFrom: LocalDate,
    val kcalTarget: Float,
    val proteinG: Float,
    val fatG: Float,
    val carbsG: Float,
    val meanDailyKcal: Float? = null,
    val ewmaStart: Float? = null,
    val ewmaEnd: Float? = null,
    val impliedTdee: Float? = null,
    val weeklyRatePercent: Float,
    val computedFromWeightKg: Float? = null,
)

/** For the Mifflin-St Jeor BMR used by the §3.5 sanity bounds. */
enum class Sex { MALE, FEMALE }

/**
 * Single row, id = 1. ratchetWeightKg is the protein ratchet of §3.4.1 — max EWMA this
 * block. heightCm/birthYear/sex exist only for the BMR calorie bounds; all nullable —
 * the bounds are skipped until they are set. lastProcessedWeekEnd is the Sunday of the
 * last week the TDEE job has examined (adjusted or skipped), so catch-up never
 * re-computes a week.
 */
@Serializable
@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 1,
    val proteinPerKg: Float = 1.8f,
    val goalRatePercent: Float = -0.5f,
    val blockStartDate: LocalDate? = null,
    val ratchetWeightKg: Float? = null,
    val heightCm: Float? = null,
    val birthYear: Int? = null,
    val sex: Sex? = null,
    val lastProcessedWeekEnd: LocalDate? = null,
    /** Last day the §5.4 automaticity engine has closed; days close on leaving the §2.1 edit window. */
    val lastHabitDayClosed: LocalDate? = null,
)
