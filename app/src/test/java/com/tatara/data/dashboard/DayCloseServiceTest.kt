package com.tatara.data.dashboard

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tatara.data.db.TataraDatabase
import com.tatara.data.db.entity.FatSource
import com.tatara.data.db.entity.Food
import com.tatara.data.db.entity.FoodEntry
import com.tatara.data.db.entity.Habit
import com.tatara.data.db.entity.HabitList
import com.tatara.data.db.entity.HabitLog
import com.tatara.data.db.entity.HabitLogStatus
import com.tatara.data.db.entity.Session
import com.tatara.data.db.entity.SleepLog
import com.tatara.data.db.entity.TargetAdjustment
import com.tatara.data.db.entity.UnitType
import com.tatara.data.db.entity.WeightEntry
import com.tatara.data.db.entity.XpEvent
import com.tatara.data.db.entity.XpEventType
import com.tatara.data.habit.Automaticity
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DayCloseServiceTest {

    private val today = LocalDate.of(2026, 7, 23)
    private val zone = ZoneOffset.UTC
    private lateinit var db: TataraDatabase
    private lateinit var service: DayCloseService

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            TataraDatabase::class.java,
        ).allowMainThreadQueries().build()
        service = DayCloseService(db)
    }

    @After
    fun tearDown() = db.close()

    private fun instantOn(date: LocalDate) = date.atTime(12, 0).toInstant(zone)

    private suspend fun habit(createdOn: LocalDate, list: HabitList = HabitList.HABIT, a: Float = 0f): Long =
        db.habitDao().insertHabit(
            Habit(name = "h", list = list, createdAt = instantOn(createdOn), automaticity = a)
        )

    private suspend fun complete(id: Long, date: LocalDate) =
        db.habitDao().insertLog(HabitLog(habitId = id, date = date, status = HabitLogStatus.COMPLETED))

    /** Sets up one closeable day (Jul 20) with every §7.1/§7.2 element satisfied. */
    private suspend fun seedPerfectDay(day: LocalDate) {
        val h = habit(day, HabitList.HABIT)
        complete(h, day)
        habit(day, HabitList.HYGIENE)   // hygiene left unticked: must not block the ring

        db.bodyDao().insertAdjustment(
            TargetAdjustment(effectiveFrom = day, kcalTarget = 2000f, proteinG = 150f,
                fatG = 70f, carbsG = 200f, weeklyRatePercent = -0.5f)
        )
        db.foodDao().insert(
            Food(id = 1, name = "meal", unitType = UnitType.GRAM, kcal = 1000f,
                protein = 40f, carbs = 100f, fat = 20f, fatSource = FatSource.MIXED)
        )
        db.foodDao().insertEntry(FoodEntry(date = day, foodId = 1, quantity = 200f, loggedAt = instantOn(day)))

        db.sleepDao().insertLog(
            SleepLog(date = day, bedTime = LocalTime.of(23, 0), wakeTime = LocalTime.of(7, 0),
                timeToFallAsleepMin = 10, wakeCount = 0, quality = 4)
        )
        db.bodyDao().insertWeight(WeightEntry(date = day, weightKg = 80f, loggedAt = instantOn(day)))
        db.trainDao().insertSession(Session(date = day))
    }

    @Test
    fun perfectDayEarnsTheFullTable() = runBlocking {
        val day = LocalDate.of(2026, 7, 20)
        seedPerfectDay(day)

        service.closeOpenDays(today, zone)

        val byType = db.dashboardDao().getAllXpEvents().groupBy { it.type }
        assertEquals(40, byType[XpEventType.RING_CLOSED]!!.single().amount)
        assertEquals(12, byType[XpEventType.HABIT_COMPLETED]!!.single().amount)
        assertEquals(8, byType[XpEventType.SLEEP_LOGGED]!!.single().amount)
        assertEquals(5, byType[XpEventType.WEIGHT_LOGGED]!!.single().amount)
        assertEquals(30, byType[XpEventType.WORKOUT_LOGGED]!!.single().amount)
        assertEquals(95L, db.dashboardDao().totalXp())

        // §2.4 — the rollup row is the cache of this judgement.
        val rollup = db.dashboardDao().getAllRollups().single { it.date == day }
        assertTrue(rollup.ringClosed)
        assertEquals(2000f, rollup.kcal, 0.01f)
        assertEquals(1, rollup.habitsCompleted)
        assertEquals(1, rollup.habitsTotal)
    }

    @Test
    fun kcalOutsideTenPercentBreaksTheRing() = runBlocking {
        val day = LocalDate.of(2026, 7, 20)
        seedPerfectDay(day)
        // Push intake to 2300 against a 2000 target: 15% over.
        db.foodDao().insertEntry(FoodEntry(date = day, foodId = 1, quantity = 30f, loggedAt = instantOn(day)))

        service.closeOpenDays(today, zone)

        assertTrue(db.dashboardDao().getAllXpEvents().none { it.type == XpEventType.RING_CLOSED })
        assertTrue(db.dashboardDao().getAllRollups().none { it.ringClosed })
    }

    @Test
    fun missPenaltiesEscalateAndHygieneCostsNothing() = runBlocking {
        // Created Jul 10, never done, through Jul 20: 11 misses on a HABIT,
        // 11 on a HYGIENE. Level stays 1 (cost 95): 0, then 2%, 5%, 8%…
        habit(LocalDate.of(2026, 7, 10), HabitList.HABIT)
        habit(LocalDate.of(2026, 7, 10), HabitList.HYGIENE)

        service.closeOpenDays(today, zone)

        val penalties = db.dashboardDao().getAllXpEvents().filter { it.type == XpEventType.HABIT_MISS_PENALTY }
        // First consecutive miss is free → 10 penalty events for 11 misses.
        assertEquals(10, penalties.size)
        assertEquals(-2, penalties[0].amount)   // 2% of cost(1)=95 → 1.9 → 2
        assertEquals(-5, penalties[1].amount)   // 5% of 95 → 4.75 → 5
        assertEquals(-8, penalties[2].amount)   // 8% of 95 → 7.6 → 8
        assertEquals(-8, penalties[9].amount)
    }

    @Test
    fun workoutXpDiminishesWithinTheWeek() = runBlocking {
        habit(LocalDate.of(2026, 7, 13))
        // Mon 13, Wed 15, Fri 17, Sat 18 — one week (Mon–Sun).
        listOf(13, 15, 17, 18).forEach {
            db.trainDao().insertSession(Session(date = LocalDate.of(2026, 7, it)))
        }

        service.closeOpenDays(today, zone)

        val workouts = db.dashboardDao().getAllXpEvents()
            .filter { it.type == XpEventType.WORKOUT_LOGGED }
            .sortedBy { it.date }
        assertEquals(listOf(30, 22, 18, 15), workouts.map { it.amount })
    }

    @Test
    fun frozenHabitDoesNotBreakTheRing() = runBlocking {
        val day = LocalDate.of(2026, 7, 20)
        seedPerfectDay(day)
        val frozenHabit = habit(day, HabitList.HABIT)
        db.habitDao().insertLog(HabitLog(habitId = frozenHabit, date = day, status = HabitLogStatus.FROZEN))

        service.closeOpenDays(today, zone)

        assertNotNull(db.dashboardDao().getAllXpEvents().find { it.type == XpEventType.RING_CLOSED })
        // Frozen earned nothing: still exactly one habit-completion award.
        assertEquals(1, db.dashboardDao().getAllXpEvents().count { it.type == XpEventType.HABIT_COMPLETED })
    }

    @Test
    fun closingIsIdempotent() = runBlocking {
        seedPerfectDay(LocalDate.of(2026, 7, 20))
        service.closeOpenDays(today, zone)
        val xp = db.dashboardDao().totalXp()

        service.closeOpenDays(today, zone)

        assertEquals(xp, db.dashboardDao().totalXp())
        assertEquals(LocalDate.of(2026, 7, 20), db.bodyDao().getSettings()!!.lastHabitDayClosed)
    }

    @Test
    fun automaticityStillMovesAndGraduates() = runBlocking {
        // The habit engine behaviour carried over from HabitCloseService.
        val id = habit(LocalDate.of(2026, 7, 20), a = 94.9f)
        complete(id, LocalDate.of(2026, 7, 20))

        service.closeOpenDays(today, zone)

        val h = db.habitDao().getAllHabits().single()
        assertTrue(h.automaticity >= Automaticity.GRADUATION)
        assertEquals(HabitList.HYGIENE, h.list)
        assertNotNull(h.graduatedAt)
    }

    @Test
    fun tierCrossingRecordedWhenABandIsEntered() = runBlocking {
        // Pre-load XP to just under level 12 (band 1 floor), then earn past it.
        val toLevel12 = Levels.cumulative(12)
        db.dashboardDao().insertXpEvent(
            XpEvent(date = LocalDate.of(2026, 7, 1), type = XpEventType.RING_CLOSED,
                amount = (toLevel12 - 20).toInt())
        )
        seedPerfectDay(LocalDate.of(2026, 7, 20))   // earns 95 → crosses

        service.closeOpenDays(today, zone)

        val crossing = db.dashboardDao().getAllTierCrossings().single()
        assertEquals("Orikaeshi", crossing.tier)
        assertEquals(12, crossing.level)
        assertEquals(LocalDate.of(2026, 7, 20), crossing.date)
    }
}
