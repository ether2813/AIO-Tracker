package com.aio.tracker.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.Serializable

// ---------------------------------------------------------------- Salat

enum class Prayer(val label: String, val arabic: String) {
    FAJR("Fajr", "الفجر"),
    DHUHR("Dhuhr", "الظهر"),
    ASR("Asr", "العصر"),
    MAGHRIB("Maghrib", "المغرب"),
    ISHA("Isha", "العشاء"),
    JUMUAH("Jumu'ah", "الجمعة");

    companion object {
        val daily = listOf(FAJR, DHUHR, ASR, MAGHRIB, ISHA)
    }
}

enum class PrayerStatus(val label: String) {
    JAMAAH("Jama'ah"),
    ON_TIME("On time"),
    LATE("Late / Qada"),
    MISSED("Missed");

    val counts: Boolean get() = this != MISSED
}

/** The local (mosque) time for each prayer. [minuteOfDay] = hour * 60 + minute. */
@Serializable
@Entity(tableName = "prayer_config")
data class PrayerConfig(
    @PrimaryKey val prayer: Prayer,
    val minuteOfDay: Int,
    val reminderEnabled: Boolean = true,
    val reminderOffsetMin: Int = 10,
)

/** Default local prayer times; users change these to match their mosque. */
object PrayerDefaults {
    val configs: List<PrayerConfig> = listOf(
        PrayerConfig(Prayer.FAJR, 5 * 60 + 30),
        PrayerConfig(Prayer.DHUHR, 13 * 60 + 15),
        PrayerConfig(Prayer.ASR, 16 * 60 + 30),
        PrayerConfig(Prayer.MAGHRIB, 18 * 60 + 15),
        PrayerConfig(Prayer.ISHA, 19 * 60 + 45),
        PrayerConfig(Prayer.JUMUAH, 13 * 60 + 30, reminderOffsetMin = 30),
    )
}

/** A specific Friday with a Jumu'ah time different from the default one. */
@Serializable
@Entity(tableName = "jumuah_override")
data class JumuahOverride(
    @PrimaryKey val epochDay: Long,
    val minuteOfDay: Int,
    val note: String = "",
)

@Serializable
@Entity(tableName = "prayer_log", primaryKeys = ["epochDay", "prayer"])
data class PrayerLog(
    val epochDay: Long,
    val prayer: Prayer,
    val status: PrayerStatus,
    val updatedAt: Long = System.currentTimeMillis(),
)

// ---------------------------------------------------------------- Fitness

enum class WorkoutType(val label: String, val hasDistance: Boolean = false) {
    STRENGTH("Strength"),
    RUN("Run", true),
    WALK("Walk", true),
    CYCLING("Cycling", true),
    SWIM("Swim", true),
    CARDIO("Cardio"),
    HIIT("HIIT"),
    YOGA("Yoga / Mobility"),
    SPORTS("Sports"),
    OTHER("Other"),
}

@Serializable
@Entity(tableName = "workout", indices = [Index("epochDay")])
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val type: WorkoutType,
    val title: String,
    val durationMin: Int,
    val calories: Int? = null,
    val distanceKm: Double? = null,
    /** 1 = easy, 2 = moderate, 3 = hard */
    val intensity: Int = 2,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "exercise",
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutId")],
)
data class ExerciseSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val name: String,
    val sets: Int,
    val reps: Int,
    val weightKg: Double? = null,
    val position: Int = 0,
)

data class WorkoutWithExercises(
    @Embedded val workout: Workout,
    @Relation(parentColumn = "id", entityColumn = "workoutId")
    val exercises: List<ExerciseSet>,
)

// ---------------------------------------------------------------- Money

enum class TxnType { EXPENSE, INCOME }

/** Money is stored in minor units (amount * 100) to avoid floating point errors. */
@Serializable
@Entity(tableName = "money_txn", indices = [Index("epochDay")])
data class MoneyTxn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxnType,
    val amountMinor: Long,
    val category: String,
    val note: String = "",
    val epochDay: Long,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class LoanDirection(val label: String) {
    LENT("I lent"),
    BORROWED("I borrowed"),
}

@Serializable
@Entity(tableName = "loan")
data class Loan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val person: String,
    val direction: LoanDirection,
    val amountMinor: Long,
    val repaidMinor: Long = 0,
    val epochDay: Long,
    val dueEpochDay: Long? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
) {
    val remainingMinor: Long get() = (amountMinor - repaidMinor).coerceAtLeast(0)
    val settled: Boolean get() = repaidMinor >= amountMinor
}

// ---------------------------------------------------------------- Tasks

enum class Priority(val label: String) { LOW("Low"), MEDIUM("Medium"), HIGH("High") }

@Serializable
@Entity(tableName = "todo")
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val dueEpochDay: Long? = null,
    val priority: Priority = Priority.MEDIUM,
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
)

// ---------------------------------------------------------------- Notes

@Serializable
@Entity(tableName = "note")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: String = "",
    /** Index into the UI note palette. 0 = default surface. */
    val color: Int = 0,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

// ---------------------------------------------------------------- AI

@Serializable
@Entity(tableName = "ai_summary")
data class AiSummary(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val scope: String,
    val periodLabel: String,
    val question: String = "",
    val content: String,
    val model: String,
)
