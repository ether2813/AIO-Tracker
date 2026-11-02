package com.aio.tracker.ui.planner

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.aio.tracker.ui.appViewModel
import com.aio.tracker.ui.components.ConfirmDialog
import com.aio.tracker.ui.theme.AppTheme
import com.aio.tracker.util.Dates
import kotlinx.coroutines.delay

@Composable
fun NoteEditorScreen(noteId: Long, onBack: () -> Unit) {
    val vm = appViewModel { NoteEditorViewModel(it, noteId) }
    var showPalette by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var closed by remember { mutableStateOf(false) }
    val bg by animateColorAsState(noteColor(vm.color).takeIf { vm.color != 0 } ?: MaterialTheme.colorScheme.surface, label = "noteBg")

    fun close() {
        if (closed) return
        closed = true
        vm.save(final = true)
        onBack()
    }

    BackHandler { close() }
    // Debounced auto-save while typing.
    LaunchedEffect(vm.title, vm.content, vm.color, vm.pinned, vm.loaded) {
        delay(1200)
        vm.save(final = false)
    }
    DisposableEffect(Unit) { onDispose { if (!closed) vm.save(final = true) } }

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = { close() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { vm.pinned = !vm.pinned }) {
                        Icon(if (vm.pinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin, if (vm.pinned) "Unpin" else "Pin")
                    }
                    IconButton(onClick = { showPalette = !showPalette }) { Icon(Icons.Rounded.Palette, "Color") }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "Delete") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg, scrolledContainerColor = bg),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
        ) {
            if (showPalette) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AppTheme.ext.notePalette.indices.forEach { i ->
                        ColorSwatch(i, vm.color == i) { vm.color = i }
                    }
                }
            }
            val fieldColors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            )
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 4.dp)) {
                TextField(
                    value = vm.title,
                    onValueChange = { vm.title = it.take(200) },
                    placeholder = { Text("Title", style = MaterialTheme.typography.headlineSmall) },
                    textStyle = MaterialTheme.typography.headlineSmall,
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = vm.content,
                    onValueChange = { vm.content = it.take(100_000) },
                    placeholder = { Text("Start writing…") },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            vm.updatedAt?.let {
                Text(
                    "Edited " + Dates.dateTime(it),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().background(bg).padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete note?",
            message = "This note will be permanently deleted.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                closed = true
                vm.delete()
                onBack()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
