package com.tatara.data.food

import org.junit.Assert.assertEquals
import org.junit.Test

/** The worked examples from §3.1, verbatim. */
class FoodInputParserTest {

    @Test fun gramsAttached() =
        assertEquals(ParsedInput(60f, "g", "rice"), FoodInputParser.parse("60g rice"))

    @Test fun gramsDetached() =
        assertEquals(ParsedInput(60f, "g", "rice"), FoodInputParser.parse("60 g rice"))

    @Test fun millilitres() =
        assertEquals(ParsedInput(200f, "ml", "milk"), FoodInputParser.parse("200ml milk"))

    @Test fun namedPortionStaysInQuery() =
        assertEquals(ParsedInput(2f, null, "katori daal"), FoodInputParser.parse("2 katori daal"))

    @Test fun bareNumber() =
        assertEquals(ParsedInput(150f, null, "chicken breast"), FoodInputParser.parse("150 chicken breast"))

    @Test fun decimalQuantity() =
        assertEquals(ParsedInput(1.5f, null, "roti"), FoodInputParser.parse("1.5 roti"))

    @Test fun pluralKeptForMatcher() =
        assertEquals(ParsedInput(3f, null, "eggs"), FoodInputParser.parse("3 eggs"))

    @Test fun noQuantity() =
        assertEquals(ParsedInput(null, null, "paneer"), FoodInputParser.parse("paneer"))

    @Test fun caseAndWhitespaceNormalised() =
        assertEquals(ParsedInput(60f, "g", "basmati rice"), FoodInputParser.parse("  60G   Basmati  Rice "))

    @Test fun gPrefixedFoodIsNotAUnit() =
        assertEquals(ParsedInput(2f, null, "guava"), FoodInputParser.parse("2 guava"))
}
