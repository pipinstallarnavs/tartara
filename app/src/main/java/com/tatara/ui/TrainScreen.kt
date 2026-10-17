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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tatara.data.db.entity.Exercise
import com.tatara.data.db.entity.Routine
import com.tatara.data.db.entity.RoutineItem
import com.tatara.data.db.entity.Session
import com.tatara.data.train.Progression
import com.tatara.data.train.RoutinePrescription
import com.tatara.data.train.TrainRepository
import com.tatara.ui.theme.LocalThemeColors
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val REST_SECONDS = 120
private val SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM")
private val REP_RANGE_OPTIONS: List<Pair<Int, Int>?> =
    listOf(5 to 8, 8 to 12, 12 to 15, null)

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
    val targetRepRange: String? = null,
    val targetRpe: Float? = null,
    val restSeconds: Int? = null,
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
    var routineItems by remember { mutableStateOf<Map<Long, List<String>>>(emptyMap()) }
    var creatingRoutine by remember { mutableStateOf(false) }
    var editorRoutine by remember { mutableStateOf<Routine?>(null) }

    suspend fun refresh() {
        val loadedRoutines = repo.routines()
        val exerciseNames = repo.exercises().associate { it.id to it.name }
        routines = loadedRoutines
        routineItems = loadedRoutines.associate { routine ->
            routine.id to repo.itemsFor(routine.id).map { exerciseNames[it.exerciseId] ?: "Unknown exercise" }
        }
        next = repo.nextRoutine()
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
                            targetRepRange = it.targetRepRange, targetRpe = it.targetRpe,
                            restSeconds = it.restSeconds,
                        )
                    },
                )
            )
        }
    }

    if (creatingRoutine || editorRoutine != null) {
        RoutineEditor(
            repo = repo,
            initialRoutine = editorRoutine,
            onCancel = {
                creatingRoutine = false
                editorRoutine = null
            },
            onSave = { name, addedItems ->
                scope.launch {
                    repo.saveRoutine(editorRoutine, name, addedItems)
                    creatingRoutine = false
                    editorRoutine = null
                    refresh()
                }
            },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TataraSpacing.ScreenHorizontal, vertical = TataraSpacing.Standard),
    ) {
        // §4.1 — the default state is one large button: the next routine in the cycle.
        next?.let { routine ->
            TataraPrimaryButton(
                label = "Start ${routine.name}",
                onClick = { start(routine) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(TataraSpacing.Tight))
        }
        TataraSecondaryButton(
            label = "Freestyle",
            onClick = { start(null) },
            modifier = Modifier.fillMaxWidth(),
        )

        stalls.forEach {
            Text(
                "${it.name} e1RM flat for 3 sessions.",
                color = c.warm,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = TataraSpacing.Tight),
            )
        }

        if (volume.isNotEmpty()) {
            TataraSectionHeader("This week, working sets")
            volume.entries.sortedByDescending { it.value }.forEach { (group, sets) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = TataraSpacing.Micro)) {
                    Text(group, color = c.muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("$sets", color = c.primary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        TataraSectionHeader("Routines")
        if (routines.isEmpty()) {
            TataraEmptyState(
                title = "No routines yet",
                message = "Create a repeatable training pattern or begin without one.",
                primaryActionLabel = "Create routine",
                onPrimaryAction = { creatingRoutine = true },
                secondaryActionLabel = "Freestyle",
                onSecondaryAction = { start(null) },
            )
        } else {
            routines.forEach { routine ->
                RoutineSummaryRow(
                    routine = routine,
                    exerciseNames = routineItems[routine.id].orEmpty(),
                    isNext = routine.id == next?.id,
                    onStart = { start(routine) },
                    onEdit = { editorRoutine = routine },
                )
            }
            Spacer(modifier = Modifier.height(TataraSpacing.Tight))
            TataraSecondaryButton(
                label = "Create routine",
                onClick = { creatingRoutine = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (history.isNotEmpty()) {
            TataraSectionHeader("History")
            val names = routines.associate { it.id to it.name }
            history.forEach { session ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = TataraSpacing.Micro)) {
                    Text(
                        session.date.format(SHORT_DATE_FORMAT),
                        color = c.muted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(modifier = Modifier.width(TataraSpacing.Control))
                    Text(
                        names[session.routineId] ?: "Freestyle",
                        color = c.primary,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                    )
                    session.durationMin?.let {
                        Text("${it}m", color = c.muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(TataraSpacing.Group))
    }
}



private data class PendingRoutineItem(
    val exerciseName: String,
    val prescription: RoutinePrescription,
)

private data class RoutineItemSummary(
    val exerciseName: String,
    val detail: String,
)

@Composable
private fun RoutineSummaryRow(
    routine: Routine,
    exerciseNames: List<String>,
    isNext: Boolean,
    onStart: () -> Unit,
    onEdit: () -> Unit,
) {
    val c = LocalThemeColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onStart)
            .padding(vertical = TataraSpacing.Tight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(routine.name, color = c.primary, fontSize = TataraType.Body)
                if (isNext) {
                    Spacer(modifier = Modifier.width(TataraSpacing.Tight))
                    Text(
                        "NEXT",
                        color = c.cool,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
            val summary = if (exerciseNames.isEmpty()) {
                "No exercises configured"
            } else {
                "${exerciseNames.size} exercise${if (exerciseNames.size == 1) "" else "s"} · " +
                    exerciseNames.take(3).joinToString(" · ")
            }
            Text(
                summary,
                color = c.muted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TataraGhostAction(label = "Edit", onClick = onEdit)
    }
    TataraDivider()
}

@Composable
private fun RoutineEditor(
    repo: TrainRepository,
    initialRoutine: Routine?,
    onCancel: () -> Unit,
    onSave: (String, List<RoutinePrescription>) -> Unit,
) {
    val c = LocalThemeColors.current
    var name by remember(initialRoutine?.id) { mutableStateOf(initialRoutine?.name.orEmpty()) }
    var exercises by remember { mutableStateOf(emptyList<Exercise>()) }
    var existingItems by remember(initialRoutine?.id) { mutableStateOf(emptyList<RoutineItemSummary>()) }
    var pendingItems by remember(initialRoutine?.id) { mutableStateOf(emptyList<PendingRoutineItem>()) }
    var search by remember { mutableStateOf("") }
    var matches by remember { mutableStateOf(emptyList<Exercise>()) }
    var configuring by remember { mutableStateOf<Exercise?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(initialRoutine?.id) {
        val loadedExercises = repo.exercises()
        val names = loadedExercises.associate { it.id to it.name }
        exercises = loadedExercises
        existingItems = initialRoutine?.let { routine ->
            repo.itemsFor(routine.id).map { item ->
                RoutineItemSummary(
                    exerciseName = names[item.exerciseId] ?: "Unknown exercise",
                    detail = item.prescriptionDetail(),
                )
            }
        }.orEmpty()
    }
    LaunchedEffect(search, exercises) {
        matches = if (search.length < 2) {
            emptyList()
        } else {
            exercises.filter { it.name.contains(search, ignoreCase = true) }.take(6)
        }
    }

    val canSave = name.isNotBlank() &&
        (initialRoutine != null || existingItems.isNotEmpty() || pendingItems.isNotEmpty())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = TataraSpacing.ScreenHorizontal, vertical = TataraSpacing.Standard),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (initialRoutine == null) "Create routine" else "Edit routine",
                color = c.primary,
                fontSize = 24.sp,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.weight(1f),
            )
            TataraGhostAction(label = "Cancel", onClick = onCancel, enabled = !saving)
        }
        Spacer(modifier = Modifier.height(TataraSpacing.Standard))
        TataraTextField(
            value = name,
            onValueChange = { name = it },
            label = "Routine name",
            placeholder = "Morning foundation",
            enabled = !saving,
        )

        TataraSectionHeader("Exercises")
        if (existingItems.isEmpty() && pendingItems.isEmpty()) {
            Text(
                "Add at least one exercise before saving.",
                color = c.muted,
                fontSize = 12.sp,
            )
        }
        existingItems.forEach { item ->
            RoutinePrescriptionRow(
                exerciseName = item.exerciseName,
                detail = item.detail,
            )
        }
        pendingItems.forEachIndexed { index, item ->
            RoutinePrescriptionRow(
                exerciseName = item.exerciseName,
                detail = item.prescription.prescriptionDetail(),
                pending = true,
                onRemove = {
                    pendingItems = pendingItems.filterIndexed { itemIndex, _ -> itemIndex != index }
                },
            )
        }

        configuring?.let { exercise ->
            Spacer(modifier = Modifier.height(TataraSpacing.Control))
            RoutineItemSetupForm(
                exerciseName = exercise.name,
                onCancel = { configuring = null },
                onSave = { sets, repLow, repHigh, startWeight, increment, restSeconds, rpe ->
                    pendingItems = pendingItems + PendingRoutineItem(
                        exerciseName = exercise.name,
                        prescription = RoutinePrescription(
                            exerciseId = exercise.id,
                            targetSets = sets,
                            repRangeLow = repLow,
                            repRangeHigh = repHigh,
                            incrementKg = increment,
                            startWeightKg = startWeight,
                            restSeconds = restSeconds,
                            targetRpe = rpe,
                        ),
                    )
                    configuring = null
                    search = ""
                },
            )
        } ?: run {
            Spacer(modifier = Modifier.height(TataraSpacing.Control))
            TataraTextField(
                value = search,
                onValueChange = { search = it },
                label = "Add exercise",
                placeholder = "Search exercises",
                enabled = !saving,
            )
            matches.forEach { exercise ->
                TataraChoiceChip(
                    label = exercise.name,
                    selected = false,
                    onClick = { configuring = exercise },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = TataraSpacing.Tight),
                )
            }
        }

        Spacer(modifier = Modifier.height(TataraSpacing.Group))
        TataraPrimaryButton(
            label = "Save routine",
            onClick = {
                saving = true
                onSave(name, pendingItems.map { it.prescription })
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = canSave && !saving,
            loading = saving,
        )
        Spacer(modifier = Modifier.height(TataraSpacing.Group))
    }
}

@Composable
private fun RoutinePrescriptionRow(
    exerciseName: String,
    detail: String,
    pending: Boolean = false,
    onRemove: (() -> Unit)? = null,
) {
    val c = LocalThemeColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = TataraSpacing.Tight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(exerciseName, color = c.primary, fontSize = TataraType.Body)
                if (pending) {
                    Spacer(modifier = Modifier.width(TataraSpacing.Tight))
                    Text("NEW", color = c.cool, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
            Text(detail, color = c.muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        if (onRemove != null) {
            TataraGhostAction(label = "Remove", onClick = onRemove)
        }
    }
    TataraDivider()
}

private fun RoutineItem.prescriptionDetail(): String = buildString {
    append("$targetSets×$repRangeLow–$repRangeHigh · ${trimF(currentWeightKg)}kg")
    append(" · +${trimF(incrementKg)}kg")
    restSeconds?.let { append(" · ${it}s") }
    targetRpe?.let { append(" · RPE ${trimF(it)}") }
}

private fun RoutinePrescription.prescriptionDetail(): String = buildString {
    append("$targetSets×$repRangeLow–$repRangeHigh · ${trimF(startWeightKg)}kg")
    append(" · +${trimF(incrementKg)}kg")
    restSeconds?.let { append(" · ${it}s") }
    targetRpe?.let { append(" · RPE ${trimF(it)}") }
}

private val REP_PRESETS = REP_RANGE_OPTIONS.filterNotNull()

/**
 * §4.1 — the prescription form: sets as a stepper (never a text field), rep range
 * as one-tap presets with a custom fallback, and the starting weight entered once
 * here rather than guessed on the run screen. Rest/RPE are optional and collapsed
 * into plain fields rather than a second popup.
 */
@Composable
private fun RoutineItemSetupForm(
    exerciseName: String,
    onCancel: () -> Unit,
    onSave: (
        sets: Int, repLow: Int, repHigh: Int, startWeight: Float,
        increment: Float, restSeconds: Int?, rpe: Float?,
    ) -> Unit,
) {
    var sets by remember { mutableIntStateOf(3) }
    var preset by remember { mutableStateOf<Pair<Int, Int>?>(REP_PRESETS[1]) }
    var customLow by remember { mutableStateOf("") }
    var customHigh by remember { mutableStateOf("") }
    var startWeight by remember { mutableStateOf("") }
    var increment by remember { mutableStateOf("2.5") }
    var restSeconds by remember { mutableStateOf("") }
    var rpe by remember { mutableStateOf("") }
    val ranges = REP_RANGE_OPTIONS
    val rangeValid = preset != null || (customLow.toIntOrNull() != null && customHigh.toIntOrNull() != null)
    val canSave = startWeight.toFloatOrNull() != null && rangeValid

    Column(modifier = Modifier.padding(top = TataraSpacing.Tight)) {
        Text(exerciseName, color = LocalThemeColors.current.primary, fontSize = TataraType.Body)
        Spacer(modifier = Modifier.height(TataraSpacing.Control))
        TataraStepper(
            value = sets,
            onDecrease = { if (sets > 1) sets-- },
            onIncrease = { sets++ },
            label = "Working sets",
            decreaseEnabled = sets > 1,
        )
        Spacer(modifier = Modifier.height(TataraSpacing.Control))
        TataraSegmentedControl(
            items = ranges,
            selected = preset,
            onSelected = { preset = it },
            label = "Rep range",
            itemLabel = { range -> range?.let { "${it.first}–${it.second}" } ?: "Custom" },
        )
        if (preset == null) {
            Spacer(modifier = Modifier.height(TataraSpacing.Control))
            Row(horizontalArrangement = Arrangement.spacedBy(TataraSpacing.Tight)) {
                TataraNumberField(customLow, { customLow = it }, "Minimum reps", Modifier.weight(1f))
                TataraNumberField(customHigh, { customHigh = it }, "Maximum reps", Modifier.weight(1f))
            }
        }
        Spacer(modifier = Modifier.height(TataraSpacing.Control))
        Row(horizontalArrangement = Arrangement.spacedBy(TataraSpacing.Tight)) {
            TataraNumberField(
                startWeight,
                { startWeight = it },
                "Starting weight",
                Modifier.weight(1f),
                suffix = "kg",
            )
            TataraNumberField(
                increment,
                { increment = it },
                "Increment",
                Modifier.weight(1f),
                suffix = "kg",
            )
        }
        Spacer(modifier = Modifier.height(TataraSpacing.Control))
        Row(horizontalArrangement = Arrangement.spacedBy(TataraSpacing.Tight)) {
            TataraNumberField(
                restSeconds,
                { restSeconds = it },
                "Rest",
                Modifier.weight(1f),
                suffix = "sec",
            )
            TataraNumberField(
                rpe,
                { rpe = it },
                "Target RPE",
                Modifier.weight(1f),
                placeholder = "optional",
            )
        }
        Spacer(modifier = Modifier.height(TataraSpacing.Control))
        Row(horizontalArrangement = Arrangement.spacedBy(TataraSpacing.Tight)) {
            TataraSecondaryButton("Cancel", onCancel, Modifier.weight(1f))
            TataraPrimaryButton(
                label = "Add exercise",
                onClick = save@{
                    val weight = startWeight.toFloatOrNull() ?: return@save
                    val inc = increment.toFloatOrNull() ?: 2.5f
                    val (low, high) = preset ?: (
                        (customLow.toIntOrNull() ?: return@save) to
                            (customHigh.toIntOrNull() ?: return@save)
                        )
                    onSave(sets, low, high, weight, inc, restSeconds.toIntOrNull(), rpe.toFloatOrNull())
                },
                modifier = Modifier.weight(1f),
                enabled = canSave,
            )
        }
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
    var restDuration by remember { mutableIntStateOf(REST_SECONDS) }
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
            restLeft = restDuration
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
            restDuration = set.restSeconds ?: REST_SECONDS
            restKey++
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = TataraSpacing.ScreenHorizontal, vertical = TataraSpacing.Standard)
    ) {
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
            TataraPrimaryButton(
                label = "Finish",
                onClick = onFinish,
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
                TataraTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = "Add exercise",
                    placeholder = "Search exercises",
                    modifier = Modifier
                        .padding(top = TataraSpacing.Standard)
                        .fillMaxWidth(),
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
    val target = buildString {
        set.targetRepRange?.let { append(it) }
        set.targetRpe?.let { append(if (isEmpty()) "RPE $it" else " @ RPE $it") }
    }

    var burst by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        // Explicit gaps: the weight cluster and the rep cluster each pack five
        // controls into one row, and without spacing their fills visually merge.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            // §4.2.1 — the theme's mark; the tap is what records the set, and it
            // throws the same ember burst as every other completion in the app.
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                MotifMark(
                    checked = set.confirmed,
                    size = 24.dp,
                    onClick = {
                        if (!set.confirmed) {
                            burst++
                            onConfirm()
                        }
                    },
                )
                EmberBurst(key = burst, modifier = Modifier.requiredSize(68.dp))
            }
            if (set.isWarmup) {
                Text("w", color = c.muted, fontSize = 11.sp)
            }
            // §4.2 — plate buttons flank the weight so a normal set never needs the keyboard.
            PlateButton("-5", enabled = !set.confirmed) { onChange(set.copy(weight = adjust(set.weight, -5f))) }
            PlateButton("-2.5", enabled = !set.confirmed) { onChange(set.copy(weight = adjust(set.weight, -2.5f))) }
            NumberBox(set.weight, enabled = !set.confirmed, color = contentColor) { onChange(set.copy(weight = it)) }
            PlateButton("+2.5", enabled = !set.confirmed) { onChange(set.copy(weight = adjust(set.weight, 2.5f))) }
            PlateButton("+5", enabled = !set.confirmed) { onChange(set.copy(weight = adjust(set.weight, 5f))) }
            Spacer(modifier = Modifier.width(5.dp))
            StepButtonSmall("-1", enabled = !set.confirmed) { onChange(set.copy(reps = adjust(set.reps, -1f, decimals = 0))) }
            NumberBox(set.reps, enabled = !set.confirmed, color = contentColor, width = 32.dp) { onChange(set.copy(reps = it)) }
            StepButtonSmall("+1", enabled = !set.confirmed) { onChange(set.copy(reps = adjust(set.reps, 1f, decimals = 0))) }
            if (set.incremented) {
                Spacer(modifier = Modifier.width(6.dp))
                // §4.2.1 — the app moved this weight.
                Box(modifier = Modifier.size(6.dp).background(c.warm, CircleShape))
            }
        }
        Row(modifier = Modifier.padding(start = 30.dp, top = 2.dp)) {
            if (target.isNotEmpty()) {
                Text(target, color = c.muted, fontSize = 11.sp)
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
                    modifier = Modifier.clickable(onClick = onDuplicate).padding(start = 10.dp),
                )
            }
        }
    }
}

private fun adjust(current: String, delta: Float, decimals: Int = -1): String {
    val next = ((current.toFloatOrNull() ?: 0f) + delta).coerceAtLeast(0f)
    return if (decimals == 0 || next == next.toInt().toFloat()) next.toInt().toString() else next.toString()
}

@Composable
private fun PlateButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val c = LocalThemeColors.current
    Text(
        label,
        color = if (enabled) c.muted else c.hairline,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .background(c.surface, RoundedCornerShape(2.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
    )
}

@Composable
private fun StepButtonSmall(label: String, enabled: Boolean, onClick: () -> Unit) {
    val c = LocalThemeColors.current
    Text(
        label,
        color = if (enabled) c.muted else c.hairline,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .background(c.surface, RoundedCornerShape(2.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
    )
}

@Composable
private fun NumberBox(
    value: String,
    enabled: Boolean,
    color: Color,
    width: androidx.compose.ui.unit.Dp = 44.dp,
    onChange: (String) -> Unit,
) {
    val c = LocalThemeColors.current
    BasicTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        singleLine = true,
        textStyle = TextStyle(color = color, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
        cursorBrush = SolidColor(c.cool),
        modifier = Modifier
            .width(width)
            .background(c.surface, RoundedCornerShape(2.dp))
            .padding(4.dp),
    )
}

private fun trimF(v: Float): String =
    if (v == v.toInt().toFloat()) v.toInt().toString() else v.toString()
