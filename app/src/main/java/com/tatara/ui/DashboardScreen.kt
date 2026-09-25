package com.tatara.ui

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import com.tatara.data.db.entity.TierCrossing
import com.tatara.data.db.entity.WeeklyReview
import com.tatara.data.food.MacroMath
import com.tatara.data.food.MacroTotals
import com.tatara.ui.hero.ProgressionHero
import com.tatara.ui.theme.AppTheme
import com.tatara.ui.theme.FoldPreferences
import com.tatara.ui.theme.LocalThemeColors
import com.tatara.ui.theme.tierName
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt
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
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    theme.tierName(band),
                    color = c.primary,
                    fontSize = 31.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Light,
                )
                Text(
                    "${theme.label.uppercase()}  ·  LEVEL $level",
                    color = c.cool,
                    fontSize = 11.sp,
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

        Spacer(modifier = Modifier.height(6.dp))
        ProgressionHero(
            motif = theme.motif,
            tier = band,
            dailyCompletion = fill,
        )

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            TierProgress(
                xp = xp,
                band = band,
                nextTierName = toNextTier?.let { theme.tierName(band + 1) },
                xpToNextTier = toNextTier,
            )

            DailyStatus(completion = fill)

            // §7.1 — the same 365-day history, now subordinate to today's state.
            Spacer(modifier = Modifier.height(12.dp))
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
                                            rollup?.ringClosed == true -> c.cool
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
                Spacer(modifier = Modifier.height(22.dp))
                Text(
                    "DAILY RESEARCH",
                    color = c.muted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(modifier = Modifier.height(7.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.surface.copy(alpha = 0.66f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                ) {
                    Text(card.text, color = c.primary.copy(alpha = 0.86f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(card.source, color = c.muted, fontSize = 9.sp)
                }
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
}

@Composable
private fun TierProgress(
    xp: Long,
    band: Int,
    nextTierName: String?,
    xpToNextTier: Long?,
) {
    val c = LocalThemeColors.current
    val startXp = if (band == 0) 0L else Levels.cumulative(Levels.bandStartLevel(band))
    val endXp = if (band >= 8) xp.coerceAtLeast(startXp + 1L)
    else Levels.cumulative(Levels.bandStartLevel(band + 1))
    val progress = if (band >= 8) 1f
    else ((xp - startXp).toFloat() / (endXp - startXp).toFloat()).coerceIn(0f, 1f)

    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                "$xp XP",
                color = c.primary,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f),
            )
            if (xpToNextTier != null && nextTierName != null) {
                Text(
                    "$xpToNextTier to $nextTierName",
                    color = c.muted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(c.hairline, RoundedCornerShape(2.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(4.dp)
                    .background(c.cool, RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Composable
private fun DailyStatus(completion: Float) {
    val c = LocalThemeColors.current
    val completed = (completion.coerceIn(0f, 1f) * 3f).roundToInt()
    Column(modifier = Modifier.padding(top = 22.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "TODAY",
                color = c.primary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f),
            )
            Text(
                "$completed of 3 complete",
                color = if (completed == 3) c.cool else c.muted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .background(
                            if (index < completed) c.cool else c.surfaceHigh,
                            RoundedCornerShape(3.dp),
                        ),
                )
            }
        }
    }
}

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
