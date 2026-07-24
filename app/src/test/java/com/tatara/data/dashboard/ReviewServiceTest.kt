package com.tatara.data.dashboard

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tatara.data.db.TataraDatabase
import com.tatara.data.db.entity.FatSource
import com.tatara.data.db.entity.Food
import com.tatara.data.db.entity.FoodEntry
import com.tatara.data.db.entity.TargetAdjustment
import com.tatara.data.db.entity.WeightEntry
import com.tatara.data.db.entity.XpEventType
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReviewServiceTest {

    private val now = ZonedDateTime.of(2026, 7, 23, 12, 0, 0, 0, ZoneOffset.UTC)
    private lateinit var db: TataraDatabase
    private lateinit var service: ReviewService

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            TataraDatabase::class.java,
        ).allowMainThreadQueries().build()
        service = ReviewService(db)
    }

    @After
    fun tearDown() = db.close()

    private fun instantOn(date: LocalDate) = date.atTime(12, 0).toInstant(ZoneOffset.UTC)

    private suspend fun seedTwoWeeks() {
        db.foodDao().insert(
            Food(id = 1, name = "meal", unitType = com.tatara.data.db.entity.UnitType.GRAM,
                kcal = 1000f, protein = 50f, carbs = 100f, fat = 20f, fatSource = FatSource.MIXED)
        )
        // Mon Jul 6 .. Sun Jul 19: entries and weights daily.
        (0 until 14).forEach { i ->
            val day = LocalDate.of(2026, 7, 6).plusDays(i.toLong())
            db.foodDao().insertEntry(FoodEntry(date = day, foodId = 1, quantity = 280f, loggedAt = instantOn(day)))
            db.bodyDao().insertWeight(WeightEntry(date = day, weightKg = 80f, loggedAt = instantOn(day)))
        }
    }

    @Test
    fun generatesOneReviewPerCompletedWeekIdempotently() = runBlocking {
        seedTwoWeeks()
        service.generateDueReviews(now)
        // Weeks of Jul 6 and Jul 13 have closed; the week of Jul 20 has not.
        assertEquals(
            listOf(LocalDate.of(2026, 7, 6), LocalDate.of(2026, 7, 13)),
            db.dashboardDao().getAllReviews().map { it.weekStart }.sorted(),
        )

        service.generateDueReviews(now)
        assertEquals(2, db.dashboardDao().getAllReviews().size)
    }

    @Test
    fun openingAwardsXpExactlyOnce() = runBlocking {
        seedTwoWeeks()
        service.generateDueReviews(now)
        val review = db.dashboardDao().getAllReviews().first()

        assertTrue(service.openReview(review.id, now.toLocalDate()))
        assertFalse(service.openReview(review.id, now.toLocalDate()))

        val awards = db.dashboardDao().getAllXpEvents().filter { it.type == XpEventType.REVIEW_OPENED }
        assertEquals(1, awards.size)
        assertEquals(25, awards[0].amount)
        assertNotNull(db.dashboardDao().reviewById(review.id)!!.openedAt)
    }

    @Test
    fun contentShowsTheArithmetic() = runBlocking {
        seedTwoWeeks()
        val weekStart = LocalDate.of(2026, 7, 13)
        // The adjustment computed from that week takes effect the Monday after.
        db.bodyDao().insertAdjustment(
            TargetAdjustment(
                effectiveFrom = weekStart.plusDays(7), kcalTarget = 2360f, proteinG = 144f,
                fatG = 78.7f, carbsG = 269f, meanDailyKcal = 2800f, ewmaStart = 80f,
                ewmaEnd = 80f, impliedTdee = 2800f, weeklyRatePercent = -0.5f,
                computedFromWeightKg = 80f,
            )
        )

        val content = service.contentFor(weekStart)

        assertEquals(7, content.loggedDays)
        assertEquals(2800f, content.meanKcal!!, 0.01f)
        assertEquals(80f, content.ewmaStart!!, 0.001f)
        assertEquals(80f, content.ewmaEnd!!, 0.001f)
        assertEquals(2360f, content.adjustment!!.kcalTarget, 0.01f)
    }
}
