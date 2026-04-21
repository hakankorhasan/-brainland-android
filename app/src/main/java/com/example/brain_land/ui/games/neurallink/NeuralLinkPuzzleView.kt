package com.example.brain_land.ui.games.neurallink

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.brain_land.data.GameType
import com.example.brain_land.ui.games.tiltmaze.GameResultSheet
import kotlin.math.abs
import kotlin.math.floor

// ─────────────────────────────────────────────────────────────────────────────
// Root composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NeuralLinkPuzzleView(
    onHome: () -> Unit,
    onNavigateToGame: (GameType) -> Unit = {}
) {
    val context = LocalContext.current
    val prefs   = remember { context.getSharedPreferences("neurallink_prefs", Context.MODE_PRIVATE) }
    val scope   = rememberCoroutineScope()
    val engine  = remember { NeuralLinkEngine(scope, prefs.getInt("currentLevel", 1)) }

    var showWin     by remember { mutableStateOf(false) }
    var timerSecs   by remember { mutableIntStateOf(0) }
    var lastElapsed by remember { mutableIntStateOf(0) }
    var neuralActivation by remember { mutableStateOf(false) }

    BackHandler(enabled = !showWin) { /* block accidental back */ }

    LaunchedEffect(Unit) { engine.loadLevel() }

    LaunchedEffect(showWin, engine.isLoading) {
        if (!showWin && !engine.isLoading) {
            timerSecs = 0; while (true) { delay(1000); timerSecs++ }
        }
    }

    LaunchedEffect(engine.isSolved) {
        if (engine.isSolved && !showWin) {
            lastElapsed = timerSecs
            prefs.edit().putInt("currentLevel", engine.levelNumber + 1).apply()
            neuralActivation = true
            delay(1000)
            showWin = true
        }
    }

    fun difficultyLevel(l: Int): Int = when (l) {
        in 1..15 -> 2; in 16..40 -> 3; in 41..80 -> 5; in 81..130 -> 6; else -> 8
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080A10))
            .systemBarsPadding()
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        if (engine.isLoading || engine.cells.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(color = Color(0xFF00E5FF), strokeWidth = 2.dp)
                    Text("Scanning neural pathways…", color = Color.White.copy(0.4f), fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
            }
        } else {
            NeuralLinkGameContent(
                engine           = engine,
                timerSecs        = timerSecs,
                neuralActivation = neuralActivation,
                onBack           = onHome,
                onReset          = { engine.resetPuzzle(); neuralActivation = false },
                onUndo           = { engine.undoLastMove(); neuralActivation = false }
            )
        }

        GameResultSheet(
            visible          = showWin,
            gameId           = "neuralLink",
            level            = engine.levelNumber,
            elapsed          = lastElapsed,
            difficulty       = difficultyLevel(engine.levelNumber),
            gridSize         = engine.gridSize,
            onNextPuzzle     = { showWin = false; neuralActivation = false; engine.nextLevel() },
            onPlayAgain      = { showWin = false; neuralActivation = false; engine.resetPuzzle() },
            onBackToGames    = onHome,
            onNavigateToGame = onNavigateToGame
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Game layout
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NeuralLinkGameContent(
    engine: NeuralLinkEngine,
    timerSecs: Int,
    neuralActivation: Boolean,
    onBack: () -> Unit,
    onReset: () -> Unit,
    onUndo: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Top bar
        Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White.copy(0.8f))
            }
            Text(
                "Neural Link",
                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
            Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = onUndo, enabled = engine.canUndo) {
                    Icon(Icons.AutoMirrored.Filled.Undo, "Undo",
                        tint = if (engine.canUndo) Color(0xFF00E5FF) else Color.White.copy(0.3f))
                }
                IconButton(onClick = onReset) {
                    Icon(Icons.Default.Refresh, "Reset", tint = Color.White.copy(0.5f))
                }
            }
        }

        // ── Level badge
        Text(
            "Level ${engine.levelNumber}  ·  ${engine.gridSize}×${engine.gridSize}  ·  ${engine.flowCount} flows",
            fontSize = 11.sp, color = Color.White.copy(0.4f), fontFamily = FontFamily.Monospace
        )

        Spacer(Modifier.height(8.dp))

        // ── Stat bar
        NeuralStatBar(engine = engine, timerSecs = timerSecs)

        Spacer(Modifier.height(8.dp))

        // ── Grid (fills remaining space)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            NeuralLinkGrid(engine = engine, neuralActivation = neuralActivation)
        }

        Spacer(Modifier.height(8.dp))

        // ── Synapse indicator (flow completion dots)
        SynapseIndicator(engine = engine)

        Spacer(Modifier.height(12.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stat bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NeuralStatBar(engine: NeuralLinkEngine, timerSecs: Int) {
    val timeStr = "%d:%02d".format(timerSecs / 60, timerSecs % 60)
    val completed = engine.completedFlows.size

    Row(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0D0F1A))
            .border(0.5.dp, Color(0xFF00E5FF).copy(0.15f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NeuralStat(icon = Icons.Default.Timer,   label = "TIME",  value = timeStr)
        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.08f)))
        NeuralStat(icon = Icons.Default.GridOn,  label = "SIZE",  value = "${engine.gridSize}×${engine.gridSize}")
        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.08f)))
        NeuralStat(icon = Icons.Default.Link,    label = "LINKS", value = "$completed/${engine.flowCount}")
        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.08f)))
        NeuralStat(icon = Icons.Default.Edit,    label = "MOVES", value = "${engine.moveCount}")
    }
}

