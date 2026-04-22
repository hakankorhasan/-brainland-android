package com.example.brain_land.ui.games.laserpuzzle

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.brain_land.ui.games.tiltmaze.GameResultSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Colours
// ─────────────────────────────────────────────────────────────────────────────

private val bgDeep   = Color(0xFF0D1017)
private val bgCard   = Color(0xFF10131B)
private val bgCell   = Color(0xFF181C2B)
private val cellBorder = Color.White.copy(alpha = 0.06f)
private val redLaser = Color(0xFFFF3030)
private val laserOrange = Color(0xFFFF8C00)

// ─────────────────────────────────────────────────────────────────────────────
// Entry Point
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LaserPuzzleView(onHome: () -> Unit) {
    val scope  = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs  = remember { context.getSharedPreferences("laser_prefs", android.content.Context.MODE_PRIVATE) }
    val startLv = remember { prefs.getInt("currentLevel", 1) }
    val gs  = remember { LPGameState(scope).also { it.loadLevel(startLv) } }

    var showWin      by remember { mutableStateOf(false) }
    var showGameOver by remember { mutableStateOf(false) }
    var timerSecs    by remember { mutableIntStateOf(0) }

    // Timer
    LaunchedEffect(gs.isSolved, gs.isGameOver, gs.isLoading) {
        if (!gs.isLoading && !gs.isSolved && !gs.isGameOver) {
            while (true) { delay(1000); timerSecs++ }
        }
    }

    LaunchedEffect(gs.isSolved) {
        if (gs.isSolved) {
            prefs.edit().putInt("currentLevel", gs.levelNumber + 1).apply()
            delay(300); showWin = true
        }
    }
    LaunchedEffect(gs.isGameOver) { if (gs.isGameOver) showGameOver = true }

    Box(
        modifier = Modifier.fillMaxSize().background(bgDeep).systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                IconButton(
                    onClick  = onHome,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White.copy(0.80f)
                    )
                }
                Text(
                    "Laser Puzzle",
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                    modifier   = Modifier.align(Alignment.Center)
                )
                IconButton(
                    onClick  = { /* info */ },
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

            // ── Level badge ──
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "LEVEL ${gs.levelNumber}",
                    fontSize      = 12.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = redLaser,
                    letterSpacing = 0.5.sp,
                    modifier      = Modifier
                        .background(redLaser.copy(0.10f), CircleShape)
                        .border(0.5.dp, redLaser.copy(0.20f), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Stats ──
            LPStatsBar(timerSecs = timerSecs, moves = gs.moveCount)

            Spacer(Modifier.height(8.dp))

            // ── Grid ──
            if (gs.isLoading) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = redLaser, modifier = Modifier.size(40.dp))
                }
            } else {
                LPGridArea(
                    gs      = gs,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Fire Button ──
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                FireButton(
                    enabled = !gs.isLaserFiring && !gs.isSolved && !gs.isGameOver && !gs.isLoading,
                    onClick = { gs.fireLaser() }
                )
            }

            Spacer(Modifier.height(24.dp))
        }

        // ── Game Over overlay ──
        if (showGameOver) {
            GameResultSheet(
                visible        = true,
                gameId         = "laserPuzzle",
                level          = gs.levelNumber,
                elapsed        = timerSecs,
                difficulty     = 5,
                gridSize       = gs.gridSize,
                correct        = false, // User lost, resetting their local laserPuzzle streak
                onNextPuzzle   = onHome,
                onPlayAgain    = {
                    showGameOver = false
                    timerSecs = 0
                    gs.resetPuzzle()
                },
                onBackToGames  = onHome
            )
        }

        // ── Win overlay ──
        if (showWin) {
            GameResultSheet(
                visible        = true,
                gameId         = "laserPuzzle",
                level          = gs.levelNumber,
                elapsed        = timerSecs,
                difficulty     = 5,
                gridSize       = gs.gridSize,
                onNextPuzzle   = {
                    showWin = false
                    timerSecs = 0
                    gs.nextLevel()
                },
                onPlayAgain    = {
                    showWin = false
                    timerSecs = 0
                    gs.resetPuzzle()
                },
                onBackToGames  = onHome
            )
        }
    }
}

