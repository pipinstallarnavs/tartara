package com.tatara.data.food

import com.tatara.data.db.entity.Food
import com.tatara.data.db.entity.UnitType

sealed interface MatchResult {
    /** quantity is in grams for GRAM foods, portions for PORTION foods; null if the input had none. */
    data class Resolved(val food: Food, val quantity: Float?) : MatchResult
    data class Ambiguous(val candidates: List<Food>, val quantity: Float?, val query: String) : MatchResult
    data class NoMatch(val query: String, val quantity: Float? = null) : MatchResult
}

/**
 * §3.1 — case-insensitive fuzzy match (Levenshtein ≤2 or prefix) against food names
 * and aliases. Rank by frequency of use, then recency, then alphabetically.
 */
class FoodMatcher(private val foods: List<Food>) {

    private enum class Quality { EXACT, PREFIX, FUZZY }

    private data class Candidate(val food: Food, val quality: Quality)

    fun match(input: ParsedInput): MatchResult {
        if (input.query.isBlank()) return MatchResult.NoMatch(input.query, input.quantity)

        val candidates = mutableMapOf<Long, Candidate>()
        for (food in foods) {
            val candidate = tryFood(food, input) ?: continue
            val existing = candidates[food.id]
            if (existing == null || candidate.quality < existing.quality) candidates[food.id] = candidate
        }
        if (candidates.isEmpty()) return MatchResult.NoMatch(input.query, input.quantity)

        val ranked = candidates.values
            .sortedWith(
                compareBy<Candidate> { it.quality }
                    .thenByDescending { it.food.useCount }
                    .thenByDescending { it.food.lastUsedAt?.toEpochMilli() ?: Long.MIN_VALUE }
                    .thenBy { it.food.name.lowercase() }
            )

        val best = ranked.first()
        val exactCount = ranked.count { it.quality == Quality.EXACT }
        val resolved = when {
            ranked.size == 1 -> best.food
            exactCount == 1 && best.quality == Quality.EXACT -> best.food
            else -> null
        }
        return if (resolved != null) {
            MatchResult.Resolved(resolved, quantityFor(resolved, input))
        } else {
            MatchResult.Ambiguous(ranked.take(5).map { it.food }, input.quantity, input.query)
        }
    }

    /** Tries the query as-is, and with the food's portion name as a leading unit token. */
    private fun tryFood(food: Food, input: ParsedInput): Candidate? {
        // "60g daal" where daal is per-katori is unresolvable — grams need a GRAM food.
        if ((input.unit == "g" || input.unit == "ml") && food.unitType != UnitType.GRAM) return null

        var query = input.query
        if (input.unit == null && food.unitType == UnitType.PORTION) {
            val tokens = query.split(" ")
            val portion = food.portionName?.lowercase()
            if (tokens.size >= 2 && portion != null && singular(tokens.first()) == portion) {
                query = tokens.drop(1).joinToString(" ")
            }
        }

        val targets = buildList {
            add(food.name.lowercase())
            food.aliases.split(",").map { it.trim().lowercase() }.filterTo(this) { it.isNotEmpty() }
        }
        val queries = listOf(query, singular(query))

        var best: Quality? = null
        for (t in targets) for (q in queries) {
            val quality = when {
                t == q -> Quality.EXACT
                t.startsWith(q) -> Quality.PREFIX
                levenshtein(t, q) <= 2 -> Quality.FUZZY
                else -> null
            }
            if (quality != null && (best == null || quality < best)) best = quality
        }
        return best?.let { Candidate(food, it) }
    }

    private fun quantityFor(food: Food, input: ParsedInput): Float? = input.quantity

    private fun singular(word: String): String = when {
        word.endsWith("es") && word.length > 3 -> word.dropLast(2)
        word.endsWith("s") && word.length > 2 -> word.dropLast(1)
        else -> word
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(curr[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            curr.copyInto(prev)
        }
        return prev[b.length]
    }
}
