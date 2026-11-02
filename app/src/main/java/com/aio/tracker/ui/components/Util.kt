package com.aio.tracker.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.LocalDateTime

/** Current time, refreshed on each [intervalMs] boundary. */
@Composable
fun rememberNow(intervalMs: Long = 30_000L): State<LocalDateTime> = produceState(LocalDateTime.now()) {
    while (true) {
        delay(intervalMs - (System.currentTimeMillis() % intervalMs) + 50)
        value = LocalDateTime.now()
    }
}

fun screenPadding(padding: PaddingValues, bottomExtra: Dp = 24.dp, horizontal: Dp = 16.dp): PaddingValues = PaddingValues(
    start = horizontal,
    end = horizontal,
    top = padding.calculateTopPadding() + 8.dp,
    bottom = padding.calculateBottomPadding() + bottomExtra,
)
