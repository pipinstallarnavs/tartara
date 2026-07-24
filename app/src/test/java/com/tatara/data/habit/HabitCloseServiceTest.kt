package com.tatara.data.habit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tatara.data.db.TataraDatabase
import com.tatara.data.db.entity.Habit
import com.tatara.data.db.entity.HabitList
import com.tatara.data.db.entity.HabitLog
import com.tatara.data.db.entity.HabitLogStatus
import java.time.LocalDate
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
class HabitCloseServiceTest {

    private val today = LocalDate.of(2026, 7, 23)
    private val zone = ZoneOffset.UTC
    private lateinit var db: TataraDatabase
    private lateinit var service: HabitCloseService

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            TataraDatabase::class.java,
        ).allowMainThreadQueries().build()
        service = HabitCloseService(db)
    }

    @After
    fun tearDown() = db.close()

    private fun instantOn(date: LocalDate) = date.atTime(12, 0).toInstant(zone)

    private suspend fun newHabit(createdOn: LocalDate, list: HabitList = HabitList.HABIT, automaticity: Float = 0f): Long =
        db.habitDao().insertHabit(
            Habit(name = "h-$createdOn", list = list, createdAt = instantOn(createdOn), automaticity = automaticity)
        )

    private suspend fun complete(habitId: Long, date: LocalDate) =
        db.habitDao().insertLog(HabitLog(habitId = habitId, date = date, status = HabitLogStatus.COMPLETED))

    @Test
    fun growthThenEscalatingPenalties() = runBlocking {
        // Created Jul 10; completed 10th–12th, then nothing through the close
        // boundary (Jul 20): 3 growth steps, then 8 misses at 0/2/5/8×5 %.
        val id = newHabit(LocalDate.of(2026, 7, 10))
        (10..12).forEach { complete(id, LocalDate.of(2026, 7, it)) }

        service.closeOpenDays(today, zone)

        val h = db.habitDao().getAllHabits().single()
        var a = 0f
        repeat(3) { a = Automaticity.afterCompletion(a) }
        for (m in 1..8) a = Automaticity.afterMiss(a, m)
        assertEquals(a, h.automaticity, 0.01f)
        assertEquals(8, h.consecutiveMisses)

        // Unlogged closed days became explicit MISSED logs.
        val missed = db.habitDao().getAllLogs().filter { it.status == HabitLogStatus.MISSED }
        assertEquals((13..20).map { LocalDate.of(2026, 7, it) }, missed.map { it.date }.sorted())

        assertEquals(LocalDate.of(2026, 7, 20), db.bodyDao().getSettings()!!.lastHabitDayClosed)
    }

    @Test
    fun closingIsIdempotent() = runBlocking {
        val id = newHabit(LocalDate.of(2026, 7, 10))
        complete(id, LocalDate.of(2026, 7, 10))
        service.closeOpenDays(today, zone)
        val after = db.habitDao().getAllHabits().single()

        service.closeOpenDays(today, zone)

        assertEquals(after, db.habitDao().getAllHabits().single())
    }

    @Test
    fun frozenDaysAreNeutral() = runBlocking {
        // Completed 10th–18th except a frozen 15th: no penalty, counter intact.
        val id = newHabit(LocalDate.of(2026, 7, 10))
        (10..18).filter { it != 15 }.forEach { complete(id, LocalDate.of(2026, 7, it)) }
        db.habitDao().insertLog(
            HabitLog(habitId = id, date = LocalDate.of(2026, 7, 15), status = HabitLogStatus.FROZEN)
        )
        // 19th and 20th missed.

        service.closeOpenDays(today, zone)

        val h = db.habitDao().getAllHabits().single()
        var a = 0f
        repeat(8) { a = Automaticity.afterCompletion(a) }   // 8 completions
        a = Automaticity.afterMiss(a, 1)                    // 19th, first miss free
        a = Automaticity.afterMiss(a, 2)                    // 20th
        assertEquals(a, h.automaticity, 0.01f)
        assertEquals(2, h.consecutiveMisses)
        // The frozen day did not gain a MISSED log.
        assertEquals(
            HabitLogStatus.FROZEN,
            db.habitDao().logFor(id, LocalDate.of(2026, 7, 15))!!.status,
        )
    }

    @Test
    fun editableDaysAreNeverClosed() = runBlocking {
        newHabit(LocalDate.of(2026, 7, 10))
        service.closeOpenDays(today, zone)
        // Jul 21–23 are inside the edit window: no MISSED logs, no closure.
        val dates = db.habitDao().getAllLogs().map { it.date }
        assertTrue(dates.none { it.isAfter(LocalDate.of(2026, 7, 20)) })
    }

    @Test
    fun daysBeforeCreationAreIgnored() = runBlocking {
        val early = newHabit(LocalDate.of(2026, 7, 10))
        complete(early, LocalDate.of(2026, 7, 10))
        val late = newHabit(LocalDate.of(2026, 7, 19))

        service.closeOpenDays(today, zone)

        // The late habit is judged only for the 19th and 20th.
        val lateMisses = db.habitDao().getAllLogs()
            .filter { it.habitId == late && it.status == HabitLogStatus.MISSED }
        assertEquals(
            listOf(LocalDate.of(2026, 7, 19), LocalDate.of(2026, 7, 20)),
            lateMisses.map { it.date }.sorted(),
        )
    }

    @Test
    fun reachingNinetyFiveGraduatesToHygiene() = runBlocking {
        // §5.1 — 94.9 + one completion crosses 95 → list flips, slot freed.
        val id = newHabit(LocalDate.of(2026, 7, 20), automaticity = 94.9f)
        complete(id, LocalDate.of(2026, 7, 20))

        service.closeOpenDays(today, zone)

        val h = db.habitDao().getAllHabits().single()
        assertTrue(h.automaticity >= 95f)
        assertEquals(HabitList.HYGIENE, h.list)
        assertNotNull(h.graduatedAt)
    }
}
