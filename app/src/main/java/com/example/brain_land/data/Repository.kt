package com.example.brain_land.data

import android.util.Log
import com.example.brain_land.data.OnboardingSlide.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

private const val TAG = "BrainLandRepo"
private const val BASE_URL =
    "https://us-central1-mini-games-9a4e1.cloudfunctions.net"

class Repository {

    // ──────────────────────────────────────────────────────────
    // ONBOARDING  (GET /getOnboardings)
    // ──────────────────────────────────────────────────────────

    suspend fun fetchOnboardingSlides(): List<OnboardingSlide> = withContext(Dispatchers.IO) {
        try {
            val text = get("$BASE_URL/getOnboardings")
            Log.d(TAG, "Onboarding raw: ${text.take(300)}")
            val json = JSONObject(text)
            if (!json.optBoolean("success", false)) return@withContext fallbackSlides()

            val rawSlides = json.getJSONArray("onboardings")
            val parsed = mutableListOf<OnboardingSlide>()

            for (i in 0 until rawSlides.length()) {
                val obj = rawSlides.getJSONObject(i)
                var imageUrl = obj.optString("imageUrl", "")
                imageUrl = fixStorageUrl(imageUrl)

                val lower = imageUrl.lowercase()
                val mediaType = if (lower.contains(".mp4") || lower.contains("video"))
                    MediaType.VIDEO else MediaType.IMAGE

                parsed.add(
                    OnboardingSlide(
                        id          = obj.optString("id"),
                        order       = obj.optInt("order", i),
                        imageUrl    = imageUrl,
                        title       = obj.optString("title"),
                        subtitle    = obj.optString("subtitle"),
                        buttonText  = obj.optString("buttonText", "Next"),
                        backgroundColor = obj.optString("backgroundColor", "#0F0F23"),
                        textColor   = obj.optString("textColor", "#FFFFFF"),
                        mediaType   = mediaType
                    )
                )
            }

            val result = if (parsed.isEmpty()) fallbackSlides() else parsed.sortedBy { it.order }
            Log.d(TAG, "Slides loaded: ${result.size}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "fetchOnboardingSlides error: ${e.message}")
            fallbackSlides()
        }
    }

    // ──────────────────────────────────────────────────────────
    // AVATARS  (GET /getAvatars)
    // ──────────────────────────────────────────────────────────

    suspend fun fetchAvatars(): List<AvatarItem> = withContext(Dispatchers.IO) {
        try {
            val text = get("$BASE_URL/getAvatars")
            Log.d(TAG, "Avatars raw: ${text.take(300)}")
            val json = JSONObject(text)
            if (!json.optBoolean("success", false)) return@withContext emptyList()

            val arr = json.getJSONArray("avatars")
            val result = mutableListOf<AvatarItem>()

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                var url = obj.optString("url", "")
                url = fixStorageUrl(url)
                result.add(
                    AvatarItem(
                        id    = obj.optString("id"),
                        url   = url,
                        order = obj.optInt("order", i)
                    )
                )
            }

            Log.d(TAG, "Avatars loaded: ${result.size}")
            result.sortedBy { it.order }
        } catch (e: Exception) {
            Log.e(TAG, "fetchAvatars error: ${e.message}")
            emptyList()
        }
    }

    // ──────────────────────────────────────────────────────────
    // PROFILE  (POST /createProfile)
    // ──────────────────────────────────────────────────────────

    suspend fun createProfile(profile: UserProfile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("deviceId",  profile.deviceId)
                put("nickname",  profile.nickname)
                put("age",       profile.age)
                put("avatarId",  profile.avatarId)
            }.toString()

            val (status, text) = post("$BASE_URL/createProfile", body)
            Log.d(TAG, "createProfile status=$status raw=${text.take(300)}")

            when (status) {
                in 200..299, 409 -> {
                    // 409 = device already has profile → treat as success
                    Result.success(Unit)
                }
                400 -> {
                    val msg = JSONObject(text).optString("error", "Invalid input.")
                    Result.failure(Exception(msg))
                }
                else -> Result.failure(Exception("Server error ($status)"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "createProfile error: ${e.message}")
            Result.failure(e)
        }
    }

    fun generateDeviceId(): String = UUID.randomUUID().toString()

    // ──────────────────────────────────────────────────────────
    // HTTP helpers
    // ──────────────────────────────────────────────────────────

    private fun get(urlStr: String): String {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10_000
        conn.readTimeout    = 10_000
        return try {
            conn.inputStream.bufferedReader().readText()
        } finally {
            conn.disconnect()
        }
    }

    private fun post(urlStr: String, jsonBody: String): Pair<Int, String> {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.requestMethod  = "POST"
        conn.doOutput       = true
        conn.connectTimeout = 10_000
        conn.readTimeout    = 15_000
        conn.setRequestProperty("Content-Type", "application/json")
        conn.outputStream.write(jsonBody.toByteArray())
        val status = conn.responseCode
        val stream = if (status in 200..299) conn.inputStream else conn.errorStream
        val text   = stream?.bufferedReader()?.readText() ?: ""
        conn.disconnect()
        return status to text
    }

    // ──────────────────────────────────────────────────────────
    // URL fixer — mirrors iOS OnboardingManager logic exactly:
    // https://storage.googleapis.com/BUCKET/path/to/file
    //   → https://firebasestorage.googleapis.com/v0/b/BUCKET/o/path%2Fto%2Ffile?alt=media
    // ──────────────────────────────────────────────────────────

    private fun fixStorageUrl(url: String): String {
        val marker = "storage.googleapis.com/"
        if (!url.contains(marker)) return url

        val afterDomain = url.substringAfter(marker)  // "BUCKET/path/to/file"
        val slashIdx = afterDomain.indexOf('/')
        if (slashIdx == -1) return url

        val bucket = afterDomain.substring(0, slashIdx)          // "BUCKET"
        val rawPath = afterDomain.substring(slashIdx + 1)        // "path/to/file"
        // Encode the path: every '/' becomes '%2F' (same as iOS addingPercentEncoding + replace)
        val encodedPath = rawPath.replace("/", "%2F")

        return "https://firebasestorage.googleapis.com/v0/b/$bucket/o/$encodedPath?alt=media"
    }

    // ──────────────────────────────────────────────────────────
    // Fallback slides
    // ──────────────────────────────────────────────────────────

    private fun fallbackSlides() = listOf(
        OnboardingSlide(
            id = "f1", order = 1, imageUrl = "",
            title = "10+ Brain Games, One App",
            subtitle = "From logic puzzles to pattern challenges — train your brain.",
            buttonText = "Next"
        ),
        OnboardingSlide(
            id = "f2", order = 2, imageUrl = "",
            title = "Daily Challenge Awaits",
            subtitle = "Complete 5 unique puzzles every day and climb the leaderboard.",
            buttonText = "Next"
        ),
        OnboardingSlide(
            id = "f3", order = 3, imageUrl = "",
            title = "Story Mode",
            subtitle = "Dive into immersive story-driven puzzles.",
            buttonText = "Get Started"
        )
    )
}
