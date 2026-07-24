package com.tatara.data.dashboard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class InsightCardsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun everyCardHasTextAndASourceLine() {
        val cards = InsightCards.load(context)
        assertEquals(10, cards.size)
        cards.forEach {
            assertTrue(it.text.isNotBlank())
            assertTrue("card needs a source: ${it.text}", it.source.isNotBlank())
            // §2.5 voice — no exclamation marks anywhere in the app.
            assertTrue(!it.text.contains("!"))
        }
    }

    @Test
    fun onePerDayDeterministic() {
        val d = LocalDate.of(2026, 7, 23)
        assertEquals(InsightCards.cardFor(context, d), InsightCards.cardFor(context, d))
        // Consecutive days rotate.
        val cards = (0L..9L).map { InsightCards.cardFor(context, d.plusDays(it)) }.toSet()
        assertEquals(10, cards.size)
    }
}
