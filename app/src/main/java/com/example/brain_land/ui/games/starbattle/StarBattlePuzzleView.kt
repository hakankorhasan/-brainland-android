package com.example.brain_land.ui.games.starbattle

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.example.brain_land.data.GameType
import com.example.brain_land.ui.games.tiltmaze.GameResultSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Colours
// ─────────────────────────────────────────────────────────────────────────────

private val bgDeep     = Color(0xFF0D1017)
private val bgCard     = Color(0xFF090C12)
private val levelPill  = Color(0xFF6D28D9)
private val accentCyan = Color(0xFF00E5FF)
private val accentPurp = Color(0xFFA78BFA)
private val accentGold = Color(0xFFFFD700)
private val accentTeal = Color(0xFF14B8A6)
private val greenOk    = Color(0xFF34D399)
private val redErr     = Color(0xFFF87171)

// ─────────────────────────────────────────────────────────────────────────────
// Entry Point
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StarBattlePuzzleView(onHome: () -> Unit, onNavigateToGame: (Any) -> Unit = {}) {
    val scope   = rememberCoroutineScope()
    var level   by remember { mutableStateOf<SBLevel?>(null) }
    var loading by remember { mutableStateOf(true) }

    // Load level 1 on first appearance (mirrors iOS onAppear GameProgressManager)
    val context = LocalContext.current
    val prefs   = remember { context.getSharedPreferences("star_battle_prefs", android.content.Context.MODE_PRIVATE) }
    val startLevel = remember { prefs.getInt("currentLevel", 1) }

    LaunchedEffect(Unit) {
        loading = true
        level   = SBRepository.fetchLevel(startLevel)
        loading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgDeep)
            .systemBarsPadding()
    ) {
        if (loading || level == null) {
            SBLoadingView(onHome = onHome)
        } else {
            SBGameView(
                level            = level!!,
                onHome           = onHome,
                onNavigateToGame = onNavigateToGame,
                onNextLevel      = { num ->
                    scope.launch {
                        loading = true
                        val next = SBRepository.fetchLevel(num)
                        if (next != null) {
                            level   = next
                            prefs.edit().putInt("currentLevel", num).apply()
                        } else {
                            onHome()
                        }
                        loading = false
                    }
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SBLoadingView(onHome: () -> Unit) {
    val inf  = rememberInfiniteTransition(label = "pulse")
    val alpha by inf.animateFloat(0.3f, 1f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("⭐", fontSize = 48.sp, modifier = Modifier.graphicsLayer(alpha = alpha))
            Text("Loading Star Battle…", fontSize = 15.sp,
                color = Color.White.copy(0.5f), fontWeight = FontWeight.Medium)
        }
        IconButton(onClick = onHome, modifier = Modifier.align(Alignment.TopStart)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White.copy(0.7f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Game View — mirrors iOS GalacticBeaconsGameView
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SBGameView(
    level: SBLevel,
    onHome: () -> Unit,
    onNavigateToGame: (Any) -> Unit,
    onNextLevel: (Int) -> Unit
) {
    val gs          = remember(level.levelNumber) { SBGameState(level) }
    var showWin     by remember { mutableStateOf(false) }
    var showInfo    by remember { mutableStateOf(false) }
    var timerSecs   by remember { mutableIntStateOf(0) }
    var finalElapsed by remember { mutableIntStateOf(0) }

    // Win trigger
    LaunchedEffect(gs.isSolved) {
        if (gs.isSolved) { finalElapsed = timerSecs; showWin = true }
    }

    // Timer
    LaunchedEffect(level.levelNumber) {
        timerSecs = 0
        while (!gs.isSolved) { delay(1000); timerSecs++ }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 4.dp, end = 16.dp)) {
            IconButton(onClick = onHome, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White.copy(0.8f))
            }
            Text("Star Battle", fontSize = 17.sp, fontWeight = FontWeight.Bold,
                color = Color.White, modifier = Modifier.align(Alignment.Center))
            IconButton(onClick = { showInfo = true }, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Default.Info, null, tint = Color.White.copy(0.5f))
            }
        }

        // ── Level pill ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .background(levelPill.copy(0.85f), RoundedCornerShape(50))
                .padding(horizontal = 18.dp, vertical = 5.dp)
        ) {
            Text("LEVEL ${level.levelNumber}", fontSize = 12.sp,
                fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
        }

        Spacer(Modifier.height(10.dp))

        // ── Stats card ───────────────────────────────────────────────────────
        val timeStr = "%d:%02d".format(timerSecs / 60, timerSecs % 60)
        Row(
            modifier = Modifier
                .fillMaxWidth().padding(horizontal = 20.dp)
                .background(Color.White.copy(0.06f), RoundedCornerShape(14.dp))
                .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(14.dp))
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TIME", fontSize = 10.sp, color = Color.White.copy(0.4f),
                    letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Default.Timer, null, tint = accentCyan, modifier = Modifier.size(16.dp))
                    Text(timeStr, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Box(Modifier.width(1.dp).height(36.dp).background(Color.White.copy(0.1f)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("MOVES", fontSize = 10.sp, color = Color.White.copy(0.4f),
                    letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Default.TouchApp, null, tint = accentPurp, modifier = Modifier.size(16.dp))
                    Text("${gs.moveCount}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Board card ───────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth().padding(horizontal = 12.dp)
                .background(bgCard, RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(16.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SBBoard(gs = gs, level = level)
        }

        Spacer(Modifier.height(24.dp))

        // ── Restart ──────────────────────────────────────────────────────────
        OutlinedButton(
            onClick = { gs.resetPuzzle() },
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.5.dp, accentTeal.copy(0.7f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentTeal),
            modifier = Modifier.height(44.dp)
        ) {
            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text("Restart", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(32.dp))
    }

    // ── Game Result Sheet ─────────────────────────────────────────────────────
    GameResultSheet(
        visible          = showWin,
        gameId           = "galacticBeacons",
        level            = level.levelNumber,
        elapsed          = finalElapsed,
        difficulty       = level.difficultyValue,
        gridSize         = level.gridSize,
        onNextPuzzle     = { showWin = false; onNextLevel(level.levelNumber + 1) },
        onPlayAgain      = { showWin = false; gs.resetPuzzle() },
        onBackToGames    = { showWin = false; onHome() },
        onNavigateToGame = { g -> onNavigateToGame(g) }
    )

    // ── Info sheet ────────────────────────────────────────────────────────────
    if (showInfo) {
        SBInfoSheet(beaconsPerUnit = level.beaconsPerUnit, onClose = { showInfo = false })
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Board — column counters + grid + row counters + region legend + error banner
// mirrors iOS boardContent
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SBBoard(gs: SBGameState, level: SBLevel) {
    val n = level.gridSize
    val b = level.beaconsPerUnit

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val counterW = 28.dp
        val spacing  = 1.dp
        val spacingPx = with(LocalDensity.current) { spacing.toPx() }
        val total = maxWidth - counterW - 8.dp - spacing * (n - 1)
        val cellSz = (total / n).coerceAtMost(64.dp)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Grid + counters row
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Left part: column counters + grid
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Column counters — top
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        for (c in 0 until n) {
                            val cnt = (0 until n).count { gs.cells[it][c].state == GBCellState.STAR }
                            val isFull = cnt == b; val isOver = cnt > b
                            Text(
                                "$cnt/$b",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = when { isOver -> redErr; isFull -> greenOk; else -> Color.White.copy(0.35f) },
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(cellSz)
                            )
                        }
                    }
                    // Grid
                    Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                        for (r in 0 until n) {
                            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                for (c in 0 until n) {
                                    SBCellView(
                                        cell     = gs.cells[r][c],
                                        cellSize = cellSz,
                                        gridCells = gs.cells,
                                        colorIdx = level.regionColors.getOrElse(gs.cells[r][c].regionId) { 0 },
                                        onTap    = { gs.tapCell(r, c) }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.width(4.dp))

                // Row counters — right side
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    // Spacer matching column counter height
                    Spacer(Modifier.height(18.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                        for (r in 0 until n) {
                            val cnt = (0 until n).count { gs.cells[r][it].state == GBCellState.STAR }
                            val isFull = cnt == b; val isOver = cnt > b
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(counterW, cellSz)
                            ) {
                                Text(
                                    "$cnt/$b",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = when { isOver -> redErr; isFull -> greenOk; else -> Color.White.copy(0.35f) }
                                )
                            }
                        }
                    }
                }
            }

            // Region legend
            SBRegionLegend(gs = gs, level = level, b = b)

            // Error banner
            gs.errorMessage?.let { msg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Red.copy(0.08f), RoundedCornerShape(10.dp))
                        .border(1.dp, Color.Red.copy(0.2f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Warning, null, tint = redErr, modifier = Modifier.size(14.dp))
                    Text(msg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = redErr)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Region legend pills
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SBRegionLegend(gs: SBGameState, level: SBLevel, b: Int) {
    val n = level.gridSize
    // Count stars per region
    val regionStars = mutableMapOf<Int, Int>()
    for (r in 0 until n) for (c in 0 until n)
        if (gs.cells[r][c].state == GBCellState.STAR)
            regionStars[gs.cells[r][c].regionId] = (regionStars[gs.cells[r][c].regionId] ?: 0) + 1

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (rid in 0 until n) {
            val colorIdx = level.regionColors.getOrElse(rid) { 0 }
            val nc       = nebulaColorPairs[colorIdx % nebulaColorPairs.size]
            val bg       = Color(nc.bg)
            val acc      = Color(nc.accent)
            val cnt      = regionStars[rid] ?: 0
            val isFull   = cnt == b; val isOver = cnt > b

            Row(
                modifier = Modifier
                    .background(bg.copy(0.3f), CircleShape)
                    .border(0.5.dp, acc.copy(0.3f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(bg, CircleShape)
                        .border(1.dp, acc.copy(0.6f), CircleShape)
                )
                Text(
                    "$cnt/$b",
                    fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                    color = when { isOver -> redErr; isFull -> greenOk; else -> Color.White.copy(0.4f) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Cell View — mirrors iOS GalacticBeaconsCellView
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SBCellView(
    cell: GBCell,
    cellSize: Dp,
    gridCells: List<List<GBCell>>,
    colorIdx: Int,
    onTap: () -> Unit
) {
    val nc  = nebulaColorPairs[colorIdx % nebulaColorPairs.size]
    val bg  = Color(nc.bg)
    val acc = Color(nc.accent)
    val n   = gridCells.size

    // Compute region borders (thick white lines)
    val bTop    = cell.row > 0     && gridCells[cell.row-1][cell.col].regionId != cell.regionId
    val bBottom = cell.row < n-1   && gridCells[cell.row+1][cell.col].regionId != cell.regionId
    val bLeft   = cell.col > 0     && gridCells[cell.row][cell.col-1].regionId != cell.regionId
    val bRight  = cell.col < n-1   && gridCells[cell.row][cell.col+1].regionId != cell.regionId

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(cellSize)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF080B14))
            .background(bg.copy(0.45f))
            .border(
                if (cell.isError) 2.dp else 0.5.dp,
                if (cell.isError) redErr.copy(0.9f) else Color.White.copy(0.08f),
                RoundedCornerShape(3.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap
            )
    ) {
        // Star icon
        if (cell.state == GBCellState.STAR) {
            // Glow ring
            Box(
                modifier = Modifier
                    .size(cellSize * 0.55f)
                    .background(accentGold.copy(0.15f), CircleShape)
                    .border(1.5.dp, accentGold.copy(0.5f), CircleShape)
            )
            Text(
                "⭐",
                fontSize = (cellSize.value * 0.38f).sp,
                modifier = Modifier.graphicsLayer {
                    shadowElevation = 12f
                }
            )
        }

        // Region border lines (drawn on top)
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width; val h = size.height
            val thick = 2.5f
            val borderClr = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
            if (bTop)    drawRect(borderClr, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(w, thick))
            if (bBottom) drawRect(borderClr, topLeft = Offset(0f, h - thick), size = androidx.compose.ui.geometry.Size(w, thick))
            if (bLeft)   drawRect(borderClr, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(thick, h))
            if (bRight)  drawRect(borderClr, topLeft = Offset(w - thick, 0f), size = androidx.compose.ui.geometry.Size(thick, h))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Info Sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SBInfoSheet(beaconsPerUnit: Int, onClose: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = Color(0xFF10131B),
        dragHandle = { BottomSheetDefaults.DragHandle() }
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
            // Hero
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier.size(72.dp)
                        .background(
                            Brush.radialGradient(listOf(levelPill.copy(0.2f), Color.Transparent)),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier.size(60.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF1A0E30), Color(0xFF0D0B2E))),
                            CircleShape
                        )
                        .border(1.5.dp,
                            Brush.linearGradient(listOf(levelPill.copy(0.5f), accentGold.copy(0.3f))),
                            CircleShape)
                )
                Text("⭐", fontSize = 26.sp)
            }

            Text("How to Play", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)

            listOf(
                "Place $beaconsPerUnit star(s) ⭐ in every row, column, and colored region",
                "No two stars may touch — not even diagonally",
                "Tap a cell to place a star. Tap again to remove it",
                "Color-coded regions help you track which areas are complete"
            ).forEachIndexed { i, text ->
                SBInfoRow(step = i + 1, text = text)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SBInfoRow(step: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(0.03f), RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(36.dp)
                .background(
                    Brush.linearGradient(listOf(levelPill.copy(0.15f), accentGold.copy(0.06f))),
                    RoundedCornerShape(10.dp)
                )
                .border(1.dp, levelPill.copy(0.25f), RoundedCornerShape(10.dp))
        ) {
            Text("$step", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accentGold)
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("STEP $step", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                color = Color.White.copy(0.3f), letterSpacing = 0.5.sp)
            Text(text, fontSize = 13.sp, color = Color.White.copy(0.75f))
        }
    }
}
