package com.aio.tracker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aio.tracker.data.db.AppDatabase
import com.aio.tracker.data.db.Prayer
import com.aio.tracker.data.db.PrayerLog
import com.aio.tracker.data.db.PrayerStatus
import com.aio.tracker.reminders.markPrayedFromReminder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The "Mark as prayed" notification action must never overwrite a better status logged in the app. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ReminderActionTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun memDb() = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()

    @Test
    fun markAsPrayedLogsOnTimeButNeverDowngrades() {
        val db = memDb()
        val dao = db.prayerDao()
        val day = 20000L
        runBlocking {
            // Nothing logged yet: the action logs the prayer.
            assertNull(dao.getLog(day, Prayer.ASR))
            assertTrue(markPrayedFromReminder(db, day, Prayer.ASR))
            assertEquals(PrayerStatus.ON_TIME, dao.getLog(day, Prayer.ASR)?.status)

            // Already logged as prayed in congregation: the action leaves it alone.
            dao.upsertLog(PrayerLog(day, Prayer.MAGHRIB, PrayerStatus.JAMAAH))
            assertFalse(markPrayedFromReminder(db, day, Prayer.MAGHRIB))
            assertEquals(PrayerStatus.JAMAAH, dao.getLog(day, Prayer.MAGHRIB)?.status)

            // Logged as missed: the user prayed after all, so the action corrects it.
            dao.upsertLog(PrayerLog(day, Prayer.ISHA, PrayerStatus.MISSED))
            assertTrue(markPrayedFromReminder(db, day, Prayer.ISHA))
            assertEquals(PrayerStatus.ON_TIME, dao.getLog(day, Prayer.ISHA)?.status)
        }
        db.close()
    }
}
