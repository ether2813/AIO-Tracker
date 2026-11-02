package com.aio.tracker.ui.planner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aio.tracker.AppContainer
import com.aio.tracker.data.db.Note
import com.aio.tracker.data.db.TodoItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PlannerViewModel(private val c: AppContainer) : ViewModel() {
    private val todoDao = c.db.todoDao()
    private val noteDao = c.db.noteDao()

    val todos: StateFlow<List<TodoItem>?> = todoDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val notes: StateFlow<List<Note>?> = noteDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveTodo(t: TodoItem) = viewModelScope.launch { todoDao.upsert(t) }
    fun toggle(t: TodoItem) = viewModelScope.launch {
        todoDao.upsert(t.copy(done = !t.done, completedAt = if (!t.done) System.currentTimeMillis() else null))
    }
    fun deleteTodo(t: TodoItem) = viewModelScope.launch { todoDao.delete(t) }
    fun restoreTodo(t: TodoItem) = viewModelScope.launch { todoDao.upsert(t) }
    fun clearCompleted() = viewModelScope.launch { todoDao.clearCompleted() }

    fun togglePin(n: Note) = viewModelScope.launch { noteDao.insert(n.copy(pinned = !n.pinned)) }
    fun deleteNote(n: Note) = viewModelScope.launch { noteDao.delete(n.id) }
    fun restoreNote(n: Note) = viewModelScope.launch { noteDao.insert(n) }
}

/** Holds one note being edited and auto-saves it safely (no duplicate inserts). */
class NoteEditorViewModel(private val c: AppContainer, initialId: Long) : ViewModel() {
    private val dao = c.db.noteDao()
    private val mutex = Mutex()
    private var id: Long = if (initialId > 0) initialId else 0L
    private var createdAt = System.currentTimeMillis()
    private var lastSaved: List<Any> = listOf("", "", 0, false)
    private var deleted = false

    var title by mutableStateOf("")
    var content by mutableStateOf("")
    var color by mutableIntStateOf(0)
    var pinned by mutableStateOf(false)
    var loaded by mutableStateOf(initialId <= 0)
        private set
    var updatedAt by mutableStateOf<Long?>(null)
        private set

    init {
        if (initialId > 0) {
            viewModelScope.launch {
                dao.get(initialId)?.let { n ->
                    title = n.title
                    content = n.content
                    color = n.color
                    pinned = n.pinned
                    createdAt = n.createdAt
                    updatedAt = n.updatedAt
                    lastSaved = listOf(n.title, n.content, n.color, n.pinned)
                }
                loaded = true
            }
        }
    }

    val isNew: Boolean get() = id == 0L

    fun save(final: Boolean) {
        if (!loaded) return
        val snapshot = listOf<Any>(title, content, color, pinned)
        c.appScope.launch {
            mutex.withLock {
                if (deleted) return@withLock
                val t = snapshot[0] as String
                val body = snapshot[1] as String
                if (t.isBlank() && body.isBlank()) {
                    if (final && id != 0L) {
                        dao.delete(id)
                        id = 0L
                    }
                    return@withLock
                }
                if (snapshot == lastSaved && id != 0L) return@withLock
                val now = System.currentTimeMillis()
                val newId = dao.insert(
                    Note(
                        id = id,
                        title = t.trim(),
                        content = body,
                        color = snapshot[2] as Int,
                        pinned = snapshot[3] as Boolean,
                        createdAt = createdAt,
                        updatedAt = now,
                    ),
                )
                if (id == 0L) id = newId
                lastSaved = snapshot
                updatedAt = now
            }
        }
    }

    fun delete() {
        c.appScope.launch {
            mutex.withLock {
                deleted = true
                if (id != 0L) dao.delete(id)
            }
        }
    }
}
