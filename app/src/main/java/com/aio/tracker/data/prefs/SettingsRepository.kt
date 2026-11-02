package com.aio.tracker.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import java.io.IOException
import java.util.Currency
import java.util.Locale

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
data class AppSettings(
    val userName: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val currencyCode: String = defaultCurrency(),
    val geminiModel: String = DEFAULT_MODEL,
    val weeklyWorkoutGoalMin: Int = 150,
    val monthlyBudgetMinor: Long = 0,
    val jumuahReplacesDhuhr: Boolean = true,
    val askedNotificationPermission: Boolean = false,
) {
    companion object {
        const val DEFAULT_MODEL = "gemini-2.5-flash"
        fun defaultCurrency(): String = runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrNull() ?: "USD"
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val userName = stringPreferencesKey("user_name")
        val themeMode = stringPreferencesKey("theme_mode")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val currency = stringPreferencesKey("currency")
        val model = stringPreferencesKey("gemini_model")
        val workoutGoal = intPreferencesKey("weekly_workout_goal")
        val budget = longPreferencesKey("monthly_budget_minor")
        val jumuahReplaces = booleanPreferencesKey("jumuah_replaces_dhuhr")
        val askedNotif = booleanPreferencesKey("asked_notif")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            val d = AppSettings()
            AppSettings(
                userName = p[Keys.userName] ?: d.userName,
                themeMode = p[Keys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: d.themeMode,
                dynamicColor = p[Keys.dynamicColor] ?: d.dynamicColor,
                currencyCode = p[Keys.currency] ?: d.currencyCode,
                geminiModel = p[Keys.model]?.takeIf { it.isNotBlank() } ?: d.geminiModel,
                weeklyWorkoutGoalMin = p[Keys.workoutGoal] ?: d.weeklyWorkoutGoalMin,
                monthlyBudgetMinor = p[Keys.budget] ?: d.monthlyBudgetMinor,
                jumuahReplacesDhuhr = p[Keys.jumuahReplaces] ?: d.jumuahReplacesDhuhr,
                askedNotificationPermission = p[Keys.askedNotif] ?: d.askedNotificationPermission,
            )
        }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setUserName(v: String) = context.dataStore.edit { it[Keys.userName] = v.trim() }
    suspend fun setThemeMode(v: ThemeMode) = context.dataStore.edit { it[Keys.themeMode] = v.name }
    suspend fun setDynamicColor(v: Boolean) = context.dataStore.edit { it[Keys.dynamicColor] = v }
    suspend fun setCurrency(v: String) = context.dataStore.edit { it[Keys.currency] = v.uppercase(Locale.ROOT) }
    suspend fun setModel(v: String) = context.dataStore.edit { it[Keys.model] = v.trim().removePrefix("models/") }
    suspend fun setWorkoutGoal(v: Int) = context.dataStore.edit { it[Keys.workoutGoal] = v.coerceIn(10, 3000) }
    suspend fun setBudget(v: Long) = context.dataStore.edit { it[Keys.budget] = v.coerceAtLeast(0) }
    suspend fun setJumuahReplacesDhuhr(v: Boolean) = context.dataStore.edit { it[Keys.jumuahReplaces] = v }
    suspend fun setAskedNotificationPermission() = context.dataStore.edit { it[Keys.askedNotif] = true }

    /** Restores the portable part of settings from a backup file. */
    suspend fun restore(s: AppSettings) = context.dataStore.edit {
        it[Keys.userName] = s.userName
        it[Keys.themeMode] = s.themeMode.name
        it[Keys.dynamicColor] = s.dynamicColor
        it[Keys.currency] = s.currencyCode
        it[Keys.model] = s.geminiModel
        it[Keys.workoutGoal] = s.weeklyWorkoutGoalMin
        it[Keys.budget] = s.monthlyBudgetMinor
        it[Keys.jumuahReplaces] = s.jumuahReplacesDhuhr
    }
}
