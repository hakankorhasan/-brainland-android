package com.example.brain_land.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val TAG = "GameResultRepo"
private const val BASE = "https://us-central1-mini-games-9a4e1.cloudfunctions.net"

// ─────────────────────────────────────────────────────────────────────────────
// GameResultResponse — mirrors iOS GameResultResponse
// ─────────────────────────────────────────────────────────────────────────────

data class GameResultResponse(
    val newRating: Int       = 0,
    val ratingChange: Int    = 0,
    val tier: String         = "Bronze",
    val scoreGained: Int     = 0,
    val newStreak: Int       = 0,
    val improved: Boolean    = false,
    val previousBest: Int?   = null,
    val previousRating: Int? = null,
    val weightedGlobalScore: Int? = null,
    val gameStats: GameStatsResult? = null
)

data class GameStatsResult(
    val gameId: String      = "",
    val bestScore: Int      = 0,
    val coefficient: Double = 1.0,
    val weightedScore: Int  = 0,
    val gamesPlayed: Int    = 0
)

// ─────────────────────────────────────────────────────────────────────────────
// GameResultRepository — POST /submitGameResult
// Mirrors iOS ScoreManager.shared.submitGameResult() exactly.
//
// ⚠️  Uses the SAME deviceId as PreferencesManager (DataStore KEY_DEVICE_ID)
//     so results are attributed to the correct account — just like iOS uses
//     StoryProgressManager.shared.deviceId throughout.
// ─────────────────────────────────────────────────────────────────────────────

class GameResultRepository(private val context: Context) {

    // Read deviceId from DataStore — same source used everywhere else in the app.
    // Falls back to a stable UUID in SharedPreferences if DataStore value is empty
    // (shouldn't happen after onboarding, but guards against edge cases).
    private suspend fun resolveDeviceId(): String {
        val prefs = PreferencesManager(context)
        val id    = prefs.deviceId.first()
        if (id.isNotEmpty()) return id

        // Fallback: stable UUID in SharedPrefs (never happens after onboarding)
        val sp = context.getSharedPreferences("device_fallback", Context.MODE_PRIVATE)
        return sp.getString("deviceId", null) ?: run {
            val generated = java.util.UUID.randomUUID().toString()
            sp.edit().putString("deviceId", generated).apply()
            Log.w(TAG, "⚠️ DataStore deviceId empty — using fallback UUID: $generated")
            generated
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /submitGameResult
    // Body mirrors iOS ScoreManager.submitGameResult() exactly:
    //   deviceId, gameId, level, difficulty, correct,
    //   responseTime, isStoryMode, hintsUsed
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun submitGameResult(
        gameId:       String,
        level:        Int,
        difficulty:   Int,
        correct:      Boolean,
        responseTime: Double,
        isStoryMode:  Boolean = false,
        hintsUsed:    Int     = 0
    ): GameResultResponse? = withContext(Dispatchers.IO) {
        try {
            val deviceId = resolveDeviceId()
            Log.d(TAG, "🚀 submitGameResult game=$gameId level=$level diff=$difficulty correct=$correct rt=$responseTime deviceId=${deviceId.take(8)}…")

            val body = JSONObject().apply {
                put("deviceId",     deviceId)
                put("gameId",       gameId)
                put("level",        level)
                put("difficulty",   difficulty)
                put("correct",      correct)
                put("responseTime", responseTime)
                put("isStoryMode",  isStoryMode)
                put("hintsUsed",    hintsUsed)
            }

            val url  = java.net.URL("$BASE/submitGameResult")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput     = true
            conn.connectTimeout = 10_000
            conn.readTimeout    = 15_000

            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val raw = try {
                conn.inputStream.bufferedReader().readText()
            } catch (e: Exception) {
                conn.errorStream?.bufferedReader()?.readText() ?: throw e
            } finally {
                conn.disconnect()
            }

            Log.d(TAG, "📦 raw: ${raw.take(400)}")
            val json = JSONObject(raw)

            if (!json.optBoolean("success", false)) {
                Log.w(TAG, "❌ server error: ${json.optString("error")}")
                return@withContext null
            }

            val gs = json.optJSONObject("gameStats")
            GameResultResponse(
                newRating           = json.optInt("newRating", 0),
                ratingChange        = json.optInt("ratingChange", 0),
                tier                = json.optString("tier", "Bronze"),
                scoreGained         = json.optInt("scoreGained", 0),
                newStreak           = json.optInt("newStreak", 0),
                improved            = json.optBoolean("improved", false),
                previousBest        = if (json.has("previousBest"))        json.optInt("previousBest")        else null,
                previousRating      = if (json.has("previousRating"))      json.optInt("previousRating")      else null,
                weightedGlobalScore = if (json.has("weightedGlobalScore")) json.optInt("weightedGlobalScore") else null,
                gameStats = gs?.let {
                    GameStatsResult(
                        gameId        = it.optString("gameId"),
                        bestScore     = it.optInt("bestScore", 0),
                        coefficient   = it.optDouble("coefficient", 1.0),
                        weightedScore = it.optInt("weightedScore", 0),
                        gamesPlayed   = it.optInt("gamesPlayed", 0)
                    )
                }
            ).also {
                val sign = if (it.ratingChange >= 0) "+" else ""
                Log.d(TAG, "✅ rating=${it.newRating} ($sign${it.ratingChange}) tier=${it.tier} score=${it.scoreGained} streak=${it.newStreak} improved=${it.improved}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ submitGameResult error: ${e.message}")
            null
        }
    }
}
