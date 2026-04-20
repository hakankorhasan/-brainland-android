package com.example.brain_land.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.brain_land.data.AvatarItem
import com.example.brain_land.ui.theme.AccentCyan
import com.example.brain_land.ui.theme.AccentIndigo
import com.example.brain_land.ui.theme.AccentPurple
import com.example.brain_land.ui.theme.AccentPurple2
import com.example.brain_land.ui.theme.BgDark
import com.example.brain_land.ui.theme.BgGlass
import com.example.brain_land.ui.theme.GlassStroke
import com.example.brain_land.ui.theme.TextHint
import com.example.brain_land.ui.theme.TextSecondary

@Composable
fun CreateProfileScreen(
    avatars: List<AvatarItem>,
    isLoadingAvatars: Boolean,
    isSaving: Boolean,
    errorMessage: String?,
    onGenerateNickname: () -> String,
    onLoadAvatars: () -> Unit,
    onCreateProfile: (nickname: String, age: Int, avatarId: String, avatarUrl: String) -> Unit,
    onDismissError: () -> Unit
) {
    // State
    var nickname by remember { mutableStateOf("") }
    var age by remember { mutableIntStateOf(18) }
    var selectedAvatarId by remember { mutableStateOf("") }
    var selectedAvatarUrl by remember { mutableStateOf("") }

    val isNicknameValid = nickname.trim().length in 3..16
    val canContinue = isNicknameValid && selectedAvatarId.isNotEmpty()

    LaunchedEffect(Unit) {
        onLoadAvatars()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // ── Decorative gradient orbs ──
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset((-100).dp, (-200).dp)
                .background(
                    Brush.radialGradient(
                        listOf(AccentPurple.copy(alpha = 0.3f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(350.dp)
                .align(Alignment.BottomEnd)
                .background(
                    Brush.radialGradient(
                        listOf(AccentIndigo.copy(alpha = 0.25f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        // ── Scrollable content ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(60.dp))

            // Title
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Create Your Profile",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "This will only take a moment",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── Nickname ──
            SectionLabel(text = "Nickname")
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = nickname,
                    onValueChange = { if (it.length <= 16) nickname = it },
                    placeholder = {
                        Text("Choose a nickname…", color = TextHint, fontSize = 15.sp)
                    },
                    singleLine = true,
                    trailingIcon = {
                        if (isNicknameValid) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = AccentCyan
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Done
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = BgGlass,
                        unfocusedContainerColor = BgGlass,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = AccentCyan
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = if (isNicknameValid) 1.5.dp else 1.dp,
                            color = if (isNicknameValid) AccentCyan.copy(alpha = 0.5f) else GlassStroke,
                            shape = RoundedCornerShape(14.dp)
                        )
                )

                Spacer(Modifier.width(12.dp))

                // Random nickname button
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(BgGlass)
                        .border(1.dp, GlassStroke, CircleShape)
                        .clickable { nickname = onGenerateNickname() }
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "Randomize",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Age ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionLabel(text = "Age")
                Text(
                    text = age.toString(),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(8.dp))

            Slider(
                value = age.toFloat(),
                onValueChange = { age = it.toInt() },
                valueRange = 5f..99f,
                steps = 93,
                colors = SliderDefaults.colors(
                    thumbColor = AccentCyan,
                    activeTrackColor = AccentCyan,
                    inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Used to personalise your experience",
                fontSize = 11.sp,
                color = TextHint
            )

            Spacer(Modifier.height(28.dp))

            // ── Avatar ──
            SectionLabel(text = "Choose Avatar")
            Spacer(Modifier.height(14.dp))

            if (isLoadingAvatars) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentCyan)
                }
            } else {
                LazyHorizontalGrid(
                    rows = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(192.dp)
                ) {
                    items(avatars) { avatar ->
                        AvatarCell(
                            avatar = avatar,
                            isSelected = avatar.id == selectedAvatarId,
                            onClick = {
                                selectedAvatarId = avatar.id
                                selectedAvatarUrl = avatar.url
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(140.dp)) // space for fixed bottom button
        }

        // ── Fixed bottom button ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, BgDark.copy(alpha = 0.85f), BgDark)
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp, top = 32.dp)
        ) {
            Text(
                text = "You can change this later",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))

            Button(
                onClick = {
                    if (canContinue && !isSaving) {
                        onCreateProfile(
                            nickname.trim(), age, selectedAvatarId, selectedAvatarUrl
                        )
                    }
                },
                enabled = canContinue && !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = if (canContinue)
                                Brush.horizontalGradient(listOf(AccentPurple, AccentPurple2))
                            else
                                Brush.linearGradient(listOf(BgGlass, BgGlass)),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Continue",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (canContinue) Color.White else Color.White.copy(alpha = 0.25f)
                        )
                    }
                }
            }
        }

        // ── Error snackbar ──
        errorMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEF4444))
                    .clickable { onDismissError() }
                    .padding(16.dp)
            ) {
                Text(text = msg, color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White.copy(alpha = 0.9f)
    )
}

@Composable
private fun AvatarCell(
    avatar: AvatarItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "avatar_scale"
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected)
                    Brush.linearGradient(
                        listOf(AccentPurple.copy(alpha = 0.3f), AccentCyan.copy(alpha = 0.15f))
                    )
                else Brush.linearGradient(listOf(BgGlass, BgGlass))
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) AccentCyan.copy(alpha = 0.6f) else GlassStroke,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (avatar.url.startsWith("http")) {
            AsyncImage(
                model = avatar.url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
        } else {
            // Fallback icon
            Icon(
                imageVector = Icons.Default.Casino,
                contentDescription = null,
                tint = if (isSelected) AccentCyan else TextSecondary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

// Needed for orb offset helper
private fun Modifier.offset(x: androidx.compose.ui.unit.Dp, y: androidx.compose.ui.unit.Dp) =
    this.then(
        Modifier.padding(start = if (x > 0.dp) x else 0.dp, top = if (y > 0.dp) y else 0.dp)
    )
