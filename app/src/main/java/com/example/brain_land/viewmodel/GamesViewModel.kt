package com.example.brain_land.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.brain_land.data.GameItem
import com.example.brain_land.data.GameType
import com.example.brain_land.data.HomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class GamesViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = HomeRepository()

    // Raw list from backend
    val games      = MutableStateFlow<List<GameItem>>(emptyList())
    val isLoading  = MutableStateFlow(false)
    val hasLoaded  = MutableStateFlow(false)

    init { fetchGames() }

    fun fetchGames() {
        if (hasLoaded.value) return   // mirrors iOS guard !hasLoaded
        viewModelScope.launch {
            isLoading.value = true
            val result = repo.fetchGameList()
            games.value  = result
            hasLoaded.value = true
            isLoading.value = false
        }
    }

    fun refresh() {
        hasLoaded.value = false
        fetchGames()
    }

    // Visible game types as enum list (for suggested games, home screen etc.)
    val visibleGameTypes: List<GameType>
        get() = games.value.mapNotNull { it.resolvedType }
}
