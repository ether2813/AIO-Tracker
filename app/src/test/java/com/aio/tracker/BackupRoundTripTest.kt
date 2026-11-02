package com.aio.tracker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aio.tracker.data.backup.BackupManager
import com.aio.tracker.data.db.AppDatabase
import com.aio.tracker.data.db.ExerciseSet
import com.aio.tracker.data.db.Loan
import com.aio.tracker.data.db.LoanDirection
import com.aio.tracker.data.db.MoneyTxn
import com.aio.tracker.data.db.Note
import com.aio.tracker.data.db.Prayer
import com.aio.tracker.data.db.PrayerLog
import com.aio.tracker.data.db.PrayerStatus
import com.aio.tracker.data.db.TodoItem
import com.aio.tracker.data.db.TxnType
import com.aio.tracker.data.db.Workout
import com.aio.tracker.data.db.WorkoutType
import com.aio.tracker.data.prefs.SettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class BackupRoundTripTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun memDb() = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()

    @Test
    fun exportThenImportRestoresEverything() = runBlocking {
        val settings = SettingsRepository(context)
        val source = memDb()
        source.ensureDefaults()
        source.prayerDao().upsertLog(PrayerLog(20000, Prayer.FAJR, PrayerStatus.JAMAAH))
        val wid = source.workoutDao().insertWorkout(Workout(epochDay = 20000, type = WorkoutType.RUN, title = "Morning run", durationMin = 30, distanceKm = 5.2))
        source.workoutDao().insertExercises(listOf(ExerciseSet(workoutId = wid, name = "Sprints", sets = 4, reps = 1)))
        source.moneyDao().upsertTxn(MoneyTxn(type = TxnType.EXPENSE, amountMinor = 1250, category = "Food", epochDay = 20000))
        source.moneyDao().upsertLoan(Loan(person = "Ali", direction = LoanDirection.LENT, amountMinor = 50000, repaidMinor = 10000, epochDay = 20000))
        source.todoDao().upsert(TodoItem(title = "Call mum"))
        source.noteDao().insert(Note(title = "Dua", content = "Rabbi zidni ilma", pinned = true))

        val sourceBackup = BackupManager(source, settings)
        val json = sourceBackup.encode(sourceBackup.snapshot())
        assertTrue(json.contains("Rabbi zidni ilma"))

        val target = memDb()
        target.todoDao().upsert(TodoItem(title = "Should be replaced"))
        val targetBackup = BackupManager(target, settings)
        targetBackup.restore(targetBackup.decode(json))

        assertEquals(1, target.prayerDao().allLogs().size)
        assertEquals(PrayerStatus.JAMAAH, target.prayerDao().allLogs().first().status)
        assertEquals(1, target.workoutDao().allWorkouts().size)
        assertEquals(1, target.workoutDao().allExercises().size)
        assertEquals(1250L, target.moneyDao().allTxns().first().amountMinor)
        assertEquals(40000L, target.moneyDao().allLoans().first().remainingMinor)
        assertEquals(listOf("Call mum"), target.todoDao().all().map { it.title })
        assertEquals("Dua", target.noteDao().all().first().title)
        assertEquals(6, target.prayerDao().getConfigs().size)
        source.close()
        target.close()
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsForeignFiles() {
        val db = memDb()
        BackupManager(db, SettingsRepository(context)).decode("""{"app":"something.else","formatVersion":1}""")
    }
}
