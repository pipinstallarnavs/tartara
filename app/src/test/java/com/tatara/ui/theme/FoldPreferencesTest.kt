package com.tatara.ui.theme

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FoldPreferencesTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("tatara_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun firstRunAdoptsExistingCrossingWithoutCelebrating() {
        // Shipping the fold must not replay a tier crossed long before it existed.
        assertFalse(FoldPreferences(context).claimUnseen(7L))
        // …and having adopted it, that same crossing stays silent.
        assertFalse(FoldPreferences(context).claimUnseen(7L))
    }

    @Test
    fun aNewerCrossingCelebratesExactlyOnce() {
        val prefs = FoldPreferences(context)
        prefs.claimUnseen(3L)
        assertTrue(prefs.claimUnseen(4L))
        // The dashboard refreshes constantly; the fold must not fire again.
        assertFalse(prefs.claimUnseen(4L))
    }

    @Test
    fun noCrossingsNeverCelebrates() {
        assertFalse(FoldPreferences(context).claimUnseen(0L))
    }
}
