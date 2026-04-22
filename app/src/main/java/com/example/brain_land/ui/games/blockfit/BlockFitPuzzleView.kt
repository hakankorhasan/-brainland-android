package com.example.brain_land.ui.games.blockfit

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.brain_land.ui.games.tiltmaze.GameResultSheet
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// Colours
// ─────────────────────────────────────────────────────────────────────────────

private val bgDeep    = Color(0xFF0D1017)
private val bgCard    = Color(0xFF121628)
private val bgCell    = Color(0xFF1A1E2E)
private val levelPill = Color(0xFF6D28D9)
private val accentCyn = Color(0xFF00E5FF)
private val accentTl  = Color(0xFF14B8A6)
private val gold1     = Color(0xFFF1C40F)
private val gold2     = Color(0xFFE67E22)

// ─────────────────────────────────────────────────────────────────────────────
// Entry Point
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BlockFitPuzzleView(onHome: () -> Unit, onNavigateToGame: (Any) -> Unit = {}) {
    val scope    = rememberCoroutineScope()
    val context  = LocalContext.current
    val prefs    = remember { context.getSharedPreferences("block_fit_prefs", android.content.Context.MODE_PRIVATE) }
    val startLv  = remember { prefs.getInt("currentLevel", 1) }
    val gs       = remember { BFGameState(initialLevel = startLv, scope = scope) }

    var showWin      by remember { mutableStateOf(false) }
    var showGameOver by remember { mutableStateOf(false) }
    var showInfo     by remember { mutableStateOf(false) }
    var finalElapsed by remember { mutableIntStateOf(0) }
    var timerSecs    by remember { mutableIntStateOf(0) }

    LaunchedEffect(gs.levelNumber) {
        timerSecs = 0
        while (!gs.isWon && !gs.isGameOver) { delay(1000); timerSecs++ }
    }
    LaunchedEffect(gs.isWon)     { if (gs.isWon)     { finalElapsed = timerSecs; showWin = true } }
    LaunchedEffect(gs.isGameOver){ if (gs.isGameOver) showGameOver = true }

    Box(modifier = Modifier.fillMaxSize().background(bgDeep).systemBarsPadding()) {
        if (gs.isLoading) {
            BFLoadingView(onHome = onHome)
        } else {
            BFMainScreen(gs = gs, timerSecs = timerSecs, onHome = onHome, onShowInfo = { showInfo = true })
        }

        GameResultSheet(
            visible          = showWin,
            gameId           = "blockFit",
            level            = gs.levelNumber,
            elapsed          = finalElapsed,
            difficulty       = gs.difficultyValue,
            gridSize         = BFGameState.GRID_SIZE,
            onNextPuzzle     = { showWin = false; prefs.edit().putInt("currentLevel", gs.levelNumber).apply(); gs.nextLevel() },
            onPlayAgain      = { showWin = false; gs.resetPuzzle() },
            onBackToGames    = { showWin = false; onHome() },
            onNavigateToGame = { g -> onNavigateToGame(g) }
        )

        AnimatedVisibility(
            visible = showGameOver,
            enter   = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit    = scaleOut() + fadeOut()
        ) {
            BFGameOverOverlay(
                score = gs.score, targetScore = gs.targetScore,
                onRetry = { showGameOver = false; gs.resetPuzzle() },
                onHome  = { showGameOver = false; onHome() }
            )
        }
    }

    if (showInfo) BFInfoSheet(onClose = { showInfo = false })
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BFLoadingView(onHome: () -> Unit) {
    val inf   = rememberInfiniteTransition(label = "p")
    val alpha by inf.animateFloat(0.3f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("🧩", fontSize = 48.sp, modifier = Modifier.graphicsLayer(alpha = alpha))
            Text("Loading Block Fit…", fontSize = 15.sp, color = Color.White.copy(0.5f), fontWeight = FontWeight.Medium)
        }
        IconButton(onClick = onHome, modifier = Modifier.align(Alignment.TopStart)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White.copy(0.7f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Screen
// Shared grid-origin state so DraggableBlock can hit-test against the grid.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BFMainScreen(
    gs: BFGameState,
    timerSecs: Int,
    onHome: () -> Unit,
    onShowInfo: () -> Unit
) {
    val density = LocalDensity.current

    // Shared between BFGridArea (writes) and DraggableBlock (reads)
    var gridOriginPx by remember { mutableStateOf(Offset.Zero) }
    var cellPx       by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 4.dp, end = 16.dp)) {
            IconButton(onClick = onHome, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White.copy(0.8f))
            }
            Text("Block Fit", fontSize = 17.sp, fontWeight = FontWeight.Bold,
                color = Color.White, modifier = Modifier.align(Alignment.Center))
            IconButton(onClick = onShowInfo, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Default.Info, null, tint = Color.White.copy(0.5f))
            }
        }

        // ── Level pill ───────────────────────────────────────────────────────
        Box(modifier = Modifier
            .background(levelPill.copy(0.85f), RoundedCornerShape(50))
            .padding(horizontal = 18.dp, vertical = 5.dp)
        ) {
            Text("LEVEL ${gs.levelNumber}", fontSize = 12.sp,
                fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
        }

        Spacer(Modifier.height(10.dp))

        // ── Time stat ────────────────────────────────────────────────────────
        val timeStr = "%d:%02d".format(timerSecs / 60, timerSecs % 60)
        Row(
            modifier = Modifier
                .fillMaxWidth().padding(horizontal = 20.dp)
                .background(Color.White.copy(0.06f), RoundedCornerShape(14.dp))
                .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(14.dp))
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TIME", fontSize = 10.sp, color = Color.White.copy(0.4f),
                    letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Default.Timer, null, tint = accentCyn, modifier = Modifier.size(16.dp))
                    Text(timeStr, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Board card ───────────────────────────────────────────────────────
        // iOS approach: cellSize = floor(availableWidth / 9), no gap in math.
        // Gap is purely visual (cell drawn slightly smaller).
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            val cardPadding = 10.dp
            val gridPadding = 8.dp   // iOS: .padding(8)
            val availWidth  = maxWidth - cardPadding * 2 - gridPadding * 2
            val n           = BFGameState.GRID_SIZE
            // iOS: floor(maxGridDim / 9)
            val cellSize    = availWidth / n.toFloat()

            SideEffect {
                cellPx = with(density) { cellSize.toPx() }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgCard, RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(16.dp))
                    .padding(cardPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BFScoreBanner(score = gs.score, targetScore = gs.targetScore)

                BFGridArea(
                    gs       = gs,
                    cellSize = cellSize,
                    gridPadding = gridPadding,
                    onOriginChanged = { origin -> gridOriginPx = origin }
                )

                BFBlockTray(
                    gs            = gs,
                    cellSize      = cellSize,
                    gridOriginPx  = gridOriginPx,
                    cellSizePx    = cellPx
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── Restart ──────────────────────────────────────────────────────────
        OutlinedButton(
            onClick = { gs.resetPuzzle() },
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.5.dp, accentTl.copy(0.7f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentTl),
            modifier = Modifier.height(44.dp)
        ) {
            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text("Restart", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(28.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Score Banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BFScoreBanner(score: Int, targetScore: Int) {
    val progress = (score.toFloat() / maxOf(1, targetScore)).coerceIn(0f, 1f)
    val animProg by animateFloatAsState(
        targetValue  = progress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "prog"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgCard.copy(0.9f), RoundedCornerShape(10.dp))
            .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("SCORE", fontSize = 9.sp, fontWeight = FontWeight.Bold,
                color = Color.White.copy(0.4f), letterSpacing = 1.2.sp)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("$score", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text("/ $targetScore", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(0.35f))
            }
        }
        Box(
            modifier = Modifier
                .weight(1f).height(8.dp)
                .background(bgCell, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animProg)
                    .background(Brush.horizontalGradient(listOf(gold1, gold2)), RoundedCornerShape(4.dp))
            )
        }
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
            if (progress >= 1f) Box(Modifier.size(28.dp).background(Color.Yellow.copy(0.2f), CircleShape))
            Icon(
                if (progress >= 1f) Icons.Default.Star else Icons.Default.StarBorder,
                null,
                tint     = if (progress >= 1f) Color.Yellow else Color.White.copy(0.3f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Grid Area
// Mirrors iOS exactly:
//   • gridOrigin = top-left of the grid area (AFTER outer padding, BEFORE
//     inner gridPadding). This is where iOS's GeometryReader reports from.
//   • Cell position:  x = col * cellSize,  y = row * cellSize  (no gap in math)
//   • Cell visual size = cellSize - gap  (gap is only cosmetic)
//   • hitTest:  col = Int((gx - gridOrigin.x + cellSize/2) / cellSize)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BFGridArea(
    gs: BFGameState,
    cellSize: Dp,
    gridPadding: Dp,
    onOriginChanged: (Offset) -> Unit
) {
    val n = BFGameState.GRID_SIZE
    val gridTotalSize = cellSize * n
    val gap = 1.5.dp   // visual only

    Box(
        modifier = Modifier
            .size(gridTotalSize + gridPadding * 2)
            .background(Color(0xFF0A0D18), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(12.dp))
            // Report origin AFTER padding — this is the cell-area origin
            .padding(gridPadding)
            .onGloballyPositioned { coords ->
                onOriginChanged(coords.positionInWindow())
            }
    ) {
        for (row in 0 until n) {
            for (col in 0 until n) {
                val key  = "$row,$col"
                val isHL = gs.highlightCells.contains(key)
                val isPre = gs.previewCells.any { it.first == row && it.second == col }
                val color = gs.grid[row][col]
                val cellFrame = cellSize - gap

                Box(
                    modifier = Modifier
                        // iOS: .position(x: col*cellSize + cellSize/2, y: row*cellSize + cellSize/2)
                        // Compose .offset is top-left based, so just col*cellSize
                        .offset(x = cellSize * col, y = cellSize * row)
                        .size(cellFrame)
                        .clip(RoundedCornerShape(4.dp))
                        .background(bgCell)
                        .border(0.5.dp, Color.Black.copy(0.25f), RoundedCornerShape(4.dp))
                ) {
                    when {
                        color != null -> BlockCellCompose(color = color, isHighlighted = isHL, modifier = Modifier.fillMaxSize())
                        isPre && gs.previewValid -> Box(
                            Modifier.fillMaxSize()
                                .background((gs.availableBlocks.filterNotNull().firstOrNull()?.color ?: Color.White).copy(0.28f), RoundedCornerShape(3.dp))
                                .border(1.dp, (gs.availableBlocks.filterNotNull().firstOrNull()?.color ?: Color.White).copy(0.55f), RoundedCornerShape(3.dp))
                        )
                        isPre -> Box(
                            Modifier.fillMaxSize()
                                .background(Color.Red.copy(0.25f), RoundedCornerShape(3.dp))
                                .border(1.dp, Color.Red.copy(0.5f), RoundedCornerShape(3.dp))
                        )
                    }
                    if (isHL) Box(Modifier.fillMaxSize().background(Color.White.copy(0.65f), RoundedCornerShape(3.dp)))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Block Tray — mirrors iOS blockTray(cellSize:)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BFBlockTray(
    gs: BFGameState,
    cellSize: Dp,
    gridOriginPx: Offset,
    cellSizePx: Float
) {
    // iOS: traySlotSize = cellSize * 2.2
    val slotSize = cellSize * 2.2f
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        for (i in 0..2) {
            val block = gs.availableBlocks.getOrNull(i)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(slotSize)
                    .background(Color(0xFF121628), RoundedCornerShape(10.dp))
                    .border(1.dp, Color.White.copy(0.04f), RoundedCornerShape(10.dp))
            ) {
                if (block != null) {
                    DraggableBlock(
                        block        = block,
                        blockIndex   = i,
                        gs           = gs,
                        cellSize     = cellSize,
                        gridOriginPx = gridOriginPx,
                        cellSizePx   = cellSizePx
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DraggableBlock — pixel-perfect port of iOS DraggableBlockView
//
// iOS source (lines 371–457 of BlockFitPuzzleView.swift):
//   • miniCellSize = maxCellSize * 0.42
//   • dragScale    = maxCellSize / miniCellSize
//   • liftY        = 100
//   • DragGesture(coordinateSpace: .global)
//     - onChanged: dragOffset = value.translation
//                  dropPoint  = (value.location.x, value.location.y - liftY)
//     - gridPosition(from:):
//         col = Int((globalPoint.x - gridOrigin.x + maxCellSize/2) / maxCellSize)
//         row = Int((globalPoint.y - gridOrigin.y + maxCellSize/2) / maxCellSize)
//
// Android equivalent:
//   • pointerInput on the OUTER slot-sized Box (same touch area as iOS)
//   • awaitEachGesture → awaitFirstDown (no slop) + while awaitPointerEvent
//   • globalFinger = slotOriginPx + startLocal + cumDelta
//   • Visual: translation = cumDelta; translationY -= liftPx
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DraggableBlock(
    block: BlockShape,
    blockIndex: Int,
    gs: BFGameState,
    cellSize: Dp,            // iOS maxCellSize
    gridOriginPx: Offset,
    cellSizePx: Float
) {
    val density = LocalDensity.current
    // iOS: liftY = 100
    val liftPx      = with(density) { 100.dp.toPx() }
    // iOS: miniCellSize = maxCellSize * 0.42
    val miniCell    = cellSize * 0.42f
    val miniPx      = with(density) { miniCell.toPx() }
    val fullPx      = with(density) { cellSize.toPx() }
    // iOS: dragScale = maxCellSize / miniCellSize
    val dragScale   = if (miniPx > 0f) fullPx / miniPx else 1f

    var slotOriginPx  by remember { mutableStateOf(Offset.Zero) }
    var isDragging     by remember { mutableStateOf(false) }
    var startLocalPos  by remember { mutableStateOf(Offset.Zero) }
    var cumDx          by remember { mutableFloatStateOf(0f) }
    var cumDy          by remember { mutableFloatStateOf(0f) }

    val w = miniCell * block.width
    val h = miniCell * block.height

    // iOS: let dropPoint = CGPoint(x: value.location.x, y: value.location.y - liftY)
    // globalFinger = slotOriginPx + startLocal + cumDelta
    fun dropPoint(): Offset {
        val gx = slotOriginPx.x + startLocalPos.x + cumDx
        val gy = slotOriginPx.y + startLocalPos.y + cumDy - liftPx
        return Offset(gx, gy)
    }

    // iOS gridPosition(from:) — exact formula
    fun gridPosition(drop: Offset): Pair<Int, Int>? {
        val x = drop.x - gridOriginPx.x + cellSizePx / 2f
        val y = drop.y - gridOriginPx.y + cellSizePx / 2f
        val col = (x / cellSizePx).toInt()
        val row = (y / cellSizePx).toInt()
        return if (row in 0 until 9 && col in 0 until 9) row to col else null
    }

    // Outer Box = full slot size = touch area
    // iOS: .frame(width: maxCellSize * 2.2, height: maxCellSize * 2.2)
    // detectDragGestures uses internal drag() which tracks pointer ID globally
    // — events arrive even when finger is far outside composable bounds.
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()    // fills slot
            .onGloballyPositioned { slotOriginPx = it.positionInWindow() }
            .pointerInput(block.id) {
                detectDragGestures(
                    onDragStart = { localOffset ->
                        startLocalPos = localOffset
                        isDragging    = true
                        cumDx         = 0f
                        cumDy         = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        cumDx += dragAmount.x
                        cumDy += dragAmount.y
                        gridPosition(dropPoint())?.let { (r, c) ->
                            gs.updatePreview(block, r, c)
                        } ?: gs.clearPreview()
                    },
                    onDragEnd = {
                        gridPosition(dropPoint())?.let { (r, c) ->
                            if (gs.canPlace(block, r, c))
                                gs.placeBlock(block, r, c, blockIndex)
                        }
                        gs.clearPreview()
                        isDragging = false
                        cumDx      = 0f
                        cumDy      = 0f
                    },
                    onDragCancel = {
                        gs.clearPreview()
                        isDragging = false
                        cumDx      = 0f
                        cumDy      = 0f
                    }
                )
            }
    ) {
        // Inner Box = mini block shape — lifted+scaled during drag (iOS style)
        Box(
            contentAlignment = Alignment.TopStart,
            modifier = Modifier
                .size(w, h)
                .graphicsLayer {
                    if (isDragging) {
                        // iOS: scaleEffect(isDragging ? dragScale * 1.02 : 1.0)
                        scaleX       = dragScale * 1.02f
                        scaleY       = dragScale * 1.02f
                        // iOS: offset(x: dragOffset.width, y: dragOffset.height - liftY)
                        translationX = cumDx
                        translationY = cumDy - liftPx
                        shadowElevation = 20f
                        alpha        = 0.95f
                    }
                }
        ) {
            block.cells.forEach { (r, c) ->
                BlockCellCompose(
                    color         = block.color,
                    isHighlighted = false,
                    modifier      = Modifier
                        .offset(miniCell * c, miniCell * r)
                        .size(miniCell - 1.5.dp)   // iOS: miniCellSize - 1.5
                )
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// Block Cell — 4-layer Block Blast raised tile
// ─────────────────────────────────────────────────────────────────────────────


@Composable
fun BlockCellCompose(
    color: Color,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 5.dp
) {
    val shadow    = lerp(color, Color.Black, 0.5f)
    val border    = lerp(color, Color.Black, 0.3f)
    val highlight = lerp(color, Color.White, 0.45f)

    Box(modifier = modifier.clip(RoundedCornerShape(cornerRadius))) {
        Box(Modifier.fillMaxSize().background(shadow, RoundedCornerShape(cornerRadius)))
        Box(Modifier.fillMaxSize().padding(1.dp).background(border, RoundedCornerShape((cornerRadius.value - 0.5f).dp)))
        Box(
            Modifier.fillMaxSize()
                .padding(top = 2.dp, start = 2.dp, bottom = 5.dp, end = 5.dp)
                .background(
                    Brush.linearGradient(listOf(highlight, color, color),
                        start = Offset(0f, 0f), end = Offset(80f, 80f)),
                    RoundedCornerShape((cornerRadius.value - 1f).dp)
                )
        )
        Box(
            Modifier.fillMaxSize()
                .padding(top = 3.dp, start = 3.dp, bottom = 7.dp, end = 7.dp)
                .background(
                    Brush.linearGradient(listOf(Color.White.copy(0.35f), Color.White.copy(0.04f), Color.Transparent),
                        start = Offset(0f, 0f), end = Offset(40f, 40f)),
                    RoundedCornerShape((cornerRadius.value - 2f).dp)
                )
        )
        if (isHighlighted) Box(Modifier.fillMaxSize().background(Color.White.copy(0.65f), RoundedCornerShape(cornerRadius)))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Game Over Overlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BFGameOverOverlay(score: Int, targetScore: Int, onRetry: () -> Unit, onHome: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.6f)), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .background(Color(0xFF1D1B29), RoundedCornerShape(28.dp))
                .border(1.dp, Color.Red.copy(0.2f), RoundedCornerShape(28.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Default.Cancel, null, tint = Color(0xFFEF4444), modifier = Modifier.size(56.dp))
            Text("Game Over", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("$score / $targetScore", fontSize = 16.sp, color = Color.White.copy(0.7f))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedButton(onClick = onRetry, shape = CircleShape,
                    border = BorderStroke(1.5.dp, Color(0xFF87D2C8).copy(0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF87D2C8))) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp)); Text("Retry")
                }
                OutlinedButton(onClick = onHome, shape = CircleShape,
                    border = BorderStroke(1.5.dp, Color(0xFFAFABE5).copy(0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFAFABE5))) {
                    Icon(Icons.Default.Home, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp)); Text("Home")
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Info Sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BFInfoSheet(onClose: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor   = Color(0xFF10131B),
        dragHandle       = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("🧩", fontSize = 40.sp)
            Text("How to Play", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            listOf(
                "Drag blocks from the tray onto the 9×9 grid",
                "Complete a full row or column to clear it and score bonus points",
                "Cleared blocks fall downward (gravity)",
                "Reach the target score to win the level"
            ).forEachIndexed { i, text ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(0.03f), RoundedCornerShape(14.dp))
                        .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(contentAlignment = Alignment.Center,
                        modifier = Modifier.size(34.dp).background(levelPill.copy(0.15f), RoundedCornerShape(10.dp))) {
                        Text("${i + 1}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = gold1)
                    }
                    Text(text, fontSize = 13.sp, color = Color.White.copy(0.75f))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
