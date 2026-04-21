package com.example.brain_land.ui.games.tiltmaze

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import kotlin.math.*

// ─────────────────────────────────────────────────────────────────────────────
// Colors
// ─────────────────────────────────────────────────────────────────────────────

private val MazeBg = Color(0xFF151821)
private val WallClr = Color.White.copy(alpha = 0.85f)
private val ACyan = Color(0xFF5CC8D4)
private val APurp = Color(0xFFA299EC)
private val COrange = Color(0xFFFF8C00)
private val CFireRed = Color(0xFFFF6B00)

// ─────────────────────────────────────────────────────────────────────────────
// TiltMazeView
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TiltMazeView(
    level: Int,
    tutorialShown: Boolean,
    onTutorialDismissed: () -> Unit,
    onTimerTick: (Int) -> Unit = {},
    onWin: (elapsedSeconds: Int) -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalContext.current.resources.displayMetrics.density
    val scope   = rememberCoroutineScope()

    // Flame tick for animations
    val infinTrans = rememberInfiniteTransition(label = "tilt")
    val flameTick by infinTrans.animateFloat(
        initialValue = 0f, targetValue = (2 * PI * 200).toFloat(),
        animationSpec = infiniteRepeatable(tween(20_000, easing = LinearEasing)),
        label = "ft"
    )

    // State
    var snap            by remember { mutableStateOf(TiltMazeSnapshot()) }
    var isInitialized   by remember { mutableStateOf(false) }
    var levelComplete   by remember { mutableStateOf(false) }
    var isMeltdown      by remember { mutableStateOf(false) }
    var showMeltdownBtn by remember { mutableStateOf(false) }
    var elapsedSecs     by remember { mutableIntStateOf(0) }
    var showTutorial    by remember { mutableStateOf(!tutorialShown) }
    var boardPx         by remember { mutableFloatStateOf(0f) }
    var currentEngine   by remember { mutableStateOf<TiltMazeEngine?>(null) }

    // Jobs
    var physicsJob by remember { mutableStateOf<Job?>(null) }
    var timerJob   by remember { mutableStateOf<Job?>(null) }

    fun stopAll() {
        physicsJob?.cancel(); physicsJob = null
        timerJob?.cancel();   timerJob  = null
    }

    fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                elapsedSecs++
                onTimerTick(elapsedSecs)
            }
        }
    }

    fun startPhysicsLoop(eng: TiltMazeEngine) {
        physicsJob?.cancel()
        physicsJob = scope.launch {
            // withFrameNanos mirrors iOS Timer(1/60s) — tied to actual display vsync
            var lastNanos = System.nanoTime()
            while (isActive) {
                val frameNanos = withFrameNanos { it }
                val dtMs = (frameNanos - lastNanos) / 1_000_000L
                lastNanos = frameNanos

                // Run multiple physics ticks if a frame took > 16ms (catch-up)
                val ticks = (dtMs / 16L).coerceIn(1L, 4L).toInt()
                for (i in 0 until ticks) {
                    eng.flamePhase += 0.1f
                    eng.stepPhysics()
                    eng.stepHeat()
                }

                snap = TiltMazeSnapshot(
                    ballX        = eng.ballPos.x,
                    ballY        = eng.ballPos.y,
                    ballVx       = eng.ballVel.x,
                    ballVy       = eng.ballVel.y,
                    mazeAngleDeg = Math.toDegrees(eng.mazeAngle.toDouble()).toFloat(),
                    maxHeat      = eng.maxHeat,
                    heatGrid     = eng.heatGrid.map { it.copyOf() }.toTypedArray(),
                    flamePhase   = eng.flamePhase,
                    shakeX       = eng.shakeX,
                    shakeY       = eng.shakeY,
                    cellSize     = eng.cellSize,
                    boardSize    = eng.cols * eng.cellSize,
                    ballRadius   = eng.ballRadius,
                    rows         = eng.rows,
                    cols         = eng.cols
                )

                if (!levelComplete && !isMeltdown) {
                    if (!showTutorial && eng.isWin()) {
                        levelComplete = true
                        timerJob?.cancel()
                        eng.heatGrid.forEach { it.fill(0f) }; eng.maxHeat = 0f
                        onWin(elapsedSecs); break
                    }
                    if (eng.maxHeat >= eng.meltdownThreshold) {
                        isMeltdown = true; timerJob?.cancel()
                        delay(1500L); showMeltdownBtn = true; break
                    }
                }
            }
        }
    }

    fun initBoard(bPx: Float) {
        if (bPx <= 0f) return
        stopAll(); levelComplete = false; isMeltdown = false; showMeltdownBtn = false; elapsedSecs = 0
        val size = tiltMazeSizeForLevel(level)
        val cs   = bPx / size
        val br   = cs * tiltMazeBallRatio(size)
        val hm   = tiltMazeHeatMult(level)
        val eng  = TiltMazeEngine(rows = size, cols = size, cellSize = cs, ballRadius = br, heatMultiplier = hm)
        currentEngine = eng
        snap = TiltMazeSnapshot(ballX = cs/2, ballY = cs/2, cellSize = cs, boardSize = bPx, ballRadius = br, rows = size, cols = size)
        isInitialized = true
        startPhysicsLoop(eng)
        if (!showTutorial) startTimer()
    }

    LaunchedEffect(boardPx, level) { if (boardPx > 0f) initBoard(boardPx) }
    DisposableEffect(Unit) { onDispose { stopAll() } }

    // Rotation angle state for the board (read from engine)
    val rotDeg  = snap.mazeAngleDeg
    val shakeXdp = (snap.shakeX / density).dp
    val shakeYdp = (snap.shakeY / density).dp
    val boardDp  = if (boardPx > 0f) (boardPx / density).dp else 0.dp

    BoxWithConstraints(modifier = modifier) {
        val sizeFloat = min(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())

        LaunchedEffect(sizeFloat) {
            if (sizeFloat > 0f && abs(sizeFloat - boardPx) > 1f) boardPx = sizeFloat
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Board ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // Rotating + shaking board box
                Box(
                    modifier = Modifier
                        .size((sizeFloat / density).dp)
                        .rotate(rotDeg)
                        .offset(x = shakeXdp, y = shakeYdp)
                        .pointerInput(level) {
                            val cx = sizeFloat / 2f
                            val cy = sizeFloat / 2f
                            var prevAngle: Double? = null
                            awaitPointerEventScope {
                                while (true) {
                                    val evt = awaitPointerEvent()
                                    val pos = evt.changes.firstOrNull()?.position
                                    when (evt.type) {
                                        PointerEventType.Press -> {
                                            if (showTutorial) {
                                                showTutorial = false
                                                onTutorialDismissed()
                                                startTimer()
                                                prevAngle = null
                                            } else if (!levelComplete && !isMeltdown && pos != null) {
                                                prevAngle = atan2((pos.y - cy).toDouble(), (pos.x - cx).toDouble())
                                            }
                                        }
                                        PointerEventType.Move -> {
                                            if (!showTutorial && !levelComplete && !isMeltdown && pos != null) {
                                                val curr = atan2((pos.y - cy).toDouble(), (pos.x - cx).toDouble())
                                                prevAngle?.let { prev ->
                                                    var delta = curr - prev
                                                    if (delta > PI) delta -= 2 * PI
                                                    if (delta < -PI) delta += 2 * PI
                                                    currentEngine?.let { it.mazeAngle += (delta * it.rotationDamping).toFloat() }
                                                }
                                                prevAngle = curr
                                            }
                                        }
                                        PointerEventType.Release -> prevAngle = null
                                        else -> {}
                                    }
                                    evt.changes.forEach { if (it.pressed) it.consume() }
                                }
                            }
                        }
                ) {
                    // ── Canvas — draw everything ──
                    val eng = currentEngine
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (eng == null || !isInitialized) {
                            drawRect(color = MazeBg)
                            return@Canvas
                        }

                        // Background
                        drawRect(color = MazeBg)

                        // Heat cells
                        drawHeatCells(snap, eng, flameTick)

                        // Walls (normal, white)
                        drawMazeWalls(eng)

                        // Heated walls at multiple thresholds
                        drawHeatedWalls(snap, eng, 0.15f, COrange.copy(alpha = 0.4f))
                        drawHeatedWalls(snap, eng, 0.40f, CFireRed.copy(alpha = 0.35f))
                        drawHeatedWalls(snap, eng, 0.65f, Color.Red.copy(alpha = 0.4f))
                        val pa = 0.45f + sin(flameTick.toDouble() * 4).toFloat() * 0.12f
                        drawHeatedWalls(snap, eng, 0.85f, Color.Red.copy(alpha = pa))

                        // Water pool (goal)
                        drawWaterPool(eng, flameTick)

                        // Ball
                        if (isInitialized) {
                            if (levelComplete) drawExtBall(snap)
                            else drawFireball(snap, flameTick)
                        }
                    }

                    // ── Overlays (above canvas) ──
                    if (showTutorial) {
                        TutorialOverlay(
                            boardSizeDp = (sizeFloat / density).dp,
                            flameTick   = flameTick,
                            onDismiss   = {
                                showTutorial = false
                                onTutorialDismissed()
                                startTimer()
                            }
                        )
                    }

                    if (isMeltdown) {
                        MeltdownOverlay(
                            flameTick   = flameTick,
                            showButtons = showMeltdownBtn,
                            onTryAgain  = {
                                isMeltdown = false; showMeltdownBtn = false; levelComplete = false; elapsedSecs = 0
                                currentEngine?.let { e ->
                                    e.ballPos = Vec2(e.cellSize / 2, e.cellSize / 2)
                                    e.ballVel = Vec2(0f, 0f); e.mazeAngle = 0f
                                    e.heatGrid.forEach { it.fill(0f) }; e.maxHeat = 0f
                                    startPhysicsLoop(e); startTimer()
                                }
                            },
                            onMenu = onRestart
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Heat bar ──
            HeatBar(maxHeat = snap.maxHeat, flameTick = flameTick)

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Canvas draw functions
// ─────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawHeatCells(snap: TiltMazeSnapshot, eng: TiltMazeEngine, tick: Float) {
    if (snap.heatGrid.isEmpty()) return
    val cs = eng.cellSize
    for (r in 0 until eng.rows) {
        for (c in 0 until eng.cols) {
            if (r >= snap.heatGrid.size || c >= snap.heatGrid[r].size) continue
            val h = snap.heatGrid[r][c]
            val alpha = when {
                h >= 0.8f  -> 0.18f + sin(tick.toDouble() * 4).toFloat() * 0.06f
                h >= 0.6f  -> 0.12f
                h >= 0.35f -> 0.10f
                h >= 0.1f  -> 0.06f
                else       -> 0f
            }
            if (alpha > 0f) {
                val clr = if (h >= 0.6f) Color.Red else COrange
                drawRect(clr.copy(alpha = alpha), Offset(c * cs, r * cs), Size(cs, cs))
            }
        }
    }
}

private fun DrawScope.drawMazeWalls(eng: TiltMazeEngine) {
    for (rect in eng.wallRects) {
        drawRect(WallClr, Offset(rect.left, rect.top), Size(rect.width, rect.height))
    }
}

private fun DrawScope.drawHeatedWalls(snap: TiltMazeSnapshot, eng: TiltMazeEngine, threshold: Float, color: Color) {
    if (snap.heatGrid.isEmpty()) return
    val cs = eng.cellSize; val wt = eng.wallThickness; val hw = wt / 2f
    // Border walls
    for (c in 0 until eng.cols) {
        val hTop = if (snap.heatGrid.isNotEmpty() && c < snap.heatGrid[0].size) snap.heatGrid[0][c] else 0f
        val hBot = if (snap.heatGrid.size > eng.rows - 1 && c < snap.heatGrid[eng.rows-1].size) snap.heatGrid[eng.rows-1][c] else 0f
        if (hTop >= threshold) drawRect(color, Offset(c * cs, 0f), Size(cs, wt))
        if (hBot >= threshold) drawRect(color, Offset(c * cs, eng.rows * cs - wt), Size(cs, wt))
    }
    for (r in 0 until eng.rows) {
        val hL = if (r < snap.heatGrid.size) snap.heatGrid[r][0] else 0f
        val hR = if (r < snap.heatGrid.size && eng.cols - 1 < snap.heatGrid[r].size) snap.heatGrid[r][eng.cols-1] else 0f
        if (hL >= threshold) drawRect(color, Offset(0f, r * cs), Size(wt, cs))
        if (hR >= threshold) drawRect(color, Offset(eng.cols * cs - wt, r * cs), Size(wt, cs))
    }
    // Interior
    for (r in 0 until eng.rows) for (c in 0 until eng.cols) {
        val cell = eng.cells[r][c]
        val x = c * cs; val y = r * cs
        if (cell.southWall && r < eng.rows - 1 && r < snap.heatGrid.size && r + 1 < snap.heatGrid.size) {
            val h = max(snap.heatGrid[r][c], snap.heatGrid[r+1][c])
            if (h >= threshold) drawRect(color, Offset(x, y + cs - hw), Size(cs, wt))
        }
        if (cell.eastWall && c < eng.cols - 1 && r < snap.heatGrid.size) {
            val h = max(snap.heatGrid[r][c], snap.heatGrid[r][c+1])
            if (h >= threshold) drawRect(color, Offset(x + cs - hw, y), Size(wt, cs))
        }
    }
}

private fun DrawScope.drawWaterPool(eng: TiltMazeEngine, tick: Float) {
    val cs = eng.cellSize
    val gx = (eng.cols - 1 + 0.5f) * cs
    val gy = (eng.rows - 1 + 0.5f) * cs
    val pulse = sin(tick * 0.8).toFloat()
    val r1 = cs * (0.55f + pulse * 0.05f) / 2f
    // Outer stroke ring
    drawCircle(Color(0xFF00D4FF).copy(alpha = 0.2f + pulse * 0.08f), r1, Offset(gx, gy), style = Stroke(1.5f))
    // Radial fill
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF00D4FF).copy(0.35f), Color(0xFF0088CC).copy(0.15f), Color.Transparent),
            center = Offset(gx, gy), radius = cs * 0.3f
        ),
        radius = cs * 0.3f, center = Offset(gx, gy)
    )
    // Drop dot
    drawCircle(Color(0xFF00D4FF).copy(0.8f), cs * 0.07f, Offset(gx, gy))
}

