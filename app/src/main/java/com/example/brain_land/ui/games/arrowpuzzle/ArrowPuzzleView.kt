package com.example.brain_land.ui.games.arrowpuzzle

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.example.brain_land.ui.games.arrowpuzzle.PCEngine.simulateMove
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import org.json.JSONObject

// ─────────────────────────────────────────────────────────────────────────────
// ArrowPuzzleViewModel — Manages state and animation
// (Combines iOS PathClearingGame ObservableObject logic)
// ─────────────────────────────────────────────────────────────────────────────

class ArrowPuzzleViewModel {
    var grid            by mutableStateOf(PCGrid(13, 13))
    var streams         by mutableStateOf<List<PCPathStream>>(emptyList())
    var exitedTrails    by mutableStateOf<List<TrailDot>>(emptyList())
    
    var levelNumber     by mutableIntStateOf(1)
    var moveCount       by mutableIntStateOf(0)
    var isSolved        by mutableStateOf(false)
    var isGenerating    by mutableStateOf(false)
    
    var activeStreamId  by mutableStateOf<String?>(null)
    var blockedStreamId by mutableStateOf<String?>(null)
    var isAnimating     by mutableStateOf(false)
    
    private val slideIntervalMs = 25L

    fun resetLevel() {
        streams.forEach { it.reset() }
        activeStreamId  = null
        blockedStreamId = null
        isAnimating     = false
        moveCount       = 0
        isSolved        = false
        exitedTrails    = emptyList()
    }

    suspend fun generateNewLevel(levelNum: Int) {
        levelNumber = levelNum
        isGenerating = true
        isSolved = false
        activeStreamId = null
        blockedStreamId = null
        moveCount = 0
        exitedTrails = emptyList()

        try {
            // First try to fetch from API
            println("🎮 [ArrowPuzzle] Fetching level $levelNumber from backend...")
            val response = withContext(Dispatchers.IO) {
                // Not using Retrofit for simplicity, just a raw URL fetch
                val url = URL("https://us-central1-mini-games-9a4e1.cloudfunctions.net/getArrowPuzzleLevels?level=$levelNumber")
                url.readText()
            }
            
            val apiObj = JSONObject(response)
            if (apiObj.getBoolean("success")) {
                val levelObj = apiObj.getJSONObject("level")
                val gridObj = levelObj.getJSONObject("grid")
                val c = gridObj.getInt("cols")
                val r = gridObj.getInt("rows")
                
                // Active cells
                var activeCellsMask: Set<PCCell>? = null
                if (levelObj.has("activeCells") && !levelObj.isNull("activeCells")) {
                    val acArray = levelObj.getJSONArray("activeCells")
                    val set = mutableSetOf<PCCell>()
                    for (i in 0 until acArray.length()) {
                        val cellObj = acArray.getJSONObject(i)
                        set.add(PCCell(cellObj.getInt("x"), cellObj.getInt("y")))
                    }
                    if (set.isNotEmpty()) activeCellsMask = set
                }

                // Streams
                val sArray = levelObj.getJSONArray("streams")
                val streamList = mutableListOf<PCPathStream>()
                for (i in 0 until sArray.length()) {
                    val sObj = sArray.getJSONObject(i)
                    val cellsArray = sObj.getJSONArray("cells")
                    val cellsList = mutableListOf<PCCell>()
                    for (j in 0 until cellsArray.length()) {
                        val cellObj = cellsArray.getJSONObject(j)
                        cellsList.add(PCCell(cellObj.getInt("x"), cellObj.getInt("y")))
                    }
                    streamList.add(
                        PCPathStream(
                            id = sObj.getString("id"),
                            label = sObj.getString("label"),
                            neonColor = findNeonColor(sObj.getString("color")),
                            direction = PCDirection.from(sObj.getString("direction")),
                            initialCells = cellsList
                        )
                    )
                }
                
                grid = PCGrid(c, r, activeCellsMask)
                streams = streamList
                isGenerating = false
                return
            }
        } catch (e: Exception) {
            println("🎮 [ArrowPuzzle] Fetch error: ${e.message} -> Using Local Generator!")
        }

        // Fallback to local generation
        val fallbackGrid = PCGrid(13, 13)
        val levelData = PCEngine.generateVerifiedLevel(fallbackGrid, levelNumber)
        
        if (levelData != null) {
            grid = fallbackGrid
            streams = levelData.paths.map { p ->
                PCPathStream(p.id, p.label, pcNeonPalette[p.colorIndex], p.direction, p.cells)
            }
        }
        isGenerating = false
    }

