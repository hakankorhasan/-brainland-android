package com.example.brain_land.ui.games.arrowpuzzle

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.brain_land.data.GameType
import com.example.brain_land.ui.games.tiltmaze.GameResultSheet
import com.example.brain_land.ui.games.tiltmaze.GameShellView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Shell: ArrowPuzzlePuzzleView
// Combines the game wrapper and win screen.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ArrowPuzzlePuzzleView(
    onHome: () -> Unit,
    onNavigateToGame: (GameType) -> Unit = {}
) {
    val context = LocalContext.current
    val prefs   = remember { context.getSharedPreferences("arrowpuzzle_prefs", Context.MODE_PRIVATE) }

    var currentLevel  by remember { mutableIntStateOf(1) }
    var boardKey      by remember { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    var showWin       by remember { mutableStateOf(false) }
    var timerSecs     by remember { mutableIntStateOf(0) }
    
    val viewModel = remember { ArrowPuzzleViewModel() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        currentLevel = prefs.getInt("currentLevel", 1)
        viewModel.generateNewLevel(currentLevel)
    }
    
    // Timer
    LaunchedEffect(boardKey, showWin) {
        if (!showWin && !viewModel.isGenerating) {
            timerSecs = 0
            while (true) {
                delay(1000)
                timerSecs++
            }
        }
    }
    
    // Win watcher
    LaunchedEffect(viewModel.isSolved) {
        if (viewModel.isSolved) {
            val saved = prefs.getInt("completedLevel", 0)
            if (currentLevel > saved) prefs.edit().putInt("completedLevel", currentLevel).apply()
            delay(800)
            showWin = true
        }
    }

    fun restart() {
        showWin = false
        timerSecs = 0
        viewModel.resetLevel()
    }

    fun advanceLevel() {
        currentLevel++
        prefs.edit().putInt("currentLevel", currentLevel).apply()
        showWin = false
        timerSecs = 0
        boardKey = java.util.UUID.randomUUID().toString()
        scope.launch {
            viewModel.generateNewLevel(currentLevel)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF10131B))
            .systemBarsPadding()
    ) {
        GameShellView(
            title      = "Arrow Puzzle",
            level      = currentLevel,
            timerSecs  = timerSecs,
            onBack     = onHome,
            onRestart  = { restart() }
        ) {
            key(boardKey) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (viewModel.isGenerating) {
                        CircularProgressIndicator(color = Color(0xFF5CC8D4))
                    } else {
                        ArrowPuzzleView(
                            viewModel = viewModel,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    }
                }
            }
        }

        // Win result
        GameResultSheet(
            visible          = showWin,
            gameId           = "arrowPuzzle",
            level            = currentLevel,
            elapsed          = timerSecs,
            difficulty       = minOf(currentLevel, 10), // Base difficulty mapping
            gridSize         = viewModel.grid.cols,
            onNextPuzzle     = { advanceLevel() },
            onPlayAgain      = {
                scope.launch {
                    viewModel.generateNewLevel(currentLevel)
                    restart()
                }
            },
            onBackToGames    = onHome,
            onNavigateToGame = { gameType -> onNavigateToGame(gameType) }
        )
    }
}
