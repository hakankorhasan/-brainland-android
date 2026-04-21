package com.example.brain_land.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.brain_land.data.AvatarItem
import com.example.brain_land.data.OnboardingSlide
import com.example.brain_land.data.PreferencesManager
import com.example.brain_land.data.Repository
import com.example.brain_land.data.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AppScreen {
    object Splash : AppScreen()
    object Onboarding : AppScreen()
    object CreateProfile : AppScreen()
    object Home : AppScreen()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    val repository = Repository()

    // ── App navigation state ──
    val currentScreen = MutableStateFlow<AppScreen>(AppScreen.Splash)

    // ── Onboarding ──
    val onboardingSlides = MutableStateFlow<List<OnboardingSlide>>(emptyList())
    val isLoadingOnboarding = MutableStateFlow(true)

    // ── Create Profile ──
    val avatars = MutableStateFlow<List<AvatarItem>>(emptyList())
    val isLoadingAvatars = MutableStateFlow(false)
    val isSavingProfile = MutableStateFlow(false)
    val profileError = MutableStateFlow<String?>(null)

    // ── Profile (persisted) ──
    val hasProfile: StateFlow<Boolean> = prefs.hasProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val nickname: StateFlow<String> = prefs.nickname
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val avatarUrl: StateFlow<String> = prefs.avatarUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // ── Device ID ──
    private var deviceId: String = ""

    // ──────────────────────────────────────────────────────────
    // Splash init
    // ──────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            // 1. Recover or Generate persistent Device ID
            var savedId = prefs.deviceId.first()
            if (savedId.isEmpty()) {
                @android.annotation.SuppressLint("HardwareIds")
                val androidId = android.provider.Settings.Secure.getString(
                    application.contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                )
                savedId = if (!androidId.isNullOrEmpty() && androidId != "9774d56d682e549c") {
                    androidId
                } else {
                    repository.generateDeviceId()
                }
                prefs.saveDeviceId(savedId)
            }
            deviceId = savedId

            // 2. Read basic local states
            var hasSeen = prefs.hasSeenOnboarding.first()
            var hasPro  = prefs.hasProfile.first()

            // 3. Transparent Re-login (Keychain-like behavior across uninstalls)
            if (!hasPro) {
                val restoredProfile = repository.getProfile(deviceId)
                if (restoredProfile != null && restoredProfile.nickname.isNotEmpty()) {
                    prefs.saveProfile(restoredProfile.nickname, restoredProfile.avatarUrl)
                    prefs.markOnboardingSeen() // Auto-skip onboarding if they played before
                    hasPro = true
                    hasSeen = true
                }
            }

            // Load onboarding slides in background
            loadOnboardingSlides()

            // Minimum splash duration
            kotlinx.coroutines.delay(1500)
            navigateAfterSplash(hasSeen, hasPro)
        }
    }

    private suspend fun navigateAfterSplash(hasSeen: Boolean, hasProf: Boolean) {
        currentScreen.value = when {
            !hasSeen -> AppScreen.Onboarding
            !hasProf -> AppScreen.CreateProfile
            else -> AppScreen.Home
        }
    }

    // ──────────────────────────────────────────────────────────
    // Onboarding
    // ──────────────────────────────────────────────────────────

    private fun loadOnboardingSlides() {
        viewModelScope.launch {
            isLoadingOnboarding.value = true
            val slides = repository.fetchOnboardingSlides()
            onboardingSlides.value = slides
            isLoadingOnboarding.value = false
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            prefs.markOnboardingSeen()
            val hasPro = prefs.hasProfile.first()
            currentScreen.value = if (hasPro) AppScreen.Home else AppScreen.CreateProfile
        }
    }

    // ──────────────────────────────────────────────────────────
    // Profile Creation
    // ──────────────────────────────────────────────────────────

    fun loadAvatars() {
        if (avatars.value.isNotEmpty()) return
        viewModelScope.launch {
            isLoadingAvatars.value = true
            avatars.value = repository.fetchAvatars()
            isLoadingAvatars.value = false
        }
    }

    fun generateRandomNickname(): String {
        val adjectives = listOf(
            "Swift", "Blazing", "Clever", "Mighty", "Silent",
            "Cosmic", "Pixel", "Storm", "Neon", "Turbo"
        )
        val nouns = listOf(
            "Fox", "Wolf", "Eagle", "Hawk", "Panda",
            "Tiger", "Lion", "Dragon", "Phoenix", "Falcon"
        )
        return "${adjectives.random()}${nouns.random()}${(10..99).random()}"
    }

    fun createProfile(nickname: String, age: Int, avatarId: String, avatarUrl: String) {
        viewModelScope.launch {
            isSavingProfile.value = true
            profileError.value = null

            val profile = UserProfile(
                deviceId = deviceId,
                nickname = nickname.trim(),
                age = age,
                avatarId = avatarId,
                avatarUrl = avatarUrl
            )

            val result = repository.createProfile(profile)
            isSavingProfile.value = false

            if (result.isSuccess) {
                prefs.saveProfile(nickname.trim(), avatarUrl)
                prefs.saveDeviceId(deviceId)
                currentScreen.value = AppScreen.Home
            } else {
                profileError.value = result.exceptionOrNull()?.message ?: "An error occurred."
            }
        }
    }
}
