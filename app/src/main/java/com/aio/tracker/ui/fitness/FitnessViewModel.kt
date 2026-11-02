package com.aio.tracker.ui.fitness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.aio.tracker.AppContainer
import com.aio.tracker.data.db.ExerciseSet
import com.aio.tracker.data.db.Workout
import com.aio.tracker.data.db.WorkoutWithExercises
import com.aio.tracker.data.prefs.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FitnessViewModel(private val c: AppContainer) : ViewModel() {
    private val dao = c.db.workoutDao()

    val settings: StateFlow<AppSettings> = c.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val workouts: StateFlow<List<WorkoutWithExercises>?> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun save(workout: Workout, exercises: List<ExerciseSet>) = viewModelScope.launch {
        c.db.withTransaction {
            val id = dao.insertWorkout(workout)
            val workoutId = if (workout.id != 0L) workout.id else id
            dao.deleteExercisesFor(workoutId)
            dao.insertExercises(exercises.mapIndexed { i, e -> e.copy(id = 0, workoutId = workoutId, position = i) })
        }
    }

    fun delete(id: Long) = viewModelScope.launch { dao.deleteWorkout(id) }

    fun setGoal(minutes: Int) = viewModelScope.launch { c.settings.setWorkoutGoal(minutes) }
}
