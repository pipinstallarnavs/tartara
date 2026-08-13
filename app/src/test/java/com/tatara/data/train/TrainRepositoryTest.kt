package com.tatara.data.train

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tatara.data.db.TataraDatabase
import com.tatara.data.db.entity.Exercise
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TrainRepositoryTest {

    private val today = LocalDate.of(2026, 7, 23)
    private lateinit var db: TataraDatabase
    private lateinit var repo: TrainRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            TataraDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = TrainRepository(db, today = { today })
    }

    @After
    fun tearDown() = db.close()

    private suspend fun exercise(name: String = "Back Squat", group: String = "Quads"): Long =
        db.trainDao().insertExercise(Exercise(name = name, muscleGroup = group, equipment = "Barbell"))

    /** Runs one full session for a routine, confirming [reps] on every working set. */
    private suspend fun runSession(routineId: Long, reps: List<Int>): FinishResult {
        val routine = repo.routines().first { it.id == routineId }
        val start = repo.startSession(routine)
        val item = repo.itemsFor(routineId).first()
        reps.forEachIndexed { i, r ->
            repo.confirmSet(start.session.id, item.exerciseId, item.id, i, item.currentWeightKg, r)
        }
        return repo.finishSession(start.session.id, 60)
    }

    @Test
    fun routinesCycleAndWrap() = runBlocking {
        val ex = exercise()
        val a = repo.createRoutine("A"); repo.addRoutineItem(a.id, ex, 3, 5, 5, 2.5f, 100f)
        val b = repo.createRoutine("B"); repo.addRoutineItem(b.id, ex, 3, 8, 10, 2.5f, 70f)
        val c = repo.createRoutine("C"); repo.addRoutineItem(c.id, ex, 3, 8, 10, 2.5f, 50f)

        // No sessions yet → first in order.
        assertEquals("A", repo.nextRoutine()!!.name)

        runSession(a.id, listOf(5, 5, 5))
        assertEquals("B", repo.nextRoutine()!!.name)
        runSession(b.id, listOf(8, 8, 8))
        assertEquals("C", repo.nextRoutine()!!.name)
        runSession(c.id, listOf(8, 8, 8))
        assertEquals("A", repo.nextRoutine()!!.name)
    }

    @Test
    fun doubleProgressionIncrementsAndPrefillsWithDot() = runBlocking {
        val ex = exercise()
        val routine = repo.createRoutine("Legs")
        repo.addRoutineItem(routine.id, ex, 3, 8, 10, 2.5f, 60f)

        // All sets at the top of the range → the slot moves to 62.5.
        val result = runSession(routine.id, listOf(10, 10, 10)) as FinishResult.Finished
        assertEquals(62.5f, result.incrementedItems.single().currentWeightKg, 0.001f)

        // §4.2.1 — next start pre-fills the incremented weight, marked, reps at the low end.
        val next = repo.startSession(routine)
        val prefill = next.prefill
        assertEquals(3, prefill.size)
        prefill.forEach {
            assertEquals(62.5f, it.weightKg, 0.001f)
            assertEquals(8, it.reps)
            assertTrue(it.incremented)
            assertEquals("60×10", it.ghost)
        }
    }

    @Test
    fun noIncrementWhenARepShortAndPrefillKeepsLastNumbers() = runBlocking {
        val ex = exercise()
        val routine = repo.createRoutine("Legs")
        repo.addRoutineItem(routine.id, ex, 3, 8, 10, 2.5f, 60f)

        runSession(routine.id, listOf(10, 10, 9))

        assertEquals(60f, repo.itemsFor(routine.id).first().currentWeightKg, 0.001f)
        val next = repo.startSession(routine)
        assertFalse(next.prefill.first().incremented)
        assertEquals(listOf(10, 10, 9), next.prefill.map { it.reps })
    }

    @Test
    fun perSlotProgressionIsIndependentButE1rmIsGlobal() = runBlocking {
        // §4.1 — squat on Legs and on Full Body progress separately…
        val ex = exercise()
        val legs = repo.createRoutine("Legs")
        repo.addRoutineItem(legs.id, ex, 3, 5, 5, 2.5f, 100f)
        val full = repo.createRoutine("Full Body")
        repo.addRoutineItem(full.id, ex, 3, 8, 10, 2.5f, 70f)

        runSession(legs.id, listOf(5, 5, 5))

        assertEquals(102.5f, repo.itemsFor(legs.id).first().currentWeightKg, 0.001f)
        assertEquals(70f, repo.itemsFor(full.id).first().currentWeightKg, 0.001f)

        // …but both feed one e1RM history for the exercise.
        runSession(full.id, listOf(10, 10, 10))
        assertEquals(2, repo.e1rmHistory(ex).size)
    }

    @Test
    fun prefillTopsUpToPrescribedSetCount() = runBlocking {
        // §4.1 — the prescription owns the set count. A short session last time
        // must not shrink the plan: 4 prescribed, 1 done → 4 rows next time.
        val ex = exercise()
        val routine = repo.createRoutine("Legs")
        repo.addRoutineItem(routine.id, ex, 4, 5, 8, 2.5f, 80f)
        val item = repo.itemsFor(routine.id).first()

        val first = repo.startSession(routine)
        assertEquals(4, first.prefill.size)
        repo.confirmSet(first.session.id, ex, item.id, 0, 80f, 6)
        repo.finishSession(first.session.id, 30)

        val next = repo.startSession(routine)
        assertEquals(4, next.prefill.size)
        // The one real set carries its ghost; the top-ups reuse its numbers.
        assertEquals("80×6", next.prefill.first().ghost)
        next.prefill.forEach { assertEquals(80f, it.weightKg, 0.001f) }
        assertEquals(listOf(6, 6, 6, 6), next.prefill.map { it.reps })
    }

    @Test
    fun emptySessionIsDiscarded() = runBlocking {
        val start = repo.startSession(null)
        assertTrue(repo.finishSession(start.session.id, 5) is FinishResult.Discarded)
        assertNull(db.trainDao().sessionById(start.session.id))
    }

    @Test
    fun stallFlagAfterThreeFlatSessions() = runBlocking {
        val ex = exercise()
        val routine = repo.createRoutine("Legs")
        repo.addRoutineItem(routine.id, ex, 3, 8, 10, 2.5f, 60f)

        // Three sessions never hitting the top: same weight, same best reps.
        repeat(3) { runSession(routine.id, listOf(9, 8, 8)) }

        assertTrue(repo.isStalled(ex))
        assertEquals(listOf("Back Squat"), repo.stallFlags().map { it.name })
    }

    @Test
    fun warmupsCountNowhere() = runBlocking {
        val ex = exercise()
        val routine = repo.createRoutine("Legs")
        repo.addRoutineItem(routine.id, ex, 1, 8, 10, 2.5f, 60f)
        val item = repo.itemsFor(routine.id).first()

        val start = repo.startSession(routine)
        // A monster warmup rep count must not trigger progression or e1RM.
        repo.confirmSet(start.session.id, ex, item.id, 0, 20f, 30, isWarmup = true)
        repo.confirmSet(start.session.id, ex, item.id, 1, 60f, 9)
        repo.finishSession(start.session.id, 30)

        assertEquals(60f, repo.itemsFor(routine.id).first().currentWeightKg, 0.001f)
        assertEquals(Progression.e1rm(60f, 9), repo.e1rmHistory(ex).single().second, 0.01f)
        // §4.4 — weekly volume counts working sets only.
        assertEquals(mapOf("Quads" to 1), repo.weeklySetsByMuscleGroup())
    }

    @Test
    fun weeklyVolumeGroupsByMuscle() = runBlocking {
        val squat = exercise("Back Squat", "Quads")
        val bench = exercise("Bench Press", "Chest")
        val start = repo.startSession(null)
        repo.confirmSet(start.session.id, squat, null, 0, 100f, 5)
        repo.confirmSet(start.session.id, squat, null, 1, 100f, 5)
        repo.confirmSet(start.session.id, bench, null, 0, 80f, 8)
        repo.finishSession(start.session.id, 45)

        assertEquals(mapOf("Quads" to 2, "Chest" to 1), repo.weeklySetsByMuscleGroup())
    }
}
