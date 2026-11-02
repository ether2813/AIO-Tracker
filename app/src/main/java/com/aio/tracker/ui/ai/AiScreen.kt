package com.aio.tracker.ui.ai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aio.tracker.data.db.AiSummary
import com.aio.tracker.domain.AiPeriod
import com.aio.tracker.domain.AiScope
import com.aio.tracker.ui.appViewModel
import com.aio.tracker.ui.components.AppCard
import com.aio.tracker.ui.components.EmptyState
import com.aio.tracker.ui.components.HeroCard
import com.aio.tracker.ui.components.IconBadge
import com.aio.tracker.ui.components.MarkdownText
import com.aio.tracker.ui.components.SectionHeader
import com.aio.tracker.ui.components.screenPadding
import com.aio.tracker.util.Dates

@Composable
fun AiScreen(onBack: () -> Unit, onOpenSettings: () -> Unit) {
    val vm = appViewModel { AiViewModel(it) }
    val hasKey by vm.hasKey.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Insights") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(contentPadding = screenPadding(padding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "intro") {
                HeroCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(28.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Your life, summarised", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Gemini reads a compact digest of your salat, workouts, money, tasks and notes and gives you honest, kind insights.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
            }
            if (!hasKey) {
                item(key = "nokey") {
                    AppCard(color = MaterialTheme.colorScheme.secondaryContainer) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Rounded.VpnKey, MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Add your API key", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text(
                                    "Get a free key at aistudio.google.com, then paste it in Settings. It's encrypted on this device and never shown again.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = onOpenSettings) { Text("Open settings") }
                    }
                }
            }
            item(key = "controls") {
                AppCard {
                    Text("Focus", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AiScope.entries) { s ->
                            FilterChip(selected = vm.scope == s, onClick = { vm.scope = s }, label = { Text(s.label) })
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Period", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        AiPeriod.entries.forEachIndexed { i, p ->
                            SegmentedButton(
                                selected = vm.period == p,
                                onClick = { vm.period = p },
                                shape = SegmentedButtonDefaults.itemShape(i, AiPeriod.entries.size),
                            ) { Text(p.label) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = vm.question,
                        onValueChange = { vm.question = it.take(500) },
                        label = { Text("Ask something (optional)") },
                        placeholder = { Text("e.g. How can I save more this month?") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                    )
                    Spacer(Modifier.height(12.dp))
                    val loading = vm.state == AiState.Loading
                    Button(
                        onClick = { vm.generate() },
                        enabled = hasKey && !loading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (loading) "Thinking…" else if (vm.question.isBlank()) "Generate summary" else "Ask Gemini")
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Only a summary of your data is sent to Google's Gemini API, over HTTPS.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item(key = "result") {
                AnimatedContent(targetState = vm.state, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) }, label = "ai") { st ->
                    when (st) {
                        AiState.Idle -> Spacer(Modifier.height(1.dp))
                        AiState.Loading -> LoadingCard(onCancel = vm::cancel)
                        is AiState.Error -> AppCard(color = MaterialTheme.colorScheme.errorContainer) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(Modifier.width(10.dp))
                                Text(st.message, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        is AiState.Done -> SummaryCard(st.summary, onCopy = { copy(context, st.summary.content) }, onDelete = { vm.delete(st.summary) })
                    }
                }
            }
            item(key = "historyH") { SectionHeader("History") }
            if (history.isEmpty()) {
                item(key = "historyEmpty") {
                    EmptyState(Icons.Rounded.History, "No summaries yet", "Your generated summaries are saved here on your device.")
                }
            }
            items(history, key = { it.id }) { s ->
                AppCard(onClick = { vm.show(s) }, contentPadding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.AutoAwesome, MaterialTheme.colorScheme.tertiary, size = 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                s.question.ifBlank { "${s.scope} · ${s.periodLabel}" },
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(Dates.dateTime(s.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { vm.delete(s) }) { Icon(Icons.Rounded.Delete, "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingCard(onCancel: () -> Unit) {
    val t = rememberInfiniteTransition(label = "shimmer")
    val a by t.animateFloat(0.25f, 0.65f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alpha")
    AppCard {
        Text("Analysing your data…", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(12.dp))
        listOf(1f, 0.9f, 0.95f, 0.6f).forEach { w ->
            Box(
                Modifier
                    .fillMaxWidth(w)
                    .height(12.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = a * 0.5f)),
            )
            Spacer(Modifier.height(10.dp))
        }
        OutlinedButton(onClick = onCancel) { Text("Cancel") }
    }
}

@Composable
private fun SummaryCard(summary: AiSummary, onCopy: () -> Unit, onDelete: () -> Unit) {
    AppCard(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(summary.question.ifBlank { "${summary.scope} · ${summary.periodLabel}" }, style = MaterialTheme.typography.titleMedium)
                Text(
                    Dates.dateTime(summary.createdAt) + " · " + summary.model,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onCopy) { Icon(Icons.Rounded.ContentCopy, "Copy") }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, "Delete") }
        }
        Spacer(Modifier.height(8.dp))
        MarkdownText(summary.content)
    }
}

private fun copy(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText("AI summary", text))
    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}
