package com.example.brain_land.ui.games.slitherlink

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val lineColor1 = Color(0xFF14B8A6)
private val lineColor2 = Color(0xFF06B6D4)
private val dotActiveColor = Color(0xFF14B8A6)
private val dotInactiveColor = Color.White.copy(alpha = 0.25f)
private val clueGreen = Color(0xFF4ADE80)
private val levelPill = Color(0xFF6D28D9) // violet/purple like iOS

@Composable
fun SlitherlinkPuzzleView(onHome: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember { SlitherlinkEngine(context, scope) }

    var showWin by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var confettiTrigger by remember { mutableStateOf(false) }
    var timerSecs by remember { mutableIntStateOf(0) }

    // Timer
    LaunchedEffect(engine.isLoading, engine.isSolved) {
        if (!engine.isLoading && !engine.isSolved) {
            while (!engine.isSolved) {
                delay(1000)
                timerSecs++
            }
        }
    }

    LaunchedEffect(engine.isSolved) {
        if (engine.isSolved) {
            confettiTrigger = true
            showWin = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1017))
            .systemBarsPadding()
    ) {
        if (engine.isLoading || engine.cells.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = lineColor1, strokeWidth = 2.dp)
                    Text(
                        "Loading puzzle...",
                        color = Color.White.copy(0.4f),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Top bar ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, start = 4.dp, end = 16.dp)
                ) {
                    IconButton(
                        onClick = onHome,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White.copy(0.8f)
                        )
                    }
                    Text(
                        "Link Circuit",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    IconButton(
                        onClick = { showInfo = true },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Info",
                            tint = Color.White.copy(0.6f)
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // ── Level badge (violet pill) ──
                Box(
                    modifier = Modifier
                        .background(levelPill.copy(alpha = 0.85f), RoundedCornerShape(50))
                        .padding(horizontal = 18.dp, vertical = 5.dp)
                ) {
                    Text(
                        "LEVEL ${engine.levelNumber}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── Stats card (TIME | MOVES) ──
                val timeStr = "%d:%02d".format(timerSecs / 60, timerSecs % 60)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Time
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "TIME",
                            fontSize = 10.sp,
                            color = Color.White.copy(0.4f),
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                tint = lineColor1,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "0:$timeStr",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(Color.White.copy(alpha = 0.1f))
                    )

                    // Moves
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "MOVES",
                            fontSize = 10.sp,
                            color = Color.White.copy(0.4f),
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "${engine.moveCount}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Board ──
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val n = engine.rows
                    val innerPadding = 20.dp
                    val availableWidth = maxWidth - innerPadding * 2
                    val cellSize = availableWidth / n

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(Color(0xFF090C12), RoundedCornerShape(16.dp))
                            .border(
                                1.5.dp,
                                Brush.linearGradient(
                                    listOf(
                                        lineColor1.copy(alpha = 0.25f),
                                        lineColor2.copy(alpha = 0.08f),
                                        Color.Transparent
                                    )
                                ),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        SlitherlinkBoardView(engine = engine, cellSize = cellSize)
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Reset button (orange outlined pill — iOS style) ──
                OutlinedButton(
                    onClick = {
                        showWin = false
                        confettiTrigger = false
                        timerSecs = 0
                        engine.resetPuzzle()
                    },
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        Color(0xFFEA580C)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEA580C)
                    ),
                    modifier = Modifier
                        .height(44.dp)
                        .widthIn(min = 130.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Reset",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(28.dp))
            }
        }

        // ── Win overlay ──
        AnimatedVisibility(
            visible = confettiTrigger,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
            exit = scaleOut()
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .background(Color(0xFF131720), RoundedCornerShape(24.dp))
                        .border(1.dp, lineColor1.copy(0.3f), RoundedCornerShape(24.dp))
                        .padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = clueGreen,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        "Puzzle Solved!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Level ${engine.levelNumber}  ·  ${"%d:%02d".format(timerSecs / 60, timerSecs % 60)}",
                        fontSize = 13.sp,
                        color = Color.White.copy(0.5f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = {
                            showWin = false
                            confettiTrigger = false
                            timerSecs = 0
                            engine.nextLevel()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = lineColor1),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Next Level", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            showWin = false
                            confettiTrigger = false
                            timerSecs = 0
                            engine.resetPuzzle()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Play Again", fontSize = 14.sp)
                    }
                }
            }
        }
    }

    if (showInfo) {
        SlitherlinkInfoSheet(onClose = { showInfo = false })
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Board — iOS SlitherlinkBoardView, birebir port
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SlitherlinkBoardView(engine: SlitherlinkEngine, cellSize: Dp) {
    val n = engine.rows
    val dotSize = (cellSize.value * 0.12f).coerceAtLeast(5f).dp
    val edgeThickness = (cellSize.value * 0.07f).coerceAtLeast(3f).dp

    Box(modifier = Modifier.size(cellSize * n, cellSize * n)) {

        // Layer 1: Clues
        for (r in 0 until n) {
            for (c in 0 until n) {
                val cell = engine.cells[r][c]
                val clue = cell.clue ?: continue
                val lines = engine.linesAroundCell(r, c)
                val isError = cell.isError

                val textColor = when {
                    isError -> Color.Red
                    lines == clue -> clueGreen
                    else -> Color.White.copy(alpha = 0.65f)
                }
                val bgColor = when {
                    isError -> Color.Red.copy(alpha = 0.08f)
                    lines == clue -> clueGreen.copy(alpha = 0.06f)
                    else -> Color.Transparent
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(cellSize)
                        .offset(x = cellSize * c, y = cellSize * r)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(cellSize * 0.65f)
                            .clip(RoundedCornerShape(cellSize * 0.15f))
                            .background(bgColor)
                    ) {
                        Text(
                            text = clue.toString(),
                            fontSize = (cellSize.value * 0.42f).sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }
            }
        }

        // Layer 2: Horizontal edges
        for (r in 0..n) {
            for (c in 0 until n) {
                val state = engine.horizontalEdges[r][c]
                HorizontalEdgeView(
                    state = state,
                    cellSize = cellSize,
                    dotSize = dotSize,
                    edgeThickness = edgeThickness,
                    onClick = { engine.tapHorizontalEdge(r, c) },
                    modifier = Modifier.offset(
                        x = cellSize * c,
                        y = cellSize * r - cellSize * 0.2f
                    )
                )
            }
        }

        // Layer 3: Vertical edges
        for (r in 0 until n) {
            for (c in 0..n) {
                val state = engine.verticalEdges[r][c]
                VerticalEdgeView(
                    state = state,
                    cellSize = cellSize,
                    dotSize = dotSize,
                    edgeThickness = edgeThickness,
                    onClick = { engine.tapVerticalEdge(r, c) },
                    modifier = Modifier.offset(
                        x = cellSize * c - cellSize * 0.2f,
                        y = cellSize * r
                    )
                )
            }
        }

        // Layer 4: Dots
        for (r in 0..n) {
            for (c in 0..n) {
                val degree = engine.linesAtDot(r, c)
                val isActive = degree > 0
                val dotDraw = if (isActive) dotSize + 3.dp else dotSize

                Box(
                    modifier = Modifier.offset(
                        x = cellSize * c - dotDraw / 2,
                        y = cellSize * r - dotDraw / 2
                    )
                ) {
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .size(dotDraw + 6.dp)
                                .offset(x = -3.dp, y = -3.dp)
                                .clip(CircleShape)
                                .background(dotActiveColor.copy(alpha = 0.25f))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(dotDraw)
                            .clip(CircleShape)
                            .background(if (isActive) dotActiveColor else dotInactiveColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun HorizontalEdgeView(
    state: SlitherlinkEdgeState,
    cellSize: Dp,
    dotSize: Dp,
    edgeThickness: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(cellSize, cellSize * 0.4f)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        when (state) {
            SlitherlinkEdgeState.NONE -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(2.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                        )
                    }
                }
            }
            SlitherlinkEdgeState.LINE -> {
                Box(
                    modifier = Modifier
                        .width(cellSize - dotSize)
                        .height(edgeThickness)
                        .clip(RoundedCornerShape(edgeThickness / 2))
                        .background(Brush.horizontalGradient(listOf(lineColor1, lineColor2)))
                        .drawBehind {
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    listOf(lineColor1.copy(alpha = 0.5f), lineColor2.copy(alpha = 0.25f))
                                ),
                                size = androidx.compose.ui.geometry.Size(size.width, size.height * 5),
                                topLeft = androidx.compose.ui.geometry.Offset(0f, -size.height * 2f)
                            )
                        }
                )
            }
            SlitherlinkEdgeState.CROSS -> {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.size(cellSize * 0.28f)
                ) {
                    val s = size.minDimension * 0.4f
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    drawLine(Color.White.copy(0.35f), androidx.compose.ui.geometry.Offset(cx - s, cy - s), androidx.compose.ui.geometry.Offset(cx + s, cy + s), 2.dp.toPx())
                    drawLine(Color.White.copy(0.35f), androidx.compose.ui.geometry.Offset(cx - s, cy + s), androidx.compose.ui.geometry.Offset(cx + s, cy - s), 2.dp.toPx())
                }
            }
        }
    }
}

@Composable
private fun VerticalEdgeView(
    state: SlitherlinkEdgeState,
    cellSize: Dp,
    dotSize: Dp,
    edgeThickness: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(cellSize * 0.4f, cellSize)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        when (state) {
            SlitherlinkEdgeState.NONE -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(2.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                        )
                    }
                }
            }
            SlitherlinkEdgeState.LINE -> {
                Box(
                    modifier = Modifier
                        .width(edgeThickness)
                        .height(cellSize - dotSize)
                        .clip(RoundedCornerShape(edgeThickness / 2))
                        .background(Brush.verticalGradient(listOf(lineColor1, lineColor2)))
                        .drawBehind {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    listOf(lineColor1.copy(alpha = 0.5f), lineColor2.copy(alpha = 0.25f))
                                ),
                                size = androidx.compose.ui.geometry.Size(size.width * 5, size.height),
                                topLeft = androidx.compose.ui.geometry.Offset(-size.width * 2f, 0f)
                            )
                        }
                )
            }
            SlitherlinkEdgeState.CROSS -> {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.size(cellSize * 0.28f)
                ) {
                    val s = size.minDimension * 0.4f
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    drawLine(Color.White.copy(0.35f), androidx.compose.ui.geometry.Offset(cx - s, cy - s), androidx.compose.ui.geometry.Offset(cx + s, cy + s), 2.dp.toPx())
                    drawLine(Color.White.copy(0.35f), androidx.compose.ui.geometry.Offset(cx - s, cy + s), androidx.compose.ui.geometry.Offset(cx + s, cy - s), 2.dp.toPx())
                }
            }
        }
    }
}
