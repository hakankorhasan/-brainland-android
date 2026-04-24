package com.example.brain_land.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.brain_land.R

@Composable
fun SplashScreen() {
    val bgColor = Color(0xFF0B0D18)

    // Subtle pulse animation on the logo
    val infiniteTransition = rememberInfiniteTransition(label = "logo_pulse")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        // App icon — brain_land.png
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(130.dp).scale(logoScale)
        ) {
            Image(
                painter = painterResource(R.drawable.brain_land),
                contentDescription = "BrainLand",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(130.dp)
            )
        }
    }
}

