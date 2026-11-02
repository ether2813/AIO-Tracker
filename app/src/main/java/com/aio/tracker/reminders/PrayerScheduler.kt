package com.aio.tracker.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.aio.tracker.AppContainer
import com.aio.tracker.data.db.Prayer
import com.aio.tracker.domain.PrayerSchedule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Keeps exactly one pending alarm per prayer: the next reminder. When it fires the
 * receiver shows the notification and calls [rescheduleAll] again for the following one.
 */
object PrayerScheduler {

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        val am = context.getSystemService(AlarmManager::class.java) ?: return false
        return am.canScheduleExactAlarms()
    }

    suspend fun rescheduleAll(context: Context, container: AppContainer) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val dao = container.db.prayerDao()
        val configs = dao.getConfigs().associateBy { it.prayer }
        val today = LocalDate.now()
        val overrides = dao.getOverridesFrom(today.toEpochDay()).associateBy { it.epochDay }
        val settings = container.settings.current()
        val now = LocalDateTime.now()

        for (prayer in Prayer.entries) {
            val pi = pendingIntent(context, prayer, null, 0)
            am.cancel(pi)
            val next = PrayerSchedule.nextReminder(prayer, now, configs, overrides, settings.jumuahReplacesDhuhr) ?: continue
            val (trigger, slot) = next
            val triggerMillis = trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val prayerMillis = slot.at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val offset = configs[prayer]?.reminderOffsetMin ?: 0
            val intent = pendingIntent(context, prayer, prayerMillis, offset)
            try {
                if (canScheduleExact(context)) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, intent)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, intent)
                }
            } catch (e: SecurityException) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, intent)
            }
        }
    }

    private fun pendingIntent(context: Context, prayer: Prayer, prayerAtMillis: Long?, offset: Int): PendingIntent {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_REMIND
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER, prayer.name)
            if (prayerAtMillis != null) putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_AT, prayerAtMillis)
            putExtra(PrayerAlarmReceiver.EXTRA_OFFSET, offset)
        }
        return PendingIntent.getBroadcast(
            context, 1000 + prayer.ordinal, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
