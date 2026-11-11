package com.aio.tracker.ui.salat

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aio.tracker.data.db.JumuahOverride
import com.aio.tracker.data.db.Prayer
import com.aio.tracker.data.db.PrayerConfig
import com.aio.tracker.data.db.PrayerLog
import com.aio.tracker.data.db.PrayerStatus
import com.aio.tracker.domain.PrayerSchedule
import com.aio.tracker.reminders.Notifications
import com.aio.tracker.reminders.PrayerScheduler
import com.aio.tracker.ui.appViewModel
import com.aio.tracker.ui.components.AppCard
import com.aio.tracker.ui.components.AppTimePickerDialog
import com.aio.tracker.ui.components.HeroCard
import com.aio.tracker.ui.components.IconBadge
import com.aio.tracker.ui.components.LegendRow
import com.aio.tracker.ui.components.LinearMeter
import com.aio.tracker.ui.components.Pill
import com.aio.tracker.ui.components.ProgressRing
import com.aio.tracker.ui.components.SectionHeader
import com.aio.tracker.ui.components.SheetTitle
import com.aio.tracker.ui.components.TimeField
import com.aio.tracker.ui.components.contentColorOn
import com.aio.tracker.ui.components.rememberNow
import com.aio.tracker.ui.components.screenPadding
import com.aio.tracker.ui.theme.AppTheme
import com.aio.tracker.util.Dates
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

@Composable
fun SalatScreen(onOpenSettings: () -> Unit) {
    val vm = appViewModel { SalatViewModel(it) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val configs by vm.configs.collectAsStateWithLifecycle()
    val overrides by vm.overrides.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val now by rememberNow()
    val today = now.toLocalDate()
    var selectedDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    val selectedDate = LocalDate.ofEpochDay(selectedDay)
    // 0 = the 7 days ending today, 1 = the 7 days before that, ...
    var weekOffset by rememberSaveable { mutableIntStateOf(0) }
    var lastToday by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    val todayEpoch = today.toEpochDay()
    LaunchedEffect(todayEpoch) {
        if (lastToday != todayEpoch) {
            // A new day started while this screen was kept in memory. Follow "today", so prayers are not
            // logged on yesterday by accident, unless an older day was selected on purpose.
            if (selectedDay == lastToday) {
                selectedDay = todayEpoch
                weekOffset = 0
            }
            lastToday = todayEpoch
        }
    }
    val stripEnd = today.minusWeeks(weekOffset.toLong())
    fun showWeek(offset: Int) {
        weekOffset = offset.coerceIn(0, MAX_WEEKS_BACK)
        selectedDay = today.minusWeeks(weekOffset.toLong()).toEpochDay()
    }
    var showTimes by remember { mutableStateOf(false) }
    var editFriday by remember { mutableStateOf<LocalDate?>(null) }
    var editPrayerTime by remember { mutableStateOf<Prayer?>(null) }
    val replaces = settings.jumuahReplacesDhuhr
    val logMap = remember(logs) { logs.associateBy { it.epochDay to it.prayer } }
    val slots = remember(selectedDate, configs, overrides, replaces) {
        PrayerSchedule.slotsFor(selectedDate, configs, overrides, replaces)
    }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Salat") },
                actions = {
                    IconButton(onClick = { showTimes = true }) { Icon(Icons.Rounded.Schedule, "Edit prayer times") }
                },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = screenPadding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "hero") {
                NextPrayerHero(now, configs, overrides, replaces, logMap, onEditTimes = { showTimes = true })
            }
            item(key = "perm") { ReminderPermissionBanner(onChanged = vm::refreshReminders) }
            item(key = "weekNav") {
                WeekNav(
                    start = stripEnd.minusDays(6),
                    end = stripEnd,
                    isCurrent = weekOffset == 0,
                    canGoBack = weekOffset < MAX_WEEKS_BACK,
                    onPrev = { showWeek(weekOffset + 1) },
                    onNext = { showWeek(weekOffset - 1) },
                    onToday = { showWeek(0) },
                )
            }
            item(key = "week") {
                WeekStrip(
                    end = stripEnd,
                    selected = selectedDate,
                    logMap = logMap,
                    replaces = replaces,
                    onSelect = { selectedDay = it.toEpochDay() },
                )
            }
            item(key = "dayTitle") {
                SectionHeader(
                    when {
                        selectedDate == today -> "Today's prayers"
                        selectedDate.year == today.year -> Dates.long(selectedDate)
                        else -> Dates.day(selectedDate)
                    },
                )
            }
            items(slots, key = { "slot-" + it.prayer.name }) { slot ->
                PrayerRow(
                    slot = slot,
                    config = configs[slot.prayer],
                    status = logMap[selectedDay to slot.prayer]?.status,
                    isUpcoming = slot.at.isAfter(now),
                    isOverride = slot.prayer == Prayer.JUMUAH && overrides.containsKey(selectedDay),
                    onStatus = { vm.setStatus(selectedDate, slot.prayer, it) },
                    onToggleReminder = { cfg -> vm.updateConfig(cfg.copy(reminderEnabled = !cfg.reminderEnabled)) },
                    onEditTime = {
                        if (slot.prayer == Prayer.JUMUAH) editFriday = selectedDate else editPrayerTime = slot.prayer
                    },
                )
            }
            item(key = "jumuah") {
                JumuahCard(
                    today = today,
                    enabled = replaces,
                    defaultMinute = configs[Prayer.JUMUAH]?.minuteOfDay ?: PrayerSchedule.defaultMinute(Prayer.JUMUAH),
                    overrides = overrides,
                    onEditFriday = { editFriday = it },
                    onEditDefault = { editPrayerTime = Prayer.JUMUAH },
                    onOpenSettings = onOpenSettings,
                )
            }
            item(key = "stats") { SalatStats(today, logs, replaces) }
        }
    }

    if (showTimes) {
        PrayerTimesSheet(
            configs = configs,
            showJumuah = replaces,
            onDismiss = { showTimes = false },
            onSave = { vm.saveConfigs(it); showTimes = false },
        )
    }
    editPrayerTime?.let { p ->
        val cfg = configs[p] ?: PrayerConfig(p, PrayerSchedule.defaultMinute(p))
        AppTimePickerDialog(
            initialMinuteOfDay = cfg.minuteOfDay,
            title = if (p == Prayer.JUMUAH) "Default Jumu'ah time" else "${p.label} time",
            onDismiss = { editPrayerTime = null },
            onConfirm = { vm.updateConfig(cfg.copy(minuteOfDay = it)) },
        )
    }
    editFriday?.let { friday ->
        FridayDialog(
            date = friday,
            override = overrides[friday.toEpochDay()],
            defaultMinute = configs[Prayer.JUMUAH]?.minuteOfDay ?: PrayerSchedule.defaultMinute(Prayer.JUMUAH),
            onDismiss = { editFriday = null },
            onSave = { minute, note -> vm.setOverride(friday, minute, note); editFriday = null },
            onReset = { vm.clearOverride(friday); editFriday = null },
        )
    }
}

@Composable
private fun NextPrayerHero(
    now: LocalDateTime,
    configs: Map<Prayer, PrayerConfig>,
    overrides: Map<Long, JumuahOverride>,
    replaces: Boolean,
    logMap: Map<Pair<Long, Prayer>, PrayerLog>,
    onEditTimes: () -> Unit,
) {
    val next = remember(now, configs, overrides, replaces) { PrayerSchedule.next(now, configs, overrides, replaces) }
    val todaySlots = remember(now.toLocalDate(), configs, overrides, replaces) {
        PrayerSchedule.slotsFor(now.toLocalDate(), configs, overrides, replaces)
    }
    val done = todaySlots.count { logMap[it.date.toEpochDay() to it.prayer]?.status?.counts == true }
    HeroCard(onClick = onEditTimes) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("NEXT PRAYER", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.75f))
                Spacer(Modifier.height(4.dp))
                if (next != null) {
                    Text(next.prayer.label, style = MaterialTheme.typography.displaySmall)
                    Text(
                        "${next.prayer.arabic}  ·  ${Dates.time(next.at.toLocalTime())}" +
                            if (next.date != now.toLocalDate()) "  ·  ${Dates.relativeDay(next.date, now.toLocalDate())}" else "",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                    Spacer(Modifier.height(10.dp))
                    Pill(countdown(now, next.at), Color.White, icon = Icons.Rounded.Alarm)
                } else {
                    Text("Set your times", style = MaterialTheme.typography.headlineSmall)
                }
            }
            ProgressRing(
                progress = if (todaySlots.isEmpty()) 0f else done.toFloat() / todaySlots.size,
                size = 84.dp,
                stroke = 9.dp,
                color = AppTheme.ext.gold,
                trackColor = Color.White.copy(alpha = 0.18f),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$done/${todaySlots.size}", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text("today", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
                }
            }
        }
    }
}

