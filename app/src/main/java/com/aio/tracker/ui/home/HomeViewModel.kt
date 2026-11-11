package com.aio.tracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aio.tracker.AppContainer
import com.aio.tracker.data.db.JumuahOverride
import com.aio.tracker.data.db.Loan
import com.aio.tracker.data.db.MoneyTxn
import com.aio.tracker.data.db.Note
import com.aio.tracker.data.db.Prayer
import com.aio.tracker.data.db.PrayerConfig
import com.aio.tracker.data.db.PrayerLog
import com.aio.tracker.data.db.PrayerStatus
import com.aio.tracker.data.db.TodoItem
import com.aio.tracker.data.db.WorkoutWithExercises
import com.aio.tracker.data.prefs.AppSettings
import com.aio.tracker.reminders.Notifications
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(private val c: AppContainer) : ViewModel() {
    // Only used as the lower bound of the queries below. The upper bounds are open, so the screen keeps
    // working when the app stays in memory across midnight.
    private val today = LocalDate.now()
    private fun <T> kotlinx.coroutines.flow.Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)

    val settings: StateFlow<AppSettings> = c.settings.settings.state(AppSettings())
    val configs: StateFlow<Map<Prayer, PrayerConfig>> = c.db.prayerDao().observeConfigs().map { l -> l.associateBy { it.prayer } }.state(emptyMap())
    val overrides: StateFlow<Map<Long, JumuahOverride>> = c.db.prayerDao().observeOverridesFrom(today.toEpochDay()).map { l -> l.associateBy { it.epochDay } }.state(emptyMap())
    val logs: StateFlow<List<PrayerLog>> = c.db.prayerDao().observeLogs(today.minusDays(1).toEpochDay(), Long.MAX_VALUE).state(emptyList())
    val todos: StateFlow<List<TodoItem>> = c.db.todoDao().observeAll().state(emptyList())
    val workouts: StateFlow<List<WorkoutWithExercises>> = c.db.workoutDao().observeAll().state(emptyList())
    val txns: StateFlow<List<MoneyTxn>> = c.db.moneyDao().observeTxns().state(emptyList())
    val loans: StateFlow<List<Loan>> = c.db.moneyDao().observeLoans().state(emptyList())
    val notes: StateFlow<List<Note>> = c.db.noteDao().observeAll().state(emptyList())
    val hasApiKey: StateFlow<Boolean> = c.secrets.hasApiKey

    fun markPrayer(date: LocalDate, prayer: Prayer, status: PrayerStatus) = viewModelScope.launch {
        c.db.prayerDao().upsertLog(PrayerLog(date.toEpochDay(), prayer, status))
        if (date == LocalDate.now()) Notifications.cancelPrayerReminder(c.appContext, prayer)
    }

    fun toggleTodo(t: TodoItem) = viewModelScope.launch {
        c.db.todoDao().upsert(t.copy(done = !t.done, completedAt = if (!t.done) System.currentTimeMillis() else null))
    }
}