@Composable
private fun NeuralStat(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, fontSize = 7.sp, color = Color.White.copy(0.3f), fontFamily = FontFamily.Monospace, letterSpacing = 0.8.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(icon, null, tint = Color(0xFF00E5FF).copy(0.7f), modifier = Modifier.size(9.dp))
            Text(value, fontSize = 10.sp, color = Color.White.copy(0.75f), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Synapse indicator
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SynapseIndicator(engine: NeuralLinkEngine) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (fi in 0 until engine.flowCount) {
            val connected = engine.completedFlows.contains(fi)
            val color = Color(neuralColors[fi % neuralColors.size])
            val scale by animateFloatAsState(
                if (connected) 1f else 0.85f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                label = "dot$fi"
            )
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .clip(CircleShape)
                    .background(color.copy(alpha = if (connected) 1f else 0.4f))
                    .border(1.dp, Color.White.copy(0.3f), CircleShape)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Neural Link Grid — drag handling + cell rendering
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NeuralLinkGrid(engine: NeuralLinkEngine, neuralActivation: Boolean) {
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val availW = with(density) { maxWidth.toPx() }
        val availH = with(density) { maxHeight.toPx() }
        val n      = engine.gridSize

        val spacing   = with(density) { 2.dp.toPx() }
        val padPx     = with(density) { 16.dp.toPx() }

        // Fit grid in both dimensions
        val stepFromW = (availW - padPx) / n
        val stepFromH = (availH - padPx) / n
        val step      = floor(minOf(stepFromW, stepFromH))
        val cellSize  = (step - spacing).coerceAtLeast(with(density) { 12.dp.toPx() })
        val gridSide  = step * n

        val cellSizeDp = with(density) { cellSize.toDp() }
        val stepDp     = with(density) { step.toDp() }
        val gridSideDp = with(density) { gridSide.toDp() }

        var lastDragCell by remember { mutableStateOf<Pair<Int,Int>?>(null) }

        val brightnessAnim by animateFloatAsState(
            targetValue = if (neuralActivation) 0.15f else 0f,
            animationSpec = tween(800),
            label = "brightnessAnim"
        )

        Box(
            modifier = Modifier
                .size(gridSideDp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF070910))
                .border(
                    width = if (neuralActivation) 1.dp else 0.5.dp,
                    color = if (neuralActivation) Color(0xFF00E5FF).copy(0.4f) else Color.White.copy(0.05f),
                    shape = RoundedCornerShape(12.dp)
                )
                .pointerInput(engine.gridSize, engine.levelNumber) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            val col = (change.position.x / step).toInt().coerceIn(0, n - 1)
                            val row = (change.position.y / step).toInt().coerceIn(0, n - 1)

                            when (event.type) {
                                PointerEventType.Press -> {
                                    engine.beginDrag(row, col)
                                    lastDragCell = row to col
                                    change.consume()
                                }
                                PointerEventType.Move -> {
                                    if (change.pressed) {
                                        val last = lastDragCell
                                        if (last == null || last.first != row || last.second != col) {
                                            // Interpolate for fast drags
                                            if (last != null) {
                                                for (c in interpolate(last, row to col, n)) {
                                                    engine.continueDrag(c.first, c.second)
                                                }
                                            } else {
                                                engine.continueDrag(row, col)
                                            }
                                            lastDragCell = row to col
                                        }
                                        change.consume()
                                    }
                                }
                                PointerEventType.Release -> {
                                    engine.endDrag()
                                    lastDragCell = null
                                    change.consume()
                                }
                                else -> {}
                            }
                        }
                    }
                }
        ) {
            // Render cells using absolute positioning (like iOS ZStack+.position)
            for (row in 0 until n) {
                for (col in 0 until n) {
                    val cell = engine.cells.getOrNull(row)?.getOrNull(col) ?: continue
                    val isComplete = cell.flowIndex?.let { engine.completedFlows.contains(it) } ?: false
                    val dirs = engine.connectedDirections(row, col)

                    NeuralLinkCellView(
                        cell       = cell,
                        cellSize   = cellSizeDp,
                        dirs       = dirs,
                        isComplete = isComplete,
                        brightness = brightnessAnim,
                        modifier   = Modifier.offset(x = stepDp * col, y = stepDp * row)
                    )
                }
            }

            // Pulse ring for newly completed flows
            for (fi in engine.completedFlows) {
                val ep = engine.endpoints.getOrNull(fi) ?: continue
                val (r, c) = ep.second
                PulseRing(
                    color    = Color(neuralColors[fi % neuralColors.size]),
                    cellSize = cellSizeDp,
                    modifier = Modifier.offset(x = stepDp * c, y = stepDp * r)
                )
            }
        }
    }
}

