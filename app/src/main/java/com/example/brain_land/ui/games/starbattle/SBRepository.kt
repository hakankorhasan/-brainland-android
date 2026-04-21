package com.example.brain_land.ui.games.starbattle

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

// ─────────────────────────────────────────────────────────────────────────────
// SBRepository — mirrors iOS GalacticBeaconsLevelManager fetch logic
// Hits the same backend endpoint: getStarBattleLevels
// ─────────────────────────────────────────────────────────────────────────────

object SBRepository {

    private const val BASE = "https://us-central1-mini-games-9a4e1.cloudfunctions.net"

    /** Fetch a single playable level. Returns null on error. */
    suspend fun fetchLevel(number: Int): SBLevel? = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE/getStarBattleLevels?level=$number"
            val raw = URL(url).readText()
            val json = JSONObject(raw)
            if (!json.optBoolean("success", false)) return@withContext null
            val ld = json.optJSONObject("level") ?: return@withContext null
            parseLevelJson(ld)
        } catch (e: Exception) {
            android.util.Log.e("SBRepo", "fetchLevel($number) error: ${e.message}")
            null
        }
    }

    /** Parse raw level JSON into SBLevel */
    private fun parseLevelJson(ld: JSONObject): SBLevel? {
        val num      = ld.optInt("levelNumber", -1).takeIf { it > 0 } ?: return null
        val gridSize = ld.optInt("gridSize", 0).takeIf { it > 0 } ?: return null
        val bpu      = ld.optInt("beaconsPerUnit", 1)
        val diff     = ld.optString("difficulty", "beginner")
        val diffVal  = ld.optInt("difficultyValue", 1)

        val regionsJson     = ld.optJSONArray("regions")       ?: return null
        val solutionJson    = ld.optJSONArray("solution")      ?: return null
        val regionColorsJson = ld.optJSONArray("regionColors") ?: return null

        val regions = (0 until regionsJson.length()).map { r ->
            val row = regionsJson.getJSONArray(r)
            (0 until row.length()).map { c -> row.getInt(c) }
        }
        val solution = (0 until solutionJson.length()).map { r ->
            val row = solutionJson.getJSONArray(r)
            (0 until row.length()).map { c -> row.getBoolean(c) }
        }
        val regionColors = (0 until regionColorsJson.length()).map { regionColorsJson.getInt(it) }

        return SBLevel(
            levelNumber   = num,
            gridSize      = gridSize,
            beaconsPerUnit = bpu,
            difficulty    = diff,
            difficultyValue = diffVal,
            regions       = regions,
            solution      = solution,
            regionColors  = regionColors
        )
    }
}
