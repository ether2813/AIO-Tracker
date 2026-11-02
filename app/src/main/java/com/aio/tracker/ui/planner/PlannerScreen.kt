package com.aio.tracker.ui.planner

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aio.tracker.data.db.Note
import com.aio.tracker.data.db.Priority
import com.aio.tracker.data.db.TodoItem
import com.aio.tracker.ui.Routes
import com.aio.tracker.ui.appViewModel
import com.aio.tracker.ui.components.AppCard
import com.aio.tracker.ui.components.ConfirmDialog
import com.aio.tracker.ui.components.DateField
import com.aio.tracker.ui.components.EmptyState
import com.aio.tracker.ui.components.LinearMeter
import com.aio.tracker.ui.components.Pill
import com.aio.tracker.ui.components.SheetTitle
import com.aio.tracker.ui.components.screenPadding
import com.aio.tracker.ui.theme.AppTheme
import com.aio.tracker.util.Dates
import com.aio.tracker.util.toLocalDate
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class TodoFilter(val label: String) { TODAY("Today"), UPCOMING("Upcoming"), ANYTIME("Anytime"), ALL("All"), DONE("Done") }

@Composable
fun Priority.color(): Color = when (this) {
    Priority.HIGH -> AppTheme.ext.expense
    Priority.MEDIUM -> AppTheme.ext.warning
    Priority.LOW -> MaterialTheme.colorScheme.tertiary
}

@Composable
fun noteColor(index: Int): Color {
    val c = AppTheme.ext.notePalette.getOrNull(index) ?: Color.Unspecified
    return if (c == Color.Unspecified) MaterialTheme.colorScheme.surfaceContainerLow else c
}

@Composable
fun PlannerScreen(onOpenNote: (Long) -> Unit) {
    val vm = appViewModel { PlannerViewModel(it) }
    val todosState by vm.todos.collectAsStateWithLifecycle()
    val notesState by vm.notes.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<TodoItem?>(null) }
    var creating by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            LargeTopAppBar(
                title = { Text(if (tab == 0) "To-Do" else "Notes") },
                actions = {
                    if (tab == 0 && todosState.orEmpty().any { it.done }) {
                        IconButton(onClick = { confirmClear = true }) { Icon(Icons.Rounded.DeleteSweep, "Clear completed") }
                    }
                },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (tab == 0) creating = true else onOpenNote(Routes.NEW_NOTE_ID) },
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text(if (tab == 0) "New task" else "New note") },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize()) {
            SingleChoiceSegmentedButtonRow(
                Modifier
                    .fillMaxWidth()
                    .padding(top = padding.calculateTopPadding())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                listOf("To-Do" to Icons.Rounded.TaskAlt, "Notes" to Icons.Rounded.Description).forEachIndexed { i, (label, icon) ->
                    SegmentedButton(
                        selected = tab == i,
                        onClick = { tab = i },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                        icon = { Icon(icon, null, Modifier.size(18.dp)) },
                    ) { Text(label) }
                }
            }
            val inner = PaddingValues(bottom = padding.calculateBottomPadding())
            if (tab == 0) {
                TodoList(
                    todos = todosState,
                    padding = inner,
                    onToggle = vm::toggle,
                    onOpen = { editing = it },
                    onQuickAdd = { vm.saveTodo(it) },
                    onDelete = { t ->
                        vm.deleteTodo(t)
                        scope.launch {
                            snackbar.currentSnackbarData?.dismiss()
                            val r = snackbar.showSnackbar("Task deleted", actionLabel = "Undo", duration = SnackbarDuration.Short)
                            if (r == SnackbarResult.ActionPerformed) vm.restoreTodo(t)
                        }
                    },
                )
            } else {
                NotesGrid(
                    notes = notesState,
                    padding = inner,
                    onOpen = { onOpenNote(it.id) },
                    onNew = { onOpenNote(Routes.NEW_NOTE_ID) },
                )
            }
        }
    }

    if (creating || editing != null) {
        TodoEditorSheet(
            existing = editing,
            onDismiss = { creating = false; editing = null },
            onSave = { vm.saveTodo(it); creating = false; editing = null },
            onDelete = { vm.deleteTodo(it); creating = false; editing = null },
        )
    }
    if (confirmClear) {
        ConfirmDialog(
            "Clear completed?",
            "All completed tasks will be removed.",
            "Clear",
            onConfirm = { vm.clearCompleted() },
            onDismiss = { confirmClear = false },
            destructive = true,
        )
    }
}

// ------------------------------------------------------------------ To-Do

private fun sortTodos(list: List<TodoItem>): List<TodoItem> = list.sortedWith(
    compareBy<TodoItem> { it.done }
        .thenBy { it.dueEpochDay ?: Long.MAX_VALUE }
        .thenByDescending { it.priority.ordinal }
        .thenByDescending { it.createdAt },
)

