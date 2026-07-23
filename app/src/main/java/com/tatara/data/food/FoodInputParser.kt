package com.tatara.data.food

/**
 * §3.1 — parses "quantity + unit + food" from the single text input.
 * quantity is null when the line has no leading number ("rice" alone).
 * unit is "g", "ml", or null; portion names ("2 katori daal") are resolved by
 * FoodMatcher against the candidate food's own portionName.
 */
data class ParsedInput(
    val quantity: Float?,
    val unit: String?,
    val query: String,
)

object FoodInputParser {

    private val LEADING_QUANTITY = Regex("""^(\d+(?:\.\d+)?)\s*""")
    private val ATTACHED_UNIT = Regex("""^(g|ml)\b\s*""", RegexOption.IGNORE_CASE)

    fun parse(raw: String): ParsedInput {
        val text = raw.trim().replace(Regex("""\s+"""), " ")
        val quantityMatch = LEADING_QUANTITY.find(text)
            ?: return ParsedInput(quantity = null, unit = null, query = text.lowercase())

        var rest = text.substring(quantityMatch.range.last + 1)
        val quantity = quantityMatch.groupValues[1].toFloat()

        var unit: String? = null
        val unitMatch = ATTACHED_UNIT.find(rest)
        if (unitMatch != null) {
            unit = unitMatch.groupValues[1].lowercase()
            rest = rest.substring(unitMatch.range.last + 1)
        }

        return ParsedInput(quantity = quantity, unit = unit, query = rest.trim().lowercase())
    }
}
