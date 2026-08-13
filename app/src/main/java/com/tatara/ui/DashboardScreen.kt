package com.tatara.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tatara.data.dashboard.InsightCard
import com.tatara.data.dashboard.InsightCards
import com.tatara.data.dashboard.Levels
import com.tatara.data.dashboard.ReviewContent
import com.tatara.data.dashboard.ReviewService
import com.tatara.data.db.TataraDatabase
import com.tatara.data.db.entity.DailyRollup
import com.tatara.data.db.entity.HabitList
import com.tatara.data.db.entity.HabitLogStatus
import com.tatara.data.db.entity.Settings
import com.tatara.data.db.entity.TierCrossing
import com.tatara.data.db.entity.WeeklyReview
import com.tatara.data.db.entity.WeightEntry
import com.tatara.data.food.MacroMath
import com.tatara.data.food.MacroTotals
import com.tatara.data.tdee.TdeeCalculator
import com.tatara.data.tdee.WeightPoint
import com.tatara.ui.theme.AppTheme
import com.tatara.ui.theme.FoldPreferences
import com.tatara.ui.theme.LocalMotif
import com.tatara.ui.theme.LocalThemeColors
import com.tatara.ui.theme.tierName
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    db: TataraDatabase,
    theme: AppTheme,
    onOpenSettings: () -> Unit,
    onTierCrossed: (String) -> Unit = {},
) {
    val c = LocalThemeColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    val reviewService = remember { ReviewService(db) }
    val foldPrefs = remember { FoldPreferences(context) }

    var level by remember { mutableStateOf(1) }
    var band by remember { mutableStateOf(0) }
    var xp by remember { mutableStateOf(0L) }
    var toNextTier by remember { mutableStateOf<Long?>(null) }
    var nearFloor by remember { mutableStateOf(false) }
    var fill by remember { mutableStateOf(0f) }
    var rollups by remember { mutableStateOf(mapOf<LocalDate, DailyRollup>()) }
    var insight by remember { mutableStateOf<InsightCard?>(null) }
    var reviews by remember { mutableStateOf(listOf<WeeklyReview>()) }
    var openContent by remember { mutableStateOf<ReviewContent?>(null) }
    var crossings by remember { mutableStateOf(listOf<TierCrossing>()) }

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
        xp = db.dashboardDao().totalXp()
        crossings = db.dashboardDao().getAllTierCrossings()
        // §7.3 — a crossing this device has not celebrated yet earns the fold. The
        // prefs claim is one-shot, so it can never replay on a later refresh.
        crossings.maxByOrNull { it.id }?.let { newest ->
            if (foldPrefs.claimUnseen(newest.id)) {
                onTierCrossed(theme.tierName(Levels.bandFor(newest.level)))
            }
        }
        val maxBand = crossings.maxOfOrNull { Levels.bandFor(it.level) } ?: -1
        level = Levels.effectiveLevel(xp, maxBand)
        band = Levels.bandFor(level)
        toNextTier = Levels.xpToNextTier(xp, band)
        nearFloor = Levels.nearFloor(level, maxBand)
        rollups = db.dashboardDao().getAllRollups().associateBy { it.date }
        insight = InsightCards.cardFor(context, today)
        reviews = db.dashboardDao().reviewsNewestFirst().take(8)

        // §7.1 — today's fill: one third per requirement met.
        val totals = db.foodDao().entriesWithFoodOn(today)
            .fold(MacroTotals()) { acc, e -> acc + MacroMath.macrosFor(e.food, e.entry.quantity) }
        val target = db.bodyDao().adjustmentOn(today)?.kcalTarget
        val kcalOk = target != null && abs(totals.kcal - target) <= 0.10f * target
        val habitLogs = db.habitDao().logsOn(today).associateBy { it.habitId }
        val habitItems = db.habitDao().getAllHabits().filter { it.list == HabitList.HABIT }
        val habitsOk = habitItems.isNotEmpty() && habitItems.all {
            habitLogs[it.id]?.status == HabitLogStatus.COMPLETED ||
                habitLogs[it.id]?.status == HabitLogStatus.FROZEN
        }
        val sleepOk = db.sleepDao().logOn(today) != null
        fill = listOf(kcalOk, habitsOk, sleepOk).count { it } / 3f

        // §3.3/§3.6 — weight trend, formula-vs-observed targets, this week's charts.
        val settings = db.bodyDao().getSettings() ?: Settings()
        val todayWeight = db.bodyDao().weightOn(today)
        weightInput = todayWeight?.weightKg?.let(::trimF) ?: ""
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 16.dp, top = 16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    theme.tierName(band),
                    color = c.primary,
                    fontSize = 26.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Light,
                )
                Text(
                    "Level $level",
                    color = c.muted,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                )
                if (nearFloor) {
                    // §7.3 — show the cliff before it arrives.
                    Text("Two levels above the floor.", color = c.warm, fontSize = 11.sp)
                }
            }
            Text(
                "Settings",
                color = c.muted,
                fontSize = 14.sp,
                modifier = Modifier.clickable(onClick = onOpenSettings).padding(4.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Hero(motif = LocalMotif.current, band = band, heat = fill)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            val next = toNextTier
            Text(
                buildString {
                    append("$xp XP")
                    if (next != null) append(" · $next to ${theme.tierName(band + 1)}")
                },
                color = c.muted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
        }

        // §7.1 — 365-day history, ji → ha.
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val start = today.minusDays(364)
            (0 until 53).forEach { week ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    (0 until 7).forEach { d ->
                        val date = start.plusDays((week * 7 + d).toLong())
                        val rollup = if (date.isAfter(today)) null else rollups[date]
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(
                                    when {
                                        rollup?.ringClosed == true -> c.primary
                                        rollup != null && rollup.kcal > 0f -> c.hairline
                                        else -> c.surface
                                    },
                                    RoundedCornerShape(1.dp),
                                )
                        )
                    }
                }
            }
        }

        insight?.let { card ->
            // §7.4 — one factual card per day, source line always shown.
            Spacer(modifier = Modifier.height(20.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.surface, RoundedCornerShape(2.dp))
                    .padding(12.dp),
            ) {
                Text(card.text, color = c.primary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(card.source, color = c.muted, fontSize = 10.sp)
            }
        }

        DashSection("Weight")
        WeightQuickEntry(
            input = weightInput,
            onInputChange = { weightInput = it },
            onLog = {
                val kg = weightInput.toFloatOrNull()
                if (kg != null) {
                    scope.launch {
                        db.bodyDao().upsertWeight(WeightEntry(date = today, weightKg = kg, loggedAt = Instant.now()))
                        refresh()
                    }
                }
            },
        )
        if (weightRaw.size >= 2) {
            Spacer(modifier = Modifier.height(8.dp))
            WeightTrendChart(raw = weightRaw, ewma = weightEwma)
        }

        DashSection("Targets")
        Text(
            formulaKcal?.let { "Formula: ${it.toInt()} kcal" }
                ?: "Formula: set height/birth year/sex/activity in Settings.",
            color = c.muted, fontSize = 12.sp, fontFamily = if (formulaKcal != null) FontFamily.Monospace else FontFamily.Default,
        )
        Text(
            observedKcal?.let { "Observed: ${it.toInt()} kcal, from your own logs" }
                ?: "Observed: not enough data yet — needs 14+ days, most weeks logged.",
            color = c.muted, fontSize = 12.sp, fontFamily = if (observedKcal != null) FontFamily.Monospace else FontFamily.Default,
            modifier = Modifier.padding(top = 2.dp),
        )
        proteinTargetG?.let {
            Text(
                "Protein: ${it.toInt()} g",
                color = c.muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        DashSection("This week")
        Text("Calories", color = c.muted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        WeekBarChart(days = weekKcal, target = weekTargetKcal, today = today)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Protein", color = c.muted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        WeekBarChart(days = weekProtein, target = weekTargetProtein, today = today)
        weeklySurplus?.let {
            Spacer(modifier = Modifier.height(8.dp))
            val ahead = it >= 0
            Text(
                "${abs(it).toInt()} kcal ${if (ahead) "ahead" else "behind"} for the week",
                color = c.muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
            )
        }

        if (reviews.isNotEmpty()) {
            DashSection("Weekly reviews")
            reviews.forEach { review ->
                val label = review.weekStart.format(DateTimeFormatter.ofPattern("d MMM"))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch {
                                reviewService.openReview(review.id, today)
                                openContent =
                                    if (openContent?.weekStart == review.weekStart) null
                                    else reviewService.contentFor(review.weekStart)
                                refresh()
                            }
                        }
                        .padding(vertical = 8.dp),
                ) {
                    Text("Week of $label", color = c.primary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    if (review.openedAt == null) {
                        Text("Review ready.", color = c.cool, fontSize = 12.sp)
                    }
                }
                if (openContent?.weekStart == review.weekStart) {
                    ReviewDetail(openContent!!)
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
            }
        }

        if (crossings.isNotEmpty()) {
            // §7.3 — the page that only ever grows.
            DashSection("Tiers")
            crossings.sortedBy { it.date }.forEach { crossing ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        crossing.date.format(DateTimeFormatter.ofPattern("d MMM uuuu")),
                        color = c.muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "${theme.tierName(Levels.bandFor(crossing.level))}. Level ${crossing.level} is now your floor.",
                        color = c.primary, fontSize = 12.sp,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun WeightQuickEntry(input: String, onInputChange: (String) -> Unit, onLog: () -> Unit) {
    val c = LocalThemeColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
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

@Composable
private fun DashSection(title: String) {
    val c = LocalThemeColors.current
    Spacer(modifier = Modifier.height(20.dp))
    Text(title, color = c.muted, fontSize = 12.sp)
    Spacer(modifier = Modifier.height(6.dp))
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
}

@Composable
private fun ReviewDetail(content: ReviewContent) {
    val c = LocalThemeColors.current
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        val lines = buildList {
            if (content.ewmaStart != null && content.ewmaEnd != null) {
                add(
                    "Weight %.1f → %.1f kg (Δ %+.2f)".format(
                        content.ewmaStart, content.ewmaEnd, content.ewmaEnd - content.ewmaStart,
                    )
                )
            }
            add("Logged ${content.loggedDays}/7 days" + (content.meanKcal?.let { ", mean ${it.toInt()} kcal" } ?: ""))
            content.proteinHitRate?.let { add("Protein hit ${(it * 100).toInt()}% of days") }
            // §3.5 — show the arithmetic, no black boxes.
            content.adjustment?.let { adj ->
                add(
                    "New targets: ${adj.kcalTarget.toInt()} kcal · P ${adj.proteinG.toInt()} · " +
                        "F ${adj.fatG.toInt()} · C ${adj.carbsG.toInt()}"
                )
                if (adj.meanDailyKcal != null && adj.impliedTdee != null) {
                    add(
                        "mean ${adj.meanDailyKcal!!.toInt()} kcal, ewma ${"%.1f".format(adj.ewmaStart)}→" +
                            "${"%.1f".format(adj.ewmaEnd)}, TDEE ${adj.impliedTdee!!.toInt()}, " +
                            "rate ${adj.weeklyRatePercent}%/wk from ${"%.1f".format(adj.computedFromWeightKg)} kg"
                    )
                }
            }
            content.meanSleepMin?.let {
                add(
                    "Sleep mean ${it.toInt() / 60}h ${it.toInt() % 60}m" +
                        (content.regularity?.let { r -> ", regularity ${r.toInt()}" } ?: "")
                )
            }
            add("Training: ${content.sessionCount} sessions" + content.setsByGroup.entries.takeIf { it.isNotEmpty() }
                ?.joinToString(prefix = " — ", separator = ", ") { "${it.key} ${it.value}" }.orEmpty())
            add("XP earned: ${content.xpEarned}")
        }
        lines.forEach {
            Text(it, color = c.muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 1.dp))
        }
        content.habits.forEach { (name, automaticity, stage) ->
            Text(
                "$name ${automaticity.toInt()}% · $stage",
                color = c.muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
