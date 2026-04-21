package com.example.brain_land.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

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
    // GET /getGameList  — mirrors iOS GameDataManager.fetchGames()
    //
    // • Only isVisible=true items are returned
    // • Sorted by `order`
    // • Local-only games (pathClearing, liquidSort) appended if not
    //   already present in the backend list (mirrors iOS visibleGameTypes)
    // ──────────────────────────────────────────────────────────

    suspend fun fetchGameList(): List<GameItem> = withContext(Dispatchers.IO) {
        try {
            val text = get("$BASE/getGameList")
            Log.d(TAG_HOME, "🎮 getGameList raw: ${text.take(400)}")

            val json = JSONObject(text)
            if (!json.optBoolean("success", false)) {
                Log.w(TAG_HOME, "🎮 getGameList not success — using local fallback")
                return@withContext localOnlyFallback()
            }

            val arr = json.optJSONArray("games") ?: return@withContext localOnlyFallback()

            val items = (0 until arr.length()).mapNotNull { i ->
                val g = arr.getJSONObject(i)
                val isVisible = g.optBoolean("isVisible", true)
                if (!isVisible) return@mapNotNull null

                GameItem(
                    id           = g.optString("id", UUID.randomUUID().toString()),
                    name         = g.optString("name", ""),
                    subtitle     = g.optString("subtitle", ""),
                    gameType     = g.optString("gameType", ""),
                    hasStoryMode = g.optBoolean("hasStoryMode", false),
                    requiresPro  = g.optBoolean("requiresPro", false),
                    order        = g.optInt("order", 0),
                    isVisible    = true
                )
            }.sortedBy { it.order }

            Log.d(TAG_HOME, "🎮 ✅ ${items.size} visible games loaded")

            // Mirror iOS: append local-only games if missing
            val result = items.toMutableList()
            addLocalOnlyIfMissing(result, ".pathClearing")
            addLocalOnlyIfMissing(result, ".liquidSort")
            result

        } catch (e: Exception) {
            Log.e(TAG_HOME, "🎮 fetchGameList error: ${e.message}")
            localOnlyFallback()
        }
    }

    // Mirror iOS local-only games (not yet on backend)
    private fun localOnlyFallback(): List<GameItem> = listOf(
        GameItem(id = "pathClearing", name = "Arrow Puzzle", gameType = ".pathClearing", order = 98),
        GameItem(id = "liquidSort",   name = "Liquid Sort",   gameType = ".liquidSort",   order = 99)
    )

    private fun addLocalOnlyIfMissing(list: MutableList<GameItem>, gameTypeStr: String) {
        val alreadyPresent = list.any { item ->
            item.gameType == gameTypeStr ||
            item.gameType == gameTypeStr.trimStart('.')
        }
        if (!alreadyPresent) {
            val gt = GameType.from(gameTypeStr) ?: return
            list.add(GameItem(id = gt.gameId, name = gt.displayName, gameType = gameTypeStr, order = 99))
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /getPlayerProfile?deviceId=XXX
    // Returns weightedGlobalScore, globalScore, rating, rank, tier
    // Mirrors iOS ScoreManager.fetchPlayerProfile()
    // ──────────────────────────────────────────────────────────────────────────

    suspend fun fetchPlayerProfile(deviceId: String): PlayerProfileData? =
        withContext(Dispatchers.IO) {
            try {
                val text = get("$BASE/getPlayerProfile?deviceId=${deviceId.ifEmpty { "unknown" }}")
                Log.d(TAG_HOME, "Profile raw: ${text.take(300)}")

                val json  = JSONObject(text)
                if (!json.optBoolean("success", false)) return@withContext null

                val stats = json.optJSONObject("stats") ?: return@withContext null
                PlayerProfileData(
                    weightedGlobalScore = stats.optInt("weightedGlobalScore", 0),
                    globalScore         = stats.optInt("globalScore", 0),
                    rating              = stats.optInt("rating", 0),
                    rank                = stats.optInt("rank", 0),
                    tier                = stats.optString("tier", "bronze").ifEmpty { "bronze" }
                )
            } catch (e: Exception) {
                Log.e(TAG_HOME, "fetchPlayerProfile error: ${e.message}")
                null
            }
        }

    // ──────────────────────────────────────────────────────────────────────────
    // HTTP helper
    // ──────────────────────────────────────────────────────────────────────────

    private fun get(urlStr: String): String {
        val conn = java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10_000
        conn.readTimeout    = 10_000
        return try { conn.inputStream.bufferedReader().readText() }
        finally { conn.disconnect() }
    }
}

// Minimal profile data for HomeScreen
data class PlayerProfileData(
    val weightedGlobalScore: Int,
    val globalScore: Int,
    val rating: Int,
    val rank: Int,
    val tier: String
)
