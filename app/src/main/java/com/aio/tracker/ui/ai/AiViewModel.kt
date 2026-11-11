package com.aio.tracker.ui.ai

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aio.tracker.AppContainer
import com.aio.tracker.data.ai.GeminiException
import com.aio.tracker.data.db.AiSummary
import com.aio.tracker.domain.AiPeriod
import com.aio.tracker.domain.AiScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AiState {
    data object Idle : AiState
    data object Loading : AiState
    data class Done(val summary: AiSummary) : AiState
    data class Error(val message: String) : AiState
}

class AiViewModel(private val c: AppContainer) : ViewModel() {
    val hasKey: StateFlow<Boolean> = c.secrets.hasApiKey
    val history: StateFlow<List<AiSummary>> = c.db.aiDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var state by mutableStateOf<AiState>(AiState.Idle)
        private set
    var scope by mutableStateOf(AiScope.EVERYTHING)
    var period by mutableStateOf(AiPeriod.MONTH)
    var question by mutableStateOf("")

    private var job: Job? = null

    fun generate() {
        if (state == AiState.Loading) return
        val scope = this.scope
        val period = this.period
        val q = question.trim()
        // Runs in the app scope: a request can take a while, and leaving this screen should not throw
        // the answer away. It is still saved to History, where it shows up when the user comes back.
        job = c.appScope.launch(Dispatchers.Main.immediate) {
            state = AiState.Loading
            state = try {
                val key = c.secrets.readApiKey() ?: throw GeminiException("No API key saved. Add it in Settings.")
                val settings = c.settings.current()
                val digest = c.insights.build(scope, period, settings)
                val prompt = buildString {
                    appendLine(
                        if (q.isNotEmpty()) "My question: $q\nAnswer it using my data below."
                        else "Please give me a summary of my ${scope.label.lowercase()} for the last ${period.label}.",
                    )
                    appendLine()
                    appendLine("=== MY DATA ===")
                    append(digest)
                }
                val text = c.gemini.generate(key, settings.geminiModel, c.insights.systemPrompt, prompt)
                val summary = AiSummary(
                    scope = scope.label,
                    periodLabel = period.label,
                    question = q,
                    content = text,
                    model = settings.geminiModel,
                )
                val id = c.db.aiDao().insert(summary)
                AiState.Done(summary.copy(id = id))
            } catch (e: GeminiException) {
                AiState.Error(e.message ?: "Something went wrong")
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                AiState.Error("Unexpected error: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    fun cancel() {
        job?.cancel()
        state = AiState.Idle
    }

    fun show(summary: AiSummary) { state = AiState.Done(summary) }
    fun delete(summary: AiSummary) = viewModelScope.launch {
        c.db.aiDao().delete(summary.id)
        val s = state
        if (s is AiState.Done && s.summary.id == summary.id) state = AiState.Idle
    }
}
