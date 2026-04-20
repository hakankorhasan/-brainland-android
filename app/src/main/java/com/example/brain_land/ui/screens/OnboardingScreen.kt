package com.example.brain_land.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.brain_land.data.OnboardingSlide
import com.example.brain_land.ui.theme.AccentCyan
import com.example.brain_land.ui.theme.AccentPurple
import com.example.brain_land.ui.theme.AccentPurple2
import com.example.brain_land.ui.theme.BgDark
import com.example.brain_land.ui.theme.TextSecondary

@Composable
fun OnboardingScreen(
    slides: List<OnboardingSlide>,
    isLoading: Boolean,
    onComplete: () -> Unit
) {
    val bgColor = BgDark

    var currentIndex by remember { mutableIntStateOf(0) }
    var appeared by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { appeared = true }

    // BoxWithConstraints gives us the real screen dimensions — mirrors iOS GeometryReader
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        val screenWidth  = maxWidth
        val screenHeight = maxHeight
        // iOS: geo.size.height * 0.90 for iPhone
        val imageHeight  = screenHeight * 0.90f

        if (isLoading || slides.isEmpty()) {
            CircularProgressIndicator(
                color = AccentCyan,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            val slide = slides[currentIndex]

            // ── Full-screen slide container ──
            Box(modifier = Modifier.fillMaxSize()) {

                // ── Media: width=full, height=90% of screen, top-aligned, clipped ──
                // Mirrors iOS: .frame(width: geo.size.width, height: geo.size.height * 0.90, alignment: .top).clipped()
                AnimatedVisibility(
                    visible = true,
                    enter = slideInHorizontally { it } + fadeIn(),
                    exit  = slideOutHorizontally { -it } + fadeOut()
                ) {
                    SlideMedia(
                        imageUrl = slide.imageUrl,
                        modifier = Modifier
                            .width(screenWidth)
                            .height(imageHeight)
                            .align(Alignment.TopStart)
                            .clip(RoundedCornerShape(0.dp)) // ensures clip like iOS .clipped()
                    )
                }

                // ── Bottom gradient overlay (same stops as iOS) ──
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.00f to Color.Transparent,
                                    0.25f to bgColor.copy(alpha = 0.30f),
                                    0.50f to bgColor.copy(alpha = 0.70f),
                                    0.80f to bgColor
                                )
                            )
                        )
                )

                // ── Top bar: Skip button ──
                if (currentIndex < slides.size - 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        AnimatedVisibility(visible = appeared) {
                            TextButton(
                                onClick = onComplete,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                            ) {
                                Text(
                                    text = "Skip",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // ── Bottom content ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title + subtitle (animate in)
                    AnimatedVisibility(visible = appeared) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = slide.title,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                lineHeight = 32.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = slide.subtitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    // Page dots with spring animation
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        slides.indices.forEach { i ->
                            val dotWidth by animateDpAsState(
                                targetValue = if (i == currentIndex) 22.dp else 6.dp,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                label = "dot_width_$i"
                            )
                            Box(
                                modifier = Modifier
                                    .height(6.dp)
                                    .width(dotWidth)
                                    .clip(CircleShape)
                                    .background(
                                        if (i == currentIndex) Color.White
                                        else Color.White.copy(alpha = 0.25f)
                                    )
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Action button (purple gradient)
                    Button(
                        onClick = {
                            if (currentIndex < slides.size - 1) currentIndex++
                            else onComplete()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(listOf(AccentPurple, AccentPurple2)),
                                    RoundedCornerShape(18.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = slide.buttonText,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (currentIndex < slides.size - 1) {
                                    Spacer(Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Legal links
                    val uriHandler = LocalUriHandler.current
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Terms of Use",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable {
                                uriHandler.openUri("https://mini-games-9a4e1.web.app/terms.html")
                            }
                        )
                        Text(
                            text = " • ",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.3f)
                        )
                        Text(
                            text = "Privacy Policy",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable {
                                uriHandler.openUri("https://mini-games-9a4e1.web.app/privacy.html")
                            }
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
// SlideMedia — mirrors iOS OnboardingSlideMediaView
// Remote image: SubcomposeAsyncImage with loading placeholder
// Fallback: gradient + brain emoji (like iOS SF Symbol path)
// ──────────────────────────────────────────────────────────

@Composable
private fun SlideMedia(imageUrl: String, modifier: Modifier = Modifier) {
    if (imageUrl.startsWith("http")) {
        // SubcomposeAsyncImage lets us show a placeholder while loading
        // ContentScale.Crop = iOS scaledToFill
        // modifier already constrains width & height to 90% — clip is applied by the caller
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
            loading = {
                // Loading placeholder — matches iOS ProgressView placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BgDark),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = AccentCyan,
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        )
    } else {
        // Fallback — gradient background + emoji (like iOS SF Symbol fallback)
        Box(
            modifier = modifier.background(
                Brush.verticalGradient(listOf(Color(0xFF1A0A3E), Color(0xFF0B0D18)))
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🧠", fontSize = 120.sp)
        }
    }
}
