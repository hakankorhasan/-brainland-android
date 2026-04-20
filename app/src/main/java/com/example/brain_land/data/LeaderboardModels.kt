package com.example.brain_land.data

// ──────────────────────────────────────────────────────────
// Mirrors iOS LeaderboardPlayer struct (ScoreManager.swift)
// ──────────────────────────────────────────────────────────

data class LeaderboardPlayer(
    val uid: String? = null,
    val deviceId: String? = null,
    val rank: Int = 0,
    val nickname: String? = null,
    val username: String? = null,
    val avatarUrl: String? = null,
    val globalScore: Int = 0,
    val weightedGlobalScore: Int? = null,
    val bestScore: Int? = null,
    val tier: String = "bronze",
    val gamesPlayed: Int = 0,
    val bestStreak: Int? = null
) {
    val id: String get() = uid ?: deviceId ?: rank.toString()

    // iOS: prefers weightedGlobalScore, falls back to globalScore
    val displayScore: Int get() = weightedGlobalScore ?: globalScore

    val displayTier: String get() = tier.replaceFirstChar { it.uppercaseChar() }

    // iOS: nickname ?? username ?? "Player"
    val displayName: String get() {
        val name = (nickname ?: username ?: "").trim()
        return if (name.isEmpty() || name == "Unknown") "Player" else name
    }
}

// ──────────────────────────────────────────────────────────
// Mirrors iOS LeaderboardResponse struct
// ──────────────────────────────────────────────────────────

data class LeaderboardResponse(
    val players: List<LeaderboardPlayer> = emptyList(),
    val myRank: Int = 0,
    val myScore: Int = 0,
    val myTier: String = "bronze",
    val myUid: String? = null
)
