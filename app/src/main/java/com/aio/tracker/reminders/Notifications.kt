package com.aio.tracker.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aio.tracker.MainActivity
import com.aio.tracker.R
import com.aio.tracker.data.db.Prayer
import com.aio.tracker.util.Dates
import java.time.LocalDateTime

object Notifications {
    const val CHANNEL_PRAYER = "prayer_reminders"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(CHANNEL_PRAYER, "Prayer reminders", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Reminders before each salat and Jumu'ah"
            enableVibration(true)
        }
        nm.createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun notificationId(prayer: Prayer) = 2000 + prayer.ordinal
    private const val TEST_NOTIFICATION_ID = 2999

    /** Removes the reminder for [prayer], e.g. once it has been logged in the app. */
    fun cancelPrayerReminder(context: Context, prayer: Prayer) {
        runCatching { NotificationManagerCompat.from(context).cancel(notificationId(prayer)) }
    }

    /**
     * @param test a preview from Settings: it has no "Mark as prayed" action (so it can't log a prayer that
     * was not prayed) and it doesn't replace a real reminder.
     */
    fun showPrayerReminder(context: Context, prayer: Prayer, prayerAt: LocalDateTime, offsetMin: Int, test: Boolean = false) {
        if (!canPost(context)) return
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_DESTINATION, "salat")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val markIntent = Intent(context, PrayerActionReceiver::class.java).apply {
            action = PrayerActionReceiver.ACTION_MARK_PRAYED
            putExtra(PrayerActionReceiver.EXTRA_PRAYER, prayer.name)
            putExtra(PrayerActionReceiver.EXTRA_EPOCH_DAY, prayerAt.toLocalDate().toEpochDay())
        }
        val mark = PendingIntent.getBroadcast(
            context, 3000 + prayer.ordinal, markIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val time = Dates.time(prayerAt.toLocalTime())
        val title = if (offsetMin <= 0) "It's time for ${prayer.label}" else "${prayer.label} in $offsetMin min"
        val text = if (prayer == Prayer.JUMUAH) "Jumu'ah prayer at $time. Time to get ready for the khutbah." else "${prayer.label} (${prayer.arabic}) at $time"
        val builder = NotificationCompat.Builder(context, CHANNEL_PRAYER)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF0A72B5.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setTimeoutAfter(if (test) 60 * 1000L else 6 * 60 * 60 * 1000L)
        if (test) builder.setSubText("Test") else builder.addAction(0, "Mark as prayed", mark)
        try {
            NotificationManagerCompat.from(context).notify(if (test) TEST_NOTIFICATION_ID else notificationId(prayer), builder.build())
        } catch (e: SecurityException) {
        }
    }
}
