package com.example.brain_land.ui.games.nonogram

import android.content.Context
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import com.example.brain_land.data.GameType
import com.example.brain_land.ui.games.tiltmaze.GameResultSheet

// ─────────────────────────────────────────────────────────────────────────────
// Root Composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NonogramPuzzleView(
    onHome: () -> Unit,
    onNavigateToGame: (GameType) -> Unit = {}
) {
    val context = LocalContext.current
    val prefs   = remember { context.getSharedPreferences("nonogram_prefs", Context.MODE_PRIVATE) }
    val scope   = rememberCoroutineScope()
    val engine  = remember { NonogramEngine(scope, prefs.getInt("currentLevel", 1)) }

    var showWin     by remember { mutableStateOf(false) }
    var timerSecs   by remember { mutableIntStateOf(0) }
    var lastElapsed by remember { mutableIntStateOf(0) }
    var hintedCell  by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Block system back gesture — only explicit back button exits the game
    BackHandler(enabled = !showWin) { /* intentionally empty — user must tap ← to leave */ }

    LaunchedEffect(Unit) { engine.loadLevel() }

    LaunchedEffect(showWin, engine.isLoading) {
        if (!showWin && !engine.isLoading) {
            timerSecs = 0
            while (true) { delay(1000); timerSecs++ }
        }
    }

    LaunchedEffect(engine.isSolved) {
        if (engine.isSolved && !showWin) {
            lastElapsed = timerSecs
            prefs.edit().putInt("currentLevel", engine.levelNumber + 1).apply()
            delay(600)
            showWin = true
        }
    }

    fun difficultyLevel(l: Int): Int = when (l) {
        in 1..15   -> 2; in 16..40  -> 3; in 41..80  -> 5; in 81..130 -> 6; else -> 8
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF10131B))
            .systemBarsPadding()
            // Tüm touch eventlerini tüket — alttaki HomeScreen'e geçmesin
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        if (engine.isLoading || engine.cells.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(color = ExcavationColors.accent, strokeWidth = 2.dp)
                    Text("Loading puzzle…", color = Color.White.copy(0.4f), fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
            }
        } else {
            NonogramGameContent(
                engine    = engine,
                timerSecs = timerSecs,
                hintedCell = hintedCell,
                onBack    = onHome,
                onRestart = { engine.resetPuzzle() },
                onHint    = {
                    scope.launch {
                        val pos = engine.revealHint()
                        if (pos != null) { hintedCell = pos; delay(1200); hintedCell = null }
                    }
                }
            )
        }

        GameResultSheet(
            visible          = showWin,
            gameId           = "pixelExcavation",
            level            = engine.levelNumber,
            elapsed          = lastElapsed,
            difficulty       = difficultyLevel(engine.levelNumber),
            gridSize         = engine.gridSize,
            onNextPuzzle     = { showWin = false; engine.nextLevel() },
            onPlayAgain      = { showWin = false; engine.resetPuzzle() },
            onBackToGames    = onHome,
            onNavigateToGame = onNavigateToGame
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Game layout
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NonogramGameContent(
    engine: NonogramEngine,
    timerSecs: Int,
    hintedCell: Pair<Int, Int>?,
    onBack: () -> Unit,
    onRestart: () -> Unit,
    onHint: () -> Unit
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
                "Nonogram",
                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
            IconButton(onClick = { /* info panel placeholder */ }, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)) {
                Icon(Icons.Default.Info, "Info", tint = Color.White.copy(0.45f), modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(6.dp))

        // ── Level badge
        Text(
            "LEVEL ${engine.levelNumber}  ·  ${engine.gridSize}×${engine.gridSize}",
            fontSize      = 12.sp,
            fontWeight    = FontWeight.Bold,
            color         = ExcavationColors.accent,
            letterSpacing = 0.5.sp,
            modifier      = Modifier
                .background(ExcavationColors.accent.copy(0.10f), CircleShape)
                .border(0.5.dp, ExcavationColors.accent.copy(0.20f), CircleShape)
                .padding(horizontal = 14.dp, vertical = 5.dp)
        )

        Spacer(Modifier.height(12.dp))

        // ── Info panel
        LevelInfoPanel(engine = engine, timerSecs = timerSecs)

        Spacer(Modifier.height(12.dp))

        // ── Grid fills the remaining space
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            NonogramGrid(engine = engine, hintedCell = hintedCell)
        }

        // ── Hint & Restart buttons
        Row(
            modifier = Modifier.padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(onClick = onRestart) {
                Icon(Icons.Default.Refresh, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Restart", color = Color.White.copy(0.7f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = onHint) {
                Icon(Icons.Default.Lightbulb, null, tint = ExcavationColors.accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Hint", color = ExcavationColors.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Level Info Panel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LevelInfoPanel(engine: NonogramEngine, timerSecs: Int) {
    val timeStr    = "%d:%02d".format(timerSecs / 60, timerSecs % 60)
    val densityStr = "${(engine.fillFraction * 100).toInt()}%"

    Row(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(10.dp))
            .background(ExcavationColors.sandDark)
            .border(0.5.dp, ExcavationColors.accent.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadarStat(icon = Icons.Default.Timer,   label = "TIME",    value = timeStr)
        InfoDivider()
        RadarStat(icon = Icons.Default.GridOn,  label = "SIZE",    value = "${engine.gridSize}×${engine.gridSize}")
        InfoDivider()
        RadarStat(icon = Icons.Default.Waves,   label = "DENSITY", value = densityStr)
        InfoDivider()
        RadarStat(icon = Icons.Default.Edit,    label = "MOVES",   value = "${engine.moveCount}")
    }
}

@Composable
private fun InfoDivider() = Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.08f)))

@Composable
private fun RadarStat(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, fontSize = 7.sp, color = Color.White.copy(0.3f), fontFamily = FontFamily.Monospace, letterSpacing = 0.8.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(icon, null, tint = ExcavationColors.radarGreen.copy(0.7f), modifier = Modifier.size(9.dp))
            Text(value, fontSize = 10.sp, color = Color.White.copy(0.75f), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Nonogram Grid
// All sizing is done in density-aware pixels, then converted back to dp.
// Cell size and clue area are calculated together so they always fill the
// available space without clipping.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NonogramGrid(engine: NonogramEngine, hintedCell: Pair<Int, Int>?) {
    val density = LocalDensity.current
    val gridSz  = engine.gridSize

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Available space in px (maxWidth/maxHeight are already in dp, .toPx() converts correctly)
        val availW = with(density) { maxWidth.toPx() }
        val availH = with(density) { maxHeight.toPx() }

        val maxRowClues = (engine.rowClues.maxOfOrNull { it.size } ?: 1).coerceAtLeast(1)
        val maxColClues = (engine.colClues.maxOfOrNull { it.size } ?: 1).coerceAtLeast(1)

        val spacing        = if (gridSz > 10) 1f else 1.5f   // px between cells (tiny)
        val separatorCount = if (gridSz > 5) (gridSz - 1) / 5 else 0
        val separatorPx    = separatorCount * with(density) { 1.5.dp.toPx() }
        val gridGapW       = spacing * (gridSz - 1)
        val outerPad       = with(density) { 16.dp.toPx() }  // total horizontal+vertical outer padding

        // ── Coupled equation to find cellSize ──────────────────────────────
        // Let C = cellSize in px.
        //   Clue font ≈ C * 0.42
        //   Row clue width  = maxRowClues * C * 0.42 * 0.65  (monospace char ≈ 65% of height)
        //   Col clue height = maxColClues * C * 0.42 * 1.35  (line height ≈ 135% of font)
        //
        // Width equation:
        //   C*gridSz + rowClueW + gridGapW + separatorPx + outerPad = availW
        //   C*(gridSz + maxRowClues*0.273) = availW - gridGapW - separatorPx - outerPad
        val rowClueRatio = maxRowClues * 0.42f * 0.65f   // = 0.273 * maxRowClues
        val colClueRatio = maxColClues * 0.42f * 1.35f   // = 0.567 * maxColClues

        val cellFromW = (availW - gridGapW - separatorPx - outerPad) / (gridSz + rowClueRatio)
        val cellFromH = (availH - gridGapW - separatorPx - outerPad) / (gridSz + colClueRatio)

        // Use the tighter axis; floor to avoid subpixel overflow; min 12dp
        val minCellPx = with(density) { 12.dp.toPx() }
        val cellSizePx = kotlin.math.floor(minOf(cellFromW, cellFromH).coerceAtLeast(minCellPx))

        // ── Derive all other dimensions from cellSizePx ────────────────────
        val fontPx          = cellSizePx * 0.42f
        val lineHeightPx    = fontPx * 1.35f
        val charWidthPx     = fontPx * 0.65f
        val rowClueWidthPx  = maxRowClues * charWidthPx + with(density) { 8.dp.toPx() }
        val colClueHeightPx = maxColClues * lineHeightPx + with(density) { 4.dp.toPx() }

        val cellSizeDp = with(density) { cellSizePx.toDp() }
        val spacingDp  = with(density) { spacing.toDp() }
        val rowClueDp  = with(density) { rowClueWidthPx.toDp() }
        val colClueDp  = with(density) { colClueHeightPx.toDp() }
        val fontSp     = with(density) { fontPx.toSp() }

        val clueComplete = ExcavationColors.radarGreen
        val clueDefault  = Color.White.copy(0.6f)
        val separator    = ExcavationColors.accent.copy(alpha = 0.12f)

        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.verticalGradient(listOf(ExcavationColors.sandDark, Color(0xFF0E1118))))
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Column clue header
            Row(verticalAlignment = Alignment.Bottom) {
                Spacer(Modifier.width(rowClueDp))
                for (col in 0 until gridSz) {
                    if (col > 0 && col % 5 == 0 && gridSz > 5) {
                        Spacer(
                            Modifier.width(1.5.dp).height(colClueDp)
                                .background(Brush.verticalGradient(listOf(separator, separator.copy(0.3f))))
                        )
                    }
                    val colDone = engine.isColComplete(col)
                    Column(
                        modifier = Modifier.width(cellSizeDp + spacingDp).height(colClueDp),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        engine.colClues.getOrNull(col)?.forEach { clue ->
                            Text(
                                "$clue",
                                fontSize = fontSp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = if (colDone) clueComplete else clueDefault,
                                lineHeight = fontSp * 1.2f
                            )
                        }
                    }
                }
            }

            // ── Grid rows
            for (row in 0 until gridSz) {
                // Thick separator every 5 rows
                if (row > 0 && row % 5 == 0 && gridSz > 5) {
                    Row {
                        Spacer(Modifier.width(rowClueDp).height(1.5.dp))
                        Spacer(
                            Modifier.height(1.5.dp).weight(1f)
                                .background(Brush.horizontalGradient(listOf(separator, separator.copy(0.3f))))
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Row clue
                    val rowDone = engine.isRowComplete(row)
                    Row(
                        modifier = Modifier
                            .width(rowClueDp)
                            .height(cellSizeDp + spacingDp)
                            .padding(end = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        engine.rowClues.getOrNull(row)?.forEach { clue ->
                            Text(
                                "$clue",
                                fontSize = fontSp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = if (rowDone) clueComplete else clueDefault,
                                modifier = Modifier.padding(horizontal = 1.dp)
                            )
                        }
                    }

                    // Cells
                    for (col in 0 until gridSz) {
                        if (col > 0 && col % 5 == 0 && gridSz > 5) {
                            Spacer(Modifier.width(1.5.dp).height(cellSizeDp + spacingDp).background(separator))
                        }
                        val cell = engine.cells.getOrNull(row)?.getOrNull(col)
                        if (cell != null) {
                            val isHinted = hintedCell?.first == row && hintedCell.second == col
                            NonogramCellView(
                                cell     = cell,
                                cellSize = cellSizeDp,
                                isHinted = isHinted,
                                onTap    = { engine.tapCell(row, col) },
                                modifier = Modifier.padding(spacingDp / 2)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Cell View
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NonogramCellView(
    cell: NonogramCell,
    cellSize: Dp,
    isHinted: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cornerRadiusDp = cellSize * 0.1f

    val hintAlpha by animateFloatAsState(
        targetValue = if (isHinted) 1f else 0f,
        animationSpec = tween(300),
        label = "hint"
    )

    Box(
        modifier = modifier
            .size(cellSize)
            .clip(RoundedCornerShape(cornerRadiusDp))
            .clickable(onClick = onTap)
            .drawBehind {
                val cr = CornerRadius(cornerRadiusDp.toPx())

                when (cell.state) {
                    NonogramCellState.EMPTY -> {
                        drawRoundRect(color = ExcavationColors.soil, cornerRadius = cr)
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                listOf(Color.Black.copy(0.15f), Color.Transparent, ExcavationColors.soilLight.copy(0.1f))
                            ),
                            cornerRadius = cr
                        )
                    }
                    NonogramCellState.FILLED -> {
                        drawRoundRect(
                            brush = Brush.linearGradient(listOf(ExcavationColors.excavated, ExcavationColors.bone)),
                            cornerRadius = cr
                        )
                        // Inner highlight
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                listOf(ExcavationColors.boneGlow.copy(0.9f), ExcavationColors.bone.copy(0.7f))
                            ),
                            cornerRadius = CornerRadius(cr.x * 0.6f),
                            topLeft = Offset(size.width * 0.1f, size.height * 0.1f),
                            size = androidx.compose.ui.geometry.Size(size.width * 0.8f, size.height * 0.8f)
                        )
                    }
                    NonogramCellState.MARKED -> {
                        drawRoundRect(color = ExcavationColors.sandDark, cornerRadius = cr)
                        drawRoundRect(color = ExcavationColors.soil.copy(0.5f), cornerRadius = cr)
                        val pad = size.width * 0.27f
                        drawLine(ExcavationColors.flagRed.copy(0.65f), Offset(pad, pad), Offset(size.width - pad, size.height - pad), strokeWidth = size.width * 0.08f, cap = StrokeCap.Round)
                        drawLine(ExcavationColors.flagRed.copy(0.65f), Offset(size.width - pad, pad), Offset(pad, size.height - pad), strokeWidth = size.width * 0.08f, cap = StrokeCap.Round)
                    }
                }

                // Border
                val borderColor = when (cell.state) {
                    NonogramCellState.EMPTY  -> ExcavationColors.soilLight.copy(0.3f)
                    NonogramCellState.FILLED -> ExcavationColors.accent.copy(0.5f)
                    NonogramCellState.MARKED -> ExcavationColors.soilLight.copy(0.2f)
                }
                drawRoundRect(color = borderColor, cornerRadius = cr, style = Stroke(0.5f))

                // Hint glow
                if (hintAlpha > 0f) {
                    drawRoundRect(color = ExcavationColors.accent.copy(hintAlpha), cornerRadius = cr, style = Stroke(size.width * 0.06f))
                }
            }
    )
}
