package com.tatara.data.habit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tatara.data.db.Seeder
import com.tatara.data.db.TataraDatabase
import com.tatara.data.db.entity.Habit
import com.tatara.data.db.entity.HabitList
import com.tatara.data.db.entity.HabitLog
import com.tatara.data.db.entity.HabitLogStatus
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HabitRepositoryTest {

    private val today = LocalDate.of(2026, 7, 23)
    private lateinit var db: TataraDatabase
    private lateinit var repo: HabitRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, TataraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = HabitRepository(db, today = { today }, now = { Instant.parse("2026-07-23T09:00:00Z") })
    }

    @After
    fun tearDown() = db.close()

    private suspend fun habit(name: String = "h", list: HabitList = HabitList.HABIT): Habit =
        repo.createHabit(Habit(name = name, list = list, createdAt = Instant.parse("2026-07-01T00:00:00Z")))!!

    @Test
    fun seedCreatesTheSpecLists() = runBlocking {
        Seeder.seedIfEmpty(context, db)
        val habits = repo.habits()
        assertEquals(10, habits.size)
        assertEquals(4, habits.count { it.list == HabitList.HABIT })
        assertEquals(6, habits.count { it.list == HabitList.HYGIENE })
        assertEquals(listOf("Morning", "Night", "Sit", "Solo"), repo.stacks().sortedBy { it.sortOrder }.map { it.name })
    }

    @Test
    fun toggleWithinWindowOnly() = runBlocking {
        val h = habit()
        assertFalse(repo.toggle(h.id, today.minusDays(3)))
        assertTrue(repo.toggle(h.id, today.minusDays(2)))
        assertEquals(HabitLogStatus.COMPLETED, db.habitDao().logFor(h.id, today.minusDays(2))!!.status)
    }

    @Test
    fun toggleIsAnUndo() = runBlocking {
        val h = habit()
        repo.toggle(h.id, today)
        repo.toggle(h.id, today)
        assertNull(db.habitDao().logFor(h.id, today))
    }

    @Test
    fun completedAtOnlyForLiveTicks() = runBlocking {
        val h = habit()
        repo.toggle(h.id, today)
        repo.toggle(h.id, today.minusDays(1))
        assertNotNull(db.habitDao().logFor(h.id, today)!!.completedAt)
        assertNull(db.habitDao().logFor(h.id, today.minusDays(1))!!.completedAt)
    }

    @Test
    fun twoFreezeTokensPerMonth() = runBlocking {
        val a = habit("a")
        val b = habit("b")
        val c = habit("c")
        assertTrue(repo.freeze(a.id, today.minusDays(1)))
        assertTrue(repo.freeze(b.id, today.minusDays(2)))
        assertFalse(repo.freeze(c.id, today.minusDays(1)))
        assertEquals(0, repo.tokensLeft(YearMonth.of(2026, 7)))

        // Unfreezing refunds the token.
        assertTrue(repo.unfreeze(a.id, today.minusDays(1)))
        assertTrue(repo.freeze(c.id, today.minusDays(1)))
    }

    @Test
    fun tokensDoNotRollOver() = runBlocking {
        val h = habit()
        // Two June freezes (inserted directly — June is far outside the edit window).
        db.habitDao().insertLog(HabitLog(habitId = h.id, date = LocalDate.of(2026, 6, 10), status = HabitLogStatus.FROZEN))
        db.habitDao().insertLog(HabitLog(habitId = h.id, date = LocalDate.of(2026, 6, 11), status = HabitLogStatus.FROZEN))
        assertEquals(0, repo.tokensLeft(YearMonth.of(2026, 6)))
        // July's allowance is untouched — and June's unused-token state is gone.
        assertEquals(2, repo.tokensLeft(YearMonth.of(2026, 7)))
    }

    @Test
    fun cannotFreezeACompletedDay() = runBlocking {
        val h = habit()
        repo.toggle(h.id, today.minusDays(1))
        assertFalse(repo.freeze(h.id, today.minusDays(1)))
    }

    @Test
    fun habitListCapIsFive() = runBlocking {
        repeat(5) { habit("h$it") }
        assertNull(repo.createHabit(Habit(name = "sixth", list = HabitList.HABIT, createdAt = Instant.EPOCH)))
        // Hygiene is uncapped.
        assertNotNull(repo.createHabit(Habit(name = "hyg", list = HabitList.HYGIENE, createdAt = Instant.EPOCH)))
    }
}
