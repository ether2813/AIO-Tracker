package com.aio.tracker.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aio.tracker.BuildConfig
import com.aio.tracker.data.prefs.ThemeMode
import com.aio.tracker.reminders.Notifications
import com.aio.tracker.reminders.PrayerScheduler
import com.aio.tracker.data.db.Prayer
import com.aio.tracker.ui.appViewModel
import com.aio.tracker.ui.components.AppCard
import com.aio.tracker.ui.components.ConfirmDialog
import com.aio.tracker.ui.components.IconBadge
import com.aio.tracker.ui.components.Pill
import com.aio.tracker.ui.components.SectionHeader
import com.aio.tracker.ui.components.screenPadding
import com.aio.tracker.ui.theme.AppTheme
import com.aio.tracker.util.Money
import java.time.LocalDate
import java.time.LocalDateTime

private val CURRENCIES = listOf("USD", "EUR", "GBP", "INR", "PKR", "BDT", "SAR", "AED", "QAR", "KWD", "EGP", "TRY", "MYR", "IDR", "NGN", "CAD", "AUD")

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm = appViewModel { SettingsViewModel(it) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val hasKey by vm.hasKey.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var dialog by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    val canNotify = remember(refresh) { Notifications.canPost(context) }
    val canExact = remember(refresh) { PrayerScheduler.canScheduleExact(context) }

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportBackup(context.contentResolver, uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImport = uri
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(contentPadding = screenPadding(padding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (vm.busy) item(key = "busy") { LinearProgressIndicator(Modifier.fillMaxWidth()) }

            item(key = "profile") {
                SectionHeader("Profile")
                AppCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                    SettingRow(Icons.Rounded.Person, "Your name", settings.userName.ifBlank { "Not set" }, onClick = { dialog = "name" })
                }
            }

            item(key = "appearance") {
                SectionHeader("Appearance")
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.Brightness6, MaterialTheme.colorScheme.primary, size = 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Theme", style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.height(10.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { i, m ->
                            SegmentedButton(
                                selected = settings.themeMode == m,
                                onClick = { vm.setTheme(m) },
                                shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                            ) { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        }
                    }
                    if (Build.VERSION.SDK_INT >= 31) {
                        Spacer(Modifier.height(8.dp))
                        SwitchRow(Icons.Rounded.Palette, "Dynamic color", "Use colors from your wallpaper", settings.dynamicColor, vm::setDynamic)
                    }
                }
            }

            item(key = "salat") {
                SectionHeader("Salat & reminders")
                AppCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                    SwitchRow(
                        Icons.Rounded.DarkMode,
                        "Jumu'ah on Fridays",
                        "Track Jumu'ah instead of Dhuhr every Friday",
                        settings.jumuahReplacesDhuhr,
                        vm::setJumuah,
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    SettingRow(
                        Icons.Rounded.Notifications,
                        "Notifications",
                        if (canNotify) "Allowed" else "Not allowed. Tap to enable",
                        trailing = { StatusPill(canNotify) },
                        onClick = {
                            if (!canNotify && Build.VERSION.SDK_INT >= 33) notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            else openAppNotificationSettings(context)
                        },
                    )
                    SettingRow(
                        Icons.Rounded.Alarm,
                        "Exact alarms",
                        if (canExact) "Reminders arrive exactly on time" else "Reminders may be a few minutes late",
                        trailing = { StatusPill(canExact) },
                        onClick = {
                            if (Build.VERSION.SDK_INT >= 31) {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                }
                            }
                        },
                    )
                    SettingRow(
                        Icons.Rounded.Notifications,
                        "Send a test reminder",
                        "Check how prayer notifications look",
                        onClick = {
                            if (!canNotify && Build.VERSION.SDK_INT >= 33) notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            else Notifications.showPrayerReminder(context, Prayer.ASR, LocalDateTime.now().plusMinutes(10), 10, test = true)
                        },
                    )
                }
            }

            item(key = "money") {
                SectionHeader("Money & fitness")
                AppCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                    SettingRow(Icons.Rounded.AccountBalanceWallet, "Currency", settings.currencyCode, onClick = { dialog = "currency" })
                    SettingRow(
                        Icons.Rounded.PieChart,
                        "Monthly budget",
                        if (settings.monthlyBudgetMinor > 0) Money.format(settings.monthlyBudgetMinor, settings.currencyCode) else "Not set",
                        onClick = { dialog = "budget" },
                    )
                    SettingRow(Icons.Rounded.FitnessCenter, "Weekly activity goal", "${settings.weeklyWorkoutGoalMin} minutes", onClick = { dialog = "goal" })
                }
            }

            item(key = "ai") {
                SectionHeader("AI (Google Gemini)")
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.VpnKey, MaterialTheme.colorScheme.tertiary, size = 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Google AI Studio API key", style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (hasKey) "Saved · ${vm.keyHint ?: "••••"}" else "Not set",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (hasKey) Pill("Encrypted", AppTheme.ext.income, icon = Icons.Rounded.Lock)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Your key is encrypted with AES-256 using the Android Keystore. It is never displayed again, never exported and never leaves this device except to call Google's API.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { dialog = "key" }) { Text(if (hasKey) "Replace key" else "Add key") }
                        if (hasKey) OutlinedButton(onClick = { dialog = "removeKey" }) { Text("Remove") }
                        TextButton(onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                        }) {
                            Text("Get a key")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Rounded.OpenInBrowser, null, Modifier.size(16.dp))
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    Row(
                        Modifier.fillMaxWidth().clickable { dialog = "model" }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconBadge(Icons.Rounded.Memory, MaterialTheme.colorScheme.primary, size = 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Model", style = MaterialTheme.typography.titleSmall)
                            Text(settings.geminiModel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            item(key = "data") {
                SectionHeader("Your data")
                AppCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                    SettingRow(
                        Icons.Rounded.SaveAlt,
                        "Export backup",
                        "Save everything to a JSON file on your device",
                        onClick = { exportLauncher.launch("aio-tracker-backup-${LocalDate.now()}.json") },
                    )
                    SettingRow(
                        Icons.Rounded.Restore,
                        "Import backup",
                        "Restore from a previously exported file",
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*")) },
                    )
                    SettingRow(
                        Icons.Rounded.DeleteForever,
                        "Erase all data",
                        "Delete all salat logs, workouts, money, tasks and notes",
                        tint = MaterialTheme.colorScheme.error,
                        onClick = { dialog = "wipe" },
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Everything is stored only on this phone. Backups include all your data except the API key. Cloud backup is disabled for privacy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            item(key = "about") {
                SectionHeader("About")
                AppCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                    SettingRow(Icons.Rounded.Info, "AIO Tracker", "Version ${BuildConfig.VERSION_NAME}")
                }
            }
        }
    }

    when (dialog) {
        "name" -> TextInputDialog("Your name", settings.userName, "Name", onDismiss = { dialog = null }) { vm.setName(it) }
        "currency" -> CurrencyDialog(settings.currencyCode, onDismiss = { dialog = null }) { vm.setCurrency(it) }
        "budget" -> TextInputDialog(
            "Monthly budget",
            if (settings.monthlyBudgetMinor > 0) Money.toInput(settings.monthlyBudgetMinor) else "",
            "Amount (${settings.currencyCode}), empty for none",
            keyboardType = KeyboardType.Decimal,
            validate = { it.isBlank() || Money.parse(it) != null },
            onDismiss = { dialog = null },
        ) { vm.setBudget(if (it.isBlank()) 0 else Money.parse(it) ?: 0) }
        "goal" -> TextInputDialog(
            "Weekly activity goal",
            settings.weeklyWorkoutGoalMin.toString(),
            "Minutes per week",
            keyboardType = KeyboardType.Number,
            validate = { (it.toIntOrNull() ?: 0) in 10..3000 },
            onDismiss = { dialog = null },
        ) { vm.setGoal(it.toInt()) }
        "key" -> ApiKeyDialog(onDismiss = { dialog = null }) { vm.saveKey(it) }
        "removeKey" -> ConfirmDialog(
            "Remove API key?",
            "AI insights will stop working until you add a key again.",
            "Remove",
            onConfirm = { vm.removeKey() },
            onDismiss = { dialog = null },
            destructive = true,
        )
        "model" -> ModelDialog(
            current = settings.geminiModel,
            models = vm.models,
            loading = vm.modelsLoading,
            onLoad = vm::loadModels,
            onDismiss = { dialog = null },
            onSave = { vm.setModel(it) },
        )
        "wipe" -> ConfirmDialog(
            "Erase all data?",
            "This permanently deletes everything in the app. Export a backup first if you might need it. Your API key is kept.",
            "Erase",
            onConfirm = { vm.wipe() },
            onDismiss = { dialog = null },
            destructive = true,
        )
    }
    pendingImport?.let { uri ->
        ConfirmDialog(
            "Replace all data?",
            "Importing will replace everything currently in the app with the contents of the backup file.",
            "Import",
            onConfirm = { vm.importBackup(context.contentResolver, uri) },
            onDismiss = { pendingImport = null },
        )
    }
}

private fun openAppNotificationSettings(context: android.content.Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

@Composable
private fun StatusPill(ok: Boolean) {
    Pill(if (ok) "On" else "Off", if (ok) AppTheme.ext.income else AppTheme.ext.warning)
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    trailing: @Composable () -> Unit = {},
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, tint, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (tint == MaterialTheme.colorScheme.error) tint else MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth().clickable { onChange(!checked) }, verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, MaterialTheme.colorScheme.primary, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun TextInputDialog(
    title: String,
    initial: String,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    validate: (String) -> Boolean = { true },
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    val ok = validate(value)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.take(60) },
                label = { Text(label) },
                singleLine = true,
                isError = !ok,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { Button(enabled = ok, onClick = { onSave(value.trim()); onDismiss() }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CurrencyDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var custom by remember { mutableStateOf(current) }
    val valid = runCatching { java.util.Currency.getInstance(custom.trim().uppercase()) }.isSuccess
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Currency") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CURRENCIES.forEach { c ->
                        FilterChip(selected = custom.equals(c, true), onClick = { custom = c }, label = { Text(c) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it.take(3).uppercase() },
                    label = { Text("ISO code (e.g. USD)") },
                    singleLine = true,
                    isError = !valid,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { Button(enabled = valid, onClick = { onSave(custom.trim().uppercase()); onDismiss() }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ApiKeyDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var key by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        // Block screenshots / screen recording while the key is on screen.
        properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
        icon = { Icon(Icons.Rounded.Lock, null) },
        title = { Text("Gemini API key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Paste your key from Google AI Studio. After saving it is encrypted and cannot be viewed again, not even by you.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it.trim() },
                    label = { Text("API key") },
                    singleLine = true,
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, if (visible) "Hide" else "Show")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(enabled = key.length >= 20, onClick = { onSave(key); key = ""; onDismiss() }) { Text("Encrypt & save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ModelDialog(
    current: String,
    models: List<String>,
    loading: Boolean,
    onLoad: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gemini model") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.trim() },
                    label = { Text("Model id") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "\"Flash\" models are fast and free-tier friendly. Load the list to see every model your key can use.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(onClick = onLoad, enabled = !loading) { Text(if (loading) "Loading…" else "Load available models") }
                if (models.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        models.forEach { m -> FilterChip(selected = value == m, onClick = { value = m }, label = { Text(m) }) }
                    }
                }
            }
        },
        confirmButton = { Button(enabled = value.isNotBlank(), onClick = { onSave(value); onDismiss() }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
