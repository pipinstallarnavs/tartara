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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tatara.data.db.entity.Exercise
import com.tatara.data.db.entity.Routine
import com.tatara.data.db.entity.Session
import com.tatara.data.train.FinishResult
import com.tatara.data.train.PrefillSet
import com.tatara.data.train.Progression
import com.tatara.data.train.TrainRepository
import com.tatara.ui.theme.LocalThemeColors
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val REST_SECONDS = 120

private data class ActiveSet(
    val exerciseId: Long,
    val routineItemId: Long?,
    val setIndex: Int,
    val weight: String,
    val reps: String,
    val isWarmup: Boolean,
    val incremented: Boolean,
    val ghost: String?,
    val confirmed: Boolean = false,
)

@Composable
fun TrainScreen(repo: TrainRepository) {
    var activeSessionId by remember { mutableStateOf<Long?>(null) }
    var activeRoutineName by remember { mutableStateOf<String?>(null) }
    var activeSets by remember { mutableStateOf(listOf<ActiveSet>()) }
    var startedAtMs by remember { mutableStateOf(0L) }

    val scope = rememberCoroutineScope()

    if (activeSessionId == null) {
        TrainHome(repo) { start ->
            activeSessionId = start.first
            activeRoutineName = start.second
            activeSets = start.third
            startedAtMs = System.currentTimeMillis()
        }
    } else {
        ActiveSession(
            repo = repo,
            sessionId = activeSessionId!!,
            routineName = activeRoutineName,
            sets = activeSets,
            onSetsChange = { activeSets = it },
            onFinish = {
                scope.launch {
                    val minutes = ((System.currentTimeMillis() - startedAtMs) / 60000L).toInt()
                    repo.finishSession(activeSessionId!!, minutes)
                    activeSessionId = null
                    activeSets = emptyList()
                }
            },
        )
    }
}

