package com.tatara.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoutineItemMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TataraDatabase::class.java,
    )

    @Test
    fun migrate3To4_preservesRoutineItemsAndAddsNullColumns() {
        helper.createDatabase(DB, 3).apply {
            execSQL(
                "INSERT INTO routine_item (id, routineId, exerciseId, targetSets, sortOrder, " +
                    "repRangeLow, repRangeHigh, incrementKg, currentWeightKg) " +
                    "VALUES (1, 1, 1, 3, 0, 8, 12, 2.5, 60.0)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            DB, 4, true,
            TataraDatabase.MIGRATION_1_2, TataraDatabase.MIGRATION_2_3, TataraDatabase.MIGRATION_3_4,
        )

        db.query("SELECT currentWeightKg, restSeconds, targetRpe FROM routine_item WHERE id = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(60.0f, c.getFloat(0), 0.001f)
            assertTrue("restSeconds should be null", c.isNull(1))
            assertTrue("targetRpe should be null", c.isNull(2))
        }
    }

    private companion object {
        const val DB = "migration-test.db"
    }
}
