package com.tatara.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.tatara.data.db.dao.EntryWithFood
import com.tatara.data.db.entity.FatSource
import com.tatara.data.db.entity.Food
import com.tatara.data.db.entity.UnitType
import com.tatara.data.food.FatPace
import com.tatara.data.food.FatPaceState
import com.tatara.data.food.FoodRepository
import com.tatara.data.food.LogResult
import com.tatara.data.food.MacroMath
import com.tatara.data.food.MacroTotals
import com.tatara.ui.theme.LocalThemeColors
import java.time.LocalDate
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun FoodScreen(repo: FoodRepository) {
    val c = LocalThemeColors.current
    val scope = rememberCoroutineScope()

    var input by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf(listOf<EntryWithFood>()) }
    var totals by remember { mutableStateOf(MacroTotals()) }
    var targets by remember { mutableStateOf<com.tatara.data.db.entity.TargetAdjustment?>(null) }
    var candidates by remember { mutableStateOf(listOf<Food>()) }
    var pendingQuantity by remember { mutableStateOf<Float?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    // Holds (query, quantity) for the "Create <query>" flow — set on a no-match,
    // or when "Add your own" is tapped instead of picking an ambiguous candidate.
    var createPrefill by remember { mutableStateOf<Pair<String, Float?>?>(null) }
    var showCreateFood by remember { mutableStateOf(false) }
    var quickAddFoods by remember { mutableStateOf(listOf<Food>()) }

    suspend fun refresh() {
        entries = repo.entriesOn()
        totals = repo.totalsOn()
        targets = repo.latestTargets()
        quickAddFoods = repo.recentFoods()
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
                    createPrefill = null
                    showCreateFood = false
                    refresh()
                }
                is LogResult.Ambiguous -> {
                    candidates = result.candidates
                    pendingQuantity = result.quantity
                    notice = null
                    createPrefill = result.query to result.quantity
                    showCreateFood = false
                }
                is LogResult.NoMatch -> {
                    candidates = emptyList()
                    notice = "No match for \"${result.query}\"."
                    createPrefill = result.query to result.quantity
                    showCreateFood = false
                }
                LogResult.OutsideEditWindow -> notice = "Outside the edit window."
                LogResult.EmptyInput -> Unit
            }
        }
    }

    fun repeatYesterday() {
        scope.launch {
            repo.repeatDay(from = LocalDate.now().minusDays(1))
            refresh()
        }
    }

    fun quickAdd(food: Food) {
        scope.launch {
            repo.quickAdd(food)
            refresh()
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

        if (candidates.isNotEmpty() && !showCreateFood) {
            // §3.1 — inline chip row, one tap to disambiguate. Never a dialog.
            // "Add your own" is always the last chip — an escape hatch when none
            // of the matches are actually what was typed (e.g. plain "rice").
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
                                    createPrefill = null
                                    refresh()
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
                item {
                    Text(
                        text = "Add your own",
                        color = c.cool,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable { showCreateFood = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }

        notice?.let {
            Text(it, color = c.warm, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
        createPrefill?.let { (query, quantity) ->
            if (showCreateFood) {
                CreateFoodForm(
                    initialName = query,
                    onCancel = {
                        showCreateFood = false
                        createPrefill = null
                        candidates = emptyList()
                    },
                    onSave = { name, unitType, portionName, kcal, protein, carbs, fat ->
                        scope.launch {
                            val food = repo.createCustomFood(
                                name = name, unitType = unitType, portionName = portionName,
                                kcal = kcal, protein = protein, carbs = carbs, fat = fat,
                                fatSource = FatSource.MIXED,
                            )
                            repo.addEntry(food, quantity ?: if (unitType == UnitType.PORTION) 1f else 100f)
                            input = ""
                            showCreateFood = false
                            createPrefill = null
                            candidates = emptyList()
                            notice = null
                            refresh()
                        }
                    },
                )
            } else if (candidates.isEmpty()) {
                Text(
                    "Create \"$query\"",
                    color = c.cool,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { showCreateFood = true }.padding(top = 6.dp),
                )
            }
        }

        if (quickAddFoods.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickAddFoods) { food ->
                    Text(
                        text = food.name,
                        color = c.primary,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .background(c.surface, RoundedCornerShape(2.dp))
                            .clickable { quickAdd(food) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TotalsLine(totals)
        targets?.let { t ->
            Spacer(modifier = Modifier.height(12.dp))
            FatPaceSection(t, totals)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(c.hairline))

        Text(
            "Repeat yesterday",
            color = c.cool,
            fontSize = 12.sp,
            modifier = Modifier.clickable { repeatYesterday() }.padding(vertical = 8.dp),
        )

        if (entries.isEmpty()) {
            EmptyState(EmptyKind.FOOD)
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
private fun CreateFoodForm(
    initialName: String,
    onCancel: () -> Unit,
    onSave: (
        name: String, unitType: UnitType, portionName: String?,
        kcal: Float, protein: Float, carbs: Float, fat: Float,
    ) -> Unit,
) {
    val c = LocalThemeColors.current
    var name by remember { mutableStateOf(initialName) }
    var unitType by remember { mutableStateOf(UnitType.GRAM) }
    var portionName by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(top = 8.dp)) {
        FormField("Name", name) { name = it }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(UnitType.GRAM to "Per 100g", UnitType.PORTION to "Per serving").forEach { (type, label) ->
                Text(
                    label,
                    color = if (unitType == type) c.primary else c.muted,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .background(if (unitType == type) c.surface else Color.Transparent, RoundedCornerShape(2.dp))
                        .clickable { unitType = type }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        if (unitType == UnitType.PORTION) {
            FormField("Serving name (e.g. katori)", portionName) { portionName = it }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField("kcal", kcal) { kcal = it }
            NumberField("P", protein) { protein = it }
            NumberField("C", carbs) { carbs = it }
            NumberField("F", fat) { fat = it }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Cancel", color = c.muted, fontSize = 13.sp, modifier = Modifier.clickable(onClick = onCancel))
            Text(
                "Save",
                color = c.cool,
                fontSize = 13.sp,
                modifier = Modifier.clickable {
                    if (name.isNotBlank()) {
                        onSave(
                            name,
                            unitType,
                            portionName.takeIf { unitType == UnitType.PORTION && it.isNotBlank() },
                            kcal.toFloatOrNull() ?: 0f, protein.toFloatOrNull() ?: 0f,
                            carbs.toFloatOrNull() ?: 0f, fat.toFloatOrNull() ?: 0f,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun FormField(label: String, value: String, onChange: (String) -> Unit) {
    val c = LocalThemeColors.current
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(label, color = c.muted, fontSize = 11.sp)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = c.primary, fontSize = 14.sp),
            cursorBrush = SolidColor(c.cool),
            modifier = Modifier
                .fillMaxWidth()
                .background(c.surface, RoundedCornerShape(2.dp))
                .padding(8.dp),
        )
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    val c = LocalThemeColors.current
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(label, color = c.muted, fontSize = 11.sp)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = TextStyle(color = c.primary, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(c.cool),
            modifier = Modifier
                .width(64.dp)
                .background(c.surface, RoundedCornerShape(2.dp))
                .padding(8.dp),
        )
    }
}

/**
 * §3.6 — one horizontal bar with tick marks at the 20% floor and 30% target, plus a
 * single line of text. That is the entire feature.
 */
@Composable
private fun FatPaceSection(t: com.tatara.data.db.entity.TargetAdjustment, totals: MacroTotals) {
    val c = LocalThemeColors.current
    val remainingKcal = t.kcalTarget - totals.kcal
    val remainingFat = t.fatG - totals.fat
    val state = FatPace.state(remainingFat, remainingKcal)
    val fillColor = when (state) {
        FatPaceState.FAT_LIGHT -> c.cool
        FatPaceState.ON_PACE -> c.primary
        FatPaceState.FAT_LOADED -> c.warm.copy(alpha = 0.6f)
        FatPaceState.SPENT -> c.warm
        FatPaceState.OVER -> c.muted
    }
    val ceilingG = 0.35f * t.kcalTarget / 9f
    val fill = (totals.fat / ceilingG).coerceIn(0f, 1f)
    val floorTick = (0.20f * t.kcalTarget / 9f) / ceilingG
    val targetTick = (0.30f * t.kcalTarget / 9f) / ceilingG

    Column {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(4.dp).background(c.surface)) {
            Box(modifier = Modifier.fillMaxWidth(fill).fillMaxHeight().background(fillColor))
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * floorTick)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(c.hairline)
            )
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * targetTick)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(c.hairline)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        val line = buildString {
            append("${String.format(Locale.US, "%,d", remainingKcal.toInt())} kcal left, ")
            append("${fmt(remainingFat)}g fat")
            state.message?.let { append(" — $it") }
        }
        Text(line, color = if (state == FatPaceState.ON_PACE) c.muted else fillColor, fontSize = 12.sp)
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
