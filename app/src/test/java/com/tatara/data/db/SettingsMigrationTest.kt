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
class SettingsMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TataraDatabase::class.java,
    )

    @Test
    fun migrate1To2_preservesSettingsAndAddsNullColumns() {
        helper.createDatabase(DB, 1).apply {
            execSQL(
                "INSERT INTO settings (id, proteinPerKg, goalRatePercent, blockStartDate, ratchetWeightKg) " +
                    "VALUES (1, 2.0, -0.5, '2026-07-01', 82.5)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 2, true, TataraDatabase.MIGRATION_1_2)

        db.query("SELECT proteinPerKg, ratchetWeightKg, heightCm, birthYear, sex, lastProcessedWeekEnd FROM settings WHERE id = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(2.0f, c.getFloat(0), 0.001f)
            assertEquals(82.5f, c.getFloat(1), 0.001f)
            assertTrue(c.isNull(2))
            assertTrue(c.isNull(3))
            assertTrue(c.isNull(4))
            assertTrue(c.isNull(5))
        }
    }

    private companion object {
        const val DB = "migration-test.db"
    }
}
