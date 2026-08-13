package com.tatara.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tatara.data.db.dao.BodyDao
import com.tatara.data.db.dao.DashboardDao
import com.tatara.data.db.dao.FoodDao
import com.tatara.data.db.dao.HabitDao
import com.tatara.data.db.dao.SleepDao
import com.tatara.data.db.dao.TrainDao
import com.tatara.data.db.entity.CurfewLog
import com.tatara.data.db.entity.DailyRollup
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

@Database(
    entities = [
        Food::class, FoodEntry::class, SavedMeal::class, SavedMealItem::class,
        WeightEntry::class, TargetAdjustment::class, Settings::class,
        Exercise::class, Routine::class, RoutineItem::class, Session::class, SetEntry::class,
        Stack::class, Habit::class, HabitLog::class,
        SleepTarget::class, SleepLog::class, CurfewLog::class, SleepChecklist::class,
        XpEvent::class, TierCrossing::class, WeeklyReview::class, DailyRollup::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class TataraDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun bodyDao(): BodyDao
    abstract fun trainDao(): TrainDao
    abstract fun habitDao(): HabitDao
    abstract fun sleepDao(): SleepDao
    abstract fun dashboardDao(): DashboardDao

    companion object {
        /** v2: BMR profile + TDEE job bookkeeping on settings (all nullable). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE settings ADD COLUMN heightCm REAL")
                db.execSQL("ALTER TABLE settings ADD COLUMN birthYear INTEGER")
                db.execSQL("ALTER TABLE settings ADD COLUMN sex TEXT")
                db.execSQL("ALTER TABLE settings ADD COLUMN lastProcessedWeekEnd TEXT")
            }
        }

        /** v3: habit day-close bookkeeping on settings. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE settings ADD COLUMN lastHabitDayClosed TEXT")
            }
        }

        /** v4: optional rest seconds + target RPE on the routine slot's prescription. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routine_item ADD COLUMN restSeconds INTEGER")
                db.execSQL("ALTER TABLE routine_item ADD COLUMN targetRpe REAL")
            }
        }

        /** v5: activity level on settings, for the formula-vs-observed maintenance estimate. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE settings ADD COLUMN activityLevel TEXT")
            }
        }

        // §2.3 — one Migration per on-device schema step. fallbackToDestructiveMigration
        // is forbidden: it deletes everything on schema change.
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)

        @Volatile
        private var instance: TataraDatabase? = null

        /** Process-wide singleton — the widget and the app share one connection. */
        fun build(context: Context): TataraDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, TataraDatabase::class.java, "tatara.db"
                )
                    .addMigrations(*MIGRATIONS)
                    .build()
                    .also { instance = it }
            }
    }
}
