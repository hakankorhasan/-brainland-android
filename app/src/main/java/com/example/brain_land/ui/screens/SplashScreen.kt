package com.example.brain_land.ui.screens

import androidx.compose.animation.core.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.brain_land.R

@Composable
fun SplashScreen() {
    val bgColor = Color(0xFF0B0D18)

    // One-time enter animation: scale 0.6 → 1.0 + fade in
    // This creates a smooth transition from the native splash (plain background)
    val enterAnim = remember { Animatable(0f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Fade in and scale up simultaneously
        coroutineScope {
            launch {
                enterAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
                )
            }
            launch {
                alphaAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 350, easing = LinearEasing)
                )
            }
        }
    }

    // Subtle idle pulse after entering
    val infiniteTransition = rememberInfiniteTransition(label = "logo_pulse")
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 1.00f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_idle_scale"
    )

    // Combine enter scale (0.6→1.0) with idle pulse (1.0→1.06)
    val logoScale = 0.6f + enterAnim.value * 0.4f * idlePulse

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.brain_land),
            contentDescription = "BrainLand",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(130.dp)
                .scale(logoScale)
                .alpha(alphaAnim.value)
        )
    }
}

