package com.example.brain_land

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.brain_land.ui.screens.CreateProfileScreen
import com.example.brain_land.ui.screens.HomeScreen
import com.example.brain_land.ui.screens.OnboardingScreen
import com.example.brain_land.ui.screens.SplashScreen
import com.example.brain_land.ui.theme.BrainlandTheme
import com.example.brain_land.viewmodel.AppScreen
import com.example.brain_land.viewmodel.MainViewModel
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()

        setContent {
            BrainlandTheme {
                val screen by viewModel.currentScreen.collectAsState()
                val slides by viewModel.onboardingSlides.collectAsState()
                val isLoadingOnboarding by viewModel.isLoadingOnboarding.collectAsState()
                val avatars by viewModel.avatars.collectAsState()
                val isLoadingAvatars by viewModel.isLoadingAvatars.collectAsState()
                val isSaving by viewModel.isSavingProfile.collectAsState()
                val profileError by viewModel.profileError.collectAsState()

                AnimatedContent(
                    targetState = screen,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen_transition"
                ) { targetScreen ->
                    when (targetScreen) {
                        AppScreen.Splash -> SplashScreen()

                        AppScreen.Onboarding -> OnboardingScreen(
                            slides = slides,
                            isLoading = isLoadingOnboarding,
                            onComplete = { viewModel.completeOnboarding() }
                        )

                        AppScreen.CreateProfile -> CreateProfileScreen(
                            avatars = avatars,
                            isLoadingAvatars = isLoadingAvatars,
                            isSaving = isSaving,
                            errorMessage = profileError,
                            onGenerateNickname = { viewModel.generateRandomNickname() },
                            onLoadAvatars = { viewModel.loadAvatars() },
                            onCreateProfile = { nickname, age, avatarId, avatarUrl ->
                                viewModel.createProfile(nickname, age, avatarId, avatarUrl)
                            },
                            onDismissError = { viewModel.profileError.value = null }
                        )

                        AppScreen.Home -> HomeScreen()
                    }
                }
            }
        }
    }
}