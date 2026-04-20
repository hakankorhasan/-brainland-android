package com.example.brain_land.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val TAG_HOME = "HomeRepo"
private const val BASE = "https://us-central1-mini-games-9a4e1.cloudfunctions.net"

class HomeRepository {

    // ── Leaderboard (top-3 + my rank) ──
    suspend fun fetchLeaderboard(limit: Int = 3): LeaderboardResponse = withContext(Dispatchers.IO) {
        try {
            val text = get("$BASE/getLeaderboard?limit=$limit")
            Log.d(TAG_HOME, "Leaderboard raw: ${text.take(400)}")
            val json = JSONObject(text)
            if (!json.optBoolean("success", false)) return@withContext LeaderboardResponse()

            val playersArr = json.optJSONArray("players") ?: return@withContext LeaderboardResponse()
            val players = (0 until playersArr.length()).map { i ->
                val p = playersArr.getJSONObject(i)
                LeaderboardPlayer(
                    uid         = p.optString("uid"),
                    rank        = p.optInt("rank", i + 1),
                    displayName = p.optString("displayName", "—"),
                    score       = p.optInt("score", 0),
                    avatarUrl   = p.optString("avatarUrl").ifEmpty { null },
                    tier        = p.optString("tier", "bronze")
                )
            }

            LeaderboardResponse(
                players = players,
                myScore = json.optInt("myScore", 0),
                myRank  = json.optInt("myRank", 0),
                myTier  = json.optString("myTier", "bronze")
            )
        } catch (e: Exception) {
            Log.e(TAG_HOME, "fetchLeaderboard error: ${e.message}")
            LeaderboardResponse()
        }
    }

    // ── Games list from Firebase ──
    suspend fun fetchGames(): List<GameItem> = withContext(Dispatchers.IO) {
        try {
            val text = get("$BASE/getGames")
            Log.d(TAG_HOME, "Games raw: ${text.take(200)}")
            val json = JSONObject(text)
            if (!json.optBoolean("success", false)) return@withContext emptyList()

            val arr = json.optJSONArray("games") ?: return@withContext emptyList()
            (0 until arr.length()).mapNotNull { i ->
                val g = arr.getJSONObject(i)
                val typeStr = g.optString("gameType")
                val gt = GameType.from(typeStr) ?: return@mapNotNull null
                GameItem(
                    id       = g.optString("id"),
                    gameType = gt,
                    name     = g.optString("name", gt.displayName),
                    subtitle = g.optString("subtitle", ""),
                    order    = g.optInt("order", 99)
                )
            }.sortedBy { it.order }
        } catch (e: Exception) {
            Log.e(TAG_HOME, "fetchGames error: ${e.message}")
            emptyList()
        }
    }

    private fun get(urlStr: String): String {
        val conn = java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        return try { conn.inputStream.bufferedReader().readText() }
        finally { conn.disconnect() }
    }
}

data class GameItem(
    val id: String,
    val gameType: GameType,
    val name: String,
    val subtitle: String,
    val order: Int
)