@Composable
private fun TrainHome(
    repo: TrainRepository,
    onStart: (Triple<Long, String?, List<ActiveSet>>) -> Unit,
) {
    val c = LocalThemeColors.current
    val scope = rememberCoroutineScope()

    var next by remember { mutableStateOf<Routine?>(null) }
    var routines by remember { mutableStateOf(listOf<Routine>()) }
    var history by remember { mutableStateOf(listOf<Session>()) }
    var stalls by remember { mutableStateOf(listOf<Exercise>()) }
    var volume by remember { mutableStateOf(mapOf<String, Int>()) }
    var newRoutineName by remember { mutableStateOf("") }
    var builderFor by remember { mutableStateOf<Long?>(null) }

    suspend fun refresh() {
        next = repo.nextRoutine()
        routines = repo.routines()
        history = repo.history().take(10)
        stalls = repo.stallFlags()
        volume = repo.weeklySetsByMuscleGroup()
    }
    LaunchedEffect(Unit) { refresh() }

    fun start(routine: Routine?) {
        scope.launch {
            val s = repo.startSession(routine)
            onStart(
                Triple(
                    s.session.id,
                    routine?.name,
                    s.prefill.map {
                        ActiveSet(
                            it.exerciseId, it.routineItemId, it.setIndex,
                            trimF(it.weightKg), it.reps.toString(),
                            it.isWarmup, it.incremented, it.ghost,
                        )
                    },
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 16.dp, top = 16.dp),
    ) {
        // §4.1 — the default state is one large button: the next routine in the cycle.
        next?.let { routine ->
            Text(
                text = routine.name,
                color = c.ground,
                fontSize = 18.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.cool, RoundedCornerShape(2.dp))
                    .clickable { start(routine) }
                    .padding(vertical = 18.dp, horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Text(
            "Freestyle",
            color = c.primary,
            fontSize = 14.sp,
            modifier = Modifier
                .background(c.surface, RoundedCornerShape(2.dp))
                .clickable { start(null) }
                .padding(vertical = 10.dp, horizontal = 16.dp),
        )

        stalls.forEach {
            Text(
                "${it.name} e1RM flat for 3 sessions.",
                color = c.warm,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (volume.isNotEmpty()) {
            TrainSection("This week, working sets")
            volume.entries.sortedByDescending { it.value }.forEach { (group, sets) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(group, color = c.muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("$sets", color = c.primary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        TrainSection("Routines")
        routines.forEach { routine ->
            RoutineRow(repo, routine, expanded = builderFor == routine.id) {
                builderFor = if (builderFor == routine.id) null else routine.id
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            BasicTextField(
                value = newRoutineName,
                onValueChange = { newRoutineName = it },
                singleLine = true,
                textStyle = TextStyle(color = c.primary, fontSize = 13.sp),
                cursorBrush = SolidColor(c.cool),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier
                            .width(160.dp)
                            .background(c.surface, RoundedCornerShape(2.dp))
                            .padding(8.dp),
                    ) {
                        if (newRoutineName.isEmpty()) Text("New routine", color = c.muted, fontSize = 13.sp)
                        inner()
                    }
                },
            )
            Text(
                "Add",
                color = c.cool,
                fontSize = 13.sp,
                modifier = Modifier
                    .clickable {
                        if (newRoutineName.isNotBlank()) scope.launch {
                            repo.createRoutine(newRoutineName)
                            newRoutineName = ""
                            refresh()
                        }
                    }
                    .padding(8.dp),
            )
        }

        if (history.isNotEmpty()) {
            TrainSection("History")
            val names = routines.associate { it.id to it.name }
            history.forEach { session ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        session.date.format(DateTimeFormatter.ofPattern("d MMM")),
                        color = c.muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        names[session.routineId] ?: "Freestyle",
                        color = c.primary, fontSize = 12.sp, modifier = Modifier.weight(1f),
                    )
                    session.durationMin?.let {
                        Text("${it}m", color = c.muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun TrainSection(title: String) {
    val c = LocalThemeColors.current
    Spacer(modifier = Modifier.height(20.dp))
    Text(title, color = c.muted, fontSize = 12.sp)
    Spacer(modifier = Modifier.height(6.dp))
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun RoutineRow(repo: TrainRepository, routine: Routine, expanded: Boolean, onToggle: () -> Unit) {
    val c = LocalThemeColors.current
    val scope = rememberCoroutineScope()
    var items by remember(routine.id, expanded) { mutableStateOf(listOf<Pair<String, String>>()) }
    var search by remember { mutableStateOf("") }
    var matches by remember { mutableStateOf(listOf<Exercise>()) }

    LaunchedEffect(routine.id, expanded) {
        if (expanded) {
            val exercises = repo.exercises().associateBy { it.id }
            items = repo.itemsFor(routine.id).map { item ->
                (exercises[item.exerciseId]?.name ?: "?") to
                    "${item.targetSets}×${item.repRangeLow}–${item.repRangeHigh} @ ${trimF(item.currentWeightKg)}kg"
            }
        }
    }
    LaunchedEffect(search) {
        matches = if (search.length < 2) emptyList()
        else repo.exercises().filter { it.name.contains(search, ignoreCase = true) }.take(5)
    }

    Column {
        Text(
            routine.name,
            color = c.primary,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 8.dp),
        )
        if (expanded) {
            items.forEach { (name, detail) ->
                Row(modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)) {
                    Text(name, color = c.muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text(detail, color = c.muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
            BasicTextField(
                value = search,
                onValueChange = { search = it },
                singleLine = true,
                textStyle = TextStyle(color = c.primary, fontSize = 13.sp),
                cursorBrush = SolidColor(c.cool),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp, top = 4.dp)
                            .fillMaxWidth()
                            .background(c.surface, RoundedCornerShape(2.dp))
                            .padding(8.dp),
                    ) {
                        if (search.isEmpty()) Text("Add exercise", color = c.muted, fontSize = 13.sp)
                        inner()
                    }
                },
            )
            matches.forEach { exercise ->
                Text(
                    exercise.name,
                    color = c.cool,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .clickable {
                            scope.launch {
                                // Defaults: 3×8–12, +2.5kg, start 20kg — editable per spec later.
                                repo.addRoutineItem(routine.id, exercise.id, 3, 8, 12, 2.5f, 20f)
                                search = ""
                            }
                        }
                        .padding(vertical = 6.dp),
                )
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
    }
}

@Composable
private fun ActiveSession(
    repo: TrainRepository,
    sessionId: Long,
    routineName: String?,
    sets: List<ActiveSet>,
    onSetsChange: (List<ActiveSet>) -> Unit,
    onFinish: () -> Unit,
) {
    val c = LocalThemeColors.current
    val scope = rememberCoroutineScope()
    var exercises by remember { mutableStateOf(mapOf<Long, Exercise>()) }
    var restKey by remember { mutableIntStateOf(0) }
    var restLeft by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var matches by remember { mutableStateOf(listOf<Exercise>()) }

    LaunchedEffect(Unit) { exercises = repo.exercises().associateBy { it.id } }
    LaunchedEffect(search) {
        matches = if (search.length < 2) emptyList()
        else repo.exercises().filter { it.name.contains(search, ignoreCase = true) }.take(5)
    }
    // §4.2 — rest timer auto-starts on set completion; tap to skip.
    LaunchedEffect(restKey) {
        if (restKey > 0) {
            restLeft = REST_SECONDS
            while (restLeft > 0) {
                delay(1000)
                restLeft--
            }
        }
    }

    fun confirm(index: Int) {
        val set = sets[index]
        val weight = set.weight.toFloatOrNull() ?: return
        val reps = set.reps.toIntOrNull() ?: return
        scope.launch {
            repo.confirmSet(sessionId, set.exerciseId, set.routineItemId, set.setIndex, weight, reps, set.isWarmup)
            onSetsChange(sets.toMutableList().also { it[index] = set.copy(confirmed = true) })
            restKey++
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 16.dp, top = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(routineName ?: "Freestyle", color = c.primary, fontSize = 20.sp, modifier = Modifier.weight(1f))
            if (restLeft > 0) {
                Text(
                    "Rest %d:%02d".format(restLeft / 60, restLeft % 60),
                    color = c.warm,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable { restLeft = 0 }.padding(8.dp),
                )
            }
            Text(
                "Finish",
                color = c.cool,
                fontSize = 14.sp,
                modifier = Modifier.clickable(onClick = onFinish).padding(8.dp),
            )
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            val grouped = sets.withIndex().groupBy { it.value.exerciseId }
            grouped.forEach { (exerciseId, indexed) ->
                item(key = "ex-$exerciseId") {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text(exercises[exerciseId]?.name ?: "…", color = c.primary, fontSize = 15.sp)
                        val working = indexed.lastOrNull { !it.value.isWarmup }?.value
                        working?.weight?.toFloatOrNull()?.let { w ->
                            Progression.plateStack(w)?.takeIf { it.isNotEmpty() }?.let { stack ->
                                Text(
                                    "Per side: ${stack.joinToString(" + ") { trimF(it) }}",
                                    color = c.muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                                )
                            }
                        }
                    }
                }
                indexed.forEach { (index, set) ->
                    item(key = "set-$exerciseId-$index") {
                        SetRow(
                            set = set,
                            onChange = { updated ->
                                onSetsChange(sets.toMutableList().also { it[index] = updated })
                            },
                            onConfirm = { confirm(index) },
                            onDuplicate = {
                                val dup = set.copy(setIndex = sets.count { it.exerciseId == exerciseId })
                                scope.launch {
                                    repo.confirmSet(
                                        sessionId, dup.exerciseId, dup.routineItemId, dup.setIndex,
                                        dup.weight.toFloatOrNull() ?: return@launch,
                                        dup.reps.toIntOrNull() ?: return@launch,
                                        dup.isWarmup,
                                    )
                                    onSetsChange(sets + dup.copy(confirmed = true, ghost = null))
                                    restKey++
                                }
                            },
                        )
                    }
                }
            }
            item(key = "add-exercise") {
                BasicTextField(
                    value = search,
                    onValueChange = { search = it },
                    singleLine = true,
                    textStyle = TextStyle(color = c.primary, fontSize = 13.sp),
                    cursorBrush = SolidColor(c.cool),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .fillMaxWidth()
                                .background(c.surface, RoundedCornerShape(2.dp))
                                .padding(10.dp),
                        ) {
                            if (search.isEmpty()) Text("Add exercise", color = c.muted, fontSize = 13.sp)
                            inner()
                        }
                    },
                )
                matches.forEach { exercise ->
                    Text(
                        exercise.name,
                        color = c.cool,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable {
                                onSetsChange(
                                    sets + ActiveSet(
                                        exerciseId = exercise.id, routineItemId = null,
                                        setIndex = sets.count { it.exerciseId == exercise.id },
                                        weight = "20", reps = "8", isWarmup = false,
                                        incremented = false, ghost = null,
                                    )
                                )
                                search = ""
                            }
                            .padding(vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SetRow(
    set: ActiveSet,
    onChange: (ActiveSet) -> Unit,
    onConfirm: () -> Unit,
    onDuplicate: () -> Unit,
) {
    val c = LocalThemeColors.current
    val contentColor = if (set.confirmed) c.primary else c.muted

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // §4.2.1 — dimmed until tapped; the tap is what records the set.
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(if (set.confirmed) c.cool else c.surface, RoundedCornerShape(2.dp))
                .clickable(enabled = !set.confirmed, onClick = onConfirm),
        )
        Spacer(modifier = Modifier.width(10.dp))
        if (set.isWarmup) {
            Text("w", color = c.muted, fontSize = 11.sp)
            Spacer(modifier = Modifier.width(6.dp))
        }
        NumberBox(set.weight, enabled = !set.confirmed, color = contentColor) { onChange(set.copy(weight = it)) }
        Text(" × ", color = c.muted, fontSize = 13.sp)
        NumberBox(set.reps, enabled = !set.confirmed, color = contentColor) { onChange(set.copy(reps = it)) }
        if (set.incremented) {
            Spacer(modifier = Modifier.width(6.dp))
            // §4.2.1 — the app moved this weight.
            Box(modifier = Modifier.size(6.dp).background(c.warm, CircleShape))
        }
        Spacer(modifier = Modifier.weight(1f))
        set.ghost?.let {
            Text(it, color = c.muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        if (set.confirmed) {
            Text(
                "again",
                color = c.muted,
                fontSize = 11.sp,
                modifier = Modifier.clickable(onClick = onDuplicate).padding(start = 10.dp, top = 4.dp, bottom = 4.dp),
            )
        }
    }
}

@Composable
private fun NumberBox(value: String, enabled: Boolean, color: androidx.compose.ui.graphics.Color, onChange: (String) -> Unit) {
    val c = LocalThemeColors.current
    BasicTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        singleLine = true,
        textStyle = TextStyle(color = color, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
        cursorBrush = SolidColor(c.cool),
        modifier = Modifier
            .width(56.dp)
            .background(c.surface, RoundedCornerShape(2.dp))
            .padding(6.dp),
    )
}

private fun trimF(v: Float): String =
    if (v == v.toInt().toFloat()) v.toInt().toString() else v.toString()