private fun DrawScope.drawFireball(snap: TiltMazeSnapshot, tick: Float) {
    if (snap.ballRadius <= 0f) return
    val cx = snap.ballX; val cy = snap.ballY; val r = snap.ballRadius
    val speed = hypot(snap.ballVx.toDouble(), snap.ballVy.toDouble()).toFloat()
    val ns = min(speed / 4f, 1f)
    val f1 = sin(tick.toDouble() * 2.4).toFloat() * 0.15f
    val f2 = cos(tick.toDouble() * 3.7).toFloat() * 0.10f
    val f3 = sin(tick.toDouble() * 5.1).toFloat() * 0.08f
    val f4 = cos(tick.toDouble() * 1.9).toFloat() * 0.12f

    // Trail
    if (ns > 0.12f) {
        val ma = if (speed > 0.05f) atan2(snap.ballVy.toDouble(), snap.ballVx.toDouble()).toFloat() else 0f
        listOf(Triple(2.0f, 0.9f, COrange.copy(0.3f * ns)), Triple(1.5f, 1.7f, Color.Red.copy(0.2f * ns)), Triple(1.0f, 2.4f, Color.Red.copy(0.1f * ns))).forEach { (s, d, c) ->
            val tc = Offset(cx - cos(ma) * r * d * ns, cy - sin(ma) * r * d * ns)
            drawCircle(Brush.radialGradient(listOf(c, Color.Transparent), center = tc, radius = r * s / 2), r * s / 2, tc)
        }
    }

    // Glow
    drawCircle(Brush.radialGradient(listOf(Color.Red.copy(0.18f + ns * 0.12f), COrange.copy(0.08f), Color.Transparent), Offset(cx, cy), r * (2.5f + f1)), r * (2.5f + f1), Offset(cx, cy))

    // Flame tips
    listOf(
        Pair(Offset(cx + f1 * r * 1.8f - r * 0.2f, cy - r * 0.6f + f3 * r * 1.5f), COrange.copy(0.5f)),
        Pair(Offset(cx + r * 0.3f + f2 * r * 1.5f, cy - r * 0.5f + f1 * r * 1.2f), Color.Yellow.copy(0.4f)),
        Pair(Offset(cx + f4 * r * 1.2f,             cy - r * 0.7f + f2 * r * 0.8f), COrange.copy(0.35f))
    ).forEach { (center, c) ->
        drawCircle(Brush.radialGradient(listOf(c, Color.Transparent), center = center, radius = r * 0.4f), r * 0.4f, center)
    }

    // Main body
    drawCircle(Brush.radialGradient(listOf(CFireRed.copy(0.95f), COrange.copy(0.7f), Color.Red.copy(0.4f), Color.Transparent), Offset(cx, cy + f1 * r * 0.12f), r * (1.15f + f2)), r * (1.15f + f2), Offset(cx, cy + f1 * r * 0.12f))

    // Inner
    drawCircle(Brush.radialGradient(listOf(Color.Yellow.copy(0.95f), Color(0xFFFFA500).copy(0.65f), COrange.copy(0.25f), Color.Transparent), Offset(cx + f3 * r * 0.1f, cy), r * (0.8f + f1 * 0.12f)), r * (0.8f + f1 * 0.12f), Offset(cx + f3 * r * 0.1f, cy))

    // Core
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(0.95f + ns * 0.05f), Color(0xFFFFFACD).copy(0.7f), Color.Yellow.copy(0.3f), Color.Transparent), Offset(cx, cy), r * (0.5f + ns * 0.12f)), r * (0.5f + ns * 0.12f), Offset(cx, cy))
}

