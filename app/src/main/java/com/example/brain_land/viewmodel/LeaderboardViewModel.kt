package com.example.brain_land.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.brain_land.data.HomeRepository
import com.example.brain_land.data.LeaderboardPlayer
import com.example.brain_land.data.LeaderboardResponse
import com.example.brain_land.data.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LeaderboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repo  = HomeRepository()
    private val prefs = PreferencesManager(application)

    // ── State ──
    val allPlayers   = MutableStateFlow<List<LeaderboardPlayer>>(emptyList())
    val response     = MutableStateFlow<LeaderboardResponse?>(null)
    val isLoading    = MutableStateFlow(false)
    val myDeviceId   = MutableStateFlow("")

    // Smart list window config (mirrors iOS constants)
    val contextRadius = 2   // ranks shown above/below current user
    val topCount      = 7   // ranks 4-10 always shown
    val tailCount     = 3   // last N always shown

    init {
        viewModelScope.launch {
            myDeviceId.value = prefs.deviceId.first()
        }
        fetchLeaderboard()
    }

    fun fetchLeaderboard() {
        viewModelScope.launch {
            isLoading.value = true
            val deviceId = prefs.deviceId.first()
            myDeviceId.value = deviceId
            val result = repo.fetchGlobalLeaderboard(limit = 200, deviceId = deviceId)
            response.value = result
            allPlayers.value = result.players
            isLoading.value = false
        }
    }

    fun isMe(player: LeaderboardPlayer): Boolean {
        val myId = myDeviceId.value
        if (myId.isEmpty()) return false
        if (!player.uid.isNullOrEmpty() && player.uid == myId) return true
        if (!player.deviceId.isNullOrEmpty() && player.deviceId == myId) return true
        return false
    }

    // ── iOS buildListItems logic ported to Kotlin ──
    // Builds rank-4+ portion as a flat list of SmartItem (player or ellipsis)
    fun buildSmartList(players: List<LeaderboardPlayer>): List<SmartItem> {
        if (players.size <= 3) return emptyList()

        val rankList = players.drop(3)  // rank 4 onward
        val n = rankList.size
        val myRank = response.value?.myRank ?: 0

        // myIdx in rankList: myRank is 1-based overall, rank 4 = index 0
        val myIdx = myRank - 4  // negative if user is in podium

        val showIndices = mutableSetOf<Int>()

        // Top block: 0 until topCount
        for (i in 0 until minOf(topCount, n)) showIndices.add(i)

        // Context block: myIdx ± contextRadius
        if (myIdx >= 0) {
            val lo = maxOf(0, myIdx - contextRadius)
            val hi = minOf(n - 1, myIdx + contextRadius)
            for (i in lo..hi) showIndices.add(i)
        }

        // Tail block: last tailCount
        for (i in maxOf(0, n - tailCount) until n) showIndices.add(i)

        val sortedIndices = showIndices.sorted()
        val items = mutableListOf<SmartItem>()
        var prev = -1

        for (idx in sortedIndices) {
            if (prev >= 0) {
                val gap = idx - prev - 1
                if (gap > 0) {
                    items.add(SmartItem.Ellipsis(skippedCount = gap, id = "ellipsis_${prev}_${idx}"))
                }
            }
            items.add(SmartItem.Player(rankList[idx]))
            prev = idx
        }
        return items
    }
}

sealed class SmartItem {
    abstract val id: String
    data class Player(val player: LeaderboardPlayer) : SmartItem() {
        override val id: String get() = player.id
    }
    data class Ellipsis(val skippedCount: Int, override val id: String) : SmartItem()
}
