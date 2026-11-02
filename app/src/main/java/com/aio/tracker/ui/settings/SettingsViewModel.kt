package com.aio.tracker.ui.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aio.tracker.AppContainer
import com.aio.tracker.data.ai.GeminiException
import com.aio.tracker.data.prefs.AppSettings
import com.aio.tracker.data.prefs.ThemeMode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val c: AppContainer) : ViewModel() {
    val settings: StateFlow<AppSettings> = c.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())
    val hasKey: StateFlow<Boolean> = c.secrets.hasApiKey

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    var keyHint by mutableStateOf(c.secrets.apiKeyHint)
        private set
    var busy by mutableStateOf(false)
        private set
    var models by mutableStateOf<List<String>>(emptyList())
        private set
    var modelsLoading by mutableStateOf(false)
        private set

    private fun say(msg: String) { _messages.trySend(msg) }

    fun setName(v: String) = viewModelScope.launch { c.settings.setUserName(v) }
    fun setTheme(v: ThemeMode) = viewModelScope.launch { c.settings.setThemeMode(v) }
    fun setDynamic(v: Boolean) = viewModelScope.launch { c.settings.setDynamicColor(v) }
    fun setCurrency(v: String) = viewModelScope.launch { c.settings.setCurrency(v) }
    fun setBudget(v: Long) = viewModelScope.launch { c.settings.setBudget(v) }
    fun setGoal(v: Int) = viewModelScope.launch { c.settings.setWorkoutGoal(v) }
    fun setModel(v: String) = viewModelScope.launch { c.settings.setModel(v) }
    fun setJumuah(v: Boolean) = viewModelScope.launch {
        c.settings.setJumuahReplacesDhuhr(v)
        c.reschedulePrayerReminders()
    }

    fun saveKey(raw: String) = viewModelScope.launch {
        val key = raw.trim()
        if (key.length < 20) {
            say("That doesn't look like a valid API key.")
            return@launch
        }
        busy = true
        try {
            c.secrets.saveApiKey(key)
            keyHint = c.secrets.apiKeyHint
            try {
                val list = c.gemini.listModels(key)
                models = list
                say("API key encrypted & verified ✓")
            } catch (e: GeminiException) {
                say("Key saved, but verification failed: ${e.message}")
            }
        } catch (e: Exception) {
            say("Could not save key: ${e.message}")
        } finally {
            busy = false
        }
    }

    fun removeKey() = viewModelScope.launch {
        c.secrets.clearApiKey()
        keyHint = null
        models = emptyList()
        say("API key removed from this device")
    }

    fun loadModels() = viewModelScope.launch {
        val key = c.secrets.readApiKey()
        if (key == null) {
            say("Add your API key first")
            return@launch
        }
        modelsLoading = true
        try {
            models = c.gemini.listModels(key)
            if (models.isEmpty()) say("No compatible models found for this key")
        } catch (e: GeminiException) {
            say(e.message ?: "Could not load models")
        } finally {
            modelsLoading = false
        }
    }

    fun exportBackup(resolver: ContentResolver, uri: Uri) = viewModelScope.launch {
        busy = true
        try {
            val n = c.backup.exportTo(resolver, uri)
            say("Exported $n items ✓")
        } catch (e: Exception) {
            say("Export failed: ${e.message}")
        } finally {
            busy = false
        }
    }

    fun importBackup(resolver: ContentResolver, uri: Uri) = viewModelScope.launch {
        busy = true
        try {
            val n = c.backup.importFrom(resolver, uri)
            c.reschedulePrayerReminders()
            say("Imported $n items ✓")
        } catch (e: kotlinx.serialization.SerializationException) {
            say("Import failed: the file is not a valid backup.")
        } catch (e: Exception) {
            say("Import failed: ${e.message}")
        } finally {
            busy = false
        }
    }

    fun wipe() = viewModelScope.launch {
        busy = true
        try {
            c.db.wipeAll()
            c.reschedulePrayerReminders()
            say("All data erased")
        } finally {
            busy = false
        }
    }
}
