@file:Suppress("NAME_SHADOWING")

package com.example.brain_land.ui.games.wordpuzzle

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.example.brain_land.data.GameType

// ─────────────────────────────────────────────────────────────────────────────
// Colour palette  (mirrors iOS WordPuzzleGameView)
// ─────────────────────────────────────────────────────────────────────────────

private val BgDark        = Color(0xFF10131B)
private val CorrectColor  = Color(0xFF2DEB6A)
private val PresentColor  = Color(0xFFFFD700)
private val AbsentColor   = Color(0xFF4B5563)
private val EmptyBorder   = Color.White.copy(alpha = 0.15f)
private val AccentPurple  = Color(0xFFA299EC)
private val AccentCyan    = Color(0xFF00E5FF)
private val KeyBg         = Color(0xFF2A2D3A)

// ─────────────────────────────────────────────────────────────────────────────
// Entry point  (mirrors WordPuzzlePuzzleView)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WordPuzzlePuzzleView(
    onHome:           () -> Unit,
    onNavigateToGame: (GameType) -> Unit = {}
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val level   = remember { WordPuzzleLocalEngine.persistedCurrentLevel(context) }
    val state   = remember { WPGameState(context, scope, level) }

    LaunchedEffect(Unit) { state.loadLevel(); state.startTimerIfNeeded() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .systemBarsPadding()
    ) {
        WordPuzzleGameView(
            state  = state,
            onBack = onHome,
            onNavigateToGame = onNavigateToGame
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main game view  (mirrors WordPuzzleGameView)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WordPuzzleGameView(
    state:            WPGameState,
    onBack:           () -> Unit,
    onNavigateToGame: (GameType) -> Unit
) {
    var showInfo by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top bar ──────────────────────────────────────────────────────
            WPTopBar(
                onBack     = onBack,
                onInfo     = { showInfo = true }
            )

            // ── Level + stats badge row ───────────────────────────────────────
            WPLevelBadge(state = state)

            Spacer(Modifier.height(8.dp))

            // ── Letter grid ───────────────────────────────────────────────────
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                LetterGrid(state = state, maxWidth = maxWidth, maxHeight = maxHeight)
            }

            // ── Hint button ───────────────────────────────────────────────────
            if (!state.solved && !state.failed) {
                HintButton(state = state)
                Spacer(Modifier.height(6.dp))
            }

            // ── Keyboard ──────────────────────────────────────────────────────
            WPKeyboard(state = state)
            Spacer(Modifier.height(8.dp))
        }

        // ── Result overlay ────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = state.showResult,
            enter   = fadeIn() + scaleIn(initialScale = 0.95f),
            exit    = fadeOut() + scaleOut(targetScale = 0.95f)
        ) {
            WPResultOverlay(
                state            = state,
                onPlayAgain      = { state.showResult = false; state.resetSession() },
                onNextLevel      = { state.showResult = false; state.nextLevel() },
                onBackToGames    = onBack,
                onNavigateToGame = onNavigateToGame
            )
        }

        // ── Alert ─────────────────────────────────────────────────────────────
        if (state.alertMessage != null) {
            AlertDialog(
                onDismissRequest = { state.alertMessage = null },
                confirmButton    = {
                    TextButton(onClick = { state.alertMessage = null }) { Text("OK") }
                },
                text             = { Text(state.alertMessage ?: "") },
                containerColor   = Color(0xFF1D1B29),
                textContentColor = Color.White
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WPTopBar(onBack: () -> Unit, onInfo: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White.copy(0.85f))
        }
        Text(
            "Word Puzzle",
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.White,
            modifier   = Modifier.align(Alignment.Center)
        )
        IconButton(onClick = onInfo, modifier = Modifier.align(Alignment.CenterEnd)) {
            Icon(Icons.Default.Info, null, tint = Color.White.copy(0.45f), modifier = Modifier.size(20.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Level badge row  (Level | Difficulty | Timer | Attempts)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WPLevelBadge(state: WPGameState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        BadgePill(icon = Icons.Default.GridView, iconTint = CorrectColor, text = "Level ${state.levelNumber}")
        BadgePill(icon = Icons.Default.Star, iconTint = AccentPurple, text = state.difficulty, tint = AccentPurple)
        BadgePill(icon = Icons.Default.Timer, iconTint = AccentCyan, text = fmtTime(state.elapsedSeconds), mono = true)
        // Attempts remaining (hearts)
        BadgePill(icon = Icons.Default.Favorite, iconTint = Color(0xFFEF4444), text = "${state.maxGuesses - state.attemptsUsed}")
    }
}

@Composable
private fun BadgePill(icon: androidx.compose.ui.graphics.vector.ImageVector, iconTint: Color, text: String, tint: Color = Color.White, mono: Boolean = false) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(0.05f))
            .border(0.5.dp, Color.White.copy(0.08f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(10.dp))
        Text(
            text,
            fontSize   = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            color      = tint
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Letter grid  (mirrors letterGrid + letterTile)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LetterGrid(state: WPGameState, maxWidth: Dp, maxHeight: Dp) {
    val rows    = state.maxGuesses
    val cols    = state.wordLength
    val spacing = 7.dp

    val tileFromW = (maxWidth  - spacing * (cols - 1)) / cols
    val tileFromH = (maxHeight - spacing * (rows - 1)) / rows
    val tileSize  = minOf(tileFromW, tileFromH, 72.dp)

    val gridW = tileSize * cols + spacing * (cols - 1)
    val gridH = tileSize * rows + spacing * (rows - 1)

    Box(
        modifier          = Modifier.fillMaxSize(),
        contentAlignment  = Alignment.Center
    ) {
        Column(
            modifier            = Modifier.width(gridW).height(gridH),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            for (row in 0 until rows) {
                val isCurrentRow = row == state.guesses.size
                val shakeOffset  = if (isCurrentRow && state.shakeRow) 8.dp else 0.dp
                val shakeAnim by animateDpAsState(shakeOffset, tween(60), label = "shake$row")

                Row(
                    modifier              = Modifier.offset(x = shakeAnim),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    for (col in 0 until cols) {
                        LetterTile(state = state, row = row, col = col, tileSize = tileSize)
                    }
                }
            }
        }
    }
}

@Composable
private fun LetterTile(state: WPGameState, row: Int, col: Int, tileSize: Dp) {
    val isSubmitted  = row < state.guesses.size
    val isCurrentRow = row == state.guesses.size

    val flipAnim by animateFloatAsState(
        targetValue   = if (state.revealingRow == row) 360f else 0f,
        animationSpec = tween(600, delayMillis = col * 100),
        label         = "flip$row$col"
    )

    Box(modifier = Modifier.size(tileSize).graphicsLayer { rotationX = flipAnim }) {
        when {
            isSubmitted -> {
                val result = state.guesses[row].results.getOrNull(col)
                if (result != null) SubmittedTile(result, tileSize) else EmptyTile(tileSize)
            }
            isCurrentRow && !state.solved && !state.failed -> CurrentTile(state, col, tileSize)
            else -> EmptyTile(tileSize)
        }
    }
}

@Composable
private fun SubmittedTile(result: WPLetterResult, tileSize: Dp) {
    val (gradColors, shadowColor) = when (result.status) {
        WPLetterStatus.CORRECT  -> listOf(CorrectColor, Color(0xFF1AB54D)) to CorrectColor.copy(0.5f)
        WPLetterStatus.PRESENT  -> listOf(PresentColor, Color(0xFFDDA400)) to PresentColor.copy(0.3f)
        WPLetterStatus.ABSENT   -> listOf(AbsentColor,  Color(0xFF374151)) to Color.Black.copy(0.3f)
        else                    -> listOf(Color.Transparent, Color.Transparent) to Color.Transparent
    }
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(gradColors, Offset(0f, 0f), Offset(200f, 200f)))
            .border(1.dp, Color.White.copy(0.2f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            result.letter,
            fontSize   = (tileSize.value * 0.45f).sp,
            fontWeight = FontWeight.ExtraBold,
            color      = Color.White
        )
    }
}

@Composable
private fun CurrentTile(state: WPGameState, col: Int, tileSize: Dp) {
    val letter   = state.currentInput.getOrNull(col)
    val isSelected = state.selectedCell == col
    val isHint   = state.hintReveals.containsKey(col)

    if (isHint) {
        // Hint-revealed — locked green
        Box(
            modifier         = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(
                    listOf(CorrectColor.copy(0.9f), Color(0xFF1AB54D)),
                    Offset(0f, 0f), Offset(200f, 200f)
                ))
                .border(1.dp, Color.White.copy(0.4f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                state.hintReveals[col] ?: "",
                fontSize   = (tileSize.value * 0.45f).sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White
            )
        }
    } else {
        val hasLetter   = letter != null
        val borderColor = if (isSelected) CorrectColor.copy(0.8f) else if (hasLetter) Color.White.copy(0.2f) else EmptyBorder
        val borderWidth = if (isSelected) 2.5.dp else 1.5.dp
        val scale by animateFloatAsState(if (isSelected) 0.95f else 1f, spring(0.7f, 400f), label = "sel$col")

        Box(
            modifier         = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        if (hasLetter) listOf(Color.White.copy(0.12f), Color.White.copy(0.06f))
                        else           listOf(Color.White.copy(0.05f), Color.White.copy(0.02f)),
                        Offset(0f, 0f), Offset(200f, 200f)
                    )
                )
                .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
                .clickable { state.selectCell(col) },
            contentAlignment = Alignment.Center
        ) {
            if (letter != null) {
                Text(
                    letter,
                    fontSize   = (tileSize.value * 0.45f).sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color.White
                )
            }
        }
    }
}

@Composable
private fun EmptyTile(tileSize: Dp) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(
                listOf(Color.White.copy(0.04f), Color.White.copy(0.01f)),
                Offset(0f, 0f), Offset(200f, 200f)
            ))
            .border(
                1.dp,
                Brush.linearGradient(listOf(Color.White.copy(0.12f), Color.White.copy(0.04f)), Offset(0f, 0f), Offset(200f, 200f)),
                RoundedCornerShape(14.dp)
            )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Hint button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HintButton(state: WPGameState) {
    val amber = Color(0xFFF59E0B)
    OutlinedButton(
        onClick  = { state.useHint() },
        enabled  = state.canUseHint,
        shape    = CircleShape,
        border   = BorderStroke(1.dp, if (state.canUseHint) amber.copy(0.5f) else Color.White.copy(0.1f)),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Icon(Icons.Default.Lightbulb, null, tint = if (state.canUseHint) Color.Yellow else Color.White.copy(0.25f), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text("Hint", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (state.canUseHint) Color.White else Color.White.copy(0.25f))
        Spacer(Modifier.width(6.dp))
        Text(
            state.hintLabel,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.White.copy(0.5f),
            modifier   = Modifier
                .clip(CircleShape)
                .background(Color.White.copy(0.1f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Keyboard  (mirrors iOS keyboard layout)
// ─────────────────────────────────────────────────────────────────────────────

private val KB_ROWS = listOf(
    listOf("Q","W","E","R","T","Y","U","I","O","P"),
    listOf("A","S","D","F","G","H","J","K","L"),
    listOf("⌫","Z","X","C","V","B","N","M","↵")
)

@Composable
private fun WPKeyboard(state: WPGameState) {
    Column(
        modifier            = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        for (row in KB_ROWS) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
            ) {
                for (key in row) {
                    WPKeyButton(key = key, state = state, modifier = Modifier.weight(if (key.length > 1) 1.4f else 1f))
                }
            }
        }
    }
}

@Composable
private fun WPKeyButton(key: String, state: WPGameState, modifier: Modifier) {
    val status = if (key !in listOf("⌫","↵")) state.keyboardStatus[key] else null
    val bg = when {
        key == "↵"                          -> if (state.isInputComplete) CorrectColor.copy(0.8f) else Color.White.copy(0.08f)
        key == "⌫"                          -> Color.White.copy(0.08f)
        status == WPLetterStatus.CORRECT    -> CorrectColor
        status == WPLetterStatus.PRESENT    -> PresentColor
        status == WPLetterStatus.ABSENT     -> AbsentColor.copy(0.8f)
        else                                -> KeyBg
    }
    val fg = when {
        key == "⌫"                          -> Color(0xFFEF4444)
        status == WPLetterStatus.ABSENT     -> Color.White.copy(0.6f)
        else                                -> Color.White
    }
    Box(
        modifier         = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(bg)
            .clickable {
                when (key) {
                    "⌫"  -> state.deleteLetter()
                    "↵"  -> state.submitGuess()
                    else -> state.typeLetter(key)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when (key) {
            "⌫"  -> Icon(Icons.Default.Backspace, null, tint = fg, modifier = Modifier.size(18.dp))
            "↵"  -> Icon(Icons.Default.KeyboardReturn, null, tint = fg, modifier = Modifier.size(18.dp))
            else -> Text(key, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = fg)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Result overlay  (mirrors iOS GameResultView shell)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WPResultOverlay(
    state:            WPGameState,
    onPlayAgain:      () -> Unit,
    onNextLevel:      () -> Unit,
    onBackToGames:    () -> Unit,
    onNavigateToGame: (GameType) -> Unit
) {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.65f))
            .clickable(enabled = false, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier            = Modifier
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1D1B29))
                .border(
                    1.dp,
                    if (state.solved) CorrectColor.copy(0.2f) else Color(0xFFEF4444).copy(0.2f),
                    RoundedCornerShape(24.dp)
                )
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon
            val iconColor = if (state.solved) Color.Yellow else Color(0xFFEF4444)
            val iconEmoji = if (state.solved) "🏆" else "❌"
            Text(iconEmoji, fontSize = 52.sp)

            // Title
            Text(
                if (state.solved) "Congratulations! 🎉" else "Not found! 😔",
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
                textAlign  = TextAlign.Center
            )

            // Sub-info
            if (state.solved) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${state.attemptsUsed}/${state.maxGuesses} attempts",
                        fontSize = 14.sp,
                        color    = Color.White.copy(0.6f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Timer, null, tint = AccentCyan, modifier = Modifier.size(14.dp))
                        Text(fmtTime(state.elapsedSeconds), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, color = Color.White.copy(0.8f))
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("The word was:", fontSize = 12.sp, color = Color.White.copy(0.4f))
                    Text(
                        (state.answer ?: "").uppercase(),
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.Black,
                        color      = CorrectColor,
                        letterSpacing = 4.sp
                    )
                }
            }

            // Action buttons
            if (state.solved) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Next level
                    Button(
                        onClick = onNextLevel,
                        colors  = ButtonDefaults.buttonColors(containerColor = CorrectColor),
                        shape   = CircleShape,
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)
                    ) {
                        Text("Next Level", fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    // Back
                    OutlinedButton(
                        onClick = onBackToGames,
                        shape   = CircleShape,
                        border  = BorderStroke(1.dp, AccentPurple.copy(0.4f)),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Home, null, tint = AccentPurple, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Menu", fontWeight = FontWeight.Bold, color = AccentPurple)
                    }
                }
            }

            // Play again (always shown on fail; optional on win)
            if (!state.solved) {
                TextButton(onClick = onPlayAgain) {
                    Text("Try Again", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(0.5f))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun fmtTime(s: Int): String {
    val m   = s / 60; val sec = s % 60
    return "${m}:${sec.toString().padStart(2, '0')}"
}
