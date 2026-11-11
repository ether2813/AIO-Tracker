package com.aio.tracker.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.room.withTransaction
import com.aio.tracker.container
import com.aio.tracker.data.db.AppDatabase
import com.aio.tracker.data.db.Prayer
import com.aio.tracker.data.db.PrayerLog
import com.aio.tracker.data.db.PrayerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

private fun BroadcastReceiver.runAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
            block()
        } catch (e: Throwable) {
        } finally {
            pending.finish()
        }
    }
}

/**
 * Logs [prayer] as prayed from the notification action. A prayer that is already logged as prayed keeps
 * its status, so tapping the notification can't downgrade e.g. "Jama'ah" to "On time".
 * Returns true when a log was written.
 */
suspend fun markPrayedFromReminder(db: AppDatabase, day: Long, prayer: Prayer): Boolean = db.withTransaction {
    val existing = db.prayerDao().getLog(day, prayer)
    if (existing?.status?.counts == true) {
        false
    } else {
        db.prayerDao().upsertLog(PrayerLog(day, prayer, PrayerStatus.ON_TIME))
        true
    }
}

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMIND) return
        val prayer = intent.getStringExtra(EXTRA_PRAYER)?.let { runCatching { Prayer.valueOf(it) }.getOrNull() } ?: return
        val at = intent.getLongExtra(EXTRA_PRAYER_AT, -1L)
        val offset = intent.getIntExtra(EXTRA_OFFSET, 0)
        val app = context.container
        val appContext = context.applicationContext
        runAsync {
            try {
                // Ignore reminders that are very stale (e.g. device was off for hours).
                if (at > 0 && System.currentTimeMillis() - at < 2 * 60 * 60 * 1000L) {
                    val prayerAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(at), ZoneId.systemDefault())
                    // No need to remind about a prayer that has already been logged as prayed.
                    val logged = runCatching {
                        app.db.prayerDao().getLog(prayerAt.toLocalDate().toEpochDay(), prayer)
                    }.getOrNull()
                    if (logged?.status?.counts != true) {
                        Notifications.showPrayerReminder(appContext, prayer, prayerAt, offset)
                    }
                }
            } finally {
                // Small delay so "now" is safely past this trigger before computing the next one.
                kotlinx.coroutines.delay(1500)
                PrayerScheduler.rescheduleAll(appContext, app)
            }
        }
    }

    companion object {
        const val ACTION_REMIND = "com.aio.tracker.action.PRAYER_REMIND"
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_PRAYER_AT = "prayer_at"
        const val EXTRA_OFFSET = "offset"
    }
}

class PrayerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MARK_PRAYED) return
        val prayer = intent.getStringExtra(EXTRA_PRAYER)?.let { runCatching { Prayer.valueOf(it) }.getOrNull() } ?: return
        val day = intent.getLongExtra(EXTRA_EPOCH_DAY, Long.MIN_VALUE)
        if (day == Long.MIN_VALUE) return
        NotificationManagerCompat.from(context).cancel(Notifications.notificationId(prayer))
        val app = context.container
        runAsync { markPrayedFromReminder(app.db, day, prayer) }
    }

    companion object {
        const val ACTION_MARK_PRAYED = "com.aio.tracker.action.MARK_PRAYED"
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_EPOCH_DAY = "epoch_day"
    }
}

/** Re-arms reminders after reboot, app update, time/time-zone change, or exact-alarm permission change. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.container
        runAsync {
            app.db.ensureDefaults()
            PrayerScheduler.rescheduleAll(context.applicationContext, app)
        }
    }
}
