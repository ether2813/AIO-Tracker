package com.aio.tracker.domain

import com.aio.tracker.data.db.AppDatabase
import com.aio.tracker.data.db.LoanDirection
import com.aio.tracker.data.db.Prayer
import com.aio.tracker.data.db.PrayerStatus
import com.aio.tracker.data.db.TxnType
import com.aio.tracker.data.prefs.AppSettings
import com.aio.tracker.util.Money
import com.aio.tracker.util.toLocalDate
import java.time.LocalDate
import java.time.LocalDateTime

enum class AiScope(val label: String) {
    EVERYTHING("Everything"),
    SALAT("Salat"),
    FITNESS("Fitness"),
    MONEY("Money"),
    TASKS("Tasks"),
    NOTES("Notes"),
}

enum class AiPeriod(val label: String, val days: Long) {
    WEEK("7 days", 7),
    MONTH("30 days", 30),
    QUARTER("90 days", 90),
}

/**
 * Turns local data into a compact, privacy-conscious text digest for the AI model.
 * Only aggregates and short entries are sent, never the API key or any device identifiers.
 */
class InsightsBuilder(private val db: AppDatabase) {

    val systemPrompt: String = """
        You are a warm, practical personal coach inside an all-in-one life tracker app used by a Muslim.
        You receive a digest of the user's own data: salat (prayer) logs, workouts, money (expenses, income,
        lendings/borrowings), to-do tasks and personal notes. Analyse it honestly and kindly.
        Rules:
        - Answer in GitHub-flavoured Markdown with short sections using "##" headings and bullet points.
        - Start with a 2-3 sentence overview, then one section per area present in the data,
          then "## Top 3 next steps" with specific, achievable actions.
        - Use concrete numbers from the data (counts, totals, trends). Never invent data that is not given.
        - For salat, be encouraging and gentle, never judgemental. For money, spot unusual spending and
          outstanding loans with due dates. For tasks, point out overdue and high-priority items.
        - Keep it under 450 words unless the user asks a specific question that needs more.
    """.trimIndent()

    suspend fun build(scope: AiScope, period: AiPeriod, settings: AppSettings, today: LocalDate = LocalDate.now()): String {
        val from = today.minusDays(period.days - 1)
        val sb = StringBuilder()
        sb.appendLine("Today: $today (${today.dayOfWeek}). Period analysed: $from to $today (${period.days} days).")
        sb.appendLine("Currency: ${settings.currencyCode}.")
        if (settings.userName.isNotBlank()) sb.appendLine("User's name: ${settings.userName}.")
        sb.appendLine()
        if (scope == AiScope.EVERYTHING || scope == AiScope.SALAT) salat(sb, from, today, settings)
        if (scope == AiScope.EVERYTHING || scope == AiScope.FITNESS) fitness(sb, from, today, settings)
        if (scope == AiScope.EVERYTHING || scope == AiScope.MONEY) money(sb, from, today, settings)
        if (scope == AiScope.EVERYTHING || scope == AiScope.TASKS) tasks(sb, from, today)
        if (scope == AiScope.EVERYTHING || scope == AiScope.NOTES) notes(sb, from, scope == AiScope.NOTES)
        return sb.toString()
    }

