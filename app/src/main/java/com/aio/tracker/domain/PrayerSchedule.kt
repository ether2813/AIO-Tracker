package com.aio.tracker.domain

import com.aio.tracker.data.db.JumuahOverride
import com.aio.tracker.data.db.Prayer
import com.aio.tracker.data.db.PrayerConfig
import com.aio.tracker.data.db.PrayerLog
import com.aio.tracker.data.db.PrayerStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Pure prayer-time logic, independent from Android so it can be unit tested. */
object PrayerSchedule {

    data class Slot(val prayer: Prayer, val date: LocalDate, val at: LocalDateTime) {
        val minuteOfDay: Int get() = at.hour * 60 + at.minute
    }

    fun isFriday(date: LocalDate) = date.dayOfWeek == DayOfWeek.FRIDAY

    fun prayersFor(date: LocalDate, jumuahReplacesDhuhr: Boolean): List<Prayer> =
        if (jumuahReplacesDhuhr && isFriday(date)) {
            listOf(Prayer.FAJR, Prayer.JUMUAH, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)
        } else {
            Prayer.daily
        }

    fun defaultMinute(prayer: Prayer): Int =
        com.aio.tracker.data.db.PrayerDefaults.configs.first { it.prayer == prayer }.minuteOfDay

    fun minuteFor(
        prayer: Prayer,
        date: LocalDate,
        configs: Map<Prayer, PrayerConfig>,
        overrides: Map<Long, JumuahOverride>,
    ): Int {
        if (prayer == Prayer.JUMUAH) {
            overrides[date.toEpochDay()]?.let { return it.minuteOfDay }
        }
        return configs[prayer]?.minuteOfDay ?: defaultMinute(prayer)
    }

    fun slotsFor(
        date: LocalDate,
        configs: Map<Prayer, PrayerConfig>,
        overrides: Map<Long, JumuahOverride>,
        jumuahReplacesDhuhr: Boolean,
    ): List<Slot> = prayersFor(date, jumuahReplacesDhuhr).map { p ->
        val m = minuteFor(p, date, configs, overrides).coerceIn(0, 24 * 60 - 1)
        Slot(p, date, date.atTime(LocalTime.of(m / 60, m % 60)))
    }

    /** The next prayer strictly after [now]. */
    fun next(
        now: LocalDateTime,
        configs: Map<Prayer, PrayerConfig>,
        overrides: Map<Long, JumuahOverride>,
        jumuahReplacesDhuhr: Boolean,
    ): Slot? {
        for (d in 0L..7L) {
            val slot = slotsFor(now.toLocalDate().plusDays(d), configs, overrides, jumuahReplacesDhuhr)
                .firstOrNull { it.at.isAfter(now) }
            if (slot != null) return slot
        }
        return null
    }

    /** The prayer currently "in progress" (the last one whose time has started today). */
    fun current(
        now: LocalDateTime,
        configs: Map<Prayer, PrayerConfig>,
        overrides: Map<Long, JumuahOverride>,
        jumuahReplacesDhuhr: Boolean,
    ): Slot? = slotsFor(now.toLocalDate(), configs, overrides, jumuahReplacesDhuhr).lastOrNull { !it.at.isAfter(now) }

    /** When the next reminder for [prayer] should fire (prayer time minus offset), strictly after [now]. */
    fun nextReminder(
        prayer: Prayer,
        now: LocalDateTime,
        configs: Map<Prayer, PrayerConfig>,
        overrides: Map<Long, JumuahOverride>,
        jumuahReplacesDhuhr: Boolean,
    ): Pair<LocalDateTime, Slot>? {
        val config = configs[prayer] ?: return null
        if (!config.reminderEnabled) return null
        if (prayer == Prayer.JUMUAH && !jumuahReplacesDhuhr) return null
        for (d in 0L..14L) {
            val date = now.toLocalDate().plusDays(d)
            if (prayer !in prayersFor(date, jumuahReplacesDhuhr)) continue
            val m = minuteFor(prayer, date, configs, overrides).coerceIn(0, 24 * 60 - 1)
            val at = date.atTime(LocalTime.of(m / 60, m % 60))
            val trigger = at.minusMinutes(config.reminderOffsetMin.toLong().coerceAtLeast(0))
            if (trigger.isAfter(now)) return trigger to Slot(prayer, date, at)
        }
        return null
    }

    // ------------------------------------------------------------- stats

    fun isDayComplete(date: LocalDate, logs: Map<Pair<Long, Prayer>, PrayerLog>, jumuahReplacesDhuhr: Boolean): Boolean =
        prayersFor(date, jumuahReplacesDhuhr).all { p -> logs[date.toEpochDay() to p]?.status?.counts == true }

    /** Consecutive days with all prayers performed. Today counts only once it is complete. */
    fun streak(today: LocalDate, logs: List<PrayerLog>, jumuahReplacesDhuhr: Boolean): Int {
        val map = logs.associateBy { it.epochDay to it.prayer }
        var day = if (isDayComplete(today, map, jumuahReplacesDhuhr)) today else today.minusDays(1)
        var count = 0
        while (count < 3650 && isDayComplete(day, map, jumuahReplacesDhuhr)) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    /** Fraction (0..1) of expected prayers performed between [from] and [to] inclusive. */
    fun completion(from: LocalDate, to: LocalDate, logs: List<PrayerLog>, jumuahReplacesDhuhr: Boolean): Float {
        val map = logs.associateBy { it.epochDay to it.prayer }
        var expected = 0
        var done = 0
        var d = from
        while (!d.isAfter(to)) {
            for (p in prayersFor(d, jumuahReplacesDhuhr)) {
                expected++
                if (map[d.toEpochDay() to p]?.status?.counts == true) done++
            }
            d = d.plusDays(1)
        }
        return if (expected == 0) 0f else done.toFloat() / expected
    }

    fun statusCounts(logs: List<PrayerLog>): Map<PrayerStatus, Int> =
        PrayerStatus.entries.associateWith { s -> logs.count { it.status == s } }
}