@Composable
private fun PulseRing(color: Color, cellSize: Dp, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.8f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseOut), RepeatMode.Restart),
        label = "pulseScale"
    )
    val alpha = if (scale > 1.2f) 0f else (0.8f * (1f - (scale - 0.3f) / 0.9f)).coerceAtLeast(0f)

    Box(modifier = modifier.size(cellSize)) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(cellSize * scale)
                .border(2.dp, color.copy(alpha = alpha * 0.5f), CircleShape)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Cell View — mirrors iOS NeuralLinkCellView exactly
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NeuralLinkCellView(
    cell: NeuralCell,
    cellSize: Dp,
    dirs: NeuralLinkEngine.Directions,
    isComplete: Boolean,
    brightness: Float = 0f,
    modifier: Modifier = Modifier
) {
    val color = cell.flowIndex?.let { Color(neuralColors[it % neuralColors.size]) }
    val thickness = cellSize * 0.30f
    val ext = 2.dp

    Box(
        modifier = modifier
            .size(cellSize)
            .clip(RoundedCornerShape(3.dp))
            .drawBehind {
                // Background
                when {
                    cell.isDead -> drawRect(Color(0xFF0F0508))
                    cell.flowIndex != null && !cell.isEndpoint -> {
                        drawRect(color!!.copy(alpha = 0.10f + brightness * 0.5f))
                        drawCircle(Brush.radialGradient(
                            listOf(color.copy(alpha = 0.08f + brightness * 0.2f), Color.Transparent),
                            radius = size.minDimension * 0.6f
                        ), radius = size.minDimension * 0.6f)
                    }
                    else -> drawRect(Color(0xFF080A18))
                }

                // Grid line for empty cells
                if (!cell.isDead && cell.flowIndex == null) {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.04f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f),
                        style = Stroke(0.5f)
                    )
                }

                // Dead neuron X
                if (cell.isDead) {
                    val pad = size.minDimension * 0.28f
                    drawLine(Color.Red.copy(0.2f), Offset(pad, pad), Offset(size.width - pad, size.height - pad), size.minDimension * 0.08f, StrokeCap.Round)
                    drawLine(Color.Red.copy(0.2f), Offset(size.width - pad, pad), Offset(pad, size.height - pad), size.minDimension * 0.08f, StrokeCap.Round)
                }
            }
    ) {
        if (!cell.isDead && color != null) {
            // Pipes
            val thickDp = thickness
            val halfCell = cellSize / 2

            // Up pipe
            if (dirs.up) {
                PipeSegment(color = color, width = thickDp, height = halfCell + ext,
                    isHorizontal = false, modifier = Modifier.align(Alignment.TopCenter).offset(y = -ext))
            }
            // Down pipe
            if (dirs.down) {
                PipeSegment(color = color, width = thickDp, height = halfCell + ext,
                    isHorizontal = false, modifier = Modifier.align(Alignment.BottomCenter).offset(y = ext))
            }
            // Left pipe
            if (dirs.left) {
                PipeSegment(color = color, width = halfCell + ext, height = thickDp,
                    isHorizontal = true, modifier = Modifier.align(Alignment.CenterStart).offset(x = -ext))
            }
            // Right pipe
            if (dirs.right) {
                PipeSegment(color = color, width = halfCell + ext, height = thickDp,
                    isHorizontal = true, modifier = Modifier.align(Alignment.CenterEnd).offset(x = ext))
            }

            // Center junction node (non-endpoint flow cells)
            if (!cell.isEndpoint) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(thickDp * 1.2f)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(color.copy(0.8f), color.copy(0.3f)))
                        )
                )
            }

            // Endpoint synapse node
            if (cell.isEndpoint) {
                SynapseNode(color = color, cellSize = cellSize)
            }
        }
    }
}

