package com.tatara.data.sleep

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tatara.data.db.TataraDatabase
import java.time.LocalDate
import java.time.LocalTime
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
class SleepRepositoryTest {

    private val today = LocalDate.of(2026, 7, 23)
    private lateinit var db: TataraDatabase
    private lateinit var repo: SleepRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            TataraDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = SleepRepository(db, today = { today })
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun defaultTargetUntilOneIsSet() = runBlocking {
        val t = repo.targetOn(today)
        assertEquals(LocalTime.of(23, 0), t.targetBedTime)
        assertEquals(LocalTime.of(7, 0), t.targetWakeTime)
        assertEquals(90, t.curfewMinutes)
        assertEquals(LocalTime.of(21, 30), repo.curfewStartFor(t))
    }

    @Test
    fun editingTargetsNeverRewritesHistory() = runBlocking {
        // §6.1 — a night is judged against the target in effect on its date.
        // Log last night's curfew under the default 21:30 start.
        repo.logCurfew(today.minusDays(1), held = true)
        assertEquals(LocalTime.of(21, 30), repo.curfewOn(today.minusDays(1))!!.curfewStart)

        // Change the target today: bed 22:00, curfew 60 → start 21:00.
        repo.editTarget(LocalTime.of(22, 0), LocalTime.of(6, 0), 60)

        // Yesterday's snapshot is untouched; tonight uses the new start.
        assertEquals(LocalTime.of(21, 30), repo.curfewOn(today.minusDays(1))!!.curfewStart)
        repo.logCurfewPrecise(today, LocalTime.of(21, 10))
        assertEquals(LocalTime.of(21, 0), repo.curfewOn(today)!!.curfewStart)
        assertFalse(repo.curfewOn(today)!!.held)

        // The edit inserted a dated row; the default was never persisted or mutated.
        assertEquals(1, db.sleepDao().getAllTargets().size)
    }

    @Test
    fun curfewMinutesClampedToSpecRange() = runBlocking {
        assertEquals(180, repo.editTarget(LocalTime.of(23, 0), LocalTime.of(7, 0), 300).curfewMinutes)
        assertEquals(30, repo.editTarget(LocalTime.of(23, 0), LocalTime.of(7, 0), 5).curfewMinutes)
    }

    @Test
    fun preciseCurfewDerivesHeld() = runBlocking {
        repo.logCurfewPrecise(today, LocalTime.of(21, 10))
        assertTrue(repo.curfewOn(today)!!.held)
        repo.logCurfewPrecise(today, LocalTime.of(0, 30))
        assertFalse(repo.curfewOn(today)!!.held)
    }

    @Test
    fun oneLogPerNightLatestWins() = runBlocking {
        repo.logSleep(today, LocalTime.of(23, 0), LocalTime.of(7, 0), 10, 1, 3)
        repo.logSleep(today, LocalTime.of(23, 30), LocalTime.of(7, 0), 20, 2, 4)
        val logs = db.sleepDao().getAllLogs()
        assertEquals(1, logs.size)
        assertEquals(LocalTime.of(23, 30), logs[0].bedTime)
        assertEquals(4, logs[0].quality)
    }

    @Test
    fun editWindowIsEnforced() = runBlocking {
        assertFalse(repo.logSleep(today.minusDays(3), LocalTime.of(23, 0), LocalTime.of(7, 0), 0, 0, 3))
        assertFalse(repo.logCurfew(today.minusDays(3), held = true))
        assertFalse(repo.logChecklist(today.minusDays(3), true, true, true, true))
        assertTrue(repo.logSleep(today.minusDays(2), LocalTime.of(23, 0), LocalTime.of(7, 0), 0, 0, 3))
    }

    @Test
    fun stripAndHeldCount() = runBlocking {
        repo.logCurfew(today, held = true)
        repo.logCurfew(today.minusDays(1), held = true)
        repo.logCurfew(today.minusDays(2), held = false)

        val strip = repo.curfewStrip()
        assertEquals(28, strip.size)
        assertEquals(true, strip[27]!!.held)   // newest last
        assertEquals(false, strip[25]!!.held)
        assertNull(strip[0])                    // unlogged
        assertEquals(2, repo.heldOfLast())
    }

    @Test
    fun regularityStatsFromLogs() = runBlocking {
        // Identical nights within the trailing 7 days → regularity 100.
        (0..4L).forEach { d ->
            db.sleepDao().upsertLog(
                com.tatara.data.db.entity.SleepLog(
                    date = today.minusDays(d), bedTime = LocalTime.of(23, 0),
                    wakeTime = LocalTime.of(7, 0), timeToFallAsleepMin = 15,
                    wakeCount = 0, quality = 4,
                )
            )
        }
        val stats = repo.stats()
        assertEquals(100f, stats.regularity!!, 0.01f)
        assertEquals(465f, stats.meanDurationMin!!, 0.01f)
    }

    @Test
    fun correlationsJoinQualityAndSuppressThinSides() = runBlocking {
        // 12 nights: caffeine cutoff met on 6 (quality 5) and missed on 6 (quality 3);
        // room dark true on only 4 of them — that side is too thin to report.
        (0..11L).forEach { d ->
            val date = today.minusDays(d)
            val met = d < 6
            db.sleepDao().upsertLog(
                com.tatara.data.db.entity.SleepLog(
                    date = date, bedTime = LocalTime.of(23, 0), wakeTime = LocalTime.of(7, 0),
                    timeToFallAsleepMin = 0, wakeCount = 0, quality = if (met) 5 else 3,
                )
            )
            db.sleepDao().upsertChecklist(
                com.tatara.data.db.entity.SleepChecklist(
                    date = date, caffeineCutoffMet = met, roomDark = d < 4,
                    roomCool = met, noLateLargeMeal = !met,
                )
            )
        }

        val comps = repo.correlations()
        val caffeine = comps.first { it.label == "Caffeine cutoff" }
        assertEquals(5f, caffeine.yesMean, 0.01f)
        assertEquals(6, caffeine.yesN)
        assertEquals(3f, caffeine.noMean, 0.01f)
        assertEquals(6, caffeine.noN)
        assertTrue(comps.none { it.label == "Room dark" })
        assertTrue(comps.none { it.label == "Screen curfew" })  // no curfew logs at all
    }
}
