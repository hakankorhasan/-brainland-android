package com.example.brain_land.ui.games.pipeconnect

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.brain_land.R
import com.example.brain_land.data.GameType
import com.example.brain_land.ui.games.tiltmaze.GameResultSheet
import com.example.brain_land.ui.games.tiltmaze.GameShellView

private val BgDark     = Color(0xFF10131B)
private val WaterBlue  = Color(0xFF00E5FF)
private val LeakRed    = Color(0xFFFF453A)
private val PipeColor  = Color(0xFFB8C0D0)
private val PipeBg     = Color(0xFF383C4A)
private val LockedBg   = Color(0xFF264C3B)
private val LockedPipe = Color(0xFF34C759)

@Composable
fun PipeConnectPuzzleView(
    onHome: () -> Unit,
    onNavigateToGame: (GameType) -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("pipeconnect_prefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    
    val currentLevel = prefs.getInt("currentLevel", 1)
    val engine = remember { PipeConnectEngine(scope = scope, initialLevel = currentLevel) }
    
    var showWin by remember { mutableStateOf(false) }
    var showGameOver by remember { mutableStateOf(false) }
    var timerSecs by remember { mutableIntStateOf(0) }
    var lastElapsed by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        engine.loadLevel()
    }
    
    LaunchedEffect(showWin, showGameOver) {
        if (!showWin && !showGameOver && !engine.isLoading) {
            timerSecs = 0
            while(true) {
                delay(1000)
                timerSecs++
            }
        }
    }
    
    LaunchedEffect(engine.isSolved) {
        if (engine.isSolved && !showWin) {
            lastElapsed = timerSecs
            val nextLvl = engine.levelNumber + 1
            prefs.edit().putInt("currentLevel", nextLvl).apply()
            val completed = prefs.getInt("completedLevel", 0)
            if (engine.levelNumber > completed) {
                prefs.edit().putInt("completedLevel", engine.levelNumber).apply()
            }
            delay(500)
            showWin = true
        }
    }
    
    LaunchedEffect(engine.isGameOver) {
        if (engine.isGameOver && !showGameOver) {
            showGameOver = true
        }
    }
    
    fun difficultyLevel(l: Int): Int = when(l) {
        in 1..15 -> 2
        in 16..40 -> 3
        in 41..80 -> 5
        in 81..130 -> 6
        in 131..180 -> 7
        in 181..220 -> 8
        else -> 10
    }

    BackHandler(enabled = !showWin && !showGameOver) { /* block accidental back */ }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .systemBarsPadding()
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        if (engine.isLoading || engine.cells.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White.copy(0.3f))
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Custom header (no restart button, matching iOS) ──
                PipeConnectHeader(
                    level = engine.levelNumber,
                    timerSecs = timerSecs,
                    moves = engine.moveCount,
                    onBack = onHome
                )

                // ── Board (fills remaining space) ──
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PipeConnectBoard(engine = engine)
                }

                // ── "Open Valve" button ──
                OpenValveButton(
                    onClick = { engine.flowWater() },
                    isAnimating = engine.isAnimatingWater
                )
            }
        }
        
        GameResultSheet(
            visible          = showWin,
            gameId           = "pipeConnect",
            level            = engine.levelNumber,
            elapsed          = lastElapsed,
            difficulty       = difficultyLevel(engine.levelNumber),
            gridSize         = engine.size,
            onNextPuzzle     = { 
                showWin = false
                engine.nextLevel()
            },
            onPlayAgain      = {
                showWin = false
                engine.resetPuzzle()
            },
            onBackToGames    = onHome,
            onNavigateToGame = onNavigateToGame
        )
        
        if (showGameOver) {
            GameOverOverlay(
                onRetry = {
                    showGameOver = false
                    engine.resetPuzzle()
                },
                onHome = onHome
            )
        }
    }
}

// ── Open Valve button ─────────────────────────────────────────────────────
// Matches the iOS orange pill at the bottom of the screen

@Composable
fun OpenValveButton(onClick: () -> Unit, isAnimating: Boolean) {
    val valveColor    = Color(0xFFD4622A)   // iOS orange
    val valveDimmed  = Color(0xFF3D2015)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp, top = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onClick,
            enabled = !isAnimating,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = valveColor,
                disabledContainerColor = valveDimmed
            ),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.game_water_man),
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (isAnimating) "Akıyor…" else "Open Valve",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

// ── Old alias kept for any remaining call sites (now unused)
@Composable
fun FlowWaterButton(onClick: () -> Unit, isAnimating: Boolean) = OpenValveButton(onClick, isAnimating)

