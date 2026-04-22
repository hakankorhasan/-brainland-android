package com.example.brain_land.ui.games.numbercircuit

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.example.brain_land.data.GameType
import com.example.brain_land.ui.games.tiltmaze.GameResultSheet
import kotlinx.coroutines.delay

private val accentCyan   = Color(0xFF00E5FF)
private val accentPurple = Color(0xFFA78BFA)
private val accentGold   = Color(0xFFFBBF24)
private val accentTeal   = Color(0xFF14B8A6)
private val bgDeep       = Color(0xFF0D1017)
private val bgCard       = Color(0xFF090C12)
private val bgTile       = Color(0xFF1A1D2E)
private val bgTileSel    = Color(0xFF0D2B3E)
private val levelPill    = Color(0xFF6D28D9)

// ─────────────────────────────────────────────────────────────────────────────
// Entry Point
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NumberCircuitPuzzleView(onHome: () -> Unit, onNavigateToGame: (Any) -> Unit = {}) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val gs      = remember { NCGameState(context, scope) }

    var showWin  by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var timerSecs by remember { mutableIntStateOf(0) }
    // finalElapsed freezes the timer at the moment of solving
    var finalElapsed by remember { mutableIntStateOf(0) }

    // Timer: resets when level changes or solved
    LaunchedEffect(gs.levelNumber) {
        timerSecs = 0
        while (!gs.isSolved) {
            delay(1000)
            timerSecs++
        }
        finalElapsed = timerSecs  // freeze timer when solved
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgDeep)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Top bar ──────────────────────────────────────────────────────
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
                    "Math Matrix",
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                    modifier   = Modifier.align(Alignment.Center)
                )
                IconButton(
                    onClick  = { showInfo = true },
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
            Text(
                "LEVEL ${gs.levelNumber}",
                fontSize      = 12.sp,
                fontWeight    = FontWeight.Bold,
                color         = accentPurple,
                letterSpacing = 0.5.sp,
                modifier      = Modifier
                    .background(accentPurple.copy(0.10f), CircleShape)
                    .border(0.5.dp, accentPurple.copy(0.20f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            )

            Spacer(Modifier.height(12.dp))

            // ── Stats card ───────────────────────────────────────────────────
            val timeStr = "%d:%02d".format(timerSecs / 60, timerSecs % 60)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(Color.White.copy(0.06f), RoundedCornerShape(14.dp))
                    .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(14.dp))
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TIME", fontSize = 10.sp, color = Color.White.copy(0.4f),
                        letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Default.Timer, null, tint = accentCyan, modifier = Modifier.size(16.dp))
                        Text("0:$timeStr", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                Box(Modifier.width(1.dp).height(36.dp).background(Color.White.copy(0.1f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("MOVES", fontSize = 10.sp, color = Color.White.copy(0.4f),
                        letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Default.TouchApp, null, tint = accentPurple, modifier = Modifier.size(16.dp))
                        Text("${gs.moveCount}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Main game card ────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .background(bgCard, RoundedCornerShape(20.dp))
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(accentCyan.copy(0.2f), accentPurple.copy(0.1f), Color.Transparent)
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Grid — centered
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val spacing  = 6.dp
                    val cellSize = ((maxWidth - spacing * (gs.gridSize - 1)) / gs.gridSize)
                        .coerceAtMost(96.dp)
                    // wrapContentWidth so the grid doesn't steal full row width
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        NCGridView(gameState = gs, cellSize = cellSize, spacing = spacing)
                    }
                }

                // TARGET row (below grid, inside card)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "TARGET",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = accentCyan, letterSpacing = 1.sp
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${gs.level.target}",
                        fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = accentCyan
                    )
                }

                // Expression bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(0.04f), RoundedCornerShape(10.dp))
                        .border(
                            1.dp,
                            Brush.linearGradient(listOf(accentCyan.copy(0.25f), Color.White.copy(0.04f))),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("f(x)", fontSize = 12.sp, color = accentCyan.copy(0.6f),
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    if (gs.expressionString.isEmpty()) {
                        Text("Connect numbers…", fontSize = 13.sp, color = Color.White.copy(0.25f),
                            modifier = Modifier.weight(1f))
                    } else {
                        Text(
                            gs.expressionString,
                            fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(0.9f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    gs.currentResult?.let { result ->
                        val isMatch = result == gs.level.target
                        Text(" = ", fontSize = 14.sp, fontFamily = FontFamily.Monospace,
                            color = if (isMatch) Color(0xFF00FF88) else Color.White.copy(0.4f))
                        Text("$result", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isMatch) Color(0xFF00FF88) else Color.White)
                    } ?: run {
                        if (gs.needsOperator) {
                            Text("?", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                color = accentGold.copy(0.8f))
                        }
                    }
                }

                // Operator row: TARGET chip + op buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // TARGET inline chip
                    Row(
                        modifier = Modifier
                            .background(Color.White.copy(0.04f), RoundedCornerShape(10.dp))
                            .border(1.dp, accentCyan.copy(0.15f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("TARGET", fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            color = Color.White.copy(0.35f), letterSpacing = 1.sp)
                        Text(
                            "${gs.level.target}",
                            fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                            color = accentCyan
                        )
                    }

                    // Operator buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        gs.level.allowedOperators.forEach { op ->
                            NCOperatorButton(op, gs.needsOperator) { gs.setOperator(op) }
                        }
                    }
                }

                // Undo + Submit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { gs.undoLast() },
                        enabled = gs.selectedPath.isNotEmpty(),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(0.6f)),
                        border = BorderStroke(1.dp, Color.White.copy(0.12f)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Undo, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Undo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    val canSubmit = gs.isExpressionComplete
                    Button(
                        onClick = {
                            val solved = gs.submit()
                            if (solved) showWin = true
                        },
                        enabled = canSubmit,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (canSubmit)
                                    Brush.horizontalGradient(listOf(accentCyan.copy(0.85f), accentPurple.copy(0.65f)))
                                else
                                    Brush.horizontalGradient(listOf(Color.White.copy(0.07f), Color.White.copy(0.04f))),
                                RoundedCornerShape(50)
                            )
                    ) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Submit", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Bottom buttons: Hint | Restart ────────────────────────────────
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hint button (amber outlined)
                OutlinedButton(
                    onClick = { gs.showNextHint() },
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.5.dp, accentGold.copy(0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accentGold),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(Icons.Default.Lightbulb, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Hint ${gs.hintLevel}/3", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                // Restart button (teal outlined)
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
            }

            Spacer(Modifier.height(32.dp))
        }

        // ── Game Result (full screen, backend score submit) ───────────────────
        GameResultSheet(
            visible          = showWin,
            gameId           = "numberCircuit",
            level            = gs.levelNumber,
            elapsed          = finalElapsed,
            difficulty       = gs.level.gridSize,
            gridSize         = gs.level.gridSize,
            onNextPuzzle     = { showWin = false; gs.nextLevel() },
            onPlayAgain      = { showWin = false; gs.resetPuzzle() },
            onBackToGames    = { showWin = false; onHome() },
            onNavigateToGame = { gameType -> onNavigateToGame(gameType) }
        )
    }

    if (showInfo) {
        NumberCircuitInfoSheet(onClose = { showInfo = false })
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// NCGridView
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NCGridView(gameState: NCGameState, cellSize: Dp, spacing: Dp) {
    val shakeOffset by animateFloatAsState(
        targetValue = if (gameState.isWrong) 1f else 0f,
        animationSpec = if (gameState.isWrong)
            repeatable(5, tween(60), RepeatMode.Reverse)
        else spring(),
        label = "shake"
    )

    // NCGridView — does NOT fillMaxWidth; it wraps the grid content so centering works
    Box(
        modifier = Modifier
            .wrapContentWidth()          // ← key: don't expand past grid width
            .offset(x = (shakeOffset * 6).dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            for (row in 0 until gameState.gridSize) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    for (col in 0 until gameState.gridSize) {
                        val pos      = NCPosition(row, col)
                        val isSel    = gameState.selectedPath.contains(pos)
                        val selIdx   = gameState.selectedPath.indexOf(pos).takeIf { it >= 0 }
                        val special  = gameState.level.specialTiles.firstOrNull { it.position == pos }
                        val isHinted = isPositionHinted(gameState, pos)

                        NCTileView(
                            value          = gameState.level.grid[row][col],
                            isSelected     = isSel,
                            selectionIndex = selIdx,
                            specialTile    = special,
                            isHinted       = isHinted,
                            cellSize       = cellSize,
                            onClick        = { gameState.selectTile(row, col) }
                        )
                    }
                }
            }
        }

        // Connection path overlay — also centred
        if (gameState.selectedPath.size >= 2) {
            NCConnectionPath(
                path      = gameState.selectedPath,
                operators = gameState.chosenOperators,
                gridSize  = gameState.gridSize,
                cellSize  = cellSize,
                spacing   = spacing
            )
        }
    }
}

private fun isPositionHinted(gs: NCGameState, pos: NCPosition) = when (gs.hintLevel) {
    1 -> pos == gs.level.hints.hint1Position
    2 -> gs.level.hints.hint2Positions.contains(pos)
    3 -> gs.level.hints.hint3Positions.contains(pos)
    else -> false
}

// ─────────────────────────────────────────────────────────────────────────────
// NCTileView
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NCTileView(
    value: Int,
    isSelected: Boolean,
    selectionIndex: Int?,
    specialTile: NCSpecialTile?,
    isHinted: Boolean,
    cellSize: Dp,
    onClick: () -> Unit
) {
    val fg = if (isSelected) Color.White else Color.White.copy(0.85f)
    val bg = if (isSelected)
        Brush.linearGradient(listOf(bgTileSel, Color(0xFF0A1F2E)))
    else
        Brush.linearGradient(listOf(bgTile, Color(0xFF14172A)))

    Box(
        modifier = Modifier
            .size(cellSize)
            .clip(RoundedCornerShape(cellSize * 0.2f))
            .background(bg)
            .border(
                if (isSelected) 2.dp else 0.5.dp,
                if (isSelected) Brush.linearGradient(listOf(accentCyan.copy(0.8f), accentPurple.copy(0.4f)))
                else Brush.linearGradient(listOf(Color.White.copy(0.07f), Color.White.copy(0.02f))),
                RoundedCornerShape(cellSize * 0.2f)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        // Main number
        Text(
            "$value",
            fontSize = (cellSize.value * 0.4f).sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.align(Alignment.Center)
        )

        // Selection index (top-right)
        selectionIndex?.let {
            Text(
                "${it + 1}",
                fontSize = (cellSize.value * 0.17f).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = accentCyan,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .background(bgDeep.copy(0.85f), CircleShape)
                    .padding(horizontal = 3.dp)
            )
        }

        // Special overlay (bottom-left)
        specialTile?.let { tile ->
            when (tile.type) {
                NCSpecialTileType.LOCKED ->
                    Icon(Icons.Default.Lock, null, tint = Color(0xFFA78BFA),
                        modifier = Modifier.size((cellSize.value * 0.22f).dp).align(Alignment.BottomStart).padding(3.dp))
                NCSpecialTileType.BOMB ->
                    Icon(Icons.Default.Whatshot, null, tint = Color(0xFFEF4444),
                        modifier = Modifier.size((cellSize.value * 0.22f).dp).align(Alignment.BottomStart).padding(3.dp))
                NCSpecialTileType.MULTIPLIER ->
                    Text("×${tile.multiplierValue}", fontSize = (cellSize.value * 0.16f).sp,
                        fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B),
                        modifier = Modifier.align(Alignment.BottomStart).padding(3.dp))
                NCSpecialTileType.FORCED_OP ->
                    tile.forcedOperator?.let { op ->
                        Text(op.symbol, fontSize = (cellSize.value * 0.18f).sp,
                            fontWeight = FontWeight.Bold, color = Color(0xFF2DD4BF),
                            modifier = Modifier.align(Alignment.BottomStart).padding(3.dp))
                    }
            }
        }

        // Hint ring
        if (isHinted && !isSelected) {
            Box(modifier = Modifier.fillMaxSize()
                .border(2.dp, accentGold.copy(0.6f), RoundedCornerShape(cellSize * 0.2f)))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NCConnectionPath — gradient line + operator circles on canvas
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NCConnectionPath(
    path: List<NCPosition>,
    operators: List<NCOperator>,
    gridSize: Int,
    cellSize: Dp,
    spacing: Dp,
    modifier: Modifier = Modifier
) {
    val density     = LocalDensity.current
    val cellPx      = with(density) { cellSize.toPx() }
    val spacingPx   = with(density) { spacing.toPx() }

    Canvas(
        modifier = modifier.size(
            width  = cellSize * gridSize + spacing * (gridSize - 1),
            height = cellSize * gridSize + spacing * (gridSize - 1)
        )
    ) {
        if (path.size < 2) return@Canvas
        val points = path.map { pos ->
            Offset(
                x = pos.col * (cellPx + spacingPx) + cellPx / 2f,
                y = pos.row * (cellPx + spacingPx) + cellPx / 2f
            )
        }

        // Gradient path line
        val linePath = androidx.compose.ui.graphics.Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
        }
        drawPath(
            path  = linePath,
            brush = Brush.linearGradient(
                listOf(accentCyan.copy(0.65f), accentCyan.copy(0.3f)),
                start = points.first(), end = points.last()
            ),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Operator circles at segment midpoints
        for (i in operators.indices) {
            if (i + 1 >= points.size) break
            val mid = Offset((points[i].x + points[i+1].x) / 2f, (points[i].y + points[i+1].y) / 2f)
            val r   = 11.dp.toPx()
            drawCircle(color = bgDeep,                  radius = r, center = mid)
            drawCircle(color = accentCyan.copy(0.55f),  radius = r, center = mid,
                style = Stroke(1.5.dp.toPx()))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NCOperatorButton
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NCOperatorButton(op: NCOperator, isActive: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue    = if (isActive) 1f else 0.88f,
        animationSpec  = spring(Spring.DampingRatioMediumBouncy),
        label          = "op_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(42.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(
                if (isActive) Brush.linearGradient(listOf(accentCyan.copy(0.15f), accentPurple.copy(0.1f)))
                else Brush.linearGradient(listOf(Color.White.copy(0.04f), Color.White.copy(0.02f)))
            )
            .border(1.2.dp, if (isActive) accentCyan.copy(0.5f) else Color.White.copy(0.08f), CircleShape)
            .clickable(
                enabled = isActive,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Text(
            op.symbol,
            fontSize = 18.sp, fontWeight = FontWeight.Bold,
            color = if (isActive) Color.White else Color.White.copy(0.3f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Info Sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberCircuitInfoSheet(onClose: () -> Unit) {
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
            // Hero icon
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            Brush.radialGradient(listOf(accentCyan.copy(0.15f), Color.Transparent)),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF0A1628), Color(0xFF0D0B2E))),
                            CircleShape
                        )
                        .border(1.5.dp,
                            Brush.linearGradient(listOf(accentCyan.copy(0.5f), accentPurple.copy(0.3f))),
                            CircleShape)
                )
                Icon(Icons.Default.Bolt, null, tint = accentCyan, modifier = Modifier.size(26.dp))
            }

            Text("How to Play", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)

            listOf(
                Triple(Icons.Default.Draw,        1, "Tap adjacent tiles to build a path"),
                Triple(Icons.Default.Calculate,   2, "Choose an operator (+  −  ×  ÷) after each tile"),
                Triple(Icons.Default.TrackChanges,3, "Your equation must equal the TARGET"),
                Triple(Icons.Default.Warning,     4, "× and ÷ are evaluated before + and −")
            ).forEach { (icon, step, text) ->
                NCInfoRow(icon = icon, step = step, text = text)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun NCInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector, step: Int, text: String
) {
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
            modifier = Modifier
                .size(36.dp)
                .background(
                    Brush.linearGradient(listOf(accentCyan.copy(0.12f), accentPurple.copy(0.06f))),
                    RoundedCornerShape(10.dp)
                )
                .border(1.dp, accentCyan.copy(0.2f), RoundedCornerShape(10.dp))
        ) {
            Icon(icon, null, tint = accentCyan, modifier = Modifier.size(17.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("STEP $step", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                color = Color.White.copy(0.3f), letterSpacing = 0.5.sp)
            Text(text, fontSize = 13.sp, color = Color.White.copy(0.75f))
        }
    }
}