    private suspend fun salat(sb: StringBuilder, from: LocalDate, to: LocalDate, s: AppSettings) {
        val logs = db.prayerDao().getLogs(from.toEpochDay(), to.toEpochDay())
        val replaces = s.jumuahReplacesDhuhr
        sb.appendLine("# SALAT")
        sb.appendLine("Status meanings: JAMAAH = prayed in congregation, ON_TIME = prayed on time alone, LATE = prayed late/qada, MISSED = missed. UNLOGGED = no entry. UPCOMING = later today, its time has not started yet (not a shortfall).")
        var expected = 0
        val perPrayer = linkedMapOf<Prayer, MutableMap<String, Int>>()
        var d = from
        val map = logs.associateBy { it.epochDay to it.prayer }
        val configs = db.prayerDao().getConfigs().associateBy { it.prayer }
        val overrides = db.prayerDao().getOverridesFrom(from.toEpochDay()).associateBy { it.epochDay }
        val now = LocalDateTime.now()
        while (!d.isAfter(to)) {
            for (slot in PrayerSchedule.slotsFor(d, configs, overrides, replaces)) {
                val p = slot.prayer
                expected++
                val key = map[d.toEpochDay() to p]?.status?.name ?: if (slot.at.isAfter(now)) "UPCOMING" else "UNLOGGED"
                val m = perPrayer.getOrPut(p) { linkedMapOf() }
                m[key] = (m[key] ?: 0) + 1
            }
            d = d.plusDays(1)
        }
        // Totals over the expected prayers only, so they always add up to "expected" (a stray log, e.g. a
        // Dhuhr entry on a Friday that now counts as Jumu'ah, must not hide an unlogged prayer).
        val totals = HashMap<String, Int>()
        perPrayer.values.forEach { m -> m.forEach { (k, v) -> totals[k] = (totals[k] ?: 0) + v } }
        sb.appendLine("Expected prayers: $expected. " + PrayerStatus.entries.joinToString(", ") { "${it.name}: ${totals[it.name] ?: 0}" } +
            ", UNLOGGED: ${totals["UNLOGGED"] ?: 0}, UPCOMING: ${totals["UPCOMING"] ?: 0}.")
        sb.appendLine("Completion: ${(PrayerSchedule.completion(from, to, logs, replaces) * 100).toInt()}%. Current full-day streak: ${PrayerSchedule.streak(to, db.prayerDao().getLogs(to.minusDays(400).toEpochDay(), to.toEpochDay()), replaces)} days.")
        perPrayer.forEach { (p, m) -> sb.appendLine("- ${p.label}: " + m.entries.joinToString(", ") { "${it.key}=${it.value}" }) }
        sb.appendLine()
    }

    private suspend fun fitness(sb: StringBuilder, from: LocalDate, to: LocalDate, s: AppSettings) {
        val list = db.workoutDao().getBetween(from.toEpochDay(), to.toEpochDay())
        sb.appendLine("# FITNESS")
        sb.appendLine("Weekly goal: ${s.weeklyWorkoutGoalMin} active minutes.")
        if (list.isEmpty()) {
            sb.appendLine("No workouts logged in this period.").appendLine()
            return
        }
        val totalMin = list.sumOf { it.workout.durationMin }
        val activeDays = list.map { it.workout.epochDay }.distinct().size
        sb.appendLine("Sessions: ${list.size}, total minutes: $totalMin, active days: $activeDays, calories logged: ${list.sumOf { it.workout.calories ?: 0 }}.")
        sb.appendLine("By type: " + list.groupBy { it.workout.type }.entries.joinToString(", ") { (t, w) -> "${t.label} ${w.size}x/${w.sumOf { it.workout.durationMin }}min" })
        sb.appendLine("Sessions (most recent last, max 40):")
        list.takeLast(40).forEach { w ->
            val ex = w.exercises.sortedBy { it.position }.joinToString("; ") { e ->
                "${e.name} ${e.sets}x${e.reps}" + (e.weightKg?.let { " @${it}kg" } ?: "")
            }
            sb.appendLine("- ${w.workout.epochDay.toLocalDate()} ${w.workout.type.label} \"${w.workout.title.take(40)}\" ${w.workout.durationMin}min, intensity ${w.workout.intensity}/3" +
                (w.workout.distanceKm?.let { ", ${it}km" } ?: "") + (if (ex.isNotEmpty()) ", exercises: $ex" else ""))
        }
        sb.appendLine()
    }