    private fun exportPathData(): List<PCPathData> = streams.map { s ->
        PCPathData(
            id = s.id, label = s.label,
            colorIndex = pcNeonPalette.indexOfFirst { it.hex == s.neonColor.hex }.coerceAtLeast(0),
            direction = s.direction, cells = s.cells.toList(), exited = s.exited
        )
    }

    suspend fun tapStream(stream: PCPathStream) {
        if (isAnimating || stream.exited || isSolved) return

        val pathData = exportPathData()
        val movingIndex = pathData.indexOfFirst { it.id == stream.id }
        if (movingIndex == -1) return

        val nextHead = PCEngine.nextHeadCell(pathData[movingIndex])

        if (PCEngine.isCandidateBlocked(pathData, movingIndex, nextHead, grid)) {
            // Blocked immediately
            triggerBounce(stream.id)
            return
        }

        val originalCells = stream.cells.toList()
        activeStreamId = stream.id
        isAnimating = true

        val rectGrid = PCGrid(grid.cols, grid.rows)

        while (true) {
            val currentPathData = exportPathData()
            val mIdx = currentPathData.indexOfFirst { it.id == stream.id }
            if (mIdx == -1) {
                revertAndFinish(stream, originalCells)
                break
            }

            val nHead = PCEngine.nextHeadCell(currentPathData[mIdx])
            val nHeadInRect = rectGrid.isInside(nHead)

            // Collision check
            if (nHeadInRect && PCEngine.isCandidateBlocked(currentPathData, mIdx, nHead, grid)) {
                revertAndFinish(stream, originalCells)
                break
            }

            // Assign nextHead so the View knows where to extend the line
            stream.nextHead = nHead

            // Smoothly animate the slither fraction from 0f to 1f over the slide interval
            val anim = androidx.compose.animation.core.Animatable(0f)
            anim.animateTo(
                targetValue = 1f,
                animationSpec = tween(slideIntervalMs.toInt(), easing = LinearEasing)
            ) {
                stream.slitherFraction = this.value
            }

            // Once the slide completes, snap the actual cells array forward
            stream.cells = PCEngine.slitherCells(stream.cells, nHead)
            stream.slitherFraction = 0f
            stream.nextHead = null
            streams = streams.toList() // Force recomposition

            // Exit check
            if (!PCEngine.hasVisibleCells(stream.cells, rectGrid)) {
                stream.exited = true
                moveCount++
                exitedTrails = exitedTrails + TrailDot(originalCells, stream.color)
                isAnimating = false
                activeStreamId = null
                resolveAfterMove()
                break
            }
        }
    }

    private suspend fun revertAndFinish(stream: PCPathStream, originalCells: List<PCCell>) {
        stream.cells = originalCells
        stream.slitherFraction = 0f
        stream.nextHead = null
        streams = streams.toList()
        triggerBounce(stream.id)
        delay(500)
        withContext(Dispatchers.Main) {
            isAnimating = false
            activeStreamId = null
        }
    }

    private suspend fun triggerBounce(streamId: String) {
        blockedStreamId = streamId
        delay(500)
        if (blockedStreamId == streamId) blockedStreamId = null
    }