@Composable
private fun TodoList(
    todos: List<TodoItem>?,
    padding: PaddingValues,
    onToggle: (TodoItem) -> Unit,
    onOpen: (TodoItem) -> Unit,
    onQuickAdd: (TodoItem) -> Unit,
    onDelete: (TodoItem) -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf(TodoFilter.TODAY) }
    var quick by rememberSaveable { mutableStateOf("") }
    val today = LocalDate.now().toEpochDay()
    val all = todos.orEmpty()
    val filtered = remember(all, filter, today) {
        sortTodos(
            when (filter) {
                TodoFilter.TODAY -> all.filter { !it.done && it.dueEpochDay != null && it.dueEpochDay <= today } +
                    all.filter { it.done && it.dueEpochDay == today }
                TodoFilter.UPCOMING -> all.filter { !it.done && it.dueEpochDay != null && it.dueEpochDay > today }
                TodoFilter.ANYTIME -> all.filter { !it.done && it.dueEpochDay == null }
                TodoFilter.ALL -> all.filter { !it.done }
                TodoFilter.DONE -> all.filter { it.done }.sortedByDescending { it.completedAt ?: 0 }
            },
        )
    }
    val dueToday = all.filter { it.dueEpochDay == today }
    val doneToday = dueToday.count { it.done }

    fun submit() {
        val text = quick.trim()
        if (text.isEmpty()) return
        val due = when (filter) {
            TodoFilter.TODAY -> today
            TodoFilter.UPCOMING -> today + 1
            else -> null
        }
        onQuickAdd(TodoItem(title = text, dueEpochDay = due))
        quick = ""
    }

    LazyColumn(
        contentPadding = screenPadding(PaddingValues(top = 0.dp, bottom = padding.calculateBottomPadding()), bottomExtra = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "progress") {
            AppCard(color = MaterialTheme.colorScheme.primaryContainer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Today", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            if (dueToday.isEmpty()) "Nothing due today" else "$doneToday of ${dueToday.size} done",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    val overdue = all.count { !it.done && (it.dueEpochDay ?: Long.MAX_VALUE) < today }
                    if (overdue > 0) Pill("$overdue overdue", AppTheme.ext.expense)
                }
                if (dueToday.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    LinearMeter(doneToday.toFloat() / dueToday.size, MaterialTheme.colorScheme.primary)
                }
            }
        }
        item(key = "quick") {
            OutlinedTextField(
                value = quick,
                onValueChange = { quick = it.take(120) },
                placeholder = { Text("Add a task…") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                trailingIcon = {
                    IconButton(onClick = { submit() }, enabled = quick.isNotBlank()) {
                        Icon(Icons.AutoMirrored.Rounded.Send, "Add")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item(key = "filters") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TodoFilter.entries) { f ->
                    FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label) })
                }
            }
        }
        if (todos != null && filtered.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    Icons.Rounded.TaskAlt,
                    when (filter) {
                        TodoFilter.DONE -> "Nothing completed yet"
                        TodoFilter.TODAY -> "All clear for today"
                        else -> "No tasks here"
                    },
                    "Type above to add a task quickly, or tap \"New task\" for due dates and priority.",
                )
            }
        }
        items(filtered, key = { it.id }) { t ->
            SwipeableTodo(t, today, onToggle = { onToggle(t) }, onOpen = { onOpen(t) }, onDelete = { onDelete(t) })
        }
    }
}

@Composable
private fun SwipeableTodo(t: TodoItem, today: Long, onToggle: () -> Unit, onOpen: () -> Unit, onDelete: () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { v ->
            when (v) {
                SwipeToDismissBoxValue.StartToEnd -> { onToggle(); false }
                SwipeToDismissBoxValue.EndToStart -> { onDelete(); true }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
    )
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            val dir = state.dismissDirection
            val color by animateColorAsState(
                when (dir) {
                    SwipeToDismissBoxValue.StartToEnd -> AppTheme.ext.income
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                    else -> Color.Transparent
                },
                label = "swipe",
            )
            Box(
                Modifier.fillMaxSize().clip(MaterialTheme.shapes.large).background(color).padding(horizontal = 20.dp),
                contentAlignment = if (dir == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                if (dir == SwipeToDismissBoxValue.StartToEnd) Icon(Icons.Rounded.CheckCircle, "Complete", tint = Color.White)
                if (dir == SwipeToDismissBoxValue.EndToStart) Icon(Icons.Rounded.Delete, "Delete", tint = Color.White)
            }
        },
    ) {
        TodoRow(t, today, onToggle, onOpen)
    }
}

