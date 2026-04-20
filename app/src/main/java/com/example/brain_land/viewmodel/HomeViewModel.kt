package com.example.brain_land.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.brain_land.data.GameItem
import com.example.brain_land.data.GameType
import com.example.brain_land.data.HomeRepository
import com.example.brain_land.data.LeaderboardResponse
import com.example.brain_land.data.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repo  = HomeRepository()
    private val prefs = PreferencesManager(application)

    // ── Profile (from DataStore) ──
    val nickname  = MutableStateFlow("")
    val avatarUrl = MutableStateFlow("")

    // ── Leaderboard ──
    val leaderboard     = MutableStateFlow<LeaderboardResponse?>(null)
    val isLoadingLeader = MutableStateFlow(true)

    // ── Games ──
    val games              = MutableStateFlow<List<GameItem>>(emptyList())
    val suggestedGames     = MutableStateFlow<List<GameType>>(emptyList())
    val isLoadingGames     = MutableStateFlow(true)

    // ── Daily challenge: simple streak & completion state ──
    val dailyStreak     = MutableStateFlow(0)
    val completedPuzzles = MutableStateFlow(setOf<Int>()) // indices 1-5

    init {
        viewModelScope.launch {
            nickname.value  = prefs.nickname.first()
            avatarUrl.value = prefs.avatarUrl.first()
        }
        fetchLeaderboard()
        fetchGames()
    }

    // ─────────────────────────────────
    // Public API
    // ─────────────────────────────────

    fun fetchLeaderboard() {
        viewModelScope.launch {
            isLoadingLeader.value = true
            val deviceId = prefs.deviceId.first()
            leaderboard.value = repo.fetchGlobalLeaderboard(limit = 3, deviceId = deviceId)
            isLoadingLeader.value = false
        }
    }

    fun fetchGames() {
        viewModelScope.launch {
            isLoadingGames.value = true
            val fetched = repo.fetchGames()
            games.value = fetched
            // Pick 4 random suggested games — fallback to static list if empty
            val source = if (fetched.isNotEmpty())
                fetched.map { it.gameType }.shuffled().take(4)
            else
                GameType.allTypes().shuffled().take(4)
            suggestedGames.value = source
            isLoadingGames.value = false
        }
    }

    fun refreshLeaderboard() = fetchLeaderboard()
}