    private fun resolveAfterMove() {
        if (streams.all { it.exited }) {
            isSolved = true
        }
    }

    fun streamAt(cell: PCCell): PCPathStream? {
        return streams.firstOrNull { !it.exited && cell in it.cells }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ArrowPuzzleView — Draws the grid & streams
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ArrowPuzzleView(
    viewModel: ArrowPuzzleViewModel,
    modifier: Modifier = Modifier
) {
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()
    
    // Animate "shake" offsets for all streams mapped by ID
    // Replaces the individual @State shakeOffset from iOS
    val bounceOffsets = viewModel.streams.associate { it.id to animateFloatAsState(
        targetValue = if (viewModel.blockedStreamId == it.id) 8f else 0f,
        animationSpec = spring(dampingRatio = 0.2f, stiffness = 1500f),
        label = "shake_${it.id}"
    ).value }
    
    val activeScales = viewModel.streams.associate { it.id to animateFloatAsState(
        targetValue = if (viewModel.activeStreamId == it.id) 1.02f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "scale_${it.id}"
    ).value }

    Box(
        modifier = modifier
            .onSizeChanged { boardSize = it }
            .pointerInput(Unit) {
                // Tap
                detectTapGestures { offset ->
                    if (boardSize.width == 0 || viewModel.grid.cols == 0) return@detectTapGestures
                    val cellSize = calculateCellSize(boardSize, viewModel.grid)
                    val c = (offset.x / cellSize).toInt()
                    val r = (offset.y / cellSize).toInt()
                    viewModel.streamAt(PCCell(c, r))?.let { stream ->
                        scope.launch { viewModel.tapStream(stream) }
                    }
                }
            }
            .pointerInput(Unit) {
                // Optional swipe/drag support -> map to tap
                detectDragGestures { change, _ ->
                    if (boardSize.width == 0 || viewModel.grid.cols == 0) return@detectDragGestures
                    val cellSize = calculateCellSize(boardSize, viewModel.grid)
                    val c = (change.position.x / cellSize).toInt()
                    val r = (change.position.y / cellSize).toInt()
                    viewModel.streamAt(PCCell(c, r))?.let { stream ->
                        if (!viewModel.isAnimating && viewModel.activeStreamId != stream.id) {
                            scope.launch { viewModel.tapStream(stream) }
                        }
                    }
                }
            }
    ) {
        if (boardSize.width > 0 && viewModel.grid.cols > 0) {
            val cellSize = calculateCellSize(boardSize, viewModel.grid)
            val gridW = cellSize * viewModel.grid.cols
            val gridH = cellSize * viewModel.grid.rows
            
            Canvas(
                modifier = Modifier
                    .size(width = androidx.compose.ui.platform.LocalDensity.current.run { gridW.toDp() }, 
                          height = androidx.compose.ui.platform.LocalDensity.current.run { gridH.toDp() })
            ) {
                // 1) Trail dots for exited
                for (trail in viewModel.exitedTrails) {
                    for (cell in trail.cells) {
                        val cx = cell.x * cellSize + cellSize / 2f
                        val cy = cell.y * cellSize + cellSize / 2f
                        val r = maxOf(2f, cellSize * 0.1f)
                        drawCircle(color = trail.color.copy(alpha = 0.35f), radius = r, center = Offset(cx, cy))
                    }
                }

                // 2) Active streams
                // iOS uses PathStreamShape which manages a discrete spring scale. We mimic it here.
                for (stream in viewModel.streams) {
                    if (stream.exited || stream.cells.isEmpty()) continue

                    val isActive = viewModel.activeStreamId == stream.id
                    val isBouncing = viewModel.blockedStreamId == stream.id
                    val shakeX = bounceOffsets[stream.id] ?: 0f
                    val scale = activeScales[stream.id] ?: 1.0f

                    val lineWidth = maxOf(1.5f, cellSize * 0.12f) * scale
                    val glowWidth = cellSize * (if (isBouncing) 0.28f else 0.22f) * scale
                    val arrowSize = maxOf(5f, cellSize * 0.38f) * scale

                    val streamCells = stream.cells
                    val nextCell = stream.nextHead
                    val f = stream.slitherFraction

                    val points = if (nextCell != null && streamCells.isNotEmpty()) {
                        val pts = mutableListOf<Offset>()
                        // Tail interpolation
                        if (streamCells.size > 1) {
                            val tailCur = streamCells[0]
                            val tailNext = streamCells[1]
                            val tailX = tailCur.x + (tailNext.x - tailCur.x) * f
                            val tailY = tailCur.y + (tailNext.y - tailCur.y) * f
                            pts.add(Offset(tailX * cellSize + cellSize / 2f + shakeX, tailY * cellSize + cellSize / 2f))
                        } else {
                            val tailCur = streamCells[0]
                            val tailX = tailCur.x + (nextCell.x - tailCur.x) * f
                            val tailY = tailCur.y + (nextCell.y - tailCur.y) * f
                            pts.add(Offset(tailX * cellSize + cellSize / 2f + shakeX, tailY * cellSize + cellSize / 2f))
                        }
                        
                        // Intermediate points
                        for (i in 1 until streamCells.size) {
                            val it = streamCells[i]
                            pts.add(Offset(it.x * cellSize + cellSize / 2f + shakeX, it.y * cellSize + cellSize / 2f))
                        }

                        // Head interpolation
                        val headCur = streamCells.last()
                        val headX = headCur.x + (nextCell.x - headCur.x) * f
                        val headY = headCur.y + (nextCell.y - headCur.y) * f
                        pts.add(Offset(headX * cellSize + cellSize / 2f + shakeX, headY * cellSize + cellSize / 2f))
                        pts
                    } else {
                        streamCells.map { 
                            Offset(
                                (it.x * cellSize + cellSize / 2f) + shakeX, 
                                (it.y * cellSize + cellSize / 2f)
                            ) 
                        }
                    }

                    // Pre-build path
                    val streamPath = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
                    }

                    // Glow line
                    val glowColor = if (isBouncing) Color.Red.copy(alpha = 0.5f) else stream.color.copy(alpha = 0.2f)
                    drawPath(
                        path = streamPath,
                        color = glowColor,
                        style = Stroke(width = glowWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Main solid line
                    drawPath(
                        path = streamPath,
                        color = stream.color,
                        style = Stroke(width = lineWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Arrowhead
                    val headPt = points.last()
                    val angle = stream.direction.angle

                    val arrowWing = arrowSize * 0.55f
                    val arrowDepth = arrowSize * 0.75f
                    val cosA = Math.cos(angle).toFloat()
                    val sinA = Math.sin(angle).toFloat()

                    val leftPt = Offset(
                        headPt.x - cosA * arrowDepth + sinA * arrowWing,
                        headPt.y - sinA * arrowDepth - cosA * arrowWing
                    )
                    val rightPt = Offset(
                        headPt.x - cosA * arrowDepth - sinA * arrowWing,
                        headPt.y - sinA * arrowDepth + cosA * arrowWing
                    )

                    val arrPath = Path().apply {
                        moveTo(leftPt.x, leftPt.y)
                        lineTo(headPt.x, headPt.y)
                        lineTo(rightPt.x, rightPt.y)
                    }
                    drawPath(
                        path = arrPath,
                        color = stream.color,
                        style = Stroke(width = lineWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }
    }
}

private fun calculateCellSize(boardSize: IntSize, grid: PCGrid): Float {
    if (grid.cols == 0 || grid.rows == 0) return 0f
    val cellW = boardSize.width.toFloat() / grid.cols
    val cellH = boardSize.height.toFloat() / grid.rows
    val raw = minOf(cellW, cellH)
    return raw.coerceIn(8f, 150f)
}