@Composable
private fun TodoRow(t: TodoItem, today: Long, onToggle: () -> Unit, onOpen: () -> Unit) {
    AppCard(onClick = onOpen, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggle) {
                Icon(
                    if (t.done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    if (t.done) "Mark not done" else "Mark done",
                    tint = if (t.done) AppTheme.ext.income else t.priority.color(),
                )
            }
            Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                Text(
                    t.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (t.done) TextDecoration.LineThrough else null,
                    color = if (t.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val due = t.dueEpochDay
                if (due != null || t.notes.isNotBlank() || t.priority == Priority.HIGH) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (due != null) {
                            val overdue = !t.done && due < today
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Event, null, Modifier.size(13.dp), tint = if (overdue) AppTheme.ext.expense else MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    Dates.relativeDay(due.toLocalDate()),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (overdue) AppTheme.ext.expense else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (t.priority == Priority.HIGH && !t.done) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Flag, null, Modifier.size(13.dp), tint = AppTheme.ext.expense)
                                Text("High", style = MaterialTheme.typography.labelMedium, color = AppTheme.ext.expense)
                            }
                        }
                        if (t.notes.isNotBlank()) {
                            Text(t.notes, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoEditorSheet(
    existing: TodoItem?,
    onDismiss: () -> Unit,
    onSave: (TodoItem) -> Unit,
    onDelete: (TodoItem) -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var due by remember { mutableStateOf(existing?.dueEpochDay?.toLocalDate() ?: if (existing == null) LocalDate.now() else null) }
    var priority by remember { mutableStateOf(existing?.priority ?: Priority.MEDIUM) }
    val today = LocalDate.now()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SheetTitle(if (existing == null) "New task" else "Edit task") {
                if (existing != null) IconButton(onClick = { onDelete(existing) }) { Icon(Icons.Rounded.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(120) },
                label = { Text("Task") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it.take(500) },
                label = { Text("Details") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Due", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = due == null, onClick = { due = null }, label = { Text("None") })
                FilterChip(selected = due == today, onClick = { due = today }, label = { Text("Today") })
                FilterChip(selected = due == today.plusDays(1), onClick = { due = today.plusDays(1) }, label = { Text("Tomorrow") })
            }
            DateField("Due date", due, { due = it }, allowClear = true, placeholder = "No due date")
            Text("Priority", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Priority.entries.forEachIndexed { i, p ->
                    SegmentedButton(
                        selected = priority == p,
                        onClick = { priority = p },
                        shape = SegmentedButtonDefaults.itemShape(i, Priority.entries.size),
                    ) { Text(p.label) }
                }
            }
            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    onSave(
                        (existing ?: TodoItem(title = "")).copy(
                            title = title.trim(),
                            notes = notes.trim(),
                            dueEpochDay = due?.toEpochDay(),
                            priority = priority,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Save") }
        }
    }
}

// ------------------------------------------------------------------ Notes

@Composable
private fun NotesGrid(
    notes: List<Note>?,
    padding: PaddingValues,
    onOpen: (Note) -> Unit,
    onNew: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val all = notes.orEmpty()
    val filtered = remember(all, query) {
        val q = query.trim()
        if (q.isEmpty()) all else all.filter { it.title.contains(q, true) || it.content.contains(q, true) }
    }
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(160.dp),
        contentPadding = screenPadding(PaddingValues(top = 0.dp, bottom = padding.calculateBottomPadding()), bottomExtra = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalItemSpacing = 10.dp,
    ) {
        item(key = "search", span = StaggeredGridItemSpan.FullLine) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search notes") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (notes != null && filtered.isEmpty()) {
            item(key = "empty", span = StaggeredGridItemSpan.FullLine) {
                if (query.isBlank()) {
                    EmptyState(
                        Icons.Rounded.Description,
                        "No notes yet",
                        "Capture ideas, duas, khutbah reflections, shopping lists… anything.",
                        actionLabel = "Write a note",
                        onAction = onNew,
                    )
                } else {
                    EmptyState(Icons.Rounded.Search, "No matches", "No notes contain \"$query\".")
                }
            }
        }
        items(filtered, key = { it.id }) { n -> NoteCard(n, onClick = { onOpen(n) }) }
    }
}

@Composable
private fun NoteCard(n: Note, onClick: () -> Unit) {
    val bg = noteColor(n.color)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = bg,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (n.color == 0) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), MaterialTheme.shapes.large)
                else Modifier,
            ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                if (n.title.isNotBlank()) {
                    Text(n.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 3, overflow = TextOverflow.Ellipsis)
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (n.pinned) Icon(Icons.Rounded.PushPin, "Pinned", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            }
            if (n.content.isNotBlank()) {
                if (n.title.isNotBlank()) Spacer(Modifier.height(6.dp))
                Text(n.content, style = MaterialTheme.typography.bodyMedium, maxLines = 10, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(8.dp))
            Text(Dates.dateTime(n.updatedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Small color swatch used by the note editor. */
@Composable
fun ColorSwatch(index: Int, selected: Boolean, onClick: () -> Unit) {
    val c = noteColor(index)
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(c)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    )
}
