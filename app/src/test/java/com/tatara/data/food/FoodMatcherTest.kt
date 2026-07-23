package com.tatara.data.food

import com.tatara.data.db.entity.FatSource
import com.tatara.data.db.entity.Food
import com.tatara.data.db.entity.UnitType
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodMatcherTest {

    private fun gram(id: Long, name: String, useCount: Int = 0, aliases: String = "") = Food(
        id = id, name = name, aliases = aliases, unitType = UnitType.GRAM,
        kcal = 100f, protein = 1f, carbs = 1f, fat = 1f,
        fatSource = FatSource.GRAIN_LEGUME, useCount = useCount,
        lastUsedAt = if (useCount > 0) Instant.EPOCH else null,
    )

    private fun portion(id: Long, name: String, portionName: String) = Food(
        id = id, name = name, portionName = portionName, unitType = UnitType.PORTION,
        kcal = 100f, protein = 1f, carbs = 1f, fat = 1f, fatSource = FatSource.GRAIN_LEGUME,
    )

    private val foods = listOf(
        gram(1, "rice", useCount = 10, aliases = "chawal"),
        gram(2, "rice flakes"),
        gram(3, "chicken breast"),
        portion(4, "daal", portionName = "katori"),
        portion(5, "egg", portionName = "egg"),
        portion(6, "roti", portionName = "roti"),
    )

    private val matcher = FoodMatcher(foods)

    private fun match(text: String) = matcher.match(FoodInputParser.parse(text))

    @Test fun exactNameWins() {
        val r = match("60g rice") as MatchResult.Resolved
        assertEquals(1L, r.food.id)
        assertEquals(60f, r.quantity)
    }

    @Test fun portionNameUnitResolves() {
        val r = match("2 katori daal") as MatchResult.Resolved
        assertEquals(4L, r.food.id)
        assertEquals(2f, r.quantity)
    }

    @Test fun pluralStripped() {
        val r = match("3 eggs") as MatchResult.Resolved
        assertEquals(5L, r.food.id)
        assertEquals(3f, r.quantity)
    }

    @Test fun fuzzyWithinTwoEdits() {
        val r = match("100 chiken brest") as MatchResult.Resolved
        assertEquals(3L, r.food.id)
    }

    @Test fun aliasMatches() {
        val r = match("60g chawal") as MatchResult.Resolved
        assertEquals(1L, r.food.id)
    }

    @Test fun prefixAmbiguityReturnsChips() {
        val r = match("50 ric") as MatchResult.Ambiguous
        assertTrue(r.candidates.map { it.id }.containsAll(listOf(1L, 2L)))
        // §3.1 — rank by frequency of use first.
        assertEquals(1L, r.candidates.first().id)
    }

    @Test fun gramsExcludePortionFoods() {
        // "60g egg" cannot mean the per-portion egg; no GRAM egg exists here.
        assertTrue(match("60g egg") is MatchResult.NoMatch)
    }

    @Test fun noMatchCarriesQuery() {
        val r = match("2 chicken tikka masala") as MatchResult.NoMatch
        assertEquals("chicken tikka masala", r.query)
    }
}
