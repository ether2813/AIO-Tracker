package com.aio.tracker.ui.fitness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Pool
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aio.tracker.data.db.ExerciseSet
import com.aio.tracker.data.db.Workout
import com.aio.tracker.data.db.WorkoutType
import com.aio.tracker.data.db.WorkoutWithExercises
import com.aio.tracker.ui.appViewModel
import com.aio.tracker.ui.components.AppCard
import com.aio.tracker.ui.components.BarChart
import com.aio.tracker.ui.components.ConfirmDialog
import com.aio.tracker.ui.components.DateField
import com.aio.tracker.ui.components.EmptyState
import com.aio.tracker.ui.components.IconBadge
import com.aio.tracker.ui.components.Pill
import com.aio.tracker.ui.components.ProgressRing
import com.aio.tracker.ui.components.SectionHeader
import com.aio.tracker.ui.components.SheetTitle
import com.aio.tracker.ui.components.StatTile
import com.aio.tracker.ui.components.screenPadding
import com.aio.tracker.ui.theme.AppTheme
import com.aio.tracker.util.Dates
import com.aio.tracker.util.toLocalDate
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

val WorkoutType.icon: ImageVector
    get() = when (this) {
        WorkoutType.STRENGTH -> Icons.Rounded.FitnessCenter
        WorkoutType.RUN -> Icons.AutoMirrored.Rounded.DirectionsRun
        WorkoutType.WALK -> Icons.AutoMirrored.Rounded.DirectionsWalk
        WorkoutType.CYCLING -> Icons.AutoMirrored.Rounded.DirectionsBike
        WorkoutType.SWIM -> Icons.Rounded.Pool
        WorkoutType.CARDIO -> Icons.Rounded.Favorite
        WorkoutType.HIIT -> Icons.Rounded.Whatshot
        WorkoutType.YOGA -> Icons.Rounded.SelfImprovement
        WorkoutType.SPORTS -> Icons.Rounded.SportsSoccer
        WorkoutType.OTHER -> Icons.Rounded.Timer
    }

@Composable
fun WorkoutType.tint(): Color = AppTheme.ext.chart[ordinal % AppTheme.ext.chart.size]

@Composable
fun FitnessScreen() {
    val vm = appViewModel { FitnessViewModel(it) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val workoutsState by vm.workouts.collectAsStateWithLifecycle()
    val workouts = workoutsState.orEmpty()
    var editing by remember { mutableStateOf<WorkoutWithExercises?>(null) }
    var creating by remember { mutableStateOf(false) }
    var editGoal by remember { mutableStateOf(false) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val today = LocalDate.now()
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    val weekMinutes = remember(workouts, weekStart) {
        (0..6).map { i ->
            val d = weekStart.plusDays(i.toLong()).toEpochDay()
            workouts.filter { it.workout.epochDay == d }.sumOf { it.workout.durationMin }.toFloat()
        }
    }
    val weekTotal = weekMinutes.sum().toInt()
    // Same Monday-to-Sunday range as the chart, so the ring, the pills and the bars always agree.
    val weekRange = weekStart.toEpochDay()..weekStart.toEpochDay() + 6
    val weekSessions = remember(workouts, weekStart) { workouts.count { it.workout.epochDay in weekRange } }
    val weekCalories = remember(workouts, weekStart) { workouts.filter { it.workout.epochDay in weekRange }.sumOf { it.workout.calories ?: 0 } }
    val monthStart = today.withDayOfMonth(1).toEpochDay()
    val monthEnd = today.with(TemporalAdjusters.lastDayOfMonth()).toEpochDay()
    val monthList = remember(workouts, monthStart, monthEnd) { workouts.filter { it.workout.epochDay in monthStart..monthEnd } }
    val activeStreak = remember(workouts, today) {
        val days = workouts.map { it.workout.epochDay }.toSet()
        var d = if (today.toEpochDay() in days) today.toEpochDay() else today.toEpochDay() - 1
        var n = 0
        while (d in days) { n++; d-- }
        n
    }
    val grouped = remember(workouts) { workouts.groupBy { it.workout.epochDay } }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Fitness") },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("Log workout") },
            )
        },
    ) { padding ->
        LazyColumn(contentPadding = screenPadding(padding, bottomExtra = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "week") {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProgressRing(
                            progress = weekTotal.toFloat() / settings.weeklyWorkoutGoalMin.coerceAtLeast(1),
                            size = 96.dp,
                            stroke = 10.dp,
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$weekTotal", style = MaterialTheme.typography.headlineSmall)
                                Text("of ${settings.weeklyWorkoutGoalMin}m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("This week", style = MaterialTheme.typography.titleMedium)
                            Text(
                                when {
                                    weekTotal >= settings.weeklyWorkoutGoalMin -> "Goal reached. MashaAllah, great work!"
                                    weekTotal == 0 -> "Let's get moving. Even 20 minutes counts."
                                    else -> "${settings.weeklyWorkoutGoalMin - weekTotal} minutes to your goal"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Pill("$weekSessions sessions", MaterialTheme.colorScheme.primary)
                                if (weekCalories > 0) Pill("$weekCalories kcal", AppTheme.ext.warning)
                            }
                        }
                        IconButton(onClick = { editGoal = true }) {
                            Icon(Icons.Rounded.Edit, "Change weekly goal", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    BarChart(
                        values = weekMinutes,
                        labels = listOf("M", "T", "W", "T", "F", "S", "S"),
                        highlightIndex = today.dayOfWeek.value - 1,
                        chartHeight = 100.dp,
                    )
                }
            }
            item(key = "tiles") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Sessions", "${monthList.size}", Icons.Rounded.FitnessCenter, MaterialTheme.colorScheme.primary, Modifier.weight(1f), sub = "this month")
                    StatTile("Active time", Dates.duration(monthList.sumOf { it.workout.durationMin }.toLong()), Icons.Rounded.Timer, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f), sub = "this month")
                    StatTile("Streak", "$activeStreak", Icons.Rounded.LocalFireDepartment, AppTheme.ext.warning, Modifier.weight(1f), sub = "days in a row")
                }
            }
            if (workoutsState != null && workouts.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        Icons.Rounded.FitnessCenter,
                        "No workouts yet",
                        "Log your first workout: a run, a gym session, a walk after Maghrib… everything counts.",
                        actionLabel = "Log workout",
                        onAction = { creating = true },
                    )
                }
            }
            grouped.forEach { (day, list) ->
                item(key = "h$day") { SectionHeader(Dates.relativeDay(day.toLocalDate(), today)) }
                items(list, key = { "w" + it.workout.id }) { w ->
                    WorkoutCard(w, onClick = { editing = w })
                }
            }
        }
    }

    if (creating || editing != null) {
        WorkoutEditorSheet(
            existing = editing,
            onDismiss = { creating = false; editing = null },
            onSave = { w, ex -> vm.save(w, ex); creating = false; editing = null },
            onDelete = { id -> vm.delete(id); creating = false; editing = null },
        )
    }
    if (editGoal) {
        GoalDialog(settings.weeklyWorkoutGoalMin, onDismiss = { editGoal = false }, onSave = { vm.setGoal(it) })
    }
}

