package com.aio.tracker

import com.aio.tracker.data.db.PrayerDefaults
import com.aio.tracker.data.db.JumuahOverride
import com.aio.tracker.data.db.Prayer
import com.aio.tracker.data.db.PrayerConfig
import com.aio.tracker.data.db.PrayerLog
import com.aio.tracker.data.db.PrayerStatus
import com.aio.tracker.domain.PrayerSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PrayerScheduleTest {
    private val configs = PrayerDefaults.configs.associateBy { it.prayer }

    // 2026-11-06 is a Friday
    private val friday = LocalDate.of(2026, 11, 6)
    private val thursday = friday.minusDays(1)

    @Test
    fun fridayUsesJumuahInsteadOfDhuhr() {
        val prayers = PrayerSchedule.prayersFor(friday, jumuahReplacesDhuhr = true)
        assertTrue(Prayer.JUMUAH in prayers)
        assertTrue(Prayer.DHUHR !in prayers)
        assertEquals(Prayer.daily, PrayerSchedule.prayersFor(friday, jumuahReplacesDhuhr = false))
        assertEquals(Prayer.daily, PrayerSchedule.prayersFor(thursday, true))
    }

    @Test
    fun overrideChangesJumuahTimeForThatFridayOnly() {
        val overrides = mapOf(friday.toEpochDay() to JumuahOverride(friday.toEpochDay(), 14 * 60))
        assertEquals(14 * 60, PrayerSchedule.minuteFor(Prayer.JUMUAH, friday, configs, overrides))
        assertEquals(13 * 60 + 30, PrayerSchedule.minuteFor(Prayer.JUMUAH, friday.plusWeeks(1), configs, overrides))
    }

    @Test
    fun nextPrayerRollsOverToTomorrow() {
        val late = thursday.atTime(23, 0)
        val next = PrayerSchedule.next(late, configs, emptyMap(), true)!!
        assertEquals(Prayer.FAJR, next.prayer)
        assertEquals(friday, next.date)
    }

    @Test
    fun reminderUsesOffsetAndSkipsPast() {
        val cfg = configs + (Prayer.ASR to PrayerConfig(Prayer.ASR, 16 * 60 + 30, true, 15))
        val now = thursday.atTime(16, 20) // past the 16:15 reminder
        val (trigger, slot) = PrayerSchedule.nextReminder(Prayer.ASR, now, cfg, emptyMap(), true)!!
        assertEquals(friday, slot.date)
        assertEquals(LocalDateTime.of(friday, java.time.LocalTime.of(16, 15)), trigger)
    }

    @Test
    fun dhuhrReminderSkipsFridayAndJumuahOnlyOnFriday() {
        val now = thursday.atTime(14, 0)
        val (_, dhuhr) = PrayerSchedule.nextReminder(Prayer.DHUHR, now, configs, emptyMap(), true)!!
        assertEquals(friday.plusDays(1), dhuhr.date)
        val (_, jumuah) = PrayerSchedule.nextReminder(Prayer.JUMUAH, now, configs, emptyMap(), true)!!
        assertEquals(friday, jumuah.date)
        assertNull(PrayerSchedule.nextReminder(Prayer.JUMUAH, now, configs, emptyMap(), false))
    }

    @Test
    fun disabledReminderReturnsNull() {
        val cfg = configs + (Prayer.ISHA to PrayerConfig(Prayer.ISHA, 20 * 60, reminderEnabled = false))
        assertNull(PrayerSchedule.nextReminder(Prayer.ISHA, thursday.atTime(1, 0), cfg, emptyMap(), true))
    }

    @Test
    fun streakCountsCompleteDays() {
        val logs = mutableListOf<PrayerLog>()
        for (d in 0L..2L) {
            val day = thursday.minusDays(d)
            PrayerSchedule.prayersFor(day, true).forEach { logs += PrayerLog(day.toEpochDay(), it, PrayerStatus.ON_TIME) }
        }
        // today (friday) incomplete -> streak counts from yesterday
        assertEquals(3, PrayerSchedule.streak(friday, logs, true))
        // a missed prayer breaks the streak
        logs += PrayerLog(thursday.minusDays(1).toEpochDay(), Prayer.ASR, PrayerStatus.MISSED)
        assertEquals(1, PrayerSchedule.streak(friday, logs, true))
    }
}
