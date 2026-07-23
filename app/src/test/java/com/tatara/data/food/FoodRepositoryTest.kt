package com.tatara.data.food

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tatara.data.db.Seeder
import com.tatara.data.db.TataraDatabase
import java.time.Instant
import java.time.LocalDate
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
class FoodRepositoryTest {

    private val today = LocalDate.of(2026, 7, 23)
    private lateinit var db: TataraDatabase
    private lateinit var repo: FoodRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, TataraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = FoodRepository(db, today = { today }, now = { Instant.parse("2026-07-23T12:00:00Z") })
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun seedIsLoadedOnceAndSpotChecked() = runBlocking {
        Seeder.seedIfEmpty(context, db)
        val count = db.foodDao().count()
        assertTrue("expected 500+ seeded foods, got $count", count >= 540)

        Seeder.seedIfEmpty(context, db)
        assertEquals("second seed must be a no-op", count, db.foodDao().count())

        // IFCT A013: Rice, raw, brown — 1480 kJ ≈ 353.7 kcal per 100g.
        val rice = db.foodDao().getAll().first { it.name == "Rice, raw, brown" }
        assertEquals(353.7f, rice.kcal, 0.5f)

        assertTrue("exercise library seeded", db.trainDao().countExercises() >= 75)
    }

    @Test
    fun logAgainstSeedResolvesInsertsAndBumpsUsage() = runBlocking {
        Seeder.seedIfEmpty(context, db)

        // "egg" is a seeded PORTION supplement with an exact name; plural is stripped.
        val logged = repo.log("3 eggs") as LogResult.Logged
        assertEquals(3f, logged.entry.quantity)
        assertEquals("egg", logged.food.name)

        val egg = db.foodDao().getAll().first { it.id == logged.food.id }
        assertEquals(1, egg.useCount)
    }

    @Test
    fun customPortionFoodLogsByItsPortionName() = runBlocking {
        val daal = repo.createCustomFood(
            "daal", com.tatara.data.db.entity.UnitType.PORTION, "katori",
            180f, 9f, 22f, 6f, com.tatara.data.db.entity.FatSource.GRAIN_LEGUME, isEstimated = true,
        )
        val logged = repo.log("2 katori daal") as LogResult.Logged
        assertEquals(daal.id, logged.food.id)
        assertEquals(2f, logged.entry.quantity)
    }

    @Test
    fun ambiguousAndNoMatchAndEmpty() = runBlocking {
        Seeder.seedIfEmpty(context, db)
        assertTrue(repo.log("50 ric") is LogResult.Ambiguous)
        assertTrue(repo.log("1 chicken tikka masala xyz") is LogResult.NoMatch)
        assertTrue(repo.log("   ") is LogResult.EmptyInput)
    }

    @Test
    fun editWindowIsHardBounded() = runBlocking {
        Seeder.seedIfEmpty(context, db)
        assertTrue(repo.log("60g rice", today.minusDays(2)) !is LogResult.OutsideEditWindow)
        assertTrue(repo.log("60g rice", today.minusDays(3)) is LogResult.OutsideEditWindow)
        assertTrue(repo.log("60g rice", today.plusDays(1)) is LogResult.OutsideEditWindow)
    }

    @Test
    fun totalsDeriveSatFat() = runBlocking {
        val ghee = repo.createCustomFood(
            "ghee", com.tatara.data.db.entity.UnitType.GRAM, null,
            900f, 0f, 0f, 100f, com.tatara.data.db.entity.FatSource.DAIRY,
        )
        val rice = repo.createCustomFood(
            "plain rice", com.tatara.data.db.entity.UnitType.GRAM, null,
            130f, 2.7f, 28.2f, 0.3f, com.tatara.data.db.entity.FatSource.GRAIN_LEGUME,
        )
        repo.addEntry(ghee, 10f)
        repo.addEntry(rice, 100f)

        val t = repo.totalsOn(today)
        assertEquals(90f + 130f, t.kcal, 0.01f)
        // ~sat: 10g ghee fat × 0.65 + 0.3g rice fat × 0.20
        assertEquals(10f * 0.65f + 0.3f * 0.20f, t.satFat, 0.001f)
    }

    @Test
    fun repeatDayClonesEntries() = runBlocking {
        val rice = repo.createCustomFood(
            "plain rice", com.tatara.data.db.entity.UnitType.GRAM, null,
            130f, 2.7f, 28.2f, 0.3f, com.tatara.data.db.entity.FatSource.GRAIN_LEGUME,
        )
        val yesterday = today.minusDays(1)
        repo.addEntry(rice, 60f, yesterday)
        repo.addEntry(rice, 80f, yesterday)

        assertEquals(2, repo.repeatDay(from = yesterday, to = today))
        val entries = repo.entriesOn(today)
        assertEquals(listOf(60f, 80f), entries.map { it.entry.quantity })
    }
}