fun countdown(now: LocalDateTime, at: LocalDateTime): String {
    val mins = Duration.between(now, at).toMinutes().coerceAtLeast(0)
    return if (mins < 1) "Now" else "in " + Dates.duration(mins)
}

@Composable
private fun ReminderPermissionBanner(onChanged: () -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++; onChanged() }
    val canNotify = remember(refresh) { Notifications.canPost(context) }
    val canExact = remember(refresh) { PrayerScheduler.canScheduleExact(context) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        refresh++
        if (!granted) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
        onChanged()
    }
    if (canNotify && canExact) return
    AppCard(color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.NotificationsActive, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Turn on prayer reminders", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(
                    when {
                        !canNotify -> "Allow notifications so the app can remind you before each salat."
                        else -> "Allow \"Alarms & reminders\" so reminders arrive exactly on time."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!canNotify && Build.VERSION.SDK_INT >= 33) {
                Button(onClick = { launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS) }) { Text("Allow notifications") }
            }
            if (!canExact && Build.VERSION.SDK_INT >= 31) {
                FilledTonalButton(onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                }) { Text("Exact alarms") }
            }
        }
    }
}

/** Browsing is limited to what [SalatViewModel.HISTORY_DAYS] keeps loaded. */
private const val MAX_WEEKS_BACK = 52

/** Lets the user step back through earlier weeks to review or catch up on logs. */
@Composable
private fun WeekNav(
    start: LocalDate,
    end: LocalDate,
    isCurrent: Boolean,
    canGoBack: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev, enabled = canGoBack) { Icon(Icons.Rounded.ChevronLeft, "Earlier days") }
        Text(
            if (isCurrent) "Last 7 days" else Dates.day(start) + " – " + Dates.day(end),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        if (!isCurrent) TextButton(onClick = onToday) { Text("Today") }
        IconButton(onClick = onNext, enabled = !isCurrent) { Icon(Icons.Rounded.ChevronRight, "Later days") }
    }
}

@Composable
private fun WeekStrip(
    end: LocalDate,
    selected: LocalDate,
    logMap: Map<Pair<Long, Prayer>, PrayerLog>,
    replaces: Boolean,
    onSelect: (LocalDate) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 6 downTo 0) {
            val d = end.minusDays(i.toLong())
            val prayers = PrayerSchedule.prayersFor(d, replaces)
            val done = prayers.count { logMap[d.toEpochDay() to it]?.status?.counts == true }
            val isSel = d == selected
            val bg by animateColorAsState(
                if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
                label = "day",
            )
            val fg = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            Column(
                Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(bg)
                    .clickable { onSelect(d) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = fg.copy(alpha = 0.8f),
                )
                Text("${d.dayOfMonth}", style = MaterialTheme.typography.titleMedium, color = fg)
                Spacer(Modifier.height(6.dp))
                ProgressRing(
                    progress = done.toFloat() / prayers.size,
                    size = 18.dp,
                    stroke = 3.dp,
                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else AppTheme.ext.income,
                )
            }
        }
    }
}

