package com.aio.tracker.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

object Money {
    private val cache = HashMap<String, NumberFormat>()

    private fun formatter(code: String): NumberFormat = synchronized(cache) {
        cache.getOrPut(code) {
            NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
                runCatching { currency = Currency.getInstance(code) }
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }
        }
    }

    fun format(minor: Long, code: String): String = synchronized(cache) { formatter(code).format(minor / 100.0) }

    /** Short form for charts and tiles, e.g. "$1.2K". */
    fun compact(minor: Long, code: String): String {
        val value = minor / 100.0
        val abs = kotlin.math.abs(value)
        val symbol = runCatching { Currency.getInstance(code).getSymbol(Locale.getDefault()) }.getOrDefault(code)
        val sign = if (value < 0) "-" else ""
        return when {
            abs >= 1_000_000 -> "$sign$symbol${trim(abs / 1_000_000)}M"
            abs >= 10_000 -> "$sign$symbol${trim(abs / 1_000)}K"
            else -> format(minor, code)
        }
    }

    private fun trim(v: Double): String = BigDecimal(v).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

    /** Parses user input such as "12", "12.5", "1,234.50" or "12,5" into minor units. */
    fun parse(text: String): Long? {
        var t = text.trim().replace(" ", "").replace(" ", "")
        if (t.isEmpty()) return null
        val lastComma = t.lastIndexOf(',')
        val lastDot = t.lastIndexOf('.')
        t = when {
            lastComma >= 0 && lastDot >= 0 ->
                if (lastComma > lastDot) t.replace(".", "").replace(',', '.') else t.replace(",", "")
            lastComma >= 0 -> {
                val decimals = t.length - lastComma - 1
                if (decimals in 1..2 && t.count { it == ',' } == 1) t.replace(',', '.') else t.replace(",", "")
            }
            else -> t
        }
        return runCatching {
            BigDecimal(t).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
        }.getOrNull()?.takeIf { it > 0 }
    }

    fun toInput(minor: Long): String = BigDecimal(minor).movePointLeft(2).stripTrailingZeros().toPlainString()
}

object Dates {
    private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    private val dayFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")
    private val dayYearFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")
    private val longFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM")
    private val monthFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val dateTimeFmt: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

    fun today(): LocalDate = LocalDate.now()

    fun time(minuteOfDay: Int): String {
        // Never crash on an out-of-range value (e.g. from a hand-edited backup file).
        val m = minuteOfDay.coerceIn(0, 24 * 60 - 1)
        return LocalTime.of(m / 60, m % 60).format(timeFmt)
    }
    fun time(t: LocalTime): String = t.format(timeFmt)

    fun relativeDay(date: LocalDate, today: LocalDate = today()): String = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        today.plusDays(1) -> "Tomorrow"
        else -> if (date.year == today.year) date.format(dayFmt) else date.format(dayYearFmt)
    }

    fun day(date: LocalDate): String = date.format(if (date.year == today().year) dayFmt else dayYearFmt)
    fun long(date: LocalDate): String = date.format(longFmt)
    fun month(date: LocalDate): String = date.format(monthFmt)
    fun dateTime(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(dateTimeFmt)

    fun epochDayToUtcMillis(day: Long): Long = day * 86_400_000L
    fun utcMillisToEpochDay(millis: Long): Long = Math.floorDiv(millis, 86_400_000L)

    fun duration(minutes: Long): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0L -> "${m}m"
            m == 0L -> "${h}h"
            else -> "${h}h ${m}m"
        }
    }
}

fun Long.toLocalDate(): LocalDate = LocalDate.ofEpochDay(this)
