package com.aio.tracker.ui.salat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aio.tracker.AppContainer
import com.aio.tracker.data.db.JumuahOverride
import com.aio.tracker.data.db.Prayer
import com.aio.tracker.data.db.PrayerConfig
import com.aio.tracker.data.db.PrayerLog
import com.aio.tracker.data.db.PrayerStatus
import com.aio.tracker.data.prefs.AppSettings
import com.aio.tracker.reminders.Notifications
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class SalatViewModel(private val c: AppContainer) : ViewModel() {
    private val dao = c.db.prayerDao()
    // Only used as the lower bound of the queries below. The upper bounds are open, so the screen keeps
    // working when the app stays in memory across midnight.
    private val today = LocalDate.now()

    val settings: StateFlow<AppSettings> = c.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val configs: StateFlow<Map<Prayer, PrayerConfig>> = dao.observeConfigs()
        .map { list -> list.associateBy { it.prayer } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val overrides: StateFlow<Map<Long, JumuahOverride>> = dao.observeOverridesFrom(today.minusDays(7).toEpochDay())
        .map { list -> list.associateBy { it.epochDay } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val logs: StateFlow<List<PrayerLog>> = dao.observeLogs(today.minusDays(HISTORY_DAYS).toEpochDay(), Long.MAX_VALUE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setStatus(date: LocalDate, prayer: Prayer, status: PrayerStatus?) = viewModelScope.launch {
        if (status == null) dao.deleteLog(date.toEpochDay(), prayer)
        else dao.upsertLog(PrayerLog(date.toEpochDay(), prayer, status))
        if (status != null && date == LocalDate.now()) Notifications.cancelPrayerReminder(c.appContext, prayer)
    }

    fun saveConfigs(list: List<PrayerConfig>) = viewModelScope.launch {
        dao.upsertConfigs(list)
        c.reschedulePrayerReminders()
    }

    fun updateConfig(config: PrayerConfig) = saveConfigs(listOf(config))

    fun setOverride(date: LocalDate, minuteOfDay: Int, note: String) = viewModelScope.launch {
        dao.upsertOverride(JumuahOverride(date.toEpochDay(), minuteOfDay, note.trim()))
        c.reschedulePrayerReminders()
    }

    fun clearOverride(date: LocalDate) = viewModelScope.launch {
        dao.deleteOverride(date.toEpochDay())
        c.reschedulePrayerReminders()
    }

    fun refreshReminders() = c.reschedulePrayerReminders()

    companion object {
        /** How far back prayer logs are loaded (for streaks and for browsing earlier weeks). */
        const val HISTORY_DAYS = 400L
    }
}