// LPTopBar removed as it has been inlined with standard GameShell top bar layout
// ─────────────────────────────────────────────────────────────────────────────
// Stats Bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LPStatsBar(timerSecs: Int, moves: Int) {
    val mins = timerSecs / 60
    val secs = timerSecs % 60
    val timeStr = "$mins:${secs.toString().padStart(2, '0')}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(Color.White.copy(0.05f), RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp, horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Time
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text   = "TIME",
                style  = MaterialTheme.typography.labelSmall,
                color  = Color.White.copy(0.4f),
                letterSpacing = 1.1.sp
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Timer, "", tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(timeStr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // Divider
        Box(Modifier.width(1.dp).height(36.dp).background(Color.White.copy(0.1f)))

        // Moves
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("MOVES", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.4f), letterSpacing = 1.1.sp)
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.TouchApp, "", tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("$moves", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Grid Area
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LPGridArea(gs: LPGameState, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val cellSize = (maxWidth - 20.dp) / gs.gridSize

        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(cellSize * gs.gridSize + 12.dp)
                    .background(Color(0xFF080B14), RoundedCornerShape(14.dp))
                    .border(
                        1.5.dp,
                        Brush.linearGradient(listOf(Color(0xFFA299EC).copy(0.2f), Color(0xFF7E7DDC).copy(0.1f))),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(6.dp)
            ) {
                // Grid cells
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    for (row in 0 until gs.gridSize) {
                        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                            for (col in 0 until gs.gridSize) {
                                LPCellView(
                                    cell     = gs.cells[row][col],
                                    cellSize = cellSize,
                                    onTap    = {
                                        val ct = gs.cells[row][col].cellType
                                        if (ct is LaserCellType.Source) {
                                            gs.fireLaser()
                                        } else {
                                            gs.rotateMirror(row, col)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Laser beam overlay
                if (gs.laserPath.isNotEmpty()) {
                    LaserBeamCanvas(
                        segments = gs.laserPath,
                        cellSizeDp = cellSize,
                        gridSize = gs.gridSize,
                        modifier = Modifier
                            .size(cellSize * gs.gridSize)
                            .align(Alignment.TopStart)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Laser Beam Canvas — mirrors iOS LaserBeamView with 4-layer glow effect
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LaserBeamCanvas(
    segments: List<LaserSegment>,
    cellSizeDp: Dp,
    gridSize: Int,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val cellSizePx = with(density) { cellSizeDp.toPx() }

    // Animated trim progress (0 → 1)
    val trimAnim = remember { Animatable(0f) }
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue  = 0.3f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(segments) {
        trimAnim.snapTo(0f)
        val dur = (segments.size * 250 + 500).coerceAtMost(3000)
        trimAnim.animateTo(1f, animationSpec = tween(dur, easing = FastOutLinearInEasing))
    }

    val progress = trimAnim.value
    val totalLength = remember(segments) {
        segments.sumOf { seg ->
            val dx = (seg.endX - seg.startX) * 1f
            val dy = (seg.endY - seg.startY) * 1f
            Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat().toDouble()
        }.toFloat()
    }

    Canvas(modifier = modifier) {
        if (segments.isEmpty() || cellSizePx == 0f) return@Canvas

        // Build cumulative path points for trim animation
        val allLines = mutableListOf<Pair<Offset, Offset>>()
        for (seg in segments) {
            val start = Offset(seg.startX * cellSizePx, seg.startY * cellSizePx)
            val end   = Offset(seg.endX * cellSizePx, seg.endY * cellSizePx)
            allLines += start to end
        }

        // Calculate how much to draw based on progress
        val totalDist = allLines.sumOf {
            val dx = (it.second.x - it.first.x).toDouble()
            val dy = (it.second.y - it.first.y).toDouble()
            Math.sqrt(dx * dx + dy * dy)
        }.toFloat()
        var remaining = totalDist * progress

        val visibleLines = mutableListOf<Pair<Offset, Offset>>()
        for (line in allLines) {
            val dx = line.second.x - line.first.x
            val dy = line.second.y - line.first.y
            val len = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            if (remaining <= 0f) break
            if (remaining >= len) { visibleLines += line; remaining -= len }
            else {
                val t = remaining / len
                visibleLines += line.first to Offset(line.first.x + dx * t, line.first.y + dy * t)
                remaining = 0f
            }
        }

        // Draw 4 layers (outer bloom → core → white-hot)
        drawBeamLayers(visibleLines, cellSizePx, progress, pulseAlpha)
    }
}

private fun DrawScope.drawBeamLayers(
    lines: List<Pair<Offset, Offset>>,
    cellSizePx: Float,
    progress: Float,
    pulseAlpha: Float
) {
    for ((start, end) in lines) {
        // Layer 1: Outer bloom (widest, most transparent)
        drawLine(
            color = Color.Red.copy(alpha = 0.12f),
            start = start, end = end,
            strokeWidth = cellSizePx * 0.28f,
            cap = StrokeCap.Round,
            blendMode = BlendMode.Plus
        )
        // Layer 2: Mid glow
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(redLaser.copy(0.35f), laserOrange.copy(0.25f)),
                start = start, end = end
            ),
            start = start, end = end,
            strokeWidth = cellSizePx * 0.14f,
            cap = StrokeCap.Round
        )
        // Layer 3: Core beam
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(redLaser, laserOrange, redLaser),
                start = start, end = end
            ),
            start = start, end = end,
            strokeWidth = cellSizePx * 0.055f,
            cap = StrokeCap.Round
        )
        // Layer 4: White-hot center
        drawLine(
            color = Color.White.copy(alpha = 0.75f),
            start = start, end = end,
            strokeWidth = cellSizePx * 0.022f,
            cap = StrokeCap.Round
        )
        // Pulsing extra glow (after fully drawn)
        if (progress >= 1f) {
            drawLine(
                color = redLaser.copy(alpha = pulseAlpha * 0.28f),
                start = start, end = end,
                strokeWidth = cellSizePx * 0.20f,
                cap = StrokeCap.Round,
                blendMode = BlendMode.Plus
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Cell View — mirrors iOS LaserCellView
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LPCellView(
    cell: LaserCell,
    cellSize: Dp,
    onTap: () -> Unit
) {
    var pulseScale by remember { mutableFloatStateOf(1f) }
    val scope = rememberCoroutineScope()

    // Tile background
    val tileBg = when {
        cell.isHitBomb -> Color(0xFFFF9800).copy(0.15f)
        cell.isLit && cell.cellType == LaserCellType.Empty -> redLaser.copy(0.05f)
        else -> Color.White.copy(0.03f)
    }
    val tileBorder = when {
        cell.isHitBomb -> Color(0xFFFF9800).copy(0.3f)
        cell.isLit     -> redLaser.copy(0.18f)
        else           -> cellBorder
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(cellSize)
            .background(tileBg, RoundedCornerShape(cellSize * 0.12f))
            .border(0.5.dp, tileBorder, RoundedCornerShape(cellSize * 0.12f))
            .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {
                val ct = cell.cellType
                if (ct is LaserCellType.Mirror || ct is LaserCellType.Splitter) {
                    pulseScale = 0.86f
                    scope.launch {
                        delay(80)
                        pulseScale = 1f
                    }
                }
                onTap()
            }
    ) {
        when (val ct = cell.cellType) {
            is LaserCellType.Source   -> SourceCellContent(ct.direction, cellSize)
            LaserCellType.Target      -> TargetCellContent(cell.isHitTarget, cellSize)
            LaserCellType.Mirror      -> MirrorContent(cell, cellSize)
            LaserCellType.Wall        -> WallContent(cellSize)
            is LaserCellType.Portal   -> PortalContent(cell, ct.pairId, cellSize)
            LaserCellType.Bomb        -> BombContent(cell.isHitBomb, cellSize)
            LaserCellType.Splitter    -> SplitterContent(cell, cellSize)
            LaserCellType.Empty       -> {}
        }

        // Lit glow overlay
        if (cell.isLit && !cell.cellType.isSource) {
            Box(Modifier.fillMaxSize().background(redLaser.copy(0.08f), RoundedCornerShape(cellSize * 0.12f)))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Source Cell
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SourceCellContent(direction: LaserDirection, cellSize: Dp) {
    val bodySize = cellSize * 0.55f
    val arrowIcon = when (direction) {
        LaserDirection.RIGHT -> Icons.AutoMirrored.Filled.ArrowForward
        LaserDirection.LEFT  -> Icons.AutoMirrored.Filled.ArrowBack
        LaserDirection.UP    -> Icons.Filled.KeyboardArrowUp
        LaserDirection.DOWN  -> Icons.Filled.KeyboardArrowDown
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        // Radial glow
        Canvas(Modifier.size(cellSize * 0.9f)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Red.copy(0.5f), Color.Red.copy(0.07f), Color.Transparent),
                    center = center,
                    radius = size.minDimension / 2f
                )
            )
        }
        // Body
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(bodySize)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF590000), Color(0xFF991A1A), Color(0xFF590000))
                    ),
                    RoundedCornerShape(bodySize * 0.15f)
                )
                .border(
                    1.dp,
                    Brush.linearGradient(listOf(Color.White.copy(0.3f), Color.Red.copy(0.5f))),
                    RoundedCornerShape(bodySize * 0.15f)
                )
        ) {
            Icon(
                imageVector        = arrowIcon,
                contentDescription = null,
                tint               = Color.Red,
                modifier           = Modifier.size(cellSize * 0.28f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Target Cell — mirrors iOS targetView
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TargetCellContent(isHitTarget: Boolean, cellSize: Dp) {
    val pulseAnim = rememberInfiniteTransition(label = "target")
    val pulse by pulseAnim.animateFloat(
        initialValue  = 1f, targetValue = 1.25f,
        animationSpec = if (isHitTarget)
            infiniteRepeatable(tween(600), RepeatMode.Reverse)
        else
            infiniteRepeatable(tween(Int.MAX_VALUE), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        if (isHitTarget) {
            Canvas(Modifier.size(cellSize * 0.9f).graphicsLayer { scaleX = pulse; scaleY = pulse }) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.Green.copy(0.45f), Color.Green.copy(0.07f), Color.Transparent),
                        center = center, radius = size.minDimension / 2f
                    )
                )
            }
        }
        // Outer ring
        Canvas(Modifier.size(cellSize * 0.58f)) {
            drawCircle(
                color = if (isHitTarget) Color.Green else Color.White.copy(0.4f),
                style = Stroke(width = size.minDimension * 0.04f)
            )
        }
        // Middle ring
        Canvas(Modifier.size(cellSize * 0.37f)) {
            drawCircle(
                color = if (isHitTarget) Color(0xFF00DD88) else Color.White.copy(0.3f),
                style = Stroke(width = size.minDimension * 0.04f)
            )
        }
        // Center dot
        Canvas(Modifier.size(cellSize * 0.14f)) {
            drawCircle(
                color = if (isHitTarget) Color.Green else Color.White.copy(0.5f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Mirror Cell — "/" or "\" metallic bar, rotates on tap
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MirrorContent(cell: LaserCell, cellSize: Dp) {
    // Animate rotation between 45° and -45°
    val targetAngle = if (cell.mirrorAngle == 0) 45f else -45f
    val angle by animateFloatAsState(
        targetValue  = targetAngle,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label        = "mirrorAngle"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        if (cell.isLit) {
            Canvas(Modifier.size(cellSize * 0.14f, cellSize * 0.75f).graphicsLayer { rotationZ = angle }) {
                drawOval(color = redLaser.copy(0.45f), blendMode = BlendMode.Plus)
            }
        }

        // Mirror bar
        Canvas(Modifier.size(cellSize * 0.10f, cellSize * 0.68f).graphicsLayer { rotationZ = angle }) {
            // Shadow / base
            drawOval(
                brush = if (cell.isFixed)
                    Brush.linearGradient(listOf(Color(0xFF998211), Color(0xFFCCBB44), Color(0xFF998211)))
                else
                    Brush.linearGradient(listOf(Color(0xFF6699DD), Color.White, Color(0xFF6699DD)))
            )
            // Highlight shine
            drawOval(
                brush = Brush.linearGradient(listOf(Color.White.copy(0.9f), Color.Transparent)),
                topLeft = Offset(size.width * 0.08f, size.height * 0.05f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.35f, size.height * 0.5f)
            )
        }

        if (cell.isFixed) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint     = Color.Yellow.copy(0.75f),
                modifier = Modifier
                    .size(cellSize * 0.18f)
                    .align(Alignment.TopStart)
                    .offset(x = cellSize * 0.04f, y = cellSize * 0.04f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Wall Cell — dark block with shield icon
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WallContent(cellSize: Dp) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(cellSize * 0.82f)
                .background(
                    Brush.linearGradient(listOf(Color(0xFF25253A), Color(0xFF181826))),
                    RoundedCornerShape(cellSize * 0.1f)
                )
                .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(cellSize * 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Shield,
                contentDescription = null,
                tint     = Color(0xFF4CAF50).copy(0.8f),
                modifier = Modifier.size(cellSize * 0.46f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Portal Cell — spinning angular gradient rings, mirrors iOS portalView
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PortalContent(cell: LaserCell, pairId: Int, cellSize: Dp) {
    val portalColors = if (pairId == 0)
        listOf(Color(0xFFAA44DD), Color(0xFF5544BB))
    else
        listOf(Color(0xFF00CCDD), Color(0xFF009988))

    val infiniteTransition = rememberInfiniteTransition(label = "portal")
    val rotation by infiniteTransition.animateFloat(
        initialValue  = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(3600, easing = LinearEasing)),
        label         = "rot"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        // Radial glow
        Canvas(Modifier.size(cellSize * 0.9f)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(portalColors[0].copy(0.45f), portalColors[1].copy(0.07f), Color.Transparent),
                    center = center, radius = size.minDimension / 2f
                )
            )
        }

        // Outer spinning ring
        Canvas(Modifier.size(cellSize * 0.58f).graphicsLayer { rotationZ = rotation }) {
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(portalColors[0], portalColors[1], portalColors[0].copy(0.3f), portalColors[1], portalColors[0])
                ),
                style = Stroke(width = size.minDimension * 0.10f)
            )
        }

        // Inner counter-spinning ring
        Canvas(Modifier.size(cellSize * 0.35f).graphicsLayer { rotationZ = -rotation * 1.5f }) {
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(portalColors[1], portalColors[0], portalColors[1])
                ),
                style = Stroke(width = size.minDimension * 0.08f)
            )
        }

        // Center void
        Canvas(Modifier.size(cellSize * 0.16f)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black, portalColors[0].copy(0.4f)),
                    center = center, radius = size.minDimension / 2f
                )
            )
        }

        // Lit glow
        if (cell.isLit) {
            Canvas(Modifier.size(cellSize * 0.65f)) {
                drawCircle(color = portalColors[0].copy(0.35f), blendMode = BlendMode.Plus)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bomb Cell — mirrors iOS bombView with explosion effect
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BombContent(isHitBomb: Boolean, cellSize: Dp) {
    val explodeAnim = rememberInfiniteTransition(label = "bomb")
    val sparkle by explodeAnim.animateFloat(
        initialValue  = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sparkle"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        if (isHitBomb) {
            // Explosion
            Canvas(Modifier.size(cellSize * 0.8f)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color.Yellow, Color(0xFFFF9800), Color.Red, Color.Transparent),
                        center = center, radius = size.minDimension / 2f
                    )
                )
            }
            Icon(Icons.Filled.LocalFireDepartment, null, tint = Color.Yellow, modifier = Modifier.size(cellSize * 0.45f))
        } else {
            // Normal bomb body
            Canvas(Modifier.size(cellSize * 0.55f)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF4C4C59), Color(0xFF1E1E26)),
                        center = Offset(size.width * 0.35f, size.height * 0.35f),
                        radius = size.minDimension / 2f
                    )
                )
                drawCircle(color = Color(0xFFFF9800).copy(0.4f), style = Stroke(1.5f))
            }
            // Fuse
            Canvas(Modifier.size(cellSize * 0.05f, cellSize * 0.22f).offset(y = -(cellSize * 0.35f))) {
                drawOval(
                    brush = Brush.linearGradient(listOf(Color(0xFFFF9800).copy(0.8f), Color.Yellow.copy(0.5f)))
                )
            }
            // Sparkle at fuse tip
            Canvas(Modifier.size(cellSize * 0.07f).offset(y = -(cellSize * 0.45f)).graphicsLayer { alpha = sparkle }) {
                drawCircle(Color.Yellow)
            }
            // Warning icon
            Icon(
                Icons.Filled.Warning,
                null,
                tint     = Color(0xFFFF9800),
                modifier = Modifier.size(cellSize * 0.30f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Splitter Cell — diamond prism with split icon, mirrors iOS splitterView
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SplitterContent(cell: LaserCell, cellSize: Dp) {
    val angle by animateFloatAsState(
        targetValue   = if (cell.mirrorAngle == 0) 0f else 90f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label         = "splitterAngle"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        if (cell.isLit) {
            Canvas(Modifier.size(cellSize * 0.9f)) {
                drawRect(color = redLaser.copy(0.22f), blendMode = BlendMode.Plus)
            }
        }

        // Diamond prism body
        Canvas(Modifier.size(cellSize * 0.75f).graphicsLayer { rotationZ = angle }) {
            val w = size.width; val h = size.height
            val half = w * 0.28f

            val diamond = Path().apply {
                moveTo(w / 2f, h / 2f - half)
                lineTo(w / 2f + half, h / 2f)
                lineTo(w / 2f, h / 2f + half)
                lineTo(w / 2f - half, h / 2f)
                close()
            }
            drawPath(
                diamond,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF4DCC80).copy(0.85f), Color(0xFF1A8099).copy(0.9f), Color(0xFF4DCC80).copy(0.85f)),
                    start  = Offset.Zero, end = Offset(w, h)
                )
            )
            drawPath(
                diamond,
                brush  = Brush.linearGradient(listOf(Color.White.copy(0.45f), Color.Cyan.copy(0.25f))),
                style  = Stroke(width = 1.5f)
            )
        }

        // Split icon
        Icon(
            Icons.AutoMirrored.Filled.CallSplit,
            null,
            tint     = Color.White.copy(0.65f),
            modifier = Modifier.size(cellSize * 0.22f)
        )

        if (cell.isFixed) {
            Icon(
                Icons.Filled.Lock,
                null,
                tint     = Color.Yellow.copy(0.75f),
                modifier = Modifier
                    .size(cellSize * 0.18f)
                    .align(Alignment.TopStart)
                    .offset(x = cellSize * 0.04f, y = cellSize * 0.04f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Lives Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LivesRow(lives: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 5) {
            val filled = i < lives
            Icon(
                imageVector = if (filled) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = if (filled) Color.Red else Color.White.copy(0.2f),
                modifier = Modifier.size(22.dp).padding(horizontal = 2.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Fire Button — mirrors iOS "⚡ Fire" button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FireButton(enabled: Boolean, onClick: () -> Unit) {
    val fireColor = Color(0xFFE8845C) // Match iOS neon hint/fire color

    val pulseAnim = rememberInfiniteTransition(label = "fire")
    val glow by pulseAnim.animateFloat(
        initialValue  = 0.3f, targetValue = 0.6f,
        animationSpec = if (enabled)
            infiniteRepeatable(tween(900), RepeatMode.Reverse)
        else
            infiniteRepeatable(tween(Int.MAX_VALUE), RepeatMode.Reverse),
        label         = "glow"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) fireColor.copy(alpha = 0.12f) else Color.White.copy(0.05f))
            .border(
                1.dp,
                if (enabled) fireColor.copy(alpha = glow) else Color.White.copy(0.1f),
                RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(
                Icons.Filled.Bolt, 
                contentDescription = null, 
                tint = if (enabled) fireColor else Color.White.copy(0.3f), 
                modifier = Modifier.size(16.dp)
            )
            Text(
                text       = "Fire",
                color      = if (enabled) fireColor else Color.White.copy(0.3f),
                fontWeight = FontWeight.SemiBold,
                fontSize   = 13.sp
            )
        }
    }
}
