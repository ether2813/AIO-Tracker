package com.aio.tracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerDao {
    @Query("SELECT * FROM prayer_config")
    fun observeConfigs(): Flow<List<PrayerConfig>>

    @Query("SELECT * FROM prayer_config")
    suspend fun getConfigs(): List<PrayerConfig>

    @Upsert
    suspend fun upsertConfigs(configs: List<PrayerConfig>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConfigsIfMissing(configs: List<PrayerConfig>)

    @Query("SELECT * FROM jumuah_override WHERE epochDay >= :fromDay ORDER BY epochDay")
    fun observeOverridesFrom(fromDay: Long): Flow<List<JumuahOverride>>

    @Query("SELECT * FROM jumuah_override WHERE epochDay >= :fromDay ORDER BY epochDay")
    suspend fun getOverridesFrom(fromDay: Long): List<JumuahOverride>

    @Upsert
    suspend fun upsertOverride(override: JumuahOverride)

    @Query("DELETE FROM jumuah_override WHERE epochDay = :day")
    suspend fun deleteOverride(day: Long)

    @Query("SELECT * FROM prayer_log WHERE epochDay BETWEEN :fromDay AND :toDay")
    fun observeLogs(fromDay: Long, toDay: Long): Flow<List<PrayerLog>>

    @Query("SELECT * FROM prayer_log WHERE epochDay BETWEEN :fromDay AND :toDay")
    suspend fun getLogs(fromDay: Long, toDay: Long): List<PrayerLog>

    @Upsert
    suspend fun upsertLog(log: PrayerLog)

    @Query("DELETE FROM prayer_log WHERE epochDay = :day AND prayer = :prayer")
    suspend fun deleteLog(day: Long, prayer: Prayer)

    // backup
    @Query("SELECT * FROM jumuah_override") suspend fun allOverrides(): List<JumuahOverride>
    @Query("SELECT * FROM prayer_log") suspend fun allLogs(): List<PrayerLog>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertOverrides(items: List<JumuahOverride>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertLogs(items: List<PrayerLog>)
    @Query("DELETE FROM prayer_config") suspend fun deleteAllConfigs()
    @Query("DELETE FROM jumuah_override") suspend fun deleteAllOverrides()
    @Query("DELETE FROM prayer_log") suspend fun deleteAllLogs()
}

@Dao
interface WorkoutDao {
    @Transaction
    @Query("SELECT * FROM workout ORDER BY epochDay DESC, createdAt DESC")
    fun observeAll(): Flow<List<WorkoutWithExercises>>

    @Transaction
    @Query("SELECT * FROM workout WHERE epochDay BETWEEN :fromDay AND :toDay ORDER BY epochDay")
    suspend fun getBetween(fromDay: Long, toDay: Long): List<WorkoutWithExercises>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: Workout): Long

    @Query("DELETE FROM exercise WHERE workoutId = :workoutId")
    suspend fun deleteExercisesFor(workoutId: Long)

    @Insert
    suspend fun insertExercises(items: List<ExerciseSet>)

    @Query("DELETE FROM workout WHERE id = :id")
    suspend fun deleteWorkout(id: Long)

    // backup
    @Query("SELECT * FROM workout") suspend fun allWorkouts(): List<Workout>
    @Query("SELECT * FROM exercise") suspend fun allExercises(): List<ExerciseSet>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertWorkouts(items: List<Workout>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertExercisesReplace(items: List<ExerciseSet>)
    @Query("DELETE FROM exercise") suspend fun deleteAllExercises()
    @Query("DELETE FROM workout") suspend fun deleteAllWorkouts()
}

@Dao
interface MoneyDao {
    @Query("SELECT * FROM money_txn ORDER BY epochDay DESC, createdAt DESC")
    fun observeTxns(): Flow<List<MoneyTxn>>

    @Query("SELECT * FROM money_txn WHERE epochDay BETWEEN :fromDay AND :toDay ORDER BY epochDay")
    suspend fun getTxnsBetween(fromDay: Long, toDay: Long): List<MoneyTxn>

    @Upsert suspend fun upsertTxn(txn: MoneyTxn)
    @Delete suspend fun deleteTxn(txn: MoneyTxn)

    @Query("SELECT * FROM loan ORDER BY epochDay DESC, createdAt DESC")
    fun observeLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loan")
    suspend fun allLoans(): List<Loan>

    @Upsert suspend fun upsertLoan(loan: Loan)
    @Delete suspend fun deleteLoan(loan: Loan)

    // backup
    @Query("SELECT * FROM money_txn") suspend fun allTxns(): List<MoneyTxn>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTxns(items: List<MoneyTxn>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertLoans(items: List<Loan>)
    @Query("DELETE FROM money_txn") suspend fun deleteAllTxns()
    @Query("DELETE FROM loan") suspend fun deleteAllLoans()
}

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo ORDER BY done ASC, CASE WHEN dueEpochDay IS NULL THEN 1 ELSE 0 END, dueEpochDay ASC, createdAt DESC")
    fun observeAll(): Flow<List<TodoItem>>

    @Query("SELECT * FROM todo")
    suspend fun all(): List<TodoItem>

    @Upsert suspend fun upsert(item: TodoItem)
    @Delete suspend fun delete(item: TodoItem)

    @Query("DELETE FROM todo WHERE done = 1")
    suspend fun clearCompleted()

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<TodoItem>)
    @Query("DELETE FROM todo") suspend fun deleteAll()
}

@Dao
interface AiDao {
    @Query("SELECT * FROM ai_summary ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AiSummary>>

    @Query("SELECT * FROM ai_summary") suspend fun all(): List<AiSummary>

    @Insert suspend fun insert(item: AiSummary): Long
    @Query("DELETE FROM ai_summary WHERE id = :id") suspend fun delete(id: Long)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<AiSummary>)
    @Query("DELETE FROM ai_summary") suspend fun deleteAll()
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM note ORDER BY pinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<Note>>

    @Query("SELECT * FROM note WHERE id = :id")
    suspend fun get(id: Long): Note?

    @Query("SELECT * FROM note") suspend fun all(): List<Note>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(note: Note): Long
    @Query("DELETE FROM note WHERE id = :id") suspend fun delete(id: Long)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<Note>)
    @Query("DELETE FROM note") suspend fun deleteAll()
}
