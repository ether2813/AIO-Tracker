package com.aio.tracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction

@Database(
    entities = [
        PrayerConfig::class,
        JumuahOverride::class,
        PrayerLog::class,
        Workout::class,
        ExerciseSet::class,
        MoneyTxn::class,
        Loan::class,
        TodoItem::class,
        Note::class,
        AiSummary::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun prayerDao(): PrayerDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun moneyDao(): MoneyDao
    abstract fun todoDao(): TodoDao
    abstract fun noteDao(): NoteDao
    abstract fun aiDao(): AiDao

    /** Default local prayer times. Users change these to match their mosque. */
    suspend fun ensureDefaults() {
        prayerDao().insertConfigsIfMissing(DEFAULT_PRAYER_CONFIGS)
    }

    /** Deletes every row. Must be called inside [withTransaction] when combined with inserts. */
    suspend fun deleteEverything() {
        workoutDao().deleteAllExercises()
        workoutDao().deleteAllWorkouts()
        prayerDao().deleteAllLogs()
        prayerDao().deleteAllOverrides()
        prayerDao().deleteAllConfigs()
        moneyDao().deleteAllTxns()
        moneyDao().deleteAllLoans()
        todoDao().deleteAll()
        noteDao().deleteAll()
        aiDao().deleteAll()
    }

    suspend fun wipeAll() {
        withTransaction {
            deleteEverything()
            ensureDefaults()
        }
    }

    companion object {
        val DEFAULT_PRAYER_CONFIGS: List<PrayerConfig> get() = PrayerDefaults.configs

        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "aio-tracker.db")
                .build()
                .also { instance = it }
        }
    }
}
