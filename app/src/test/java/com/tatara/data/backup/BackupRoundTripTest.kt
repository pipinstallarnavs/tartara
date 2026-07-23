package com.tatara.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tatara.data.db.TataraDatabase
import com.tatara.data.db.entity.CurfewLog
import com.tatara.data.db.entity.DailyRollup
import com.tatara.data.db.entity.Exercise
import com.tatara.data.db.entity.FatSource
import com.tatara.data.db.entity.Food
import com.tatara.data.db.entity.FoodEntry
import com.tatara.data.db.entity.Habit
import com.tatara.data.db.entity.HabitList
import com.tatara.data.db.entity.HabitLog
import com.tatara.data.db.entity.HabitLogStatus
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
import com.tatara.data.db.entity.UnitType
import com.tatara.data.db.entity.WeeklyReview
import com.tatara.data.db.entity.WeightEntry
import com.tatara.data.db.entity.XpEvent
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupRoundTripTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var db: TataraDatabase
    private lateinit var backup: BackupManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TataraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        backup = BackupManager(db, appVersion = "1.0.0")
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun exportWipeImport_roundTripsLosslessly() = runBlocking {
        seedEverything()

        val before = snapshot()
        // Guard: an empty table here would make the round-trip assertion vacuous.
        before.forEach { (table, rows) ->
            assertTrue("table '$table' was not seeded", rows.isNotEmpty())
        }

        val exported = backup.export()

        db.clearAllTables()
        snapshot().forEach { (table, rows) ->
            assertTrue("table '$table' not wiped", rows.isEmpty())
        }

        backup.import(exported)

        assertEquals(before, snapshot())
        // The rollup cache is excluded from export and cleared on import.
        assertTrue(db.dashboardDao().getAllRollups().isEmpty())
    }

    @Test
    fun import_refusesHigherSchemaVersion_andLeavesDataUntouched() = runBlocking {
        seedEverything()
        val before = snapshot()

        val exported = backup.export()
        val root = Json.parseToJsonElement(exported).jsonObject
        val tampered = Json.encodeToString(
            kotlinx.serialization.json.JsonObject.serializer(),
            kotlinx.serialization.json.JsonObject(
                root.toMutableMap().apply { put("schemaVersion", JsonPrimitive(SchemaMigrations.CURRENT + 1)) }
            ),
        )

        assertThrows(UnsupportedSchemaException::class.java) {
            runBlocking { backup.import(tampered) }
        }
        assertEquals(before, snapshot())
    }

    @Test
    fun import_refusesNonBackupJson() = runBlocking {
        seedEverything()
        val before = snapshot()

        assertThrows(NotABackupException::class.java) {
            runBlocking { backup.import("""{"hello": "world"}""") }
        }
        assertEquals(before, snapshot())
    }

    @Test
    fun export_hasRequiredMetadata() = runBlocking {
        val root = Json.parseToJsonElement(backup.export()).jsonObject
        assertEquals(SchemaMigrations.CURRENT, root["schemaVersion"]!!.let { (it as JsonPrimitive).content.toInt() })
        assertTrue(root.containsKey("exportedAt"))
        assertEquals("1.0.0", (root["appVersion"] as JsonPrimitive).content)
        assertTrue(root.containsKey("data"))
    }

    @Test
    fun writeBackup_keepsNewestFourteen() = runBlocking {
        val dir = tmp.newFolder("Tatara")
        // 16 stale backups plus today's write = 17 candidates; only 14 must survive.
        val start = LocalDate.of(2026, 6, 1)
        repeat(16) { i ->
            File(dir, "tatara-backup-${start.plusDays(i.toLong())}.json").writeText("{}")
        }
        File(dir, "unrelated.txt").writeText("keep me")

        backup.writeBackup(dir, today = LocalDate.of(2026, 7, 23))

        val backups = dir.listFiles { f -> f.name.startsWith("tatara-backup-") }!!.map { it.name }.sorted()
        assertEquals(14, backups.size)
        assertTrue("today's backup must survive pruning", "tatara-backup-2026-07-23.json" in backups)
        assertEquals("oldest files are pruned first", "tatara-backup-2026-06-04.json", backups.first())
        assertTrue("non-backup files are untouched", File(dir, "unrelated.txt").exists())
    }

    /** One map key per exported table, so a mismatch names the table that broke. */
    private suspend fun snapshot(): Map<String, List<Any>> = mapOf(
        "settings" to listOfNotNull(db.bodyDao().getSettings()),
        "food" to db.foodDao().getAll(),
        "food_entry" to db.foodDao().getAllEntries(),
        "saved_meal" to db.foodDao().getAllSavedMeals(),
        "saved_meal_item" to db.foodDao().getAllSavedMealItems(),
        "weight_entry" to db.bodyDao().getAllWeights(),
        "target_adjustment" to db.bodyDao().getAllAdjustments(),
        "exercise" to db.trainDao().getAllExercises(),
        "routine" to db.trainDao().getAllRoutines(),
        "routine_item" to db.trainDao().getAllRoutineItems(),
        "session" to db.trainDao().getAllSessions(),
        "set_entry" to db.trainDao().getAllSets(),
        "stack" to db.habitDao().getAllStacks(),
        "habit" to db.habitDao().getAllHabits(),
        "habit_log" to db.habitDao().getAllLogs(),
        "sleep_target" to db.sleepDao().getAllTargets(),
        "sleep_log" to db.sleepDao().getAllLogs(),
        "curfew_log" to db.sleepDao().getAllCurfewLogs(),
        "sleep_checklist" to db.sleepDao().getAllChecklists(),
        "xp_event" to db.dashboardDao().getAllXpEvents(),
        "tier_crossing" to db.dashboardDao().getAllTierCrossings(),
        "weekly_review" to db.dashboardDao().getAllReviews(),
    )

    /**
     * Seeds every table, exercising nullable fields both null and set, every enum in
     * at least one row, non-ASCII text, and the per-slot progression case from §4.1
     * (same exercise on two routines at different weights).
     */
    private suspend fun seedEverything() {
        val d0 = LocalDate.of(2026, 7, 21)
        val d1 = LocalDate.of(2026, 7, 22)
        val t0 = Instant.parse("2026-07-22T08:15:30.123456Z")

        db.bodyDao().upsertSettings(
            Settings(proteinPerKg = 1.8f, goalRatePercent = -0.5f, blockStartDate = d0, ratchetWeightKg = 82.4f)
        )

        db.foodDao().insertAll(
            listOf(
                Food(id = 1, name = "rice", aliases = "chawal,चावल", unitType = UnitType.GRAM,
                    kcal = 130f, protein = 2.7f, carbs = 28.2f, fat = 0.3f,
                    fatSource = FatSource.GRAIN_LEGUME, lastUsedAt = t0, useCount = 42),
                Food(id = 2, name = "daal", unitType = UnitType.PORTION, portionName = "katori",
                    kcal = 180f, protein = 9f, carbs = 22f, fat = 6f,
                    fatSource = FatSource.GRAIN_LEGUME),
                Food(id = 3, name = "पनीर भुर्जी", isCustom = true, isEstimated = true,
                    unitType = UnitType.PORTION, portionName = "plate",
                    kcal = 320f, protein = 18f, carbs = 8f, fat = 24f,
                    fatSource = FatSource.DAIRY),
            )
        )
        db.foodDao().insertEntries(
            listOf(
                FoodEntry(id = 1, date = d0, foodId = 1, quantity = 60f, loggedAt = t0),
                FoodEntry(id = 2, date = d1, foodId = 2, quantity = 2f, loggedAt = t0),
            )
        )
        db.foodDao().insertSavedMeals(listOf(SavedMeal(id = 1, name = "post-gym")))
        db.foodDao().insertSavedMealItems(
            listOf(
                SavedMealItem(id = 1, savedMealId = 1, foodId = 1, quantity = 100f),
                SavedMealItem(id = 2, savedMealId = 1, foodId = 3, quantity = 1f),
            )
        )

        db.bodyDao().insertWeights(
            listOf(
                WeightEntry(id = 1, date = d0, weightKg = 82.4f, loggedAt = t0),
                WeightEntry(id = 2, date = d1, weightKg = 81.9f, isBackfilled = true, loggedAt = t0),
            )
        )
        db.bodyDao().insertAdjustments(
            listOf(
                TargetAdjustment(id = 1, effectiveFrom = d0, kcalTarget = 2900f, proteinG = 148f,
                    fatG = 96.7f, carbsG = 359f, meanDailyKcal = 3100f, ewmaStart = 83.1f,
                    ewmaEnd = 82.6f, impliedTdee = 3350f, weeklyRatePercent = -0.5f,
                    computedFromWeightKg = 82.6f),
                TargetAdjustment(id = 2, effectiveFrom = d1, kcalTarget = 2900f, proteinG = 148f,
                    fatG = 96.7f, carbsG = 359f, weeklyRatePercent = -0.5f),
            )
        )

        db.trainDao().insertExercises(
            listOf(
                Exercise(id = 1, name = "Back Squat", muscleGroup = "Quads", equipment = "Barbell"),
                Exercise(id = 2, name = "Bench Press", muscleGroup = "Chest", equipment = "Barbell"),
            )
        )
        db.trainDao().insertRoutines(
            listOf(Routine(id = 1, name = "Legs", sortOrder = 0), Routine(id = 2, name = "Full Body", sortOrder = 1))
        )
        db.trainDao().insertRoutineItems(
            listOf(
                RoutineItem(id = 1, routineId = 1, exerciseId = 1, targetSets = 5, sortOrder = 0,
                    repRangeLow = 5, repRangeHigh = 5, incrementKg = 2.5f, currentWeightKg = 100f),
                RoutineItem(id = 2, routineId = 2, exerciseId = 1, targetSets = 3, sortOrder = 0,
                    repRangeLow = 8, repRangeHigh = 10, incrementKg = 2.5f, currentWeightKg = 70f),
            )
        )
        db.trainDao().insertSessions(
            listOf(
                Session(id = 1, date = d0, routineId = 1, durationMin = 62, notes = "felt heavy"),
                Session(id = 2, date = d1),
            )
        )
        db.trainDao().insertSets(
            listOf(
                SetEntry(id = 1, sessionId = 1, exerciseId = 1, routineItemId = 1, setIndex = 0,
                    weightKg = 60f, reps = 5, isWarmup = true),
                SetEntry(id = 2, sessionId = 1, exerciseId = 1, routineItemId = 1, setIndex = 1,
                    weightKg = 100f, reps = 5, rpe = 8.5f),
                SetEntry(id = 3, sessionId = 2, exerciseId = 2, setIndex = 0, weightKg = 80f, reps = 8),
            )
        )

        db.habitDao().insertStacks(
            listOf(Stack(id = 1, name = "Morning", sortOrder = 0), Stack(id = 2, name = "Sit", sortOrder = 1))
        )
        db.habitDao().insertHabits(
            listOf(
                Habit(id = 1, name = "brush (morning)", list = HabitList.HYGIENE, stackId = 1,
                    cue = "after I wake", intendedTime = LocalTime.of(7, 30),
                    location = "bathroom", intention = "When I wake, I will brush",
                    automaticity = 96f, createdAt = t0, graduatedAt = t0),
                Habit(id = 2, name = "mindfulness 10 min", list = HabitList.HABIT, stackId = 2,
                    cue = "after tea", intention = "When I finish tea, I will sit",
                    automaticity = 31.5f, consecutiveMisses = 2, createdAt = t0),
            )
        )
        db.habitDao().insertLogs(
            listOf(
                HabitLog(id = 1, habitId = 1, date = d0, status = HabitLogStatus.COMPLETED, completedAt = t0),
                HabitLog(id = 2, habitId = 2, date = d0, status = HabitLogStatus.MISSED),
                HabitLog(id = 3, habitId = 2, date = d1, status = HabitLogStatus.FROZEN),
            )
        )

        db.sleepDao().insertTargets(
            listOf(
                SleepTarget(id = 1, effectiveFrom = d0, targetBedTime = LocalTime.of(23, 0),
                    targetWakeTime = LocalTime.of(7, 0)),
                SleepTarget(id = 2, effectiveFrom = d1, targetBedTime = LocalTime.of(22, 30),
                    targetWakeTime = LocalTime.of(6, 30), curfewMinutes = 120),
            )
        )
        db.sleepDao().insertLogs(
            listOf(
                SleepLog(id = 1, date = d0, bedTime = LocalTime.of(23, 12), wakeTime = LocalTime.of(6, 58),
                    timeToFallAsleepMin = 15, wakeCount = 1, quality = 4),
            )
        )
        db.sleepDao().insertCurfewLogs(
            listOf(
                CurfewLog(id = 1, date = d0, curfewStart = LocalTime.of(21, 30),
                    lastScreenAt = LocalTime.of(21, 10), held = true),
                CurfewLog(id = 2, date = d1, curfewStart = LocalTime.of(21, 30), held = false),
            )
        )
        db.sleepDao().insertChecklists(
            listOf(
                SleepChecklist(id = 1, date = d0, caffeineCutoffMet = true, roomDark = true,
                    roomCool = false, noLateLargeMeal = true),
            )
        )

        db.dashboardDao().insertXpEvents(
            listOf(
                XpEvent(id = 1, date = d0, type = com.tatara.data.db.entity.XpEventType.RING_CLOSED, amount = 40),
                XpEvent(id = 2, date = d0, type = com.tatara.data.db.entity.XpEventType.HABIT_MISS_PENALTY, amount = -23),
            )
        )
        db.dashboardDao().insertTierCrossings(
            listOf(TierCrossing(id = 1, tier = "Orikaeshi", level = 12, date = d0))
        )
        db.dashboardDao().insertReviews(
            listOf(
                WeeklyReview(id = 1, weekStart = d0.minusDays(7), generatedAt = t0, openedAt = t0),
                WeeklyReview(id = 2, weekStart = d0, generatedAt = t0),
            )
        )
        // Cache row: must NOT survive export/import.
        db.dashboardDao().upsertRollup(DailyRollup(date = d0, kcal = 2870f, ringClosed = true))
    }
}
