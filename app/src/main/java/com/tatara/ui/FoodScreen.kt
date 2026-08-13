package com.tatara.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.tatara.data.db.TataraDatabase
import com.tatara.data.db.dao.EntryWithFood
import com.tatara.data.db.entity.FatSource
import com.tatara.data.db.entity.Food
import com.tatara.data.db.entity.Settings
import com.tatara.data.db.entity.UnitType
import com.tatara.data.db.entity.WeightEntry
import com.tatara.data.food.FatPace
import com.tatara.data.food.FatPaceState
import com.tatara.data.food.FoodRepository
import com.tatara.data.food.LogResult
import com.tatara.data.food.MacroMath
import com.tatara.data.food.MacroTotals
import com.tatara.data.tdee.TdeeCalculator
import com.tatara.data.tdee.WeightPoint
import com.tatara.ui.theme.LocalThemeColors
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun FoodScreen(repo: FoodRepository, db: TataraDatabase) {
    val c = LocalThemeColors.current
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }

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

    var weightInput by remember { mutableStateOf("") }
    var weightRaw by remember { mutableStateOf(listOf<WeightPoint>()) }
    var weightEwma by remember { mutableStateOf(listOf<WeightPoint>()) }
    var formulaKcal by remember { mutableStateOf<Float?>(null) }
    var observedKcal by remember { mutableStateOf<Float?>(null) }
    var proteinTargetG by remember { mutableStateOf<Float?>(null) }
    var weekKcal by remember { mutableStateOf(listOf<Pair<LocalDate, Float>>()) }
    var weekProtein by remember { mutableStateOf(listOf<Pair<LocalDate, Float>>()) }
    var weekTargetKcal by remember { mutableStateOf<Float?>(null) }
    var weekTargetProtein by remember { mutableStateOf<Float?>(null) }
    var weeklySurplus by remember { mutableStateOf<Float?>(null) }

    suspend fun refresh() {
        entries = repo.entriesOn()
        totals = repo.totalsOn()
        targets = repo.latestTargets()
        quickAddFoods = repo.recentFoods()

        // §3.3/§3.6 — weight trend, formula-vs-observed targets, this week's charts.
        val settings = db.bodyDao().getSettings() ?: Settings()
        weightInput = db.bodyDao().weightOn(today)?.weightKg?.let(::trimF) ?: ""
        val recentWeights = db.bodyDao().weightsBetween(today.minusDays(29), today)
        weightRaw = recentWeights.map { WeightPoint(it.date, it.weightKg) }
        weightEwma = TdeeCalculator.ewmaSeries(weightRaw)
        val latestWeightKg = recentWeights.lastOrNull()?.weightKg
        formulaKcal = latestWeightKg?.let { TdeeCalculator.formulaMaintenance(settings, it, today) }
        val latestAdj = db.bodyDao().latestAdjustment()
        observedKcal = latestAdj?.impliedTdee
        proteinTargetG = latestAdj?.proteinG ?: latestWeightKg?.let { settings.proteinPerKg * it }
        weekTargetKcal = latestAdj?.kcalTarget
        weekTargetProtein = latestAdj?.proteinG

        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEntries = db.foodDao().entriesWithFoodBetween(weekStart, today)
        val kcalByDate = weekEntries.groupBy { it.entry.date }
            .mapValues { (_, es) -> es.fold(0f) { acc, e -> acc + MacroMath.macrosFor(e.food, e.entry.quantity).kcal } }
        val proteinByDate = weekEntries.groupBy { it.entry.date }
            .mapValues { (_, es) -> es.fold(0f) { acc, e -> acc + MacroMath.macrosFor(e.food, e.entry.quantity).protein } }
        val weekDates = (0..6).map { weekStart.plusDays(it.toLong()) }
        weekKcal = weekDates.map { d -> d to (kcalByDate[d] ?: 0f) }
        weekProtein = weekDates.map { d -> d to (proteinByDate[d] ?: 0f) }
        weeklySurplus = weekTargetKcal?.let { target ->
            weekKcal.filter { it.second > 0f }.fold(0f) { acc, (_, kcal) -> acc + (kcal - target) }
        }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 16.dp, top = 16.dp),
    ) {
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
            // A plain Column, not a LazyColumn: the page itself scrolls now that
            // weight and the weekly view sit below, and a day's log is short.
            Column(modifier = Modifier.padding(top = 8.dp)) {
                entries.forEach { EntryRow(it) }
            }
        }

        FoodSection("Weight")
        WeightQuickEntry(
            input = weightInput,
            onInputChange = { weightInput = it },
            onLog = {
                val kg = weightInput.toFloatOrNull()
                if (kg != null) {
                    scope.launch {
                        db.bodyDao().upsertWeight(
                            WeightEntry(date = today, weightKg = kg, loggedAt = Instant.now())
                        )
                        refresh()
                    }
                }
            },
        )
        if (weightRaw.size >= 2) {
            Spacer(modifier = Modifier.height(8.dp))
            WeightTrendChart(raw = weightRaw, ewma = weightEwma)
        }

        FoodSection("Targets")
        Text(
            formulaKcal?.let { "Formula: ${it.toInt()} kcal" }
                ?: "Formula: set height/birth year/sex/activity in Settings.",
            color = c.muted, fontSize = 12.sp,
            fontFamily = if (formulaKcal != null) FontFamily.Monospace else FontFamily.Default,
        )
        Text(
            observedKcal?.let { "Observed: ${it.toInt()} kcal, from your own logs" }
                ?: "Observed: not enough data yet — needs 14+ days, most weeks logged.",
            color = c.muted, fontSize = 12.sp,
            fontFamily = if (observedKcal != null) FontFamily.Monospace else FontFamily.Default,
            modifier = Modifier.padding(top = 2.dp),
        )
        proteinTargetG?.let {
            Text(
                "Protein: ${it.toInt()} g",
                color = c.muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        FoodSection("This week")
        Text("Calories", color = c.muted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        WeekBarChart(days = weekKcal, target = weekTargetKcal, today = today)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Protein", color = c.muted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        WeekBarChart(days = weekProtein, target = weekTargetProtein, today = today)
        weeklySurplus?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${abs(it).toInt()} kcal ${if (it >= 0) "ahead" else "behind"} for the week",
                color = c.muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun FoodSection(title: String) {
    val c = LocalThemeColors.current
    Spacer(modifier = Modifier.height(18.dp))
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        MotifGlyph(size = 9.dp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(title, color = c.muted, fontSize = 12.sp)
    }
    Spacer(modifier = Modifier.height(6.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(c.surfaceHigh, RoundedCornerShape(1.dp))
    )
    Spacer(modifier = Modifier.height(10.dp))
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

// ---------------------------------------------------------------- body & week
// §3.3/§3.6 — weight, targets, and the weekly view live on Food, not the
// Dashboard: they are nutrition instruments, and the Dashboard is the hero.

@Composable
private fun WeightQuickEntry(input: String, onInputChange: (String) -> Unit, onLog: () -> Unit) {
    val c = LocalThemeColors.current
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        BasicTextField(
            value = input,
            onValueChange = onInputChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = TextStyle(color = c.primary, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(c.cool),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .width(88.dp)
                        .background(c.surface, RoundedCornerShape(2.dp))
                        .padding(8.dp),
                ) {
                    if (input.isEmpty()) Text("kg today", color = c.muted, fontSize = 13.sp)
                    inner()
                }
            },
        )
        Text(
            "Log",
            color = c.cool,
            fontSize = 13.sp,
            modifier = Modifier.clickable(onClick = onLog).padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
        )
    }
}

/** §3.6 — raw daily points faint behind the EWMA line: the trend is what carries signal. */
@Composable
private fun WeightTrendChart(raw: List<WeightPoint>, ewma: List<WeightPoint>) {
    val c = LocalThemeColors.current
    val minKg = raw.minOf { it.kg }
    val maxKg = raw.maxOf { it.kg }
    val range = (maxKg - minKg).coerceAtLeast(0.5f)
    val start = raw.first().date
    val totalDays = ChronoUnit.DAYS.between(start, raw.last().date).toFloat().coerceAtLeast(1f)

    Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
        fun point(p: WeightPoint): Offset {
            val x = (ChronoUnit.DAYS.between(start, p.date) / totalDays) * size.width
            val y = size.height * (1f - (p.kg - minKg) / range)
            return Offset(x, y)
        }
        raw.forEach { drawCircle(color = c.muted.copy(alpha = 0.35f), radius = 2.dp.toPx(), center = point(it)) }
        val path = Path().apply {
            ewma.forEachIndexed { i, p ->
                val o = point(p)
                if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
            }
        }
        drawPath(path, color = c.cool, style = Stroke(width = 2.dp.toPx()))
    }
}

/** §3.6 — bars for the day's actual value, a reference line for the target. */
@Composable
private fun WeekBarChart(days: List<Pair<LocalDate, Float>>, target: Float?, today: LocalDate) {
    val c = LocalThemeColors.current
    val maxVal = maxOf(days.maxOfOrNull { it.second } ?: 0f, target ?: 0f, 1f)

    Canvas(modifier = Modifier.fillMaxWidth().height(64.dp)) {
        val barWidth = size.width / days.size
        days.forEachIndexed { i, (date, value) ->
            val barHeight = size.height * (value / maxVal).coerceIn(0f, 1f)
            val left = i * barWidth + barWidth * 0.28f
            drawRect(
                color = if (date == today) c.primary else c.cool,
                topLeft = Offset(left, size.height - barHeight),
                size = Size(barWidth * 0.44f, barHeight),
            )
        }
        target?.let {
            val y = size.height * (1f - (it / maxVal).coerceIn(0f, 1f))
            drawLine(color = c.warm, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEach { (date, _) ->
            Text(
                date.dayOfWeek.name.take(1),
                color = if (date == today) c.primary else c.muted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

private fun trimF(v: Float): String =
    if (v == v.toInt().toFloat()) v.toInt().toString() else v.toString()
