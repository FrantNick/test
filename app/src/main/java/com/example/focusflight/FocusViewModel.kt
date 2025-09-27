package com.example.focusflight

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusflight.data.Achievement
import com.example.focusflight.data.FlightData
import com.example.focusflight.data.FlightLogEntry
import com.example.focusflight.data.FlightRepository
import com.example.focusflight.data.FlightRoute
import com.example.focusflight.data.SeatClass
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

enum class FocusPhase { PLANNING, FLYING, LANDED }

data class FocusUiState(
    val availableRoutes: List<FlightRoute> = FlightData.routes,
    val selectedRoute: FlightRoute = FlightData.routes.first(),
    val selectedCategory: SeatClass = SeatClass.values().first(),
    val phase: FocusPhase = FocusPhase.PLANNING,
    val remainingSeconds: Int = FlightData.routes.first().durationMinutes * 60,
    val history: List<FlightLogEntry> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val totalMiles: Int = 0,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val lastFlight: FlightLogEntry? = null,
    val lastFlightCompleted: Boolean = false,
    val dndAccessGranted: Boolean = false
)

class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FlightRepository(application)

    var uiState: FocusUiState by mutableStateOf(FocusUiState())
        private set

    private var timerJob: Job? = null
    private var activeStartTime: Long = 0L

    init {
        val history = repository.getHistory()
        val sound = repository.getSoundEnabled()
        val haptics = repository.getHapticsEnabled()
        val selected = history.firstOrNull()?.routeId?.let { id ->
            FlightData.routes.find { it.id == id }
        } ?: FlightData.routes.first()
        uiState = uiState.copy(
            history = history,
            achievements = computeAchievements(history),
            totalMiles = history.filter { it.completed }.sumOf { it.milesEarned },
            soundEnabled = sound,
            hapticsEnabled = haptics,
            selectedRoute = selected,
            remainingSeconds = selected.durationMinutes * 60
        )
    }

    fun updateDndAccess(granted: Boolean) {
        uiState = uiState.copy(dndAccessGranted = granted)
    }

    fun selectRoute(route: FlightRoute) {
        uiState = uiState.copy(
            selectedRoute = route,
            remainingSeconds = route.durationMinutes * 60
        )
    }

    fun selectCategory(category: SeatClass) {
        uiState = uiState.copy(selectedCategory = category)
    }

    fun toggleSound() {
        val enabled = !uiState.soundEnabled
        uiState = uiState.copy(soundEnabled = enabled)
        repository.setSoundEnabled(enabled)
    }

    fun toggleHaptics() {
        val enabled = !uiState.hapticsEnabled
        uiState = uiState.copy(hapticsEnabled = enabled)
        repository.setHapticsEnabled(enabled)
    }

    fun startSession() {
        val route = uiState.selectedRoute
        val category = uiState.selectedCategory
        timerJob?.cancel()
        val totalSeconds = route.durationMinutes * 60
        activeStartTime = System.currentTimeMillis()
        uiState = uiState.copy(
            phase = FocusPhase.FLYING,
            remainingSeconds = totalSeconds
        )
        timerJob = viewModelScope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                delay(1_000)
                remaining -= 1
                uiState = uiState.copy(remainingSeconds = remaining)
            }
            finalizeSession(completed = true, category = category, route = route)
        }
    }

    fun cancelSession() {
        val route = uiState.selectedRoute
        val category = uiState.selectedCategory
        timerJob?.cancel()
        finalizeSession(completed = false, category = category, route = route)
    }

    private fun finalizeSession(
        completed: Boolean,
        category: SeatClass,
        route: FlightRoute
    ) {
        val elapsedSeconds = route.durationMinutes * 60 - uiState.remainingSeconds
        val actualDurationMinutes = if (completed) route.durationMinutes else (elapsedSeconds / 60).coerceAtLeast(1)
        val entry = FlightLogEntry(
            id = UUID.randomUUID().toString(),
            routeId = route.id,
            routeLabel = "${route.origin} ✈ ${route.destination}",
            durationMinutes = actualDurationMinutes,
            category = category,
            timestamp = activeStartTime,
            completed = completed,
            milesEarned = actualDurationMinutes * route.milesPerMinute
        )
        repository.addEntry(entry)
        val history = repository.getHistory()
        uiState = uiState.copy(
            phase = FocusPhase.LANDED,
            remainingSeconds = 0,
            history = history,
            achievements = computeAchievements(history),
            totalMiles = history.filter { it.completed }.sumOf { it.milesEarned },
            lastFlight = entry,
            lastFlightCompleted = completed
        )
        timerJob = null
    }

    fun acknowledgeLanding() {
        timerJob?.cancel()
        val route = uiState.selectedRoute
        uiState = uiState.copy(
            phase = FocusPhase.PLANNING,
            remainingSeconds = route.durationMinutes * 60,
            lastFlight = null
        )
    }

    private fun computeAchievements(history: List<FlightLogEntry>): List<Achievement> {
        val completedFlights = history.count { it.completed }
        val totalMinutes = history.filter { it.completed }.sumOf { it.durationMinutes }
        val uniqueRoutes = history.filter { it.completed }.map { it.routeId }.toSet().size
        val achievements = listOf(
            Achievement(
                id = "first_flight",
                title = "First Flight",
                description = "Complete your first focus flight.",
                achieved = completedFlights >= 1
            ),
            Achievement(
                id = "frequent_flyer",
                title = "Frequent Flyer",
                description = "Complete 5 focus flights.",
                achieved = completedFlights >= 5
            ),
            Achievement(
                id = "world_traveler",
                title = "World Traveler",
                description = "Visit 6 destinations.",
                achieved = uniqueRoutes >= 6
            ),
            Achievement(
                id = "long_haul",
                title = "Long Haul Captain",
                description = "Accumulate 600 focus minutes.",
                achieved = totalMinutes >= 600
            )
        )
        return achievements
    }
}
