package com.example.brain_land.data

// Onboarding slide fetched from backend (mirrors iOS OnboardingSlide)
data class OnboardingSlide(
    val id: String,
    val order: Int,
    val imageUrl: String,
    val title: String,
    val subtitle: String,
    val buttonText: String,
    val backgroundColor: String = "#0F0F23",
    val textColor: String = "#FFFFFF",
    val mediaType: MediaType = MediaType.IMAGE
) {
    enum class MediaType { IMAGE, VIDEO }
}

// Avatar item fetched from Firebase
data class AvatarItem(
    val id: String,
    val url: String,
    val order: Int = 0
)

// Profile saved to Firestore
data class UserProfile(
    val deviceId: String = "",
    val nickname: String = "",
    val age: Int = 18,
    val avatarId: String = "",
    val avatarUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