@Composable
private fun PipeSegment(
    color: Color,
    width: Dp,
    height: Dp,
    isHorizontal: Boolean = false,
    modifier: Modifier = Modifier
) {
    val brush = if (isHorizontal)
        Brush.verticalGradient(listOf(color.copy(0.7f), color.copy(0.9f), color.copy(0.7f)))
    else
        Brush.horizontalGradient(listOf(color.copy(0.7f), color.copy(0.9f), color.copy(0.7f)))

    Box(modifier = modifier.size(width, height)) {
        // Subtle glow halo (no blur needed)
        Box(
            modifier = Modifier
                .size(width + 4.dp, height + 4.dp)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.18f))
        )
        // Core pipe
        Box(
            modifier = Modifier
                .size(width, height)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(3.dp))
                .background(brush)
        )
    }
}

@Composable
private fun SynapseNode(color: Color, cellSize: Dp) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(cellSize * 0.05f),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow
        Box(
            modifier = Modifier
                .size(cellSize * 0.95f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(color.copy(0.6f), color.copy(0.2f), color.copy(0f))
                    )
                )
        )
        // Neon ring
        Box(
            modifier = Modifier
                .size(cellSize * 0.65f)
                .clip(CircleShape)
                .border(2.dp, color.copy(0.5f), CircleShape)
        )
        // Core
        Box(
            modifier = Modifier
                .size(cellSize * 0.5f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color.White.copy(0.9f), color, color.copy(0.8f))
                    )
                )
                .border(1.5.dp, Color.White.copy(0.6f), CircleShape)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Interpolation helper (mirrors iOS interpolateCells)
// ─────────────────────────────────────────────────────────────────────────────

private fun interpolate(from: Pair<Int,Int>, to: Pair<Int,Int>, n: Int): List<Pair<Int,Int>> {
    val result = mutableListOf<Pair<Int,Int>>()
    var r = from.first; var c = from.second
    while (r != to.first || c != to.second) {
        if (r < to.first) r++ else if (r > to.first) r--
        else if (c < to.first) c++ else if (c > to.second) c--
        result.add((r.coerceIn(0, n-1)) to (c.coerceIn(0, n-1)))
    }
    return result
}
