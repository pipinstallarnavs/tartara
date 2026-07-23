package com.tatara.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tatara.data.db.dao.EntryWithFood
import com.tatara.data.db.entity.Food
import com.tatara.data.food.FoodRepository
import com.tatara.data.food.LogResult
import com.tatara.data.food.MacroMath
import com.tatara.data.food.MacroTotals
import com.tatara.ui.theme.LocalThemeColors
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun FoodScreen(repo: FoodRepository) {
    val c = LocalThemeColors.current
    val scope = rememberCoroutineScope()

    var input by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf(listOf<EntryWithFood>()) }
    var totals by remember { mutableStateOf(MacroTotals()) }
    var candidates by remember { mutableStateOf(listOf<Food>()) }
    var pendingQuantity by remember { mutableStateOf<Float?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        entries = repo.entriesOn()
        totals = repo.totalsOn()
    }

    LaunchedEffect(Unit) { refresh() }

    fun submit() {
        val text = input
        scope.launch {
            when (val result = repo.log(text)) {
                is LogResult.Logged -> {
                    input = ""
                    candidates = emptyList()
                    notice = null
                    refresh()
                }
                is LogResult.Ambiguous -> {
                    candidates = result.candidates
                    pendingQuantity = result.quantity
                    notice = null
                }
                is LogResult.NoMatch -> {
                    candidates = emptyList()
                    notice = "No match for \"${result.query}\"."
                }
                LogResult.OutsideEditWindow -> notice = "Outside the edit window."
                LogResult.EmptyInput -> Unit
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 16.dp, top = 16.dp)) {
        BasicTextField(
            value = input,
            onValueChange = { input = it },
            singleLine = true,
            textStyle = TextStyle(color = c.primary, fontSize = 16.sp),
            cursorBrush = SolidColor(c.cool),
            decorationBox = { inner ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.surface, RoundedCornerShape(2.dp))
                        .padding(12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (input.isEmpty()) {
                            Text("60g rice", color = c.muted, fontSize = 16.sp)
                        }
                        inner()
                    }
                    Text(
                        text = "Log",
                        color = c.cool,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { submit() }.padding(start = 12.dp),
                    )
                }
            },
        )

        if (candidates.isNotEmpty()) {
            // §3.1 — inline chip row, one tap to disambiguate. Never a dialog.
            LazyRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(candidates) { food ->
                    Text(
                        text = food.name,
                        color = c.primary,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .background(c.surface, RoundedCornerShape(2.dp))
                            .clickable {
                                scope.launch {
                                    repo.addEntry(food, pendingQuantity ?: 100f)
                                    input = ""
                                    candidates = emptyList()
                                    refresh()
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }

        notice?.let {
            Text(it, color = c.warm, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        TotalsLine(totals)
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(c.hairline))

        if (entries.isEmpty()) {
            Text(
                "Nothing logged.",
                color = c.muted,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                items(entries) { e ->
                    EntryRow(e)
                }
            }
        }
    }
}

@Composable
private fun TotalsLine(t: MacroTotals) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Metric("kcal", t.kcal.toInt().toString())
        Metric("P", fmt(t.protein))
        Metric("C", fmt(t.carbs))
        Metric("F", fmt(t.fat))
        // §3.3 — derived values carry a ~ prefix everywhere.
        Metric("sat", "~" + fmt(t.satFat))
    }
}

@Composable
private fun Metric(label: String, value: String) {
    val c = LocalThemeColors.current
    Row(verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
        Text(value, color = c.primary, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.width(3.dp))
        Text(label, color = c.muted, fontSize = 11.sp)
    }
}

@Composable
private fun EntryRow(e: EntryWithFood) {
    val c = LocalThemeColors.current
    val m = MacroMath.macrosFor(e.food, e.entry.quantity)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row {
                Text(e.food.name, color = c.primary, fontSize = 14.sp)
                if (e.food.isEstimated) {
                    // §3.2 — estimated foods show a dot marker.
                    Text(" •", color = c.muted, fontSize = 14.sp)
                }
            }
            Text(quantityLabel(e), color = c.muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        Text(
            "${m.kcal.toInt()} kcal",
            color = c.primary,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

private fun quantityLabel(e: EntryWithFood): String = when (e.food.unitType) {
    com.tatara.data.db.entity.UnitType.GRAM -> "${fmt(e.entry.quantity)}g"
    com.tatara.data.db.entity.UnitType.PORTION ->
        "${fmt(e.entry.quantity)} ${e.food.portionName ?: "portion"}"
}

private fun fmt(v: Float): String =
    if (v == v.toInt().toFloat()) v.toInt().toString()
    else String.format(Locale.US, "%.1f", v)
