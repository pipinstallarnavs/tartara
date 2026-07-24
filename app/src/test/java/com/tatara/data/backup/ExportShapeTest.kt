package com.tatara.data.backup

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the exported JSON shape for schema v1. If this test fails, the export shape
 * changed: bump SchemaMigrations.CURRENT, add a converter step (§2.3), and only then
 * update the expectations here.
 *
 * No database needed — the shape is a property of the serializers alone.
 */
class ExportShapeTest {

    private val json = Json { encodeDefaults = true }

    private inline fun <reified T> keysOf(sample: T): Set<String> =
        (json.encodeToJsonElement(serializer<T>(), sample) as JsonObject).keys

    @Test
    fun exportFileRootShape_isV1() {
        val sample = ExportFile(1, "2026-07-23T09:14:00+05:30", "1.0.0", ExportData())
        assertEquals(setOf("schemaVersion", "exportedAt", "appVersion", "data"), keysOf(sample))
    }

    @Test
    fun exportDataTables_areV1() {
        assertEquals(
            setOf(
                "settings", "foods", "foodEntries", "savedMeals", "savedMealItems",
                "weightEntries", "targetAdjustments",
                "exercises", "routines", "routineItems", "sessions", "setEntries",
                "stacks", "habits", "habitLogs",
                "sleepTargets", "sleepLogs", "curfewLogs", "sleepChecklists",
                "xpEvents", "tierCrossings", "weeklyReviews",
            ),
            keysOf(ExportData()),
        )
    }