private fun DrawScope.drawExtBall(snap: TiltMazeSnapshot) {
    if (snap.ballRadius <= 0f) return
    drawCircle(Brush.radialGradient(listOf(Color.Gray.copy(0.5f), Color.Gray.copy(0.15f), Color.Transparent), Offset(snap.ballX, snap.ballY), snap.ballRadius * 0.8f), snap.ballRadius * 1.1f, Offset(snap.ballX, snap.ballY))
}

// ─────────────────────────────────────────────────────────────────────────────
// Heat Bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeatBar(maxHeat: Float, flameTick: Float) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp).height(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.LocalFireDepartment, null,
            tint = if (maxHeat > 0.6f) Color.Red else COrange.copy(0.5f),
            modifier = Modifier.size(12.dp))

        Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(Color.White.copy(0.08f))) {
            val heatColors = when {
                maxHeat >= 0.7f -> listOf(COrange, Color.Red, Color(0xFFFF2222))
                maxHeat >= 0.4f -> listOf(Color.Yellow, COrange, Color.Red.copy(0.7f))
                else            -> listOf(Color.Yellow.copy(0.7f), COrange.copy(0.6f))
            }
            Box(Modifier.fillMaxHeight().fillMaxWidth(maxHeat.coerceIn(0f, 1f)).clip(CircleShape).background(Brush.horizontalGradient(heatColors)))
        }

        if (maxHeat > 0.7f) {
            val pulse = (0.6f + sin(flameTick.toDouble() * 4).toFloat() * 0.4f).coerceIn(0f, 1f)
            Text("DANGER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Red.copy(pulse))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tutorial Overlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TutorialOverlay(boardSizeDp: Dp, flameTick: Float, onDismiss: () -> Unit) {
    val arrowAnim = rememberInfiniteTransition(label = "arr")
    val arrowRot by arrowAnim.animateFloat(-8f, 8f, infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ar")
    val tapScale by arrowAnim.animateFloat(1f, 1.04f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "ts")

    Box(
        modifier = Modifier
            .size(boardSizeDp)
            .background(Color.Black.copy(0.72f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Text("How to Play", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)

            Box(Modifier.size(boardSizeDp * 0.42f), contentAlignment = Alignment.Center) {
                Box(Modifier.size(boardSizeDp * 0.42f).border(2.dp, ACyan.copy(0.15f), CircleShape))
                Icon(Icons.Default.Refresh, null, tint = ACyan,
                    modifier = Modifier.size(boardSizeDp * 0.095f).offset(x = -(boardSizeDp * 0.14f)).rotate(arrowRot))
                Icon(Icons.Default.Refresh, null, tint = APurp,
                    modifier = Modifier.size(boardSizeDp * 0.095f).offset(x = boardSizeDp * 0.14f).rotate(-arrowRot))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Rotate the entire maze", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text("Drag anywhere on the board\nto tilt and guide the 🔥 to 💧",
                    fontSize = 12.sp, color = Color.White.copy(0.55f), textAlign = TextAlign.Center, lineHeight = 18.sp)
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(ACyan.copy(0.08f))
                    .border(1.dp, ACyan.copy(0.25f), CircleShape)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .graphicsLayer(scaleX = tapScale, scaleY = tapScale)
            ) {
                Text("Tap to start", fontSize = 11.sp, color = ACyan.copy(0.7f))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Meltdown Overlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MeltdownOverlay(flameTick: Float, showButtons: Boolean, onTryAgain: () -> Unit, onMenu: () -> Unit) {
    val pulse = (1f + sin(flameTick.toDouble() * 3).toFloat() * 0.06f)
    Box(
        Modifier.fillMaxSize().background(Color.Red.copy((0.25f + sin(flameTick.toDouble() * 5).toFloat() * 0.1f).coerceIn(0.15f, 0.5f))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(24.dp)) {
            Icon(Icons.Default.LocalFireDepartment, null, tint = Color.Yellow,
                modifier = Modifier.size(52.dp).graphicsLayer(scaleX = pulse, scaleY = pulse))

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("OVERHEATED!", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text("The fire ball couldn't keep moving", fontSize = 12.sp, color = Color.White.copy(0.55f), textAlign = TextAlign.Center)
            }

            if (showButtons) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    MeltBtn("Try Again", Icons.Default.Refresh, ACyan, onTryAgain)
                    MeltBtn("Menu", Icons.Default.List, APurp, onMenu)
                }
            } else {
                CircularProgressIndicator(color = Color.White.copy(0.4f), modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun MeltBtn(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    OutlinedButton(onClick, border = BorderStroke(1.2.dp, color.copy(0.6f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        modifier = Modifier.background(color.copy(0.08f), CircleShape)
    ) {
        Icon(icon, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