@Composable
private fun PrayerRow(
    slot: PrayerSchedule.Slot,
    config: PrayerConfig?,
    status: PrayerStatus?,
    isUpcoming: Boolean,
    isOverride: Boolean,
    onStatus: (PrayerStatus?) -> Unit,
    onToggleReminder: (PrayerConfig) -> Unit,
    onEditTime: () -> Unit,
) {
    val tint = prayerTint(slot.prayer)
    AppCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(slot.prayer.icon, tint, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(slot.prayer.label, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(slot.prayer.arabic, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(
                    Modifier.clip(MaterialTheme.shapes.small).clickable(onClick = onEditTime).padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(Dates.time(slot.at.toLocalTime()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Rounded.Edit, "Edit time", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    if (isOverride) {
                        Spacer(Modifier.width(6.dp))
                        Pill("This Friday", AppTheme.ext.gold)
                    }
                    if (isUpcoming && status == null) {
                        Spacer(Modifier.width(6.dp))
                        Text("upcoming", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (config != null) {
                IconButton(onClick = { onToggleReminder(config) }) {
                    Icon(
                        if (config.reminderEnabled) Icons.Rounded.Notifications else Icons.Rounded.NotificationsOff,
                        if (config.reminderEnabled) "Reminder on" else "Reminder off",
                        tint = if (config.reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PrayerStatus.entries.forEach { s ->
                StatusChip(
                    label = when (s) {
                        PrayerStatus.JAMAAH -> "Jama'ah"
                        PrayerStatus.ON_TIME -> "On time"
                        PrayerStatus.LATE -> "Late"
                        PrayerStatus.MISSED -> "Missed"
                    },
                    color = s.color(),
                    selected = status == s,
                    onClick = { onStatus(if (status == s) null else s) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, color: Color, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(if (selected) color else MaterialTheme.colorScheme.surfaceContainerHighest, label = "chip")
    val fg = if (selected) contentColorOn(color) else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(onClick = onClick, modifier = modifier.height(38.dp), shape = MaterialTheme.shapes.small, color = bg) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(Icons.Rounded.Check, null, tint = fg, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(2.dp))
            }
            Text(label, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun JumuahCard(
    today: LocalDate,
    enabled: Boolean,
    defaultMinute: Int,
    overrides: Map<Long, JumuahOverride>,
    onEditFriday: (LocalDate) -> Unit,
    onEditDefault: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    SectionHeader("Jumu'ah")
    AppCard {
        if (!enabled) {
            Text("Jumu'ah tracking is turned off. Fridays use Dhuhr instead.", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onOpenSettings) { Text("Open settings") }
        } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Prayer.JUMUAH.icon, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Default time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Dates.time(defaultMinute), style = MaterialTheme.typography.titleLarge)
            }
            FilledTonalButton(onClick = onEditDefault) { Text("Change") }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Tap a Friday to set the time announced by your mosque for that day.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        val firstFriday = today.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.FRIDAY))
        for (i in 0 until 6) {
            val friday = firstFriday.plusWeeks(i.toLong())
            val ov = overrides[friday.toEpochDay()]
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onEditFriday(friday) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(Dates.relativeDay(friday, today), style = MaterialTheme.typography.bodyLarge)
                    if (ov != null && ov.note.isNotBlank()) {
                        Text(ov.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
                if (ov != null) {
                    Pill("Custom", AppTheme.ext.gold)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    Dates.time(ov?.minuteOfDay ?: defaultMinute),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (ov != null) AppTheme.ext.gold else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        }
    }
}

@Composable
private fun SalatStats(today: LocalDate, logs: List<PrayerLog>, replaces: Boolean) {
    val week = remember(logs, today, replaces) { PrayerSchedule.completion(today.minusDays(6), today, logs, replaces) }
    val month = remember(logs, today, replaces) { PrayerSchedule.completion(today.minusDays(29), today, logs, replaces) }
    val streak = remember(logs, today, replaces) { PrayerSchedule.streak(today, logs, replaces) }
    val from30 = today.minusDays(29).toEpochDay()
    val recent = remember(logs, today) { logs.filter { it.epochDay in from30..today.toEpochDay() } }
    val counts = remember(recent) { PrayerSchedule.statusCounts(recent) }

    SectionHeader("Your progress")
    AppCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatRing("7 days", week, AppTheme.ext.income)
            StatRing("30 days", month, MaterialTheme.colorScheme.tertiary)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.LocalFireDepartment, null, tint = AppTheme.ext.warning, modifier = Modifier.size(28.dp))
                        Text("$streak", style = MaterialTheme.typography.titleLarge)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Day streak", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Last 30 days", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        PrayerStatus.entries.forEach { s -> LegendRow(s.color(), s.label, "${counts[s] ?: 0}") }
        Spacer(Modifier.height(12.dp))
        val perPrayer = Prayer.entries.filter { it != Prayer.JUMUAH || replaces }
        perPrayer.forEach { p ->
            var expected = 0
            var done = 0
            var d = today.minusDays(29)
            while (!d.isAfter(today)) {
                if (p in PrayerSchedule.prayersFor(d, replaces)) {
                    expected++
                    if (recent.any { it.epochDay == d.toEpochDay() && it.prayer == p && it.status.counts }) done++
                }
                d = d.plusDays(1)
            }
            val frac = if (expected == 0) 0f else done.toFloat() / expected
            Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(p.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(76.dp))
                LinearMeter(frac, prayerTint(p), Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                Text("${(frac * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
            }
        }
    }
}

@Composable
private fun StatRing(label: String, value: Float, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ProgressRing(value, size = 72.dp, color = color) {
            Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val OFFSETS = listOf(0, 5, 10, 15, 20, 30, 45, 60)

@Composable
private fun PrayerTimesSheet(
    configs: Map<Prayer, PrayerConfig>,
    showJumuah: Boolean,
    onDismiss: () -> Unit,
    onSave: (List<PrayerConfig>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var drafts by remember {
        mutableStateOf(Prayer.entries.associateWith { p -> configs[p] ?: PrayerConfig(p, PrayerSchedule.defaultMinute(p)) })
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            SheetTitle("Local prayer times")
            Text(
                "Enter the times of your local mosque. Update them whenever the timetable changes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Prayer.entries.filter { it != Prayer.JUMUAH || showJumuah }.forEach { p ->
                val cfg = drafts.getValue(p)
                AppCard(color = MaterialTheme.colorScheme.surfaceContainerHigh, contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(p.icon, prayerTint(p), size = 36.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(if (p == Prayer.JUMUAH) "Jumu'ah (default)" else p.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Switch(
                            checked = cfg.reminderEnabled,
                            onCheckedChange = { drafts = drafts + (p to cfg.copy(reminderEnabled = it)) },
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TimeField("Time", cfg.minuteOfDay, { drafts = drafts + (p to cfg.copy(minuteOfDay = it)) }, Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        OffsetPicker(
                            value = cfg.reminderOffsetMin,
                            enabled = cfg.reminderEnabled,
                            onChange = { drafts = drafts + (p to cfg.copy(reminderOffsetMin = it)) },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { onSave(drafts.values.toList()) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Save times")
            }
        }
    }
}

@Composable
private fun OffsetPicker(value: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }, enabled = enabled) {
            Text(if (value == 0) "At time" else "$value min before")
            Icon(Icons.Rounded.ExpandMore, null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            OFFSETS.forEach { o ->
                DropdownMenuItem(
                    text = { Text(if (o == 0) "At prayer time" else "$o minutes before") },
                    onClick = { onChange(o); open = false },
                )
            }
        }
    }
}

@Composable
private fun FridayDialog(
    date: LocalDate,
    override: JumuahOverride?,
    defaultMinute: Int,
    onDismiss: () -> Unit,
    onSave: (Int, String) -> Unit,
    onReset: () -> Unit,
) {
    var minute by remember { mutableIntStateOf(override?.minuteOfDay ?: defaultMinute) }
    var note by remember { mutableStateOf(override?.note ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Jumu'ah · ${Dates.day(date)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Default is ${Dates.time(defaultMinute)}. Set a specific time for this Friday.", style = MaterialTheme.typography.bodyMedium)
                TimeField("Jumu'ah time", minute, { minute = it })
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(80) },
                    label = { Text("Note (optional)") },
                    placeholder = { Text("e.g. Masjid Al-Noor, 2nd khutbah") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { Button(onClick = { onSave(minute, note) }) { Text("Save") } },
        dismissButton = {
            if (override != null) TextButton(onClick = onReset) { Text("Use default") }
            else TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