@Composable
private fun GoalDialog(current: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var text by remember { mutableStateOf(current.toString()) }
    val value = text.toIntOrNull()
    val valid = value != null && value in 10..3000
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Weekly activity goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { v -> text = v.filter(Char::isDigit).take(4) },
                    label = { Text("Minutes per week") },
                    supportingText = { Text("Between 10 and 3000 minutes") },
                    singleLine = true,
                    isError = !valid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(75, 150, 300).forEach { m ->
                        FilterChip(selected = value == m, onClick = { text = m.toString() }, label = { Text("$m min") })
                    }
                }
            }
        },
        confirmButton = { Button(enabled = valid, onClick = { onSave(value ?: current); onDismiss() }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun WorkoutCard(item: WorkoutWithExercises, onClick: () -> Unit) {
    val w = item.workout
    AppCard(onClick = onClick, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(w.type.icon, w.type.tint(), size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(w.title.ifBlank { w.type.label }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val parts = buildList {
                    add(Dates.duration(w.durationMin.toLong()))
                    w.distanceKm?.let { add("${trimNum(it)} km") }
                    w.calories?.let { add("$it kcal") }
                    add(listOf("Easy", "Moderate", "Hard")[(w.intensity - 1).coerceIn(0, 2)])
                }
                Text(parts.joinToString("  ·  "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (item.exercises.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                item.exercises.sortedBy { it.position }.joinToString("\n") { e ->
                    "• ${e.name}  ${e.sets}×${e.reps}" + (e.weightKg?.let { "  @ ${trimNum(it)} kg" } ?: "")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (w.notes.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(w.notes, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun trimNum(v: Double): String = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()

private data class ExerciseDraft(val uid: Long, val name: String = "", val sets: String = "3", val reps: String = "10", val weight: String = "")

private fun String.toPosDouble(): Double? = replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }

@Composable
private fun WorkoutEditorSheet(
    existing: WorkoutWithExercises?,
    onDismiss: () -> Unit,
    onSave: (Workout, List<ExerciseSet>) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val w = existing?.workout
    var type by remember { mutableStateOf(w?.type ?: WorkoutType.STRENGTH) }
    var title by remember { mutableStateOf(w?.title ?: "") }
    var date by remember { mutableStateOf(w?.epochDay?.toLocalDate() ?: LocalDate.now()) }
    var duration by remember { mutableStateOf(w?.durationMin?.toString() ?: "45") }
    var calories by remember { mutableStateOf(w?.calories?.toString() ?: "") }
    var distance by remember { mutableStateOf(w?.distanceKm?.let { trimNum(it) } ?: "") }
    var intensity by remember { mutableIntStateOf(w?.intensity ?: 2) }
    var notes by remember { mutableStateOf(w?.notes ?: "") }
    var nextKey by remember { mutableStateOf(1000L) }
    var exercises by remember {
        mutableStateOf<List<ExerciseDraft>>(
            existing?.exercises?.sortedBy { it.position }?.mapIndexed { i, e ->
                ExerciseDraft(i.toLong(), e.name, e.sets.toString(), e.reps.toString(), e.weightKg?.let { trimNum(it) } ?: "")
            } ?: emptyList(),
        )
    }
    var confirmDelete by remember { mutableStateOf(false) }
    val durationValue = duration.toIntOrNull()
    val valid = durationValue != null && durationValue in 1..1440

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SheetTitle(if (existing == null) "Log workout" else "Edit workout") {
                if (existing != null) {
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                WorkoutType.entries.forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        label = { Text(t.label) },
                        leadingIcon = { Icon(t.icon, null, Modifier.size(FilterChipDefaults.IconSize)) },
                    )
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(60) },
                label = { Text("Title") },
                placeholder = { Text(type.label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            DateField("Date", date, { if (it != null) date = it })
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it.filter(Char::isDigit).take(4) },
                    label = { Text("Minutes") },
                    singleLine = true,
                    isError = !valid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it.filter(Char::isDigit).take(5) },
                    label = { Text("Calories") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                if (type.hasDistance) {
                    OutlinedTextField(
                        value = distance,
                        onValueChange = { v -> distance = v.filter { it.isDigit() || it == '.' || it == ',' }.take(6) },
                        label = { Text("Km") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Text("Intensity", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("Easy", "Moderate", "Hard").forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = intensity == i + 1,
                        onClick = { intensity = i + 1 },
                        shape = SegmentedButtonDefaults.itemShape(i, 3),
                    ) { Text(label) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Exercises", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = { exercises = exercises + ExerciseDraft(nextKey); nextKey++ }) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add exercise")
                }
            }
            exercises.forEach { ex -> key(ex.uid) {
                AppCard(color = MaterialTheme.colorScheme.surfaceContainerHigh, contentPadding = PaddingValues(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = ex.name,
                            onValueChange = { v -> exercises = exercises.map { if (it.uid == ex.uid) it.copy(name = v.take(40)) else it } },
                            label = { Text("Exercise") },
                            placeholder = { Text("e.g. Bench press") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { exercises = exercises.filter { it.uid != ex.uid } }) { Icon(Icons.Rounded.Close, "Remove") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallNumberField("Sets", ex.sets, Modifier.weight(1f)) { v -> exercises = exercises.map { if (it.uid == ex.uid) it.copy(sets = v) else it } }
                        SmallNumberField("Reps", ex.reps, Modifier.weight(1f)) { v -> exercises = exercises.map { if (it.uid == ex.uid) it.copy(reps = v) else it } }
                        SmallNumberField("Kg", ex.weight, Modifier.weight(1f), decimal = true) { v -> exercises = exercises.map { if (it.uid == ex.uid) it.copy(weight = v) else it } }
                    }
                }
            } }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it.take(500) },
                label = { Text("Notes") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                enabled = valid,
                onClick = {
                    val workout = Workout(
                        id = w?.id ?: 0,
                        epochDay = date.toEpochDay(),
                        type = type,
                        title = title.trim(),
                        durationMin = durationValue ?: 0,
                        calories = calories.toIntOrNull(),
                        distanceKm = if (type.hasDistance) distance.toPosDouble() else null,
                        intensity = intensity,
                        notes = notes.trim(),
                        createdAt = w?.createdAt ?: System.currentTimeMillis(),
                    )
                    val sets = exercises.filter { it.name.isNotBlank() }.map {
                        ExerciseSet(
                            workoutId = workout.id,
                            name = it.name.trim(),
                            sets = it.sets.toIntOrNull() ?: 1,
                            reps = it.reps.toIntOrNull() ?: 1,
                            weightKg = it.weight.toPosDouble(),
                        )
                    }
                    onSave(workout, sets)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(if (existing == null) "Save workout" else "Save changes") }
            if (existing == null) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        }
    }
    if (confirmDelete && w != null) {
        ConfirmDialog(
            title = "Delete workout?",
            message = "This workout and its exercises will be removed.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { onDelete(w.id) },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun SmallNumberField(label: String, value: String, modifier: Modifier, decimal: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() || (decimal && (it == '.' || it == ',')) }.take(6)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier,
    )
}
