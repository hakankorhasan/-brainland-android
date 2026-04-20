package com.example.brain_land.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private const val TAG_HOME = "HomeRepo"
private const val BASE = "https://us-central1-mini-games-9a4e1.cloudfunctions.net"

class HomeRepository {

    // ──────────────────────────────────────────────────────────
    // GET /getGlobalLeaderboard?limit=N&deviceId=XXX
    // ──────────────────────────────────────────────────────────

    suspend fun fetchGlobalLeaderboard(
        limit: Int = 200,
        deviceId: String = ""
    ): LeaderboardResponse = withContext(Dispatchers.IO) {
        try {
            val clampedLimit = limit.coerceIn(1, 200)
            val url = "$BASE/getGlobalLeaderboard?limit=$clampedLimit&deviceId=${deviceId.ifEmpty { "unknown" }}"
            val text = get(url)
            Log.d(TAG_HOME, "Leaderboard raw: ${text.take(400)}")

            val json = JSONObject(text)
            if (!json.optBoolean("success", false)) return@withContext LeaderboardResponse()

            val arr = json.optJSONArray("players") ?: JSONArray()
            val players = (0 until arr.length()).map { i ->
                val p = arr.getJSONObject(i)
                LeaderboardPlayer(
                    uid         = p.optString("uid").ifEmpty { null },
                    deviceId    = p.optString("deviceId").ifEmpty { null },
                    rank        = p.optInt("rank", i + 1),
                    nickname    = p.optString("nickname").ifEmpty { null },
                    username    = p.optString("username").ifEmpty { null },
                    avatarUrl   = p.optString("avatarUrl").ifEmpty { null },
                    globalScore = p.optInt("globalScore", 0),
                    weightedGlobalScore = if (p.has("weightedGlobalScore")) p.optInt("weightedGlobalScore", 0) else null,
                    bestScore   = if (p.has("bestScore")) p.optInt("bestScore", 0) else null,
                    tier        = p.optString("tier", "bronze").ifEmpty { "bronze" },
                    gamesPlayed = p.optInt("gamesPlayed", 0),
                    bestStreak  = if (p.has("bestStreak")) p.optInt("bestStreak", 0) else null
                )
            }

            LeaderboardResponse(
                players = players,
                myRank  = json.optInt("myRank", 0),
                myScore = json.optInt("myScore", 0),
                myTier  = json.optString("myTier", "bronze"),
                myUid   = json.optString("myUid").ifEmpty { null }
            )
        } catch (e: Exception) {
            Log.e(TAG_HOME, "fetchGlobalLeaderboard error: ${e.message}")
            LeaderboardResponse()
        }
    }

    // ──────────────────────────────────────────────────────────
    // GET /getGames  (game list for home screen)
    // ──────────────────────────────────────────────────────────

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

    // ──────────────────────────────────────────────────────────
    // HTTP helper
    // ──────────────────────────────────────────────────────────

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
