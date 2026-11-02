package com.aio.tracker

import android.app.Application
import android.content.Context
import com.aio.tracker.reminders.Notifications
import kotlinx.coroutines.launch

class AioApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
        container.appScope.launch {
            runCatching { container.db.ensureDefaults() }
            container.reschedulePrayerReminders()
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as AioApp).container
