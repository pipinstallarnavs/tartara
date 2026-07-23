package com.tatara.data.backup

import com.tatara.data.db.entity.CurfewLog
import com.tatara.data.db.entity.Exercise
import com.tatara.data.db.entity.Food
import com.tatara.data.db.entity.FoodEntry
import com.tatara.data.db.entity.Habit
import com.tatara.data.db.entity.HabitLog
import com.tatara.data.db.entity.Routine
import com.tatara.data.db.entity.RoutineItem
import com.tatara.data.db.entity.SavedMeal
import com.tatara.data.db.entity.SavedMealItem
import com.tatara.data.db.entity.Session
import com.tatara.data.db.entity.SetEntry
import com.tatara.data.db.entity.Settings
import com.tatara.data.db.entity.SleepChecklist
import com.tatara.data.db.entity.SleepLog
import com.tatara.data.db.entity.SleepTarget
import com.tatara.data.db.entity.Stack
import com.tatara.data.db.entity.TargetAdjustment
import com.tatara.data.db.entity.TierCrossing
import com.tatara.data.db.entity.WeeklyReview
import com.tatara.data.db.entity.WeightEntry
import com.tatara.data.db.entity.XpEvent
import kotlinx.serialization.Serializable

/**
 * §2.3 — any change to this shape (or to a @Serializable entity) requires bumping
 * SchemaMigrations.CURRENT and adding a converter step. ExportShapeTest pins the
 * current shape so an accidental change fails the build.
 */
@Serializable
data class ExportFile(
    val schemaVersion: Int,
    val exportedAt: String,
    val appVersion: String,
    val data: ExportData,
)

/** daily_rollup is deliberately absent: it is a derivable cache (§2.4). */
@Serializable
data class ExportData(
    val settings: Settings? = null,
    val foods: List<Food> = emptyList(),
    val foodEntries: List<FoodEntry> = emptyList(),
    val savedMeals: List<SavedMeal> = emptyList(),
    val savedMealItems: List<SavedMealItem> = emptyList(),
    val weightEntries: List<WeightEntry> = emptyList(),
    val targetAdjustments: List<TargetAdjustment> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val routines: List<Routine> = emptyList(),
    val routineItems: List<RoutineItem> = emptyList(),
    val sessions: List<Session> = emptyList(),
    val setEntries: List<SetEntry> = emptyList(),
    val stacks: List<Stack> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val habitLogs: List<HabitLog> = emptyList(),
    val sleepTargets: List<SleepTarget> = emptyList(),
    val sleepLogs: List<SleepLog> = emptyList(),
    val curfewLogs: List<CurfewLog> = emptyList(),
    val sleepChecklists: List<SleepChecklist> = emptyList(),
    val xpEvents: List<XpEvent> = emptyList(),
    val tierCrossings: List<TierCrossing> = emptyList(),
    val weeklyReviews: List<WeeklyReview> = emptyList(),
)
