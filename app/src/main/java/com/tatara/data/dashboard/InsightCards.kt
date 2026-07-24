package com.tatara.data.dashboard

import android.content.Context
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** §7.4 — rotating factual cards, one per day. Every card carries a source line. */
@Serializable
data class InsightCard(val text: String, val source: String)

object InsightCards {

    private var cache: List<InsightCard>? = null

    fun load(context: Context): List<InsightCard> =
        cache ?: Json.decodeFromString<List<InsightCard>>(
            context.assets.open("insights.json").bufferedReader().readText()
        ).also { cache = it }

    fun cardFor(context: Context, date: LocalDate): InsightCard? {
        val cards = load(context)
        if (cards.isEmpty()) return null
        return cards[(date.toEpochDay() % cards.size).toInt()]
    }
}
