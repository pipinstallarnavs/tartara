package com.tatara.data.backup

import androidx.room.withTransaction
import com.tatara.data.db.TataraDatabase
import java.io.File
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

class BackupManager(
    private val db: TataraDatabase,
    private val appVersion: String,
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    suspend fun export(exportedAt: OffsetDateTime = OffsetDateTime.now()): String {
        val data = db.withTransaction {
            ExportData(
                settings = db.bodyDao().getSettings(),
                foods = db.foodDao().getAll(),
                foodEntries = db.foodDao().getAllEntries(),
                savedMeals = db.foodDao().getAllSavedMeals(),
                savedMealItems = db.foodDao().getAllSavedMealItems(),
                weightEntries = db.bodyDao().getAllWeights(),
                targetAdjustments = db.bodyDao().getAllAdjustments(),
                exercises = db.trainDao().getAllExercises(),
                routines = db.trainDao().getAllRoutines(),
                routineItems = db.trainDao().getAllRoutineItems(),
                sessions = db.trainDao().getAllSessions(),
                setEntries = db.trainDao().getAllSets(),
                stacks = db.habitDao().getAllStacks(),
                habits = db.habitDao().getAllHabits(),
                habitLogs = db.habitDao().getAllLogs(),
                sleepTargets = db.sleepDao().getAllTargets(),
                sleepLogs = db.sleepDao().getAllLogs(),
                curfewLogs = db.sleepDao().getAllCurfewLogs(),
                sleepChecklists = db.sleepDao().getAllChecklists(),
                xpEvents = db.dashboardDao().getAllXpEvents(),
                tierCrossings = db.dashboardDao().getAllTierCrossings(),
                weeklyReviews = db.dashboardDao().getAllReviews(),
            )
        }
        val file = ExportFile(
            schemaVersion = SchemaMigrations.CURRENT,
            exportedAt = exportedAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            appVersion = appVersion,
            data = data,
        )
        return json.encodeToString(ExportFile.serializer(), file)
    }

    /**
     * §2.2 — full replace, never merge. The file is parsed, migrated, and decoded in
     * full before the transaction starts, so a bad file can never destroy data.
     */
    suspend fun import(text: String) {
        val root = json.parseToJsonElement(text).jsonObject
        val migrated = SchemaMigrations.migrate(root)
        val file: ExportFile = json.decodeFromJsonElement(migrated)

        db.withTransaction {
            // Children before parents.
            db.trainDao().deleteAllSets()
            db.trainDao().deleteAllSessions()
            db.trainDao().deleteAllRoutineItems()
            db.trainDao().deleteAllRoutines()
            db.trainDao().deleteAllExercises()
            db.foodDao().deleteAllSavedMealItems()
            db.foodDao().deleteAllSavedMeals()
            db.foodDao().deleteAllEntries()
            db.foodDao().deleteAll()
            db.habitDao().deleteAllLogs()
            db.habitDao().deleteAllHabits()
            db.habitDao().deleteAllStacks()
            db.sleepDao().deleteAllTargets()
            db.sleepDao().deleteAllLogs()
            db.sleepDao().deleteAllCurfewLogs()
            db.sleepDao().deleteAllChecklists()
            db.bodyDao().deleteAllWeights()
            db.bodyDao().deleteAllAdjustments()
            db.bodyDao().deleteSettings()
            db.dashboardDao().deleteAllXpEvents()
            db.dashboardDao().deleteAllTierCrossings()
            db.dashboardDao().deleteAllReviews()
            // The rollup cache is not in the export; clear it so stale aggregates
            // can't survive an import.
            db.dashboardDao().deleteAllRollups()

            // Parents before children.
            file.data.settings?.let { db.bodyDao().upsertSettings(it) }
            db.foodDao().insertAll(file.data.foods)
            db.foodDao().insertEntries(file.data.foodEntries)
            db.foodDao().insertSavedMeals(file.data.savedMeals)
            db.foodDao().insertSavedMealItems(file.data.savedMealItems)
            db.bodyDao().insertWeights(file.data.weightEntries)
            db.bodyDao().insertAdjustments(file.data.targetAdjustments)
            db.trainDao().insertExercises(file.data.exercises)
            db.trainDao().insertRoutines(file.data.routines)
            db.trainDao().insertRoutineItems(file.data.routineItems)
            db.trainDao().insertSessions(file.data.sessions)
            db.trainDao().insertSets(file.data.setEntries)
            db.habitDao().insertStacks(file.data.stacks)
            db.habitDao().insertHabits(file.data.habits)
            db.habitDao().insertLogs(file.data.habitLogs)
            db.sleepDao().insertTargets(file.data.sleepTargets)
            db.sleepDao().insertLogs(file.data.sleepLogs)
            db.sleepDao().insertCurfewLogs(file.data.curfewLogs)
            db.sleepDao().insertChecklists(file.data.sleepChecklists)
            db.dashboardDao().insertXpEvents(file.data.xpEvents)
            db.dashboardDao().insertTierCrossings(file.data.tierCrossings)
            db.dashboardDao().insertReviews(file.data.weeklyReviews)
        }
    }

    /**
     * §2.2 — writes tatara-backup-YYYY-MM-DD.json into [dir], keeping the newest
     * [keep] backups. ISO dates sort lexicographically, so name order is date order.
     */
    suspend fun writeBackup(dir: File, today: LocalDate = LocalDate.now(), keep: Int = 14): File {
        dir.mkdirs()
        val file = File(dir, "tatara-backup-$today.json")
        file.writeText(export())
        dir.listFiles { f -> BACKUP_NAME.matches(f.name) }
            ?.sortedByDescending { it.name }
            ?.drop(keep)
            ?.forEach { it.delete() }
        return file
    }

    companion object {
        private val BACKUP_NAME = Regex("""tatara-backup-\d{4}-\d{2}-\d{2}\.json""")
    }
}