@Composable
fun GameOverOverlay(onRetry: () -> Unit, onHome: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.6f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF1D1B29))
                .border(1.dp, Color.Red.copy(0.2f), RoundedCornerShape(28.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // sewage icon from iOS assets
            Image(
                painter = painterResource(id = R.drawable.game_sewage),
                contentDescription = null,
                modifier = Modifier.size(56.dp)
            )
            Text("Game Over", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Can bitti!", fontSize = 16.sp, color = Color.White.copy(0.7f))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262935))) {
                    Text("Tekrar", color = Color(0xFF87D2C8), fontWeight = FontWeight.SemiBold)
                }
                Button(onClick = onHome, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262935))) {
                    Text("Ana Menü", color = Color(0xFFAFABE5), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Custom header — matches iOS (title, level badge, time/moves, NO restart) ───────

@Composable
private fun PipeConnectHeader(level: Int, timerSecs: Int, moves: Int, onBack: () -> Unit) {
    val timeStr = "%d:%02d".format(timerSecs / 60, timerSecs % 60)
    val AccentPurp = Color(0xFFB88AE8)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Top bar
        Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White.copy(0.80f))
            }
            Text(
                "Pipe Connect",
                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
            IconButton(
                onClick  = { /* info sheet */ },
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Info",
                    tint = Color.White.copy(0.45f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Level badge
        Text(
            "LEVEL $level",
            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentPurp,
            letterSpacing = 0.5.sp,
            modifier = Modifier
                .background(AccentPurp.copy(0.10f), CircleShape)
                .border(0.5.dp, AccentPurp.copy(0.20f), CircleShape)
                .padding(horizontal = 14.dp, vertical = 5.dp)
        )

        Spacer(Modifier.height(12.dp))

        // Stats row (TIME | MOVES) — same card style as iOS
        Row(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(0.04f))
                .border(0.5.dp, Color.White.copy(0.08f), RoundedCornerShape(14.dp))
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TIME
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TIME", fontSize = 10.sp, color = Color.White.copy(0.35f), fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Timer, null, tint = Color(0xFFFFBF24), modifier = Modifier.size(14.dp))
                    Text(timeStr, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            // Divider
            Box(Modifier.width(1.dp).height(30.dp).background(Color.White.copy(0.08f)))
            // MOVES
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("MOVES", fontSize = 10.sp, color = Color.White.copy(0.35f), fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.TouchApp, null, tint = Color(0xFFB88AE8), modifier = Modifier.size(14.dp))
                    Text("$moves", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ── Board ──────────────────────────────────────────────────────────────────

@Composable
fun PipeConnectBoard(engine: PipeConnectEngine) {
    val size = engine.size
    val totalAvailableWidth = LocalDensity.current.run { 
        val w = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
        (w - 48.dp).toPx()
    }
    
    val sideWidthRatio = 0.7f
    val cellSize = totalAvailableWidth / (size + sideWidthRatio * 2)
    val cellSizeDp = LocalDensity.current.run { cellSize.toDp() }
    val sideSizeDp = LocalDensity.current.run { (cellSize * sideWidthRatio).toDp() }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column {
            // Top Row (source/sink if direction is UP)
            if (engine.sourceDirection == PipeDirection.UP || engine.sinkDirection == PipeDirection.UP) {
                Row {
                    Spacer(modifier = Modifier.size(sideSizeDp))
                    for (col in 0 until size) {
                        Box(modifier = Modifier.size(cellSizeDp, sideSizeDp), contentAlignment = Alignment.Center) {
                            if (engine.sourceRow == col && engine.sourceDirection == PipeDirection.UP) {
                                WaterSourceArrow(cellSize = cellSizeDp, dir = PipeDirection.UP) { engine.flowWater() }
                            }
                            if (engine.sinkRow == col && engine.sinkDirection == PipeDirection.UP) {
                                WaterSinkArrow(cellSize = cellSizeDp, dir = PipeDirection.UP)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.size(sideSizeDp))
                }
            }
            
            // Middle: Left icons | Grid | Right icons
            Row {
                // Left Column
                Column {
                    for (row in 0 until size) {
                        Box(modifier = Modifier.size(sideSizeDp, cellSizeDp), contentAlignment = Alignment.Center) {
                            if (engine.sourceRow == row && engine.sourceDirection == PipeDirection.LEFT) {
                                WaterSourceArrow(cellSize = cellSizeDp, dir = PipeDirection.LEFT) { engine.flowWater() }
                            }
                            if (engine.sinkRow == row && engine.sinkDirection == PipeDirection.LEFT) {
                                WaterSinkArrow(cellSize = cellSizeDp, dir = PipeDirection.LEFT)
                            }
                        }
                    }
                }
                
                // Grid Cells container with stylized dark background
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF1E212B), Color(0xFF13151D))
                            )
                        )
                        .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(12.dp))
                        .padding(2.dp)
                ) {
                    Column {
                        for (row in 0 until size) {
                            Row {
                                for (col in 0 until size) {
                                    val cell = engine.cells.getOrNull(row)?.getOrNull(col)
                                    if (cell != null) {
                                        PipeCellView(
                                            cell = cell,
                                            isAnimating = engine.isAnimatingWater,
                                            cellSize = cellSizeDp,
                                            onTap = { engine.rotatePipe(row, col) }
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.size(cellSizeDp))
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Right Column
                Column {
                    for (row in 0 until size) {
                        Box(modifier = Modifier.size(sideSizeDp, cellSizeDp), contentAlignment = Alignment.Center) {
                            if (engine.sourceRow == row && engine.sourceDirection == PipeDirection.RIGHT) {
                                WaterSourceArrow(cellSize = cellSizeDp, dir = PipeDirection.RIGHT) { engine.flowWater() }
                            }
                            if (engine.sinkRow == row && engine.sinkDirection == PipeDirection.RIGHT) {
                                WaterSinkArrow(cellSize = cellSizeDp, dir = PipeDirection.RIGHT)
                            }
                        }
                    }
                }
            }
            
            // Bottom Row (source/sink if direction is DOWN)
            if (engine.sourceDirection == PipeDirection.DOWN || engine.sinkDirection == PipeDirection.DOWN) {
                Row {
                    Spacer(modifier = Modifier.size(sideSizeDp))
                    for (col in 0 until size) {
                        Box(modifier = Modifier.size(cellSizeDp, sideSizeDp), contentAlignment = Alignment.Center) {
                            if (engine.sourceCol == col && engine.sourceDirection == PipeDirection.DOWN) {
                                WaterSourceArrow(cellSize = cellSizeDp, dir = PipeDirection.DOWN) { engine.flowWater() }
                            }
                            if (engine.sinkCol == col && engine.sinkDirection == PipeDirection.DOWN) {
                                WaterSinkArrow(cellSize = cellSizeDp, dir = PipeDirection.DOWN)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.size(sideSizeDp))
                }
            }
        }
    }
}

@Composable
fun WaterSourceArrow(cellSize: androidx.compose.ui.unit.Dp, dir: PipeDirection, onTap: () -> Unit) {
    // iOS: water-man image, tappable to open valve
    Box(
        modifier = Modifier
            .size(cellSize * 0.7f)
            .offset(y = -(cellSize * 0.13f))
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.game_water_man),
            contentDescription = "Su kaynağı",
            modifier = Modifier.size(cellSize * 0.65f)
        )
    }
}

@Composable
fun WaterSinkArrow(cellSize: androidx.compose.ui.unit.Dp, dir: PipeDirection) {
    // iOS: sewage / drain image
    Box(
        modifier = Modifier
            .size(cellSize * 0.7f)
            .offset(y = (cellSize * 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.game_sewage),
            contentDescription = "Drenaj",
            modifier = Modifier.size(cellSize * 0.65f)
        )
    }
}

@Composable
fun PipeCellView(cell: PipeCell, isAnimating: Boolean, cellSize: androidx.compose.ui.unit.Dp, onTap: () -> Unit) {
    val rotationAngle by animateFloatAsState(
        targetValue = cell.rotation * 90f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f)
    )
    
    val trimFraction by animateFloatAsState(
        targetValue = if (cell.isFilled) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = LinearEasing)
    )
    
    val isLeaking = cell.isLeaking
    val leakAlpha by animateFloatAsState(
        targetValue = if (isLeaking) 1f else 0f,
        animationSpec = infiniteRepeatable(tween(200), RepeatMode.Reverse)
    )

    Box(
        modifier = Modifier
            .size(cellSize)
            .background(Color.White.copy(0.015f))
            .border(0.5.dp, Color.White.copy(0.02f))
            .clickable(enabled = !cell.isBlocked && !cell.isLocked && !isAnimating) { onTap() },
        contentAlignment = Alignment.Center
    ) {
        if (cell.isBlocked) {
            // Wall asset — mirrors iOS Image("wall\(cell.wallVariant)")
            val wallRes = when (cell.wallVariant) {
                1 -> R.drawable.game_wall1
                2 -> R.drawable.game_wall2
                3 -> R.drawable.game_wall3
                else -> R.drawable.game_wall4
            }
            Image(
                painter = painterResource(id = wallRes),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .size(cellSize)
                    .clip(RoundedCornerShape(6.dp))
            )
        } else {
            // Draw Pipe Body
            val pipeBg = if (cell.isLocked) LockedBg else PipeBg
            val pipeFg = if (cell.isLocked) LockedPipe else PipeColor
            
            Canvas(modifier = Modifier.fillMaxSize().rotate(rotationAngle)) {
                drawPipeShape(cell.pipeType.baseConnections, pipeFg, pipeBg)
            }
            
            // Draw Water
            if (cell.isFilled && cell.waterEntry != null) {
                // If it's filled and we know the entry, we can draw the animated directional flow
                DirectionalWaterFlowView(
                    modifier = Modifier.fillMaxSize(),
                    entry = cell.waterEntry!!,
                    exits = cell.waterDirections.filter { it != cell.waterEntry }.toList(),
                    waterColor = WaterBlue,
                    trimFraction = trimFraction
                )
            } else if (cell.isFilled && cell.waterEntry == null) {
                // Fallback direct fill (e.g. source cell immediately)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawWaterFlow(cell.connections, WaterBlue)
                }
            }
            
            // If Leaking, draw red leak
            if (isLeaking) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LeakRed.copy(alpha = leakAlpha * 0.4f))
                        .border(2.dp, LeakRed.copy(alpha = leakAlpha))
                )
            }
        }
    }
}
