package com.tatara.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tatara.data.db.entity.CurfewLog
import com.tatara.data.db.entity.SleepTarget
import com.tatara.data.sleep.SleepMath
import com.tatara.data.sleep.SleepRepository
import com.tatara.data.sleep.SleepStats
import com.tatara.ui.theme.LocalThemeColors
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private val SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM")
private val SLEEP_QUALITY_OPTIONS = (1..5).toList()
private val CURFEW_OPTIONS = listOf(true, false)
private val ENVIRONMENT_ITEMS =
    listOf("Caffeine cutoff", "Room dark", "Room cool", "No late meal")

@Composable
fun SleepScreen(repo: SleepRepository) {
    val c = LocalThemeColors.current
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }

    var selectedDate by remember { mutableStateOf(today) }
    var stats by remember { mutableStateOf(SleepStats(null, null)) }
    var target by remember { mutableStateOf<SleepTarget?>(null) }
    var strip by remember { mutableStateOf(listOf<CurfewLog?>()) }
    var heldCount by remember { mutableStateOf(0) }
    var correlations by remember { mutableStateOf(listOf<SleepMath.Comparison>()) }

    var bed by remember { mutableStateOf("") }
    var wake by remember { mutableStateOf("") }
    var ttfa by remember { mutableStateOf("") }
    var wakeCount by remember { mutableStateOf("") }
    var quality by remember { mutableStateOf(0) }
    var curfewHeld by remember { mutableStateOf<Boolean?>(null) }
    var checklist by remember { mutableStateOf(BooleanArray(4)) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        stats = repo.stats()
        target = repo.targetOn(selectedDate)
        strip = repo.curfewStrip()
        heldCount = repo.heldOfLast()
        correlations = repo.correlations()
        val log = repo.logOn(selectedDate)
        bed = log?.bedTime?.toString() ?: ""
        wake = log?.wakeTime?.toString() ?: ""
        ttfa = log?.timeToFallAsleepMin?.toString() ?: ""
        wakeCount = log?.wakeCount?.toString() ?: ""
        quality = log?.quality ?: 0
        curfewHeld = repo.curfewOn(selectedDate)?.held
        val cl = repo.checklistOn(selectedDate)
        checklist = booleanArrayOf(
            cl?.caffeineCutoffMet ?: false,
            cl?.roomDark ?: false,
            cl?.roomCool ?: false,
            cl?.noLateLargeMeal ?: false,
        )
        notice = null
    }

    LaunchedEffect(selectedDate) { refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TataraSpacing.ScreenHorizontal, vertical = TataraSpacing.Standard),
    ) {
        // §6.3 — regularity is the large number; duration is secondary.
        Row(verticalAlignment = Alignment.Bottom) {
            // With no nights logged the dash is a placeholder, not a reading —
            // render it dim so it never looks like a value or a broken glyph.
            Text(
                stats.regularity?.toInt()?.toString() ?: "—",
                color = if (stats.regularity != null) c.primary else c.hairline,
                fontSize = 44.sp,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.padding(bottom = 6.dp)) {
                Text("regularity", color = c.muted, fontSize = 11.sp)
                Text(
                    stats.meanDurationMin?.let { formatDuration(it.toInt()) } ?: "no nights logged",
                    color = c.muted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        val editDates = remember(today) { (2L downTo 0L).map { today.minusDays(it) } }
        TataraSegmentedControl(
            items = editDates,
            selected = selectedDate,
            onSelected = { selectedDate = it },
            label = "Log date",
            itemLabel = { date ->
                if (date == today) "Today" else date.format(SHORT_DATE_FORMAT)
            },
        )

        TataraSectionHeader("Sleep log")
        Row(horizontalArrangement = Arrangement.spacedBy(TataraSpacing.Tight)) {
            TataraTimeField(
                bed,
                { bed = it; notice = null },
                "Bed time",
                Modifier.weight(1f),
                error = if (notice != null && parseTime(bed) == null) "Use HH:mm." else null,
            )
            TataraTimeField(
                wake,
                { wake = it; notice = null },
                "Wake time",
                Modifier.weight(1f),
                placeholder = "07:00",
                error = if (notice != null && parseTime(wake) == null) "Use HH:mm." else null,
            )
        }
        Spacer(modifier = Modifier.height(TataraSpacing.Control))
        Row(horizontalArrangement = Arrangement.spacedBy(TataraSpacing.Tight)) {
            TataraNumberField(ttfa, { ttfa = it }, "Sleep latency", Modifier.weight(1f), suffix = "min")
            TataraNumberField(wakeCount, { wakeCount = it }, "Wake count", Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(TataraSpacing.Control))
        TataraSegmentedControl(
            items = SLEEP_QUALITY_OPTIONS,
            selected = quality.takeIf { it > 0 },
            onSelected = { quality = it; notice = null },
            label = "Sleep quality",
            itemLabel = { it.toString() },
        )
        Spacer(modifier = Modifier.height(TataraSpacing.Control))
        TataraPrimaryButton(
            label = "Save night",
            onClick = {
                scope.launch {
                    val b = parseTime(bed)
                    val w = parseTime(wake)
                    if (b == null || w == null || quality == 0) {
                        notice = "Bed, wake, and quality are required."
                    } else {
                        repo.logSleep(
                            selectedDate,
                            b,
                            w,
                            ttfa.toIntOrNull() ?: 0,
                            wakeCount.toIntOrNull() ?: 0,
                            quality,
                        )
                        refresh()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        notice?.let { Text(it, color = c.warm, fontSize = 12.sp) }

        target?.let { t ->
            TataraSectionHeader("Screen curfew")
            val start = repo.curfewStartFor(t)
            Text("Curfew starts $start.", color = c.muted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            TataraSegmentedControl(
                items = CURFEW_OPTIONS,
                selected = curfewHeld,
                onSelected = { held ->
                    scope.launch { repo.logCurfew(selectedDate, held = held); refresh() }
                },
                label = "Curfew result",
                itemLabel = { if (it) "Held" else "Broke it" },
                selectedColor = if (curfewHeld == false) c.warm else c.cool,
            )
            Spacer(modifier = Modifier.height(10.dp))
            // §6.4 — the 28-night strip: cool held, warm broken, surface unlogged.
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                strip.forEach { night ->
                    Box(
                        modifier = Modifier
                            .size(width = 8.dp, height = 14.dp)
                            .background(
                                when (night?.held) {
                                    true -> c.cool
                                    false -> c.warm
                                    null -> c.surface
                                },
                                RoundedCornerShape(1.dp),
                            )
                    )
                }
            }
            Text(
                "$heldCount/28 held",
                color = c.muted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        TataraSectionHeader("Environment")
        ENVIRONMENT_ITEMS.forEachIndexed { i, label ->
            TataraCheckbox(
                checked = checklist[i],
                onCheckedChange = { checked ->
                    val next = checklist.copyOf().also { it[i] = checked }
                    checklist = next
                    scope.launch {
                        repo.logChecklist(selectedDate, next[0], next[1], next[2], next[3])
                        correlations = repo.correlations()
                    }
                },
                label = label,
            )
        }

        if (correlations.isNotEmpty()) {
            TataraSectionHeader("Analytics")
            correlations.forEach { comp ->
                Text(
                    "${comp.label}: %.1f (n=%d) vs %.1f (n=%d)".format(
                        comp.yesMean, comp.yesN, comp.noMean, comp.noN,
                    ),
                    color = c.muted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }

        target?.let { t ->
            TataraSectionHeader("Target")
            TargetEditor(t, onSave = { b, w, m ->
                scope.launch {
                    repo.editTarget(b, w, m)
                    refresh()
                }
            })
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun TargetEditor(target: SleepTarget, onSave: (LocalTime, LocalTime, Int) -> Unit) {
    var bed by remember(target.id) { mutableStateOf(target.targetBedTime.toString()) }
    var wake by remember(target.id) { mutableStateOf(target.targetWakeTime.toString()) }
    var curfew by remember(target.id) { mutableStateOf(target.curfewMinutes.toString()) }
    var error by remember { mutableStateOf(false) }

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(TataraSpacing.Tight)) {
            TataraTimeField(
                value = bed,
                onValueChange = { bed = it; error = false },
                label = "Bed time",
                modifier = Modifier.weight(1f),
                error = if (error && parseTime(bed) == null) "Use HH:mm." else null,
            )
            TataraTimeField(
                value = wake,
                onValueChange = { wake = it; error = false },
                label = "Wake time",
                modifier = Modifier.weight(1f),
                placeholder = "07:00",
                error = if (error && parseTime(wake) == null) "Use HH:mm." else null,
            )
        }
        Spacer(modifier = Modifier.height(TataraSpacing.Control))
        TataraNumberField(
            value = curfew,
            onValueChange = { curfew = it; error = false },
            label = "Screen curfew",
            suffix = "min",
            error = if (error && curfew.toIntOrNull() == null) "Enter minutes." else null,
        )
        Spacer(modifier = Modifier.height(TataraSpacing.Control))
        TataraPrimaryButton(
            label = "Save target",
            onClick = {
                val b = parseTime(bed)
                val w = parseTime(wake)
                val m = curfew.toIntOrNull()
                if (b != null && w != null && m != null) {
                    error = false
                    onSave(b, w, m)
                } else {
                    error = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun parseTime(text: String): LocalTime? = try {
    LocalTime.parse(if (text.length == 4 && text[1] == ':') "0$text" else text)
} catch (_: Exception) {
    null
}

private fun formatDuration(minutes: Int): String =
    "${minutes / 60}h ${(minutes % 60).toString().padStart(2, '0')}m"
