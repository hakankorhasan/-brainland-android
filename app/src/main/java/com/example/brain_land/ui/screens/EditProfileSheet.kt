package com.example.brain_land.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.brain_land.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BgColor = Color(0xFF0B0D18)
private val PCyan = Color(0xFF00E5FF)
private val PPurple = Color(0xFF7C3AED)
private val GlassWhite = Color.White.copy(alpha = 0.08f)
private val GlassStroke = Color.White.copy(alpha = 0.12f)
private val CapsuleShape = RoundedCornerShape(50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileSheet(
    vm: HomeViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BgColor,
        dragHandle = null,
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        EditProfileContent(vm, onDismiss)
    }
}

@Composable
private fun EditProfileContent(vm: HomeViewModel, onDismiss: () -> Unit) {
    val avatars by vm.avatars.collectAsState()
    val isLoadingAvatars by vm.isLoadingAvatars.collectAsState()
    val isSaving by vm.isSavingProfile.collectAsState()
    val currentNickname by vm.nickname.collectAsState()
    val currentAvatarId = vm.playerProfileFull.collectAsState().value?.profile?.avatarId ?: ""
    val currentAvatarUrl by vm.avatarUrl.collectAsState()

    var editedNickname by remember { mutableStateOf(currentNickname) }
    var selectedAvatarId by remember { mutableStateOf(currentAvatarId) }
    var showSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.fetchAvatars()
    }
    
    LaunchedEffect(avatars) {
        if (selectedAvatarId.isEmpty() && avatars.isNotEmpty()) {
            selectedAvatarId = avatars.first().id
        }
    }

    val hasChanges = (editedNickname.trim() != currentNickname && editedNickname.trim().isNotEmpty()) || 
                     (selectedAvatarId.isNotEmpty() && selectedAvatarId != currentAvatarId)
                     
    val isNicknameValid = editedNickname.trim().length in 3..16 && editedNickname.trim().matches("^[a-zA-Z0-9_]+$".toRegex())

    Box(Modifier.fillMaxSize()) {
        // Background Orbs
        Box(Modifier.fillMaxSize().blur(50.dp)) {
            Box(
                Modifier
                    .size(350.dp)
                    .offset(x = (-80).dp, y = (-200).dp)
                    .background(Brush.radialGradient(listOf(PPurple.copy(0.2f), Color.Transparent)))
            )
            Box(
                Modifier
                    .size(280.dp)
                    .offset(x = 100.dp, y = 200.dp)
                    .align(Alignment.Center)
                    .background(Brush.radialGradient(listOf(PCyan.copy(0.1f), Color.Transparent)))
            )
        }

        Column(Modifier.fillMaxSize()) {
            // Header
            Column(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.padding(top = 12.dp).width(36.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(0.2f)))
                
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, contentPadding = PaddingValues(0.dp)) {
                        Text("Cancel", color = Color.White.copy(0.5f), fontSize = 16.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Edit Profile", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            if (!isNicknameValid && editedNickname.trim() != currentNickname) {
                                errorMessage = "Nickname must be 3-16 characters and contain only letters, numbers, and underscores."
                                return@TextButton
                            }
                            val newNickname = if (editedNickname.trim() != currentNickname) editedNickname.trim() else null
                            val newAvatarId = if (selectedAvatarId != currentAvatarId) selectedAvatarId else null
                            
                            vm.updateProfile(
                                newNickname = newNickname,
                                newAvatarId = newAvatarId,
                                onSuccess = {
                                    showSuccess = true
                                    scope.launch {
                                        delay(1200)
                                        onDismiss()
                                    }
                                },
                                onError = { errorMessage = it }
                            )
                        },
                        enabled = hasChanges && !isSaving,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Save", color = if (hasChanges) PCyan else Color.White.copy(0.2f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Scrollable Content
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar Preview
                val previewUrl = avatars.firstOrNull { it.id == selectedAvatarId }?.url ?: currentAvatarUrl
                Box(contentAlignment = Alignment.Center) {
                    Box(Modifier.size(140.dp).background(Brush.radialGradient(listOf(PPurple.copy(0.15f), PCyan.copy(0.05f), Color.Transparent))))
                    
                    if (previewUrl.isNotEmpty()) {
                        AsyncImage(
                            model = previewUrl,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(100.dp).clip(CircleShape)
                                .border(3.dp, Brush.linearGradient(listOf(PCyan.copy(0.5f), PPurple.copy(0.5f))), CircleShape)
                        )
                    } else {
                        Box(
                            Modifier.size(100.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF6FE4CF), Color(0xFFB88AE8)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(editedNickname.take(1).uppercase(), fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    
                    Box(
                        Modifier.size(32.dp).offset(x = 35.dp, y = 35.dp).clip(CircleShape).background(PPurple),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                Text("Tap an avatar below to change", fontSize = 12.sp, color = Color.White.copy(0.3f))

                // Nickname Editor
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Nickname", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(0.8f))
                    
                    var isFocused by remember { mutableStateOf(false) }
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(GlassWhite)
                            .border(if (isFocused) 1.5.dp else 1.dp, if (isFocused) PCyan.copy(0.4f) else GlassStroke, RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = editedNickname,
                            onValueChange = { editedNickname = it },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                cursorColor = PCyan,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            placeholder = { Text("Enter nickname", color = Color.White.copy(0.2f)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                            modifier = Modifier.weight(1f).onFocusChanged { isFocused = it.isFocused }
                        )
                        
                        if (isNicknameValid && editedNickname != currentNickname) {
                            Icon(Icons.Default.CheckCircle, null, tint = PCyan)
                        }
                    }
                    
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("3-16 chars, letters & numbers only", fontSize = 10.sp, color = Color.White.copy(0.2f))
                        Spacer(Modifier.weight(1f))
                        Box(
                            Modifier.clip(CapsuleShape).background(Color.White.copy(0.05f))
                                .clickable { editedNickname = "Player_${(1000..9999).random()}" }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text("🎲 Random", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(0.4f))
                        }
                    }
                }

                // Avatar Selector
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Choose Avatar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(0.8f))
                    
                    if (isLoadingAvatars) {
                        Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PCyan)
                        }
                    } else {
                        val columns = 4
                        val rows = (avatars.size + columns - 1) / columns
                        val gridHeight = (rows * 72 + (rows - 1) * 14).coerceAtLeast(0).dp
                        
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            modifier = Modifier.fillMaxWidth().height(gridHeight),
                            userScrollEnabled = false,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(avatars) { avatar ->
                                val isSelected = selectedAvatarId == avatar.id
                                Box(
                                    Modifier.fillMaxWidth().height(72.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) Brush.linearGradient(listOf(PPurple.copy(0.25f), PCyan.copy(0.1f))) else Brush.linearGradient(listOf(GlassWhite, Color.White.copy(0.03f))))
                                        .border(if (isSelected) 2.dp else 1.dp, if (isSelected) PCyan.copy(0.5f) else GlassStroke, RoundedCornerShape(14.dp))
                                        .clickable {
                                            selectedAvatarId = avatar.id
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = avatar.url,
                                        contentDescription = null,
                                        modifier = Modifier.size(44.dp).clip(CircleShape)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.CheckCircle, null, 
                                            tint = PCyan, 
                                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(16.dp).background(BgColor, CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
        
        if (errorMessage != null) {
            AlertDialog(
                onDismissRequest = { errorMessage = null },
                confirmButton = { TextButton(onClick = { errorMessage = null }) { Text("OK", color = PCyan) } },
                title = { Text("Error") },
                text = { Text(errorMessage!!) },
                containerColor = BgColor,
                titleContentColor = Color.White,
                textContentColor = Color.White.copy(0.8f)
            )
        }

        AnimatedVisibility(
            visible = showSuccess,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.6f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(80.dp).background(PCyan.copy(0.15f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CheckCircle, null, tint = PCyan, modifier = Modifier.size(44.dp))
                    }
                    Text("Profile Updated!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
