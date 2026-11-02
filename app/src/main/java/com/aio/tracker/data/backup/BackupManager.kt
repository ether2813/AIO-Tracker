package com.aio.tracker.data.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import com.aio.tracker.data.db.AiSummary
import com.aio.tracker.data.db.AppDatabase
import com.aio.tracker.data.db.ExerciseSet
import com.aio.tracker.data.db.JumuahOverride
import com.aio.tracker.data.db.Loan
import com.aio.tracker.data.db.MoneyTxn
import com.aio.tracker.data.db.Note
import com.aio.tracker.data.db.PrayerConfig
import com.aio.tracker.data.db.PrayerLog
import com.aio.tracker.data.db.TodoItem
import com.aio.tracker.data.db.Workout
import com.aio.tracker.data.prefs.AppSettings
import com.aio.tracker.data.prefs.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Everything the app knows, except the API key (which never leaves the device). */
@Serializable
data class BackupFile(
    val app: String = APP_ID,
    val formatVersion: Int = FORMAT_VERSION,
    val exportedAt: Long = System.currentTimeMillis(),
    val settings: AppSettings? = null,
    val prayerConfigs: List<PrayerConfig> = emptyList(),
    val jumuahOverrides: List<JumuahOverride> = emptyList(),
    val prayerLogs: List<PrayerLog> = emptyList(),
    val workouts: List<Workout> = emptyList(),
    val exercises: List<ExerciseSet> = emptyList(),
    val transactions: List<MoneyTxn> = emptyList(),
    val loans: List<Loan> = emptyList(),
    val todos: List<TodoItem> = emptyList(),
    val notes: List<Note> = emptyList(),
    val aiSummaries: List<AiSummary> = emptyList(),
) {
    val totalItems: Int
        get() = prayerLogs.size + jumuahOverrides.size + workouts.size + transactions.size +
            loans.size + todos.size + notes.size + aiSummaries.size

    companion object {
        const val APP_ID = "com.aio.tracker"
        const val FORMAT_VERSION = 1
    }
}

class BackupManager(
    private val db: AppDatabase,
    private val settings: SettingsRepository,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    suspend fun snapshot(): BackupFile = withContext(Dispatchers.IO) {
        db.withTransaction {
            BackupFile(
                settings = settings.current().copy(askedNotificationPermission = false),
                prayerConfigs = db.prayerDao().getConfigs(),
                jumuahOverrides = db.prayerDao().allOverrides(),
                prayerLogs = db.prayerDao().allLogs(),
                workouts = db.workoutDao().allWorkouts(),
                exercises = db.workoutDao().allExercises(),
                transactions = db.moneyDao().allTxns(),
                loans = db.moneyDao().allLoans(),
                todos = db.todoDao().all(),
                notes = db.noteDao().all(),
                aiSummaries = db.aiDao().all(),
            )
        }
    }

    fun encode(backup: BackupFile): String = json.encodeToString(BackupFile.serializer(), backup)

    fun decode(text: String): BackupFile {
        val file = json.decodeFromString(BackupFile.serializer(), text)
        require(file.app == BackupFile.APP_ID) { "This file is not an AIO Tracker backup." }
        require(file.formatVersion <= BackupFile.FORMAT_VERSION) {
            "This backup was made by a newer version of the app. Please update first."
        }
        return file
    }

    suspend fun exportTo(resolver: ContentResolver, uri: Uri): Int = withContext(Dispatchers.IO) {
        val backup = snapshot()
        val out = resolver.openOutputStream(uri, "wt") ?: error("Could not open file for writing")
        out.bufferedWriter(Charsets.UTF_8).use { it.write(encode(backup)) }
        backup.totalItems
    }

    suspend fun importFrom(resolver: ContentResolver, uri: Uri): Int = withContext(Dispatchers.IO) {
        val text = resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: error("Could not open file")
        val backup = decode(text)
        restore(backup)
        backup.totalItems
    }

    /** Replaces all data atomically: either everything is restored or nothing changes. */
    suspend fun restore(backup: BackupFile) = withContext(Dispatchers.IO) {
        val workoutIds = backup.workouts.map { it.id }.toSet()
        db.withTransaction {
            db.deleteEverything()
            db.ensureDefaults()
            if (backup.prayerConfigs.isNotEmpty()) db.prayerDao().upsertConfigs(backup.prayerConfigs)
            db.prayerDao().insertOverrides(backup.jumuahOverrides)
            db.prayerDao().insertLogs(backup.prayerLogs)
            db.workoutDao().insertWorkouts(backup.workouts)
            db.workoutDao().insertExercisesReplace(backup.exercises.filter { it.workoutId in workoutIds })
            db.moneyDao().insertTxns(backup.transactions)
            db.moneyDao().insertLoans(backup.loans)
            db.todoDao().insertAll(backup.todos)
            db.noteDao().insertAll(backup.notes)
            db.aiDao().insertAll(backup.aiSummaries)
        }
        backup.settings?.let { settings.restore(it) }
    }
}
