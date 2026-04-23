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
    // GET /getPlayerProfile?deviceId=XXX  — mirrors iOS ScoreManager.fetchPlayerProfile()
    // Full response with stats, tierProgress, dailyChallenge, games + 5 min cache
    // ──────────────────────────────────────────────────────────────────────────

    // In-memory cache (mirrors iOS profileCache / lastProfileFetch)
    private var cachedFullProfile: PlayerProfileFullResponse? = null
    private var lastProfileFetchMs: Long = 0L
    private val CACHE_TTL_MS = 5 * 60 * 1_000L // 5 minutes

    /** Simplified profile used on HomeScreen */
    suspend fun fetchPlayerProfile(deviceId: String): PlayerProfileData? =
        fetchFullProfile(deviceId)?.toProfileData()

    /** Full profile — used by ProfileScreen */
    suspend fun fetchFullProfile(
        deviceId: String,
        forceRefresh: Boolean = false
    ): PlayerProfileFullResponse? = withContext(Dispatchers.IO) {
        // Check cache (mirrors iOS: Date().timeIntervalSince(lastFetch) < 300)
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedFullProfile != null && (now - lastProfileFetchMs) < CACHE_TTL_MS) {
            Log.d(TAG_HOME, "👤 Profile cache hit")
            return@withContext cachedFullProfile
        }

        try {
            val text = get("$BASE/getPlayerProfile?deviceId=${deviceId.ifEmpty { "unknown" }}")
            Log.d(TAG_HOME, "👤 Profile raw: ${text.take(400)}")

            val json = JSONObject(text)
            if (!json.optBoolean("success", false)) return@withContext null

            // --- profile info ---
            val profileJson = json.optJSONObject("profile") ?: JSONObject()
            val profileInfo = PlayerProfileInfo(
                nickname  = profileJson.optString("nickname").ifEmpty { null },
                avatarId  = profileJson.optString("avatarId").ifEmpty { null },
                avatarUrl = profileJson.optString("avatarUrl").ifEmpty { null },
                age       = if (profileJson.has("age")) profileJson.optInt("age") else null
            )

            // --- stats ---
            val statsJson = json.optJSONObject("stats") ?: JSONObject()
            val stats = PlayerProfileStats(
                gamesPlayed         = statsJson.optInt("gamesPlayed", 0),
                correctAnswers      = statsJson.optInt("correctAnswers", 0),
                winRate             = statsJson.optDouble("winRate", 0.0),
                bestStreak          = statsJson.optInt("bestStreak", 0),
                currentStreak       = statsJson.optInt("currentStreak", 0),
                weightedGlobalScore = statsJson.optInt("weightedGlobalScore", 0),
                globalScore         = statsJson.optInt("globalScore", 0),
                rating              = statsJson.optInt("rating", 0),
                tier                = statsJson.optString("tier", "Bronze").ifEmpty { "Bronze" },
                rank                = statsJson.optInt("rank", 0),
                uniqueGamesPlayed   = statsJson.optInt("uniqueGamesPlayed", 0),
                totalGames          = statsJson.optInt("totalGames", 12),
                memberSince         = statsJson.optString("memberSince").ifEmpty { null }
            )

            // --- daily challenge ---
            val dcJson = json.optJSONObject("dailyChallenge") ?: JSONObject()
            val dailyChallenge = PlayerProfileDailyChallenge(
                currentStreak      = dcJson.optInt("currentStreak", 0),
                bestStreak         = dcJson.optInt("bestStreak", 0),
                totalDaysCompleted = dcJson.optInt("totalDaysCompleted", 0),
                totalPuzzlesSolved = dcJson.optInt("totalPuzzlesSolved", 0)
            )

            // --- tier progress ---
            val tpJson = json.optJSONObject("tierProgress") ?: JSONObject()
            val tierProgress = PlayerProfileTierProgress(
                currentTier    = tpJson.optString("currentTier", "Bronze").ifEmpty { "Bronze" },
                currentTierMin = tpJson.optInt("currentTierMin", 0),
                currentTierMax = tpJson.optInt("currentTierMax", 999),
                nextTier       = tpJson.optString("nextTier").ifEmpty { null },
                nextTierMin    = if (tpJson.has("nextTierMin")) tpJson.optInt("nextTierMin") else null,
                progress       = tpJson.optDouble("progress", 0.0),
                pointsToNext   = tpJson.optInt("pointsToNext", 0)
            )

            // --- per-game stats ---
            val games = mutableListOf<PlayerProfileGame>()
            val gamesArr = json.optJSONArray("games")
            if (gamesArr != null) {
                for (i in 0 until gamesArr.length()) {
                    val g = gamesArr.getJSONObject(i)
                    games.add(PlayerProfileGame(
                        gameId        = g.optString("gameId"),
                        gameName      = g.optString("gameName"),
                        bestScore     = g.optInt("bestScore", 0),
                        coefficient   = g.optDouble("coefficient", 1.0),
                        weightedScore = g.optInt("weightedScore", 0),
                        gamesPlayed   = g.optInt("gamesPlayed", 0),
                        avgScore      = g.optDouble("avgScore", 0.0)
                    ))
                }
            }

            val response = PlayerProfileFullResponse(
                profile        = profileInfo,
                stats          = stats,
                tierProgress   = tierProgress,
                dailyChallenge = dailyChallenge,
                games          = games
            )

            cachedFullProfile = response
            lastProfileFetchMs = now
            Log.d(TAG_HOME, "👤 ✅ Profile: rank=${stats.rank} tier=${stats.tier} games=${stats.gamesPlayed}")
            response
        } catch (e: Exception) {
            Log.e(TAG_HOME, "fetchFullProfile error: ${e.message}")
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

// ────────────────────────────────────────────────────────────
// Data Models — mirrors iOS PlayerProfileResponse family
// ────────────────────────────────────────────────────────────

/** Minimal model still used on HomeScreen's RatingCard */
data class PlayerProfileData(
    val weightedGlobalScore: Int,
    val globalScore: Int,
    val rating: Int,
    val rank: Int,
    val tier: String
)

data class PlayerProfileInfo(
    val nickname: String?,
    val avatarId: String?,
    val avatarUrl: String?,
    val age: Int?
)

data class PlayerProfileStats(
    val gamesPlayed: Int,
    val correctAnswers: Int,
    val winRate: Double,
    val bestStreak: Int,
    val currentStreak: Int,
    val weightedGlobalScore: Int,
    val globalScore: Int,
    val rating: Int,
    val tier: String,
    val rank: Int,
    val uniqueGamesPlayed: Int,
    val totalGames: Int,
    val memberSince: String?
)

data class PlayerProfileDailyChallenge(
    val currentStreak: Int,
    val bestStreak: Int,
    val totalDaysCompleted: Int,
    val totalPuzzlesSolved: Int
)

data class PlayerProfileTierProgress(
    val currentTier: String,
    val currentTierMin: Int,
    val currentTierMax: Int,
    val nextTier: String?,
    val nextTierMin: Int?,
    val progress: Double,      // 0.0 – 1.0
    val pointsToNext: Int
)

data class PlayerProfileGame(
    val gameId: String,
    val gameName: String,
    val bestScore: Int,
    val coefficient: Double,
    val weightedScore: Int,
    val gamesPlayed: Int,
    val avgScore: Double
)

/** Full response — mirrors iOS PlayerProfileResponse */
data class PlayerProfileFullResponse(
    val profile: PlayerProfileInfo,
    val stats: PlayerProfileStats,
    val tierProgress: PlayerProfileTierProgress,
    val dailyChallenge: PlayerProfileDailyChallenge,
    val games: List<PlayerProfileGame>
) {
    /** Convert to the slim model used on HomeScreen */
    fun toProfileData() = PlayerProfileData(
        weightedGlobalScore = stats.weightedGlobalScore,
        globalScore         = stats.globalScore,
        rating              = stats.rating,
        rank                = stats.rank,
        tier                = stats.tier
    )
}
