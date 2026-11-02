package com.aio.tracker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aio.tracker.data.db.Prayer
import com.aio.tracker.data.db.PrayerStatus
import com.aio.tracker.data.db.TxnType
import com.aio.tracker.domain.PrayerSchedule
import com.aio.tracker.ui.Routes
import com.aio.tracker.ui.TopDestination
import com.aio.tracker.ui.appViewModel
import com.aio.tracker.ui.components.AppCard
import com.aio.tracker.ui.components.HeroCard
import com.aio.tracker.ui.components.IconBadge
import com.aio.tracker.ui.components.LinearMeter
import com.aio.tracker.ui.components.Pill
import com.aio.tracker.ui.components.SectionHeader
import com.aio.tracker.ui.components.StatTile
import com.aio.tracker.ui.components.rememberNow
import com.aio.tracker.ui.planner.noteColor
import com.aio.tracker.ui.planner.color
import com.aio.tracker.ui.salat.color
import com.aio.tracker.ui.salat.countdown
import com.aio.tracker.ui.salat.icon
import com.aio.tracker.ui.theme.AppTheme
import com.aio.tracker.util.Dates
import com.aio.tracker.util.Money
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

@Composable
fun HomeScreen(
    onOpen: (String) -> Unit,
    onOpenAi: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNote: (Long) -> Unit,
) {
    val vm = appViewModel { HomeViewModel(it) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val configs by vm.configs.collectAsStateWithLifecycle()
    val overrides by vm.overrides.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val todos by vm.todos.collectAsStateWithLifecycle()
    val workouts by vm.workouts.collectAsStateWithLifecycle()
    val txns by vm.txns.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val hasKey by vm.hasApiKey.collectAsStateWithLifecycle()
    val now by rememberNow()
    val today = now.toLocalDate()
    val replaces = settings.jumuahReplacesDhuhr
    val code = settings.currencyCode

    val slots = remember(today, configs, overrides, replaces) { PrayerSchedule.slotsFor(today, configs, overrides, replaces) }
    val logMap = remember(logs) { logs.associateBy { it.epochDay to it.prayer } }
    val next = remember(now, configs, overrides, replaces) { PrayerSchedule.next(now, configs, overrides, replaces) }
    val current = remember(now, configs, overrides, replaces) { PrayerSchedule.current(now, configs, overrides, replaces) }
    val todayEpoch = today.toEpochDay()
    val dueToday = remember(todos, todayEpoch) { todos.filter { it.dueEpochDay != null && it.dueEpochDay <= todayEpoch && (!it.done || it.dueEpochDay == todayEpoch) } }
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()
    val weekMinutes = remember(workouts, weekStart) { workouts.filter { it.workout.epochDay >= weekStart }.sumOf { it.workout.durationMin } }
    val monthStart = today.withDayOfMonth(1).toEpochDay()
    val monthTx = remember(txns, monthStart) { txns.filter { it.epochDay >= monthStart } }
    val income = monthTx.filter { it.type == TxnType.INCOME }.sumOf { it.amountMinor }
    val expense = monthTx.filter { it.type == TxnType.EXPENSE }.sumOf { it.amountMinor }
    val featuredNotes = remember(notes) { notes.take(8) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            Row(
                Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(Dates.long(today), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Assalamu alaikum" + if (settings.userName.isNotBlank()) ",\n${settings.userName}" else "",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
                FilledTonalIconButton(onClick = onOpenAi) { Icon(Icons.Rounded.AutoAwesome, "AI insights") }
                IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Settings, "Settings") }
            }
        }

        item(key = "prayer") {
            HeroCard(onClick = { onOpen(TopDestination.SALAT.route) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("NEXT PRAYER", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.75f))
                        Text(next?.prayer?.label ?: "—", style = MaterialTheme.typography.headlineLarge)
                        if (next != null) {
                            Text(
                                Dates.time(next.at.toLocalTime()) + if (next.date != today) " · " + Dates.relativeDay(next.date, today) else "",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.9f),
                            )
                        }
                    }
                    if (next != null) Pill(countdown(now, next.at), Color.White)
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    slots.forEach { s ->
                        val status = logMap[todayEpoch to s.prayer]?.status
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (status != null) status.color() else Color.White.copy(alpha = 0.14f))
                                    .then(if (next?.prayer == s.prayer && next.date == today) Modifier.border(2.dp, AppTheme.ext.gold, CircleShape) else Modifier),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    if (status != null && status != PrayerStatus.MISSED) Icons.Rounded.Check else s.prayer.icon,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(s.prayer.label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                        }
                    }
                }
                val pending = current?.takeIf { logMap[todayEpoch to it.prayer] == null }
                if (pending != null) {
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        onClick = { vm.markPrayer(today, pending.prayer, PrayerStatus.ON_TIME) },
                        shape = MaterialTheme.shapes.medium,
                        color = Color.White,
                        contentColor = AppTheme.ext.heroEnd,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("I prayed ${pending.prayer.label}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        item(key = "tiles") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    "Tasks today",
                    "${dueToday.count { it.done }}/${dueToday.size}",
                    Icons.Rounded.TaskAlt,
                    MaterialTheme.colorScheme.tertiary,
                    Modifier.weight(1f),
                    onClick = { onOpen(TopDestination.PLANNER.route) },
                )
                StatTile(
                    "Active · week",
                    "${weekMinutes}m",
                    Icons.Rounded.FitnessCenter,
                    MaterialTheme.colorScheme.primary,
                    Modifier.weight(1f),
                    sub = "goal ${settings.weeklyWorkoutGoalMin}m",
                    onClick = { onOpen(TopDestination.FITNESS.route) },
                )
                StatTile(
                    "Net · month",
                    Money.compact(income - expense, code),
                    Icons.Rounded.AccountBalanceWallet,
                    if (income - expense >= 0) AppTheme.ext.income else AppTheme.ext.expense,
                    Modifier.weight(1f),
                    onClick = { onOpen(TopDestination.MONEY.route) },
                )
            }
        }

        item(key = "ai") {
            val shape = MaterialTheme.shapes.large
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.secondaryContainer),
                        ),
                    )
                    .clickable(onClick = onOpenAi)
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.AutoAwesome, MaterialTheme.colorScheme.tertiary, size = 46.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("AI life summary", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Text(
                            if (hasKey) "Get insights on your salat, fitness, money and tasks with Gemini."
                            else "Add your Google AI Studio key to unlock personal insights.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }

        item(key = "tasksH") { SectionHeader("Due today", action = "See all", onAction = { onOpen(TopDestination.PLANNER.route) }) }
        item(key = "tasks") {
            AppCard(contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp)) {
                if (dueToday.isEmpty()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.TaskAlt, null, tint = AppTheme.ext.income)
                        Spacer(Modifier.width(10.dp))
                        Text("No tasks due today. Enjoy your day!", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    dueToday.sortedBy { it.done }.take(5).forEach { t ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { vm.toggleTodo(t) }) {
                                Icon(
                                    if (t.done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                    null,
                                    tint = if (t.done) AppTheme.ext.income else t.priority.color(),
                                )
                            }
                            Text(
                                t.title,
                                style = MaterialTheme.typography.bodyLarge,
                                textDecoration = if (t.done) TextDecoration.LineThrough else null,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (t.dueEpochDay != null && t.dueEpochDay < todayEpoch) {
                                Pill("Overdue", AppTheme.ext.expense, Modifier.padding(end = 8.dp))
                            }
                        }
                    }
                }
            }
        }

        item(key = "moneyH") { SectionHeader("This month", action = "Open", onAction = { onOpen(TopDestination.MONEY.route) }) }
        item(key = "money") {
            AppCard(onClick = { onOpen(TopDestination.MONEY.route) }) {
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("Income", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Money.format(income, code), style = MaterialTheme.typography.titleMedium, color = AppTheme.ext.income, maxLines = 1)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Expenses", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Money.format(expense, code), style = MaterialTheme.typography.titleMedium, color = AppTheme.ext.expense, maxLines = 1)
                    }
                }
                if (settings.monthlyBudgetMinor > 0) {
                    Spacer(Modifier.height(12.dp))
                    val frac = expense.toFloat() / settings.monthlyBudgetMinor
                    Text(
                        "${(frac * 100).toInt()}% of monthly budget used",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearMeter(frac, if (frac > 1f) AppTheme.ext.expense else MaterialTheme.colorScheme.primary)
                }
            }
        }

        item(key = "notesH") {
            SectionHeader("Notes", action = "New", onAction = { onOpenNote(Routes.NEW_NOTE_ID) })
        }
        item(key = "notes") {
            if (featuredNotes.isEmpty()) {
                AppCard(onClick = { onOpenNote(Routes.NEW_NOTE_ID) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("Write your first note", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(featuredNotes, key = { it.id }) { n ->
                        Surface(
                            onClick = { onOpenNote(n.id) },
                            shape = MaterialTheme.shapes.large,
                            color = noteColor(n.color),
                            modifier = Modifier.width(170.dp).height(120.dp),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row {
                                    Text(
                                        n.title.ifBlank { "Untitled" },
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (n.pinned) Icon(Icons.Rounded.PushPin, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(n.content, style = MaterialTheme.typography.bodySmall, maxLines = 4, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}
