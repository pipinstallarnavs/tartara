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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
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
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private val SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM")
private val HABIT_LIST_OPTIONS = listOf(HabitList.HABIT, HabitList.HYGIENE)

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
    var showAddHabit by remember { mutableStateOf(false) }
    var newHabitName by remember { mutableStateOf("") }
    var newHabitStackId by remember { mutableStateOf<Long?>(null) }
    var addHabitError by remember { mutableStateOf<String?>(null) }
    var editingHabitId by remember { mutableStateOf<Long?>(null) }
    var editName by remember { mutableStateOf("") }
    var editStackId by remember { mutableStateOf<Long?>(null) }
    var editError by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        habits = repo.habits()
        stacks = repo.stacks()
        logs = repo.logsOn(selectedDate)
        allLogs = repo.logsByHabit()
        tokensLeft = repo.tokensLeft(YearMonth.from(selectedDate))
    }

    LaunchedEffect(selectedDate) { refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = TataraSpacing.ScreenHorizontal, vertical = TataraSpacing.Standard)
    ) {
        // §2.1 — the edit window is today, yesterday, D-2. Nothing older.
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
        Spacer(modifier = Modifier.height(TataraSpacing.Control))
        TataraSegmentedControl(
            items = HABIT_LIST_OPTIONS,
            selected = segment,
            onSelected = { segment = it },
            label = "List",
            itemLabel = { if (it == HabitList.HABIT) "Habits" else "Hygiene" },
        )
        if (segment == HabitList.HABIT) {
            Text(
                "$tokensLeft freeze left",
                color = c.muted,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = TataraSpacing.Tight),
            )
        }
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
                            onEdit = {
                                if (editingHabitId == habit.id) {
                                    editingHabitId = null
                                } else {
                                    editingHabitId = habit.id
                                    editName = habit.name
                                    editStackId = habit.stackId
                                    editError = null
                                }
                            },
                        )
                    }
                    if (editingHabitId == habit.id) {
                        item(key = "edit-${habit.id}") {
                            EditHabitForm(
                                name = editName,
                                onNameChange = { editName = it; editError = null },
                                stacks = stacks,
                                selectedStackId = editStackId,
                                onSelectStack = { editStackId = it },
                                error = editError,
                                onCancel = { editingHabitId = null },
                                onDelete = {
                                    scope.launch {
                                        repo.deleteHabit(habit.id)
                                        editingHabitId = null
                                        refresh()
                                    }
                                },
                                onSave = {
                                    val name = editName.trim()
                                    if (name.isNotEmpty()) {
                                        scope.launch {
                                            val ok = repo.updateHabit(
                                                habit.copy(name = name, stackId = editStackId)
                                            )
                                            if (!ok) {
                                                editError = "Habits list is capped at ${HabitRepository.HABIT_CAP}."
                                            } else {
                                                editingHabitId = null
                                                refresh()
                                            }
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        if (showAddHabit) {
            AddHabitForm(
                name = newHabitName,
                onNameChange = { newHabitName = it; addHabitError = null },
                stacks = stacks,
                selectedStackId = newHabitStackId,
                onSelectStack = { newHabitStackId = it },
                error = addHabitError,
                onCancel = {
                    showAddHabit = false
                    newHabitName = ""
                    newHabitStackId = null
                    addHabitError = null
                },
                onSave = {
                    val name = newHabitName.trim()
                    if (name.isNotEmpty()) {
                        scope.launch {
                            val created = repo.createHabit(
                                Habit(
                                    name = name,
                                    list = segment,
                                    stackId = newHabitStackId,
                                    createdAt = Instant.now(),
                                )
                            )
                            if (created == null) {
                                addHabitError = "Habits list is capped at ${HabitRepository.HABIT_CAP}."
                            } else {
                                showAddHabit = false
                                newHabitName = ""
                                newHabitStackId = null
                                addHabitError = null
                                refresh()
                            }
                        }
                    }
                },
            )
        } else {
            TataraGhostAction(
                label = "+ Add habit",
                onClick = { showAddHabit = true },
                modifier = Modifier.fillMaxWidth(),
            )
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
private fun AddHabitForm(
    name: String,
    onNameChange: (String) -> Unit,
    stacks: List<Stack>,
    selectedStackId: Long?,
    onSelectStack: (Long?) -> Unit,
    error: String?,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    val c = LocalThemeColors.current
    Column(modifier = Modifier.padding(top = TataraSpacing.Control, bottom = TataraSpacing.Micro)) {
        TataraTextField(
            value = name,
            onValueChange = onNameChange,
            label = "Habit name",
            placeholder = "Read after breakfast",
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf<Stack?>(null) + stacks.sortedBy { it.sortOrder }).forEach { stack ->
                TataraChoiceChip(
                    label = stack?.name ?: "No group",
                    selected = stack?.id == selectedStackId,
                    onClick = { onSelectStack(stack?.id) },
                )
            }
        }
        error?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Text(it, color = c.warm, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(TataraSpacing.Tight)) {
            TataraSecondaryButton("Cancel", onCancel, Modifier.weight(1f))
            TataraPrimaryButton("Add habit", onSave, Modifier.weight(1f), enabled = name.isNotBlank())
        }
    }
}

@Composable
private fun EditHabitForm(
    name: String,
    onNameChange: (String) -> Unit,
    stacks: List<Stack>,
    selectedStackId: Long?,
    onSelectStack: (Long?) -> Unit,
    error: String?,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
) {
    val c = LocalThemeColors.current
    Column(
        modifier = Modifier.padding(
            start = TataraSpacing.Section,
            top = TataraSpacing.Micro,
            bottom = TataraSpacing.Tight,
        )
    ) {
        TataraTextField(
            value = name,
            onValueChange = onNameChange,
            label = "Habit name",
        )
        Spacer(modifier = Modifier.height(TataraSpacing.Tight))
        Row(horizontalArrangement = Arrangement.spacedBy(TataraSpacing.Tight)) {
            (listOf<Stack?>(null) + stacks.sortedBy { it.sortOrder }).forEach { stack ->
                TataraChoiceChip(
                    label = stack?.name ?: "No group",
                    selected = stack?.id == selectedStackId,
                    onClick = { onSelectStack(stack?.id) },
                )
            }
        }
        error?.let {
            Spacer(modifier = Modifier.height(TataraSpacing.Tight))
            Text(it, color = c.warm, fontSize = TataraType.Label)
        }
        Spacer(modifier = Modifier.height(TataraSpacing.Tight))
        Row(horizontalArrangement = Arrangement.spacedBy(TataraSpacing.Tight)) {
            TataraSecondaryButton("Cancel", onCancel, Modifier.weight(1f))
            TataraDestructiveButton("Delete", onDelete, Modifier.weight(1f))
            TataraPrimaryButton("Save", onSave, Modifier.weight(1f), enabled = name.isNotBlank())
        }
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
    onEdit: () -> Unit,
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

    var burst by remember { mutableIntStateOf(0) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // §2.5 — the theme's own mark, not a stock checkbox. Completing throws a
        // short ember burst from the tap point; pending is an empty vessel.
        // The burst overlay is requiredSize so it can spill past the mark without
        // inflating the row — otherwise completing a habit shifts the layout.
        Box(modifier = Modifier.size(26.dp), contentAlignment = Alignment.Center) {
            MotifMark(
                checked = completed,
                frozen = frozen,
                onClick = {
                    if (!completed) burst++
                    onToggle()
                },
            )
            EmberBurst(key = burst, modifier = Modifier.requiredSize(72.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f).clickable(onClick = onEdit)) {
            Text(
                habit.name,
                color = if (completed) c.muted else c.primary,
                fontSize = 14.sp,
                textDecoration = if (completed) TextDecoration.LineThrough else null,
            )
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
