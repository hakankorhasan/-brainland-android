package com.example.brain_land.ui.games.liquidsort

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.foundation.Canvas
import kotlinx.coroutines.launch

// ──────────────────────────────────────────────────────────────────────────────
// Colors — mirrors iOS palette
// ──────────────────────────────────────────────────────────────────────────────

private val BgDark    = Color(0xFF10131B)
private val AccCyan   = Color(0xFF00E5FF)
private val AccPurple = Color(0xFFA78BFA)

// ──────────────────────────────────────────────────────────────────────────────
// LiquidSortPuzzleView — top-level composable
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun LiquidSortPuzzleView(
    onHome:           () -> Unit,
    onNavigateToGame: (com.example.brain_land.data.GameType) -> Unit = {}
) {
    val context  = LocalContext.current
    val prefs    = remember { context.getSharedPreferences("liquidsort_prefs", Context.MODE_PRIVATE) }
    val scope    = rememberCoroutineScope()
    val engine   = remember { LiquidSortEngine(scope) }

    var showWin     by remember { mutableStateOf(false) }
    var timerSecs   by remember { mutableIntStateOf(0) }
    var lastElapsed by remember { mutableIntStateOf(0) }

    // Load saved level on first composition
    LaunchedEffect(Unit) {
        val saved = prefs.getInt("currentLevel", 1)
        engine.loadLevel(saved)
    }

    // Timer — counts up while playing
    LaunchedEffect(showWin) {
        if (!showWin) {
            timerSecs = 0
            while (true) {
                kotlinx.coroutines.delay(1000)
                timerSecs++
            }
        }
    }

    // Win detection
    LaunchedEffect(engine.isSolved) {
        if (engine.isSolved && !showWin) {
            lastElapsed = timerSecs
            val next = engine.levelNumber + 1
            prefs.edit().putInt("currentLevel", next).apply()
            showWin = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .systemBarsPadding()
    ) {
        LiquidSortShell(
            engine    = engine,
            timerSecs = timerSecs,
            onBack    = onHome,
            onUndo    = { engine.undoLastMove() }
        )

        // ── Result sheet ──
        if (showWin) {
            com.example.brain_land.ui.games.tiltmaze.GameResultSheet(
                visible    = true,
                gameId     = "liquidSort",
                level      = engine.levelNumber,
                elapsed    = lastElapsed,
                difficulty = LiquidSortGenerator.difficultyLevel(engine.levelNumber),
                gridSize   = engine.bottles.size,
                onNextPuzzle = {
                    showWin = false
                    timerSecs = 0
                    engine.nextLevel()
                    prefs.edit().putInt("currentLevel", engine.levelNumber).apply()
                },
                onPlayAgain     = {
                    showWin = false
                    timerSecs = 0
                    engine.restart()
                },
                onBackToGames   = onHome,
                onNavigateToGame = onNavigateToGame
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// LiquidSortShell — mirrors iOS GameShellView layout exactly:
//  • Back | Title/Level | Info
//  • TIME | MOVES glass cards side-by-side
//  • Bottle grid (weighted, centred)
//  • Undo button at bottom
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun LiquidSortShell(
    engine:    LiquidSortEngine,
    timerSecs: Int,
    onBack:    () -> Unit,
    onUndo:    () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── Top bar ─────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Back button
            IconButton(
                onClick  = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White.copy(0.85f)
                )
            }

            // Title + level badge
            Column(
                modifier             = Modifier.align(Alignment.Center),
                horizontalAlignment  = Alignment.CenterHorizontally,
                verticalArrangement  = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Liquid Sort",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(AccPurple.copy(0.18f))
                        .border(1.dp, AccPurple.copy(0.35f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text(
                        "LEVEL ${engine.levelNumber}",
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color      = AccPurple
                    )
                }
            }

            // Info button (right) — mirrors iOS info icon
            IconButton(
                onClick  = { /* info sheet */ },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Info",
                    tint = Color.White.copy(0.45f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ── TIME | MOVES glass card ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(0.05f))
                .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(14.dp))
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // TIME
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Timer, null, tint = AccCyan, modifier = Modifier.size(16.dp))
                Column {
                    Text(
                        "TIME",
                        fontSize   = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White.copy(0.4f)
                    )
                    Text(
                        fmtTime(timerSecs),
                        fontSize    = 15.sp,
                        fontWeight  = FontWeight.Bold,
                        fontFamily  = FontFamily.Monospace,
                        color       = Color.White
                    )
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(Color.White.copy(0.10f))
            )

            // MOVES
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.TouchApp, null, tint = AccPurple, modifier = Modifier.size(16.dp))
                Column {
                    Text(
                        "MOVES",
                        fontSize   = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White.copy(0.4f)
                    )
                    Text(
                        "${engine.moveCount}",
                        fontSize    = 15.sp,
                        fontWeight  = FontWeight.Bold,
                        fontFamily  = FontFamily.Monospace,
                        color       = Color.White
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Bottle Grid ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            BottleGridView(engine = engine)
        }

        // ── Undo button — mirrors iOS bottom bar with undo ───────────────────
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onUndo,
            enabled = engine.canUndo,
            shape   = RoundedCornerShape(50),
            border  = BorderStroke(
                1.5.dp,
                if (engine.canUndo) AccCyan.copy(0.5f) else Color.White.copy(0.12f)
            ),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Undo,
                contentDescription = "Undo",
                tint     = if (engine.canUndo) AccCyan else Color.White.copy(0.25f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Undo",
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = if (engine.canUndo) AccCyan else Color.White.copy(0.25f)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// BottleGridView — mirrors iOS BottleGridView
// Fixed 5 columns (iOS uses columnsPerRow=5), centres bottles
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun BottleGridView(engine: LiquidSortEngine) {
    val bottles = engine.bottles
    val count   = bottles.size
    // iOS uses columnsPerRow = 5 always; mirror that
    val cols    = when {
        count <= 5  -> 3
        count <= 8  -> 4
        count <= 12 -> 4
        else        -> 5
    }
    val density = LocalDensity.current

    val shouldShowStream = engine.isStreamVisible &&
        engine.pourStartPoint != Offset.Zero &&
        engine.pourEndPoint   != Offset.Zero

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

        // ── Bottle grid ──
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier            = Modifier.padding(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 8.dp)
        ) {
            val rows = (count + cols - 1) / cols
            for (row in 0 until rows) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment     = Alignment.Bottom
                ) {
                    for (col in 0 until cols) {
                        val idx = row * cols + col
                        if (idx < count) {
                            val bottle       = bottles[idx]
                            val isPourSource = engine.pourSourceIndex == idx
                            val isPourTarget = engine.pourTargetIndex == idx

                            val offsetXDp by animateDpAsState(
                                if (isPourSource) with(density) { engine.sourceOffsetX.toDp() } else 0.dp,
                                animationSpec = spring(0.75f, 400f), label = "ox$idx"
                            )
                            val offsetYDp by animateDpAsState(
                                if (isPourSource) with(density) { engine.sourceOffsetY.toDp() } else 0.dp,
                                animationSpec = spring(0.75f, 400f), label = "oy$idx"
                            )
                            val tiltDeg by animateFloatAsState(
                                if (isPourSource) engine.sourceTilt else 0f,
                                animationSpec = spring(0.75f, 400f), label = "tl$idx"
                            )
                            val scaleFactor by animateFloatAsState(
                                if (isPourSource) engine.sourceScale else 1f,
                                animationSpec = spring(0.75f, 600f), label = "sf$idx"
                            )

                            Box(
                                modifier = Modifier
                                    .offset(offsetXDp, offsetYDp)
                                    .graphicsLayer {
                                        rotationZ      = tiltDeg
                                        scaleX         = scaleFactor
                                        scaleY         = scaleFactor
                                        shadowElevation = if (isPourSource) 24f else 0f
                                    }
                                    .onGloballyPositioned { coords ->
                                        val bounds = coords.boundsInWindow()
                                        engine.bottleFrames[idx] = Rect(
                                            Offset(bounds.left, bounds.top),
                                            androidx.compose.ui.geometry.Size(bounds.width, bounds.height)
                                        )
                                    }
                            ) {
                                BottleView(
                                    bottle          = bottle,
                                    index           = idx,
                                    isSelected      = engine.selected == idx,
                                    isPendingSource = engine.pendingSourceIndex == idx,
                                    isShaking       = engine.invalidShakeIndex == idx,
                                    isPourSource    = isPourSource,
                                    isPourTarget    = isPourTarget,
                                    pourColor       = engine.pourColor,
                                    drainProgress   = if (isPourSource) engine.drainProgress  else 0f,
                                    fillProgress    = if (isPourTarget) engine.fillProgress   else 0f,
                                    liquidTilt      = engine.liquidTiltFactor,
                                    flowBias        = engine.flowBias,
                                    splashProgress  = if (isPourTarget) engine.splashProgress else 0f,
                                    onTap           = { engine.selectBottle(idx) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Pour stream overlay (Canvas, full-size, no hit-testing) ──
        if (shouldShowStream) {
            PourStreamOverlay(
                startPoint     = engine.pourStartPoint,
                endPoint       = engine.pourEndPoint,
                streamProgress = engine.streamProgress,
                color          = engine.pourColor ?: LiquidColor.CYAN
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// PourStreamOverlay — Bezier arc stream between bottle mouths
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun PourStreamOverlay(
    startPoint:     Offset,
    endPoint:       Offset,
    streamProgress: Float,
    color:          LiquidColor
) {
    val animProg by animateFloatAsState(
        streamProgress,
        tween(120, easing = FastOutSlowInEasing),
        label = "sp"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (animProg <= 0f) return@Canvas

        val ctrlX = (startPoint.x + endPoint.x) / 2f
        val ctrlY = minOf(startPoint.y, endPoint.y) - 40f

        val path = Path().apply {
            moveTo(startPoint.x, startPoint.y)
            quadraticTo(ctrlX, ctrlY, endPoint.x, endPoint.y)
        }

        val streamW = 9f * animProg

        // Main stream
        drawPath(
            path  = path,
            brush = Brush.linearGradient(
                listOf(color.topColor, color.bottomColor),
                start = startPoint, end = endPoint
            ),
            style = Stroke(width = streamW, cap = StrokeCap.Round),
            alpha = animProg
        )

        // Inner highlight
        drawPath(
            path  = path,
            color = Color.White.copy(0.35f * animProg),
            style = Stroke(width = streamW * 0.3f, cap = StrokeCap.Round)
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Helpers
// ──────────────────────────────────────────────────────────────────────────────

private fun fmtTime(s: Int): String {
    val m   = s / 60
    val sec = s % 60
    return if (m > 0) "${m}m ${sec.toString().padStart(2, '0')}s"
           else "${sec.toString().padStart(2, '0')}s"
}
