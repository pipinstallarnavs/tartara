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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
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
            .padding(start = 24.dp, end = 16.dp, top = 16.dp),
    ) {
        // §6.3 — regularity is the large number; duration is secondary.
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                stats.regularity?.toInt()?.toString() ?: "—",
                color = c.primary,
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (2L downTo 0L).map { today.minusDays(it) }.forEach { date ->
                val label = if (date == today) "Today" else date.format(DateTimeFormatter.ofPattern("d MMM"))
                Text(
                    text = label,
                    color = if (date == selectedDate) c.primary else c.muted,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .background(if (date == selectedDate) c.surface else c.ground, RoundedCornerShape(2.dp))
                        .clickable { selectedDate = date }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }

        SleepSection("Log")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeField("bed", bed) { bed = it }
            TimeField("wake", wake) { wake = it }
            TimeField("to sleep, min", ttfa) { ttfa = it }
            TimeField("wakes", wakeCount) { wakeCount = it }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Quality", color = c.muted, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(12.dp))
            (1..5).forEach { q ->
                Text(
                    "$q",
                    color = if (q == quality) c.ground else c.muted,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .background(if (q == quality) c.cool else c.surface, RoundedCornerShape(2.dp))
                        .clickable { quality = q }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "Save",
                color = c.cool,
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable {
                        scope.launch {
                            val b = parseTime(bed)
                            val w = parseTime(wake)
                            if (b == null || w == null || quality == 0) {
                                notice = "Bed, wake, and quality are required."
                            } else {
                                repo.logSleep(
                                    selectedDate, b, w,
                                    ttfa.toIntOrNull() ?: 0,
                                    wakeCount.toIntOrNull() ?: 0,
                                    quality,
                                )
                                refresh()
                            }
                        }
                    }
                    .padding(8.dp),
            )
        }
        notice?.let { Text(it, color = c.warm, fontSize = 12.sp) }

        target?.let { t ->
            SleepSection("Screen curfew")
            val start = repo.curfewStartFor(t)
            Text("Curfew starts $start.", color = c.muted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CurfewChip("Held", selected = curfewHeld == true, selectedColor = c.cool) {
                    scope.launch { repo.logCurfew(selectedDate, held = true); refresh() }
                }
                CurfewChip("Broke it", selected = curfewHeld == false, selectedColor = c.warm) {
                    scope.launch { repo.logCurfew(selectedDate, held = false); refresh() }
                }
            }
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

        SleepSection("Checklist")
        listOf("Caffeine cutoff", "Room dark", "Room cool", "No late meal").forEachIndexed { i, label ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val next = checklist.copyOf().also { it[i] = !it[i] }
                        checklist = next
                        scope.launch {
                            repo.logChecklist(selectedDate, next[0], next[1], next[2], next[3])
                            correlations = repo.correlations()
                        }
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(if (checklist[i]) c.cool else c.surface, RoundedCornerShape(2.dp)),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(label, color = c.primary, fontSize = 14.sp)
            }
        }

        if (correlations.isNotEmpty()) {
            SleepSection("Cold maths, trailing 30 days")
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
            SleepSection("Target")
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
private fun SleepSection(title: String) {
    val c = LocalThemeColors.current
    Spacer(modifier = Modifier.height(20.dp))
    Text(title, color = c.muted, fontSize = 12.sp)
    Spacer(modifier = Modifier.height(6.dp))
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun TimeField(label: String, value: String, onChange: (String) -> Unit) {
    val c = LocalThemeColors.current
    Column {
        Text(label, color = c.muted, fontSize = 10.sp)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = c.primary, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(c.cool),
            modifier = Modifier
                .width(74.dp)
                .background(c.surface, RoundedCornerShape(2.dp))
                .padding(8.dp),
        )
    }
}

@Composable
private fun CurfewChip(label: String, selected: Boolean, selectedColor: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    val c = LocalThemeColors.current
    Text(
        label,
        color = if (selected) c.ground else c.primary,
        fontSize = 13.sp,
        modifier = Modifier
            .background(if (selected) selectedColor else c.surface, RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun TargetEditor(target: SleepTarget, onSave: (LocalTime, LocalTime, Int) -> Unit) {
    val c = LocalThemeColors.current
    var bed by remember(target.id) { mutableStateOf(target.targetBedTime.toString()) }
    var wake by remember(target.id) { mutableStateOf(target.targetWakeTime.toString()) }
    var curfew by remember(target.id) { mutableStateOf(target.curfewMinutes.toString()) }
    var error by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        TimeField("bed", bed) { bed = it }
        TimeField("wake", wake) { wake = it }
        TimeField("curfew, min", curfew) { curfew = it }
        Text(
            "Save",
            color = c.cool,
            fontSize = 14.sp,
            modifier = Modifier
                .clickable {
                    val b = parseTime(bed)
                    val w = parseTime(wake)
                    val m = curfew.toIntOrNull()
                    if (b != null && w != null && m != null) {
                        error = false
                        onSave(b, w, m)
                    } else error = true
                }
                .padding(8.dp),
        )
    }
    if (error) Text("Times as HH:mm.", color = c.warm, fontSize = 12.sp)
}

private fun parseTime(text: String): LocalTime? = try {
    LocalTime.parse(if (text.length == 4 && text[1] == ':') "0$text" else text)
} catch (_: Exception) {
    null
}

private fun formatDuration(minutes: Int): String =
    "${minutes / 60}h ${(minutes % 60).toString().padStart(2, '0')}m"
