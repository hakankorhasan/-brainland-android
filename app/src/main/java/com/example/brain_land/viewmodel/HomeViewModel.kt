package com.example.brain_land.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.brain_land.data.DailyChallengeResponse
import com.example.brain_land.data.DailyStreak
import com.example.brain_land.data.DailySubmitResponse
import com.example.brain_land.data.GameItem
import com.example.brain_land.data.GameType
import com.example.brain_land.data.HomeRepository
import com.example.brain_land.data.LeaderboardResponse
import com.example.brain_land.data.PreferencesManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    // ── Player Profile (score, rating, rank, tier from /getPlayerProfile) ──
    val playerProfile   = MutableStateFlow<com.example.brain_land.data.PlayerProfileData?>(null)
    val isLoadingProfile = MutableStateFlow(true)

    // ── Full Player Profile (for ProfileScreen) — mirrors iOS ProfileView ──
    val playerProfileFull     = MutableStateFlow<com.example.brain_land.data.PlayerProfileFullResponse?>(null)
    val isLoadingProfileFull  = MutableStateFlow(false)
    val isRefreshingProfile   = MutableStateFlow(false)

    // ── Leaderboard ──
    val leaderboard     = MutableStateFlow<LeaderboardResponse?>(null)
    val isLoadingLeader = MutableStateFlow(true)

    // ── Games ──
    val games              = MutableStateFlow<List<GameItem>>(emptyList())
    val suggestedGames     = MutableStateFlow<List<GameType>>(emptyList())
    val isLoadingGames     = MutableStateFlow(true)

    // ── Daily Challenge — mirrors iOS DailyChallengeManager ──
    val dailyChallenge      = MutableStateFlow<DailyChallengeResponse?>(null)
    val isLoadingDaily      = MutableStateFlow(false)
    val isSubmittingDaily   = MutableStateFlow(false)
    val lastSubmitResult    = MutableStateFlow<DailySubmitResponse?>(null)
    val countdownText       = MutableStateFlow("--:--:--")
    val showCompletionOverlay = MutableStateFlow(false)

    // Legacy simple accessors used by HomeScreen mini-card
    val dailyStreak: StateFlow<Int> get() = _dailyStreak
    val completedPuzzles: StateFlow<Set<Int>> get() = _completedPuzzles
    private val _dailyStreak      = MutableStateFlow(0)
    private val _completedPuzzles = MutableStateFlow(setOf<Int>())

    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            nickname.value  = prefs.nickname.first()
            avatarUrl.value = prefs.avatarUrl.first()
        }
        fetchLeaderboard()
        fetchGames()
        fetchProfile()
        fetchDaily()
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

    /** Slim profile for HomeScreen RatingCard */
    fun fetchProfile() {
        viewModelScope.launch {
            isLoadingProfile.value = true
            val deviceId = prefs.deviceId.first()
            if (deviceId.isNotEmpty()) {
                playerProfile.value = repo.fetchPlayerProfile(deviceId)
            }
            isLoadingProfile.value = false
        }
    }

    /** Full profile for ProfileScreen — uses 5 min cache, mirrors iOS fetchAll() */
    fun fetchFullProfile(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!forceRefresh && playerProfileFull.value != null) return@launch
            isLoadingProfileFull.value = true
            val deviceId = prefs.deviceId.first()
            if (deviceId.isNotEmpty()) {
                playerProfileFull.value = repo.fetchFullProfile(deviceId, forceRefresh)
                // Also update the slim profile on HomeScreen
                playerProfileFull.value?.toProfileData()?.let { playerProfile.value = it }
            }
            isLoadingProfileFull.value = false
        }
    }

    /** Pull-to-refresh for ProfileScreen — force bypasses cache */
    fun refreshProfile() {
        viewModelScope.launch {
            isRefreshingProfile.value = true
            val deviceId = prefs.deviceId.first()
            if (deviceId.isNotEmpty()) {
                playerProfileFull.value = repo.fetchFullProfile(deviceId, forceRefresh = true)
                playerProfileFull.value?.toProfileData()?.let { playerProfile.value = it }
            }
            isRefreshingProfile.value = false
        }
    }

    fun fetchGames() {
        viewModelScope.launch {
            isLoadingGames.value = true
            val fetched = repo.fetchGameList()
            games.value = fetched
            // Pick 4 random suggested games — fallback to static list if empty
            val source = if (fetched.isNotEmpty())
                fetched.mapNotNull { it.resolvedType }.shuffled().take(4)
            else
                GameType.allTypes().shuffled().take(4)
            suggestedGames.value = source
            isLoadingGames.value = false
        }
    }

    // ── Daily Challenge ──

    fun fetchDaily(force: Boolean = false) {
        viewModelScope.launch {
            if (!force && dailyChallenge.value != null) return@launch
            isLoadingDaily.value = true
            val deviceId = prefs.deviceId.first()
            if (deviceId.isNotEmpty()) {
                val resp = repo.fetchDailyChallenge(deviceId)
                if (resp != null) {
                    dailyChallenge.value = resp
                    _dailyStreak.value      = resp.streak.currentStreak
                    _completedPuzzles.value = resp.completedPuzzles.toSet()
                    startCountdown(resp.nextResetIn)
                }
            }
            isLoadingDaily.value = false
        }
    }

    fun submitDailyPuzzle(
        puzzleIndex: Int,
        correct: Boolean,
        responseTime: Double,
        gameId: String,
        difficulty: Int,
        onResult: (DailySubmitResponse?) -> Unit = {}
    ) {
        viewModelScope.launch {
            isSubmittingDaily.value = true
            val deviceId = prefs.deviceId.first()
            val result = repo.submitDailyPuzzle(deviceId, puzzleIndex, correct, responseTime, gameId, difficulty)
            if (result != null) {
                lastSubmitResult.value = result
                // Update local state
                val current = dailyChallenge.value
                if (current != null) {
                    val newCompleted = (current.completedPuzzles + puzzleIndex).distinct().sorted()
                    dailyChallenge.value = current.copy(
                        completedPuzzles = newCompleted,
                        totalScore       = result.totalDailyScore,
                        allCompleted     = result.allCompleted,
                        streak           = result.streak
                    )
                }
                _dailyStreak.value = result.streak.currentStreak
                _completedPuzzles.value = dailyChallenge.value?.completedPuzzles?.toSet() ?: emptySet()
                if (result.allCompleted) {
                    showCompletionOverlay.value = true
                }
            }
            isSubmittingDaily.value = false
            onResult(result)
        }
    }

    private fun startCountdown(seconds: Int) {
        countdownJob?.cancel()
        var remaining = seconds
        countdownJob = viewModelScope.launch {
            while (remaining > 0) {
                val h = remaining / 3600
                val m = (remaining % 3600) / 60
                val s = remaining % 60
                countdownText.value = "%02d:%02d:%02d".format(h, m, s)
                delay(1_000)
                remaining--
            }
            countdownText.value = "New puzzles!"
            fetchDaily(force = true)
        }
    }

    fun refreshLeaderboard() = fetchLeaderboard()
}
