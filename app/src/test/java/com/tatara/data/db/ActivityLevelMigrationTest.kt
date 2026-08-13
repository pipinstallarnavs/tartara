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
class ActivityLevelMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TataraDatabase::class.java,
    )

    @Test
    fun migrate4To5_preservesSettingsAndAddsNullActivityLevel() {
        helper.createDatabase(DB, 4).apply {
            execSQL(
                "INSERT INTO settings (id, proteinPerKg, goalRatePercent, blockStartDate, ratchetWeightKg) " +
                    "VALUES (1, 2.0, -0.5, '2026-07-01', 82.5)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            DB, 5, true,
            TataraDatabase.MIGRATION_1_2, TataraDatabase.MIGRATION_2_3,
            TataraDatabase.MIGRATION_3_4, TataraDatabase.MIGRATION_4_5,
        )

        db.query("SELECT proteinPerKg, activityLevel FROM settings WHERE id = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(2.0f, c.getFloat(0), 0.001f)
            assertTrue("activityLevel should be null", c.isNull(1))
        }
    }

    private companion object {
        const val DB = "migration-test.db"
    }
}