    @Test
    fun rowShapes_areV1() {
        val expected = mapOf(
            "food" to setOf(
                "id", "name", "aliases", "isCustom", "isEstimated", "unitType", "portionName",
                "kcal", "protein", "carbs", "fat", "fatSource", "lastUsedAt", "useCount",
            ),
            "foodEntry" to setOf("id", "date", "foodId", "quantity", "loggedAt"),
            "savedMeal" to setOf("id", "name"),
            "savedMealItem" to setOf("id", "savedMealId", "foodId", "quantity"),
            "weightEntry" to setOf("id", "date", "weightKg", "isBackfilled", "loggedAt"),
            "targetAdjustment" to setOf(
                "id", "effectiveFrom", "kcalTarget", "proteinG", "fatG", "carbsG",
                "meanDailyKcal", "ewmaStart", "ewmaEnd", "impliedTdee",
                "weeklyRatePercent", "computedFromWeightKg",
            ),
            "settings" to setOf(
                "id", "proteinPerKg", "goalRatePercent", "blockStartDate", "ratchetWeightKg",
                "heightCm", "birthYear", "sex", "lastProcessedWeekEnd",
            ),
            "exercise" to setOf("id", "name", "muscleGroup", "equipment"),
            "routine" to setOf("id", "name", "sortOrder"),
            "routineItem" to setOf(
                "id", "routineId", "exerciseId", "targetSets", "sortOrder",
                "repRangeLow", "repRangeHigh", "incrementKg", "currentWeightKg",
            ),
            "session" to setOf("id", "date", "routineId", "durationMin", "notes"),
            "setEntry" to setOf(
                "id", "sessionId", "exerciseId", "routineItemId", "setIndex",
                "weightKg", "reps", "rpe", "isWarmup",
            ),
            "stack" to setOf("id", "name", "sortOrder"),
            "habit" to setOf(
                "id", "name", "list", "stackId", "cue", "intendedTime", "location",
                "intention", "automaticity", "consecutiveMisses", "createdAt", "graduatedAt",
            ),
            "habitLog" to setOf("id", "habitId", "date", "status", "completedAt"),
            "sleepTarget" to setOf("id", "effectiveFrom", "targetBedTime", "targetWakeTime", "curfewMinutes"),
            "sleepLog" to setOf(
                "id", "date", "bedTime", "wakeTime", "timeToFallAsleepMin", "wakeCount", "quality",
            ),
            "curfewLog" to setOf("id", "date", "curfewStart", "lastScreenAt", "held"),
            "sleepChecklist" to setOf(
                "id", "date", "caffeineCutoffMet", "roomDark", "roomCool", "noLateLargeMeal",
            ),
            "xpEvent" to setOf("id", "date", "type", "amount"),
            "tierCrossing" to setOf("id", "tier", "level", "date"),
            "weeklyReview" to setOf("id", "weekStart", "generatedAt", "openedAt"),
        )

        val d = java.time.LocalDate.of(2026, 7, 23)
        val t = java.time.LocalTime.NOON
        val i = java.time.Instant.EPOCH
        val actual = mapOf(
            "food" to keysOf(
                com.tatara.data.db.entity.Food(
                    name = "x", unitType = com.tatara.data.db.entity.UnitType.GRAM,
                    kcal = 0f, protein = 0f, carbs = 0f, fat = 0f,
                    fatSource = com.tatara.data.db.entity.FatSource.MIXED,
                )
            ),
            "foodEntry" to keysOf(com.tatara.data.db.entity.FoodEntry(date = d, foodId = 1, quantity = 1f, loggedAt = i)),
            "savedMeal" to keysOf(com.tatara.data.db.entity.SavedMeal(name = "x")),
            "savedMealItem" to keysOf(com.tatara.data.db.entity.SavedMealItem(savedMealId = 1, foodId = 1, quantity = 1f)),
            "weightEntry" to keysOf(com.tatara.data.db.entity.WeightEntry(date = d, weightKg = 1f, loggedAt = i)),
            "targetAdjustment" to keysOf(
                com.tatara.data.db.entity.TargetAdjustment(
                    effectiveFrom = d, kcalTarget = 1f, proteinG = 1f, fatG = 1f, carbsG = 1f,
                    weeklyRatePercent = 0f,
                )
            ),
            "settings" to keysOf(com.tatara.data.db.entity.Settings()),
            "exercise" to keysOf(com.tatara.data.db.entity.Exercise(name = "x", muscleGroup = "x", equipment = "x")),
            "routine" to keysOf(com.tatara.data.db.entity.Routine(name = "x", sortOrder = 0)),
            "routineItem" to keysOf(
                com.tatara.data.db.entity.RoutineItem(
                    routineId = 1, exerciseId = 1, targetSets = 1, sortOrder = 0,
                    repRangeLow = 1, repRangeHigh = 1, incrementKg = 1f, currentWeightKg = 1f,
                )
            ),
            "session" to keysOf(com.tatara.data.db.entity.Session(date = d)),
            "setEntry" to keysOf(
                com.tatara.data.db.entity.SetEntry(sessionId = 1, exerciseId = 1, setIndex = 0, weightKg = 1f, reps = 1)
            ),
            "stack" to keysOf(com.tatara.data.db.entity.Stack(name = "x", sortOrder = 0)),
            "habit" to keysOf(
                com.tatara.data.db.entity.Habit(
                    name = "x", list = com.tatara.data.db.entity.HabitList.HABIT, createdAt = i,
                )
            ),
            "habitLog" to keysOf(
                com.tatara.data.db.entity.HabitLog(
                    habitId = 1, date = d, status = com.tatara.data.db.entity.HabitLogStatus.COMPLETED,
                )
            ),
            "sleepTarget" to keysOf(
                com.tatara.data.db.entity.SleepTarget(effectiveFrom = d, targetBedTime = t, targetWakeTime = t)
            ),
            "sleepLog" to keysOf(
                com.tatara.data.db.entity.SleepLog(
                    date = d, bedTime = t, wakeTime = t, timeToFallAsleepMin = 0, wakeCount = 0, quality = 3,
                )
            ),
            "curfewLog" to keysOf(com.tatara.data.db.entity.CurfewLog(date = d, curfewStart = t, held = true)),
            "sleepChecklist" to keysOf(
                com.tatara.data.db.entity.SleepChecklist(
                    date = d, caffeineCutoffMet = true, roomDark = true, roomCool = true, noLateLargeMeal = true,
                )
            ),
            "xpEvent" to keysOf(
                com.tatara.data.db.entity.XpEvent(
                    date = d, type = com.tatara.data.db.entity.XpEventType.RING_CLOSED, amount = 1,
                )
            ),
            "tierCrossing" to keysOf(com.tatara.data.db.entity.TierCrossing(tier = "x", level = 1, date = d)),
            "weeklyReview" to keysOf(com.tatara.data.db.entity.WeeklyReview(weekStart = d, generatedAt = i)),
        )

        assertEquals(expected.keys, actual.keys)
        expected.forEach { (table, keys) ->
            assertEquals("shape of '$table' changed — see class doc", keys, actual[table])
        }
    }
}
