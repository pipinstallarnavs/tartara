package com.tatara.data.tdee

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tatara.data.db.TataraDatabase
import com.tatara.data.db.entity.FatSource
import com.tatara.data.db.entity.Food
import com.tatara.data.db.entity.FoodEntry
import com.tatara.data.db.entity.UnitType
import com.tatara.data.db.entity.WeightEntry
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TdeeServiceTest {

    private lateinit var db: TataraDatabase
    private lateinit var service: TdeeService

    // Mon 2026-06-29 .. Sun 2026-07-19: three full weeks; "now" is Thu the 23rd.
    private val firstDay = LocalDate.of(2026, 6, 29)
    private val now = ZonedDateTime.of(2026, 7, 23, 12, 0, 0, 0, ZoneOffset.UTC)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            TataraDatabase::class.java,
        ).allowMainThreadQueries().build()
        service = TdeeService(db)
    }

    @After
    fun tearDown() = db.close()

    private fun seedThreeWeeks() = runBlocking {
        // 1000 kcal per 100g × 280g = 2800 kcal/day; weight flat 80kg, weighed daily.
        db.foodDao().insertAll(
            listOf(
                Food(id = 1, name = "test meal", unitType = UnitType.GRAM,
                    kcal = 1000f, protein = 40f, carbs = 100f, fat = 20f, fatSource = FatSource.MIXED)
            )
        )
        (0 until 21).forEach { i ->
            val day = firstDay.plusDays(i.toLong())
            val noon = day.atTime(12, 0).toInstant(ZoneOffset.UTC)
            db.foodDao().insertEntry(FoodEntry(date = day, foodId = 1, quantity = 280f, loggedAt = noon))
            db.bodyDao().insertWeight(WeightEntry(date = day, weightKg = 80f, loggedAt = noon))
        }
    }

    @Test
    fun catchUpProcessesEachPassedSundayOnce() = runBlocking {
        seedThreeWeeks()

        val results = service.catchUp(now)

        // Sundays Jul 5 (inside the 14-day baseline → skipped), Jul 12, Jul 19.
        assertEquals(
            listOf(LocalDate.of(2026, 7, 5), LocalDate.of(2026, 7, 12), LocalDate.of(2026, 7, 19)),
            results.map { it.weekEnd },
        )
        assertEquals(SkipReason.TOO_EARLY, (results[0].outcome as TdeeOutcome.Skipped).reason)
        assertTrue(results[1].outcome is TdeeOutcome.Adjusted)
        assertTrue(results[2].outcome is TdeeOutcome.Adjusted)

        // Flat 80kg at 2800 kcal on a 0.5% cut → 2360 both weeks (week 3 capped vs 2360 is a no-op).
        val adjustments = db.bodyDao().getAllAdjustments()
        assertEquals(2, adjustments.size)
        adjustments.forEach { assertEquals(2360f, it.kcalTarget, 1f) }
        assertEquals(LocalDate.of(2026, 7, 13), adjustments[0].effectiveFrom)
        assertEquals(LocalDate.of(2026, 7, 20), adjustments[1].effectiveFrom)
        assertEquals(144f, adjustments[0].proteinG, 0.1f)

        val settings = db.bodyDao().getSettings()!!
        assertEquals(LocalDate.of(2026, 7, 19), settings.lastProcessedWeekEnd)
        assertEquals(80f, settings.ratchetWeightKg!!, 0.01f)
    }

    @Test
    fun secondCatchUpIsANoOp() = runBlocking {
        seedThreeWeeks()
        service.catchUp(now)

        val again = service.catchUp(now)

        assertTrue(again.isEmpty())
        assertEquals(2, db.bodyDao().getAllAdjustments().size)
    }

    @Test
    fun sundayNotYetAtElevenPmIsNotProcessed() = runBlocking {
        seedThreeWeeks()
        // Sunday Jul 19 at 22:59 — that week must not close yet.
        val results = service.catchUp(ZonedDateTime.of(2026, 7, 19, 22, 59, 0, 0, ZoneOffset.UTC))
        assertEquals(
            listOf(LocalDate.of(2026, 7, 5), LocalDate.of(2026, 7, 12)),
            results.map { it.weekEnd },
        )
    }

    @Test
    fun noDataMeansNoProcessing() = runBlocking {
        assertTrue(service.catchUp(now).isEmpty())
        assertEquals(null, db.bodyDao().getSettings()?.lastProcessedWeekEnd)
    }

    @Test
    fun midweekRefreshFlagFollowsEwmaDrift() = runBlocking {
        seedThreeWeeks()
        service.catchUp(now)
        // Flat weight: no drift.
        assertTrue(!service.shouldOfferMidweekRefresh(now.toInstant()))

        // A large post-adjustment move: four days at 85kg pulls ewma past 2%.
        (1..4).forEach { i ->
            val day = LocalDate.of(2026, 7, 19).plusDays(i.toLong())
            db.bodyDao().insertWeight(
                WeightEntry(date = day, weightKg = 85f, loggedAt = day.atTime(12, 0).toInstant(ZoneOffset.UTC))
            )
        }
        assertTrue(service.shouldOfferMidweekRefresh(now.toInstant()))
    }
}
