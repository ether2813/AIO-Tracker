package com.aio.tracker

import android.content.Context
import com.aio.tracker.data.ai.GeminiClient
import com.aio.tracker.data.backup.BackupManager
import com.aio.tracker.data.db.AppDatabase
import com.aio.tracker.data.prefs.SettingsRepository
import com.aio.tracker.data.secure.SecretStore
import com.aio.tracker.domain.InsightsBuilder
import com.aio.tracker.reminders.PrayerScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Simple manual dependency container, created once by [AioApp]. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val db: AppDatabase = AppDatabase.get(appContext)
    val settings = SettingsRepository(appContext)
    val secrets = SecretStore(appContext)
    val gemini = GeminiClient()
    val backup = BackupManager(db, settings)
    val insights = InsightsBuilder(db)
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun reschedulePrayerReminders() {
        appScope.launch { runCatching { PrayerScheduler.rescheduleAll(appContext, this@AppContainer) } }
    }
}