    private suspend fun money(sb: StringBuilder, from: LocalDate, to: LocalDate, s: AppSettings) {
        val code = s.currencyCode
        val txns = db.moneyDao().getTxnsBetween(from.toEpochDay(), to.toEpochDay())
        val loans = db.moneyDao().allLoans()
        sb.appendLine("# MONEY")
        val income = txns.filter { it.type == TxnType.INCOME }.sumOf { it.amountMinor }
        val expense = txns.filter { it.type == TxnType.EXPENSE }.sumOf { it.amountMinor }
        sb.appendLine("Income: ${Money.format(income, code)}, expenses: ${Money.format(expense, code)}, net: ${Money.format(income - expense, code)}.")
        if (s.monthlyBudgetMinor > 0) sb.appendLine("Monthly budget: ${Money.format(s.monthlyBudgetMinor, code)}.")
        val byCat = txns.filter { it.type == TxnType.EXPENSE }.groupBy { it.category }
            .mapValues { e -> e.value.sumOf { it.amountMinor } }.entries.sortedByDescending { it.value }
        if (byCat.isNotEmpty()) sb.appendLine("Expenses by category: " + byCat.joinToString(", ") { "${it.key} ${Money.format(it.value, code)}" })
        val incByCat = txns.filter { it.type == TxnType.INCOME }.groupBy { it.category }
            .mapValues { e -> e.value.sumOf { it.amountMinor } }.entries.sortedByDescending { it.value }
        if (incByCat.isNotEmpty()) sb.appendLine("Income by category: " + incByCat.joinToString(", ") { "${it.key} ${Money.format(it.value, code)}" })
        val largest = txns.filter { it.type == TxnType.EXPENSE }.sortedByDescending { it.amountMinor }.take(8)
        if (largest.isNotEmpty()) {
            sb.appendLine("Largest expenses:")
            largest.forEach { sb.appendLine("- ${it.epochDay.toLocalDate()} ${it.category} ${Money.format(it.amountMinor, code)}" + (if (it.note.isNotBlank()) " (${it.note.take(40)})" else "")) }
        }
        val open = loans.filter { !it.settled }
        if (open.isNotEmpty()) {
            sb.appendLine("Open lendings/borrowings:")
            open.forEach { l ->
                sb.appendLine("- ${if (l.direction == LoanDirection.LENT) "Lent to" else "Borrowed from"} ${l.person.take(30)}: remaining ${Money.format(l.remainingMinor, code)} of ${Money.format(l.amountMinor, code)}, since ${l.epochDay.toLocalDate()}" +
                    (l.dueEpochDay?.let { ", due ${it.toLocalDate()}" + if (it < to.toEpochDay()) " (OVERDUE)" else "" } ?: ""))
            }
        } else {
            sb.appendLine("No open lendings or borrowings.")
        }
        sb.appendLine()
    }

    private suspend fun tasks(sb: StringBuilder, from: LocalDate, to: LocalDate) {
        val all = db.todoDao().all()
        val today = to.toEpochDay()
        val open = all.filter { !it.done }
        val fromMillis = from.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val completed = all.filter { it.done && (it.completedAt ?: 0) >= fromMillis }
        sb.appendLine("# TASKS")
        sb.appendLine("Open: ${open.size} (overdue: ${open.count { (it.dueEpochDay ?: Long.MAX_VALUE) < today }}, due today: ${open.count { it.dueEpochDay == today }}). Completed in period: ${completed.size}. Created in period: ${all.count { it.createdAt >= fromMillis }}.")
        open.sortedWith(compareBy({ it.dueEpochDay ?: Long.MAX_VALUE }, { -it.priority.ordinal })).take(25).forEach {
            sb.appendLine("- [ ] ${it.title.take(60)} (priority ${it.priority.name}" + (it.dueEpochDay?.let { d -> ", due ${d.toLocalDate()}" + if (d < today) " OVERDUE" else "" } ?: "") + ")")
        }
        completed.take(15).forEach { sb.appendLine("- [x] ${it.title.take(60)}") }
        sb.appendLine()
    }

    private suspend fun notes(sb: StringBuilder, from: LocalDate, detailed: Boolean) {
        val fromMillis = from.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val all = db.noteDao().all().sortedWith(compareBy({ !it.pinned }, { -it.updatedAt }))
        val recent = all.filter { it.pinned || it.updatedAt >= fromMillis }
        sb.appendLine("# NOTES")
        sb.appendLine("Total notes: ${all.size}. Pinned or edited in period: ${recent.size}.")
        val limit = if (detailed) 1200 else 220
        recent.take(if (detailed) 30 else 12).forEach { n ->
            val title = n.title.ifBlank { "(untitled)" }.take(60)
            val body = n.content.replace("\n", " ").take(limit)
            sb.appendLine("- ${if (n.pinned) "[pinned] " else ""}$title: $body")
        }
        sb.appendLine()
    }
}
