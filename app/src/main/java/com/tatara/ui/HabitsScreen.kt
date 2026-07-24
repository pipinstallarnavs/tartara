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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.tatara.data.db.entity.Habit
import com.tatara.data.db.entity.HabitList
import com.tatara.data.db.entity.HabitLog
import com.tatara.data.db.entity.HabitLogStatus
import com.tatara.data.db.entity.Stack
import com.tatara.data.habit.Automaticity
import com.tatara.data.habit.HabitMetrics
import com.tatara.data.habit.HabitRepository
import com.tatara.ui.theme.LocalThemeColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@Composable
fun HabitsScreen(repo: HabitRepository) {
    val c = LocalThemeColors.current
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }

    var selectedDate by remember { mutableStateOf(today) }
    var segment by remember { mutableStateOf(HabitList.HABIT) }
    var habits by remember { mutableStateOf(listOf<Habit>()) }
    var stacks by remember { mutableStateOf(listOf<Stack>()) }
    var logs by remember { mutableStateOf(mapOf<Long, HabitLog>()) }
    var allLogs by remember { mutableStateOf(mapOf<Long, List<HabitLog>>()) }
    var tokensLeft by remember { mutableStateOf(0) }

    suspend fun refresh() {
        habits = repo.habits()
        stacks = repo.stacks()
        logs = repo.logsOn(selectedDate)
        allLogs = repo.logsByHabit()
        tokensLeft = repo.tokensLeft(YearMonth.from(selectedDate))
    }

    LaunchedEffect(selectedDate) { refresh() }

    Column(modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 16.dp, top = 16.dp)) {
        // §2.1 — the edit window is today, yesterday, D-2. Nothing older.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (2L downTo 0L).map { today.minusDays(it) }.forEach { date ->
                val label = if (date == today) "Today"
                else date.format(DateTimeFormatter.ofPattern("d MMM"))
                Text(
                    text = label,
                    color = if (date == selectedDate) c.primary else c.muted,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .background(
                            if (date == selectedDate) c.surface else c.ground,
                            RoundedCornerShape(2.dp),
                        )
                        .clickable { selectedDate = date }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(HabitList.HABIT to "Habits", HabitList.HYGIENE to "Hygiene").forEach { (list, label) ->
                Text(
                    text = label,
                    color = if (segment == list) c.primary else c.muted,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { segment = list }.padding(vertical = 4.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (segment == HabitList.HABIT) {
                Text(
                    "$tokensLeft freeze left",
                    color = c.muted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(c.hairline))

        val visible = habits.filter { it.list == segment }
        val byStack = visible.groupBy { it.stackId }
        val stackOrder = stacks.sortedBy { it.sortOrder }

        LazyColumn(modifier = Modifier.weight(1f)) {
            stackOrder.forEach { stack ->
                val members = byStack[stack.id] ?: return@forEach
                item(key = "stack-${stack.id}-$segment") {
                    Text(
                        stack.name,
                        color = c.muted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                    )
                }
                members.forEach { habit ->
                    item(key = "habit-${habit.id}") {
                        HabitRow(
                            habit = habit,
                            log = logs[habit.id],
                            history = allLogs[habit.id].orEmpty(),
                            today = today,
                            canFreeze = tokensLeft > 0 && selectedDate != today,
                            onToggle = {
                                scope.launch {
                                    repo.toggle(habit.id, selectedDate)
                                    refresh()
                                }
                            },
                            onFreeze = {
                                scope.launch {
                                    if (logs[habit.id]?.status == HabitLogStatus.FROZEN) {
                                        repo.unfreeze(habit.id, selectedDate)
                                    } else {
                                        repo.freeze(habit.id, selectedDate)
                                    }
                                    refresh()
                                }
                            },
                        )
                    }
                }
            }
        }

        // §5.5 — permanent footnote.
        Text(
            "Median 59–66 days, range 4–335. Variation is normal.",
            color = c.muted,
            fontSize = 10.sp,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun HabitRow(
    habit: Habit,
    log: HabitLog?,
    history: List<HabitLog>,
    today: LocalDate,
    canFreeze: Boolean,
    onToggle: () -> Unit,
    onFreeze: () -> Unit,
) {
    val c = LocalThemeColors.current
    val completed = log?.status == HabitLogStatus.COMPLETED
    val frozen = log?.status == HabitLogStatus.FROZEN
    val consistency = HabitMetrics.consistency(
        history,
        createdOn = habit.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
        today = today,
    )
    val streak = HabitMetrics.currentStreak(history, today)

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // §2.5 — completed is cool (earned); pending is surface.
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(
                    when {
                        completed -> c.cool
                        frozen -> c.hairline
                        else -> c.surface
                    },
                    RoundedCornerShape(2.dp),
                )
                .clickable(onClick = onToggle),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(habit.name, color = c.primary, fontSize = 14.sp)
            val detail = buildString {
                append("${habit.automaticity.toInt()}%")
                append(" · ${Automaticity.stage(habit.automaticity).label}")
                consistency?.let { append(" · 28d ${it.toInt()}%") }
                if (streak > 0) append(" · $streak")
            }
            Text(detail, color = c.muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            if (habit.consecutiveMisses >= 2) {
                Text(
                    "Missed. ${ordinal(habit.consecutiveMisses)} in a row.",
                    color = c.warm,
                    fontSize = 11.sp,
                )
            }
        }
        if (frozen) {
            Text(
                "Frozen",
                color = c.cool,
                fontSize = 12.sp,
                modifier = Modifier.clickable(onClick = onFreeze).padding(8.dp),
            )
        } else if (!completed && canFreeze) {
            Text(
                "Freeze",
                color = c.muted,
                fontSize = 12.sp,
                modifier = Modifier.clickable(onClick = onFreeze).padding(8.dp),
            )
        }
    }
}

private fun ordinal(n: Int): String = when (n) {
    2 -> "Second"
    3 -> "Third"
    else -> "${n}th"
}
