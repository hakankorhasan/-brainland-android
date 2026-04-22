package com.example.brain_land.ui.games.tiltmaze

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

private val BgDark     = Color(0xFF10131B)
private val AccentCyan = Color(0xFF5CC8D4)
private val AccentPurp = Color(0xFFA299EC)

// ─────────────────────────────────────────────────────────────────────────────
// TiltMazePuzzleView
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TiltMazePuzzleView(
    onHome: () -> Unit,
    onNavigateToGame: (com.example.brain_land.data.GameType) -> Unit = {}
) {
    val context = LocalContext.current
    val prefs   = remember { context.getSharedPreferences("tiltmaze_prefs", Context.MODE_PRIVATE) }

    var currentLevel  by remember { mutableIntStateOf(1) }
    var boardKey      by remember { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    var showWin       by remember { mutableStateOf(false) }
    var lastElapsed   by remember { mutableIntStateOf(0) }
    var tutorialShown by remember { mutableStateOf(prefs.getBoolean("tutorialShown", false)) }
    var timerSecs     by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        currentLevel = prefs.getInt("currentLevel", 1)
    }

    fun restart() {
        showWin   = false
        timerSecs = 0
        boardKey  = java.util.UUID.randomUUID().toString()
    }

    fun advanceLevel() {
        currentLevel++
        prefs.edit().putInt("currentLevel", currentLevel).apply()
        showWin     = false
        lastElapsed = 0
        timerSecs   = 0
        boardKey    = java.util.UUID.randomUUID().toString()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .systemBarsPadding()
    ) {
        GameShellView(
            title      = "Tilt Maze",
            level      = currentLevel,
            timerSecs  = timerSecs,
            onBack     = onHome,
            onRestart  = { restart() }
        ) {
            key(boardKey) {
                TiltMazeView(
                    level               = currentLevel,
                    tutorialShown       = tutorialShown,
                    onTutorialDismissed = {
                        tutorialShown = true
                        prefs.edit().putBoolean("tutorialShown", true).apply()
                    },
                    onTimerTick = { timerSecs = it },
                    onWin = { elapsed ->
                        lastElapsed = elapsed
                        val saved = prefs.getInt("completedLevel", 0)
                        if (currentLevel > saved) prefs.edit().putInt("completedLevel", currentLevel).apply()
                        showWin = true
                    },
                    onRestart = { restart() },
                    modifier  = Modifier.fillMaxSize()
                )
            }
        }

        // iOS-style result sheet (Dialog — covers navbar)
        GameResultSheet(
            visible          = showWin,
            gameId           = "tiltMaze",
            level            = currentLevel,
            elapsed          = lastElapsed,
            difficulty       = tiltMazeDifficulty(tiltMazeSizeForLevel(currentLevel)),
            gridSize         = tiltMazeSizeForLevel(currentLevel),
            onNextPuzzle     = { advanceLevel() },
            onPlayAgain      = { restart() },
            onBackToGames    = onHome,
            onNavigateToGame = { gameType -> onNavigateToGame(gameType) }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GameShellView — exactly mirrors iOS GameShellView layout:
//   Row: < back    |   Title (centered)   |  [spacer 48dp]
//   LEVEL N — purple capsule (centered)
//   Glass TIME card
//   Board (with glow frame)
//   Heat bar (from TiltMazeView)
//   🔄 Restart neon capsule button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GameShellView(
    title:     String,
    level:     Int,
    timerSecs: Int,
    onBack:    () -> Unit,
    onRestart: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Top bar: < Title (spacer placeholder right) ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack, "Back",
                    tint = Color.White.copy(0.8f)
                )
            }
            Text(
                title,
                fontSize  = 17.sp, fontWeight = FontWeight.Bold, color = Color.White,
                modifier  = Modifier.align(Alignment.Center)
            )
            // Invisible spacer to balance left icon (keeps title truly centered)
            Spacer(Modifier.size(48.dp).align(Alignment.CenterEnd))
        }

        Spacer(Modifier.height(6.dp))

        // ── Level badge ──
        Text(
            "LEVEL $level",
            fontSize     = 12.sp,
            fontWeight   = FontWeight.Bold,
            color        = AccentPurp,
            letterSpacing = 0.5.sp,
            modifier     = Modifier
                .background(AccentPurp.copy(0.10f), CircleShape)
                .border(0.5.dp, AccentPurp.copy(0.20f), CircleShape)
                .padding(horizontal = 14.dp, vertical = 5.dp)
        )

        Spacer(Modifier.height(12.dp))

        // ── TIME glass card (mirrors iOS statsCard) ──
        TimeCard(timerSecs = timerSecs)

        Spacer(Modifier.height(12.dp))

        // ── Board ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            content()
        }

        Spacer(Modifier.height(12.dp))

        // ── Restart neon capsule button (mirrors iOS neonButton) ──
        OutlinedButton(
            onClick = onRestart,
            shape   = CircleShape,
            border  = BorderStroke(1.2.dp, AccentCyan.copy(0.6f)),
            colors  = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan),
            modifier = Modifier
                .background(AccentCyan.copy(0.08f), CircleShape)
        ) {
            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("Restart", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TIME glass card — mirrors iOS statsCard with glass background + dividers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TimeCard(timerSecs: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(0.03f))
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(0.08f),
                        Color.White.copy(0.03f),
                        Color.White.copy(0.06f)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // "TIME" label
            Text(
                "TIME",
                fontSize      = 9.sp,
                fontWeight    = FontWeight.Medium,
                color         = Color.White.copy(0.30f),
                letterSpacing = 1.2.sp
            )

            // Clock icon + time value
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Schedule, null,
                    tint     = AccentCyan,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    formatTimer(timerSecs),
                    fontSize     = 20.sp,
                    fontWeight   = FontWeight.Bold,
                    fontFamily   = FontFamily.Monospace,
                    color        = Color.White
                )
            }
        }
    }
}

private fun formatTimer(secs: Int): String {
    val m = secs / 60; val s = secs % 60
    return "%d:%02d".format(m, s)
}
