package com.example.brain_land.ui.games.tiltmaze

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import kotlin.math.*

// ─────────────────────────────────────────────────────────────────────────────
// Data
// ─────────────────────────────────────────────────────────────────────────────

data class MazeCell(
    var northWall: Boolean = true,
    var southWall: Boolean = true,
    var eastWall: Boolean  = true,
    var westWall: Boolean  = true,
    var visited: Boolean   = false
)

data class Vec2(val x: Float, val y: Float) {
    operator fun plus(o: Vec2)  = Vec2(x + o.x, y + o.y)
    operator fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)
    operator fun times(s: Float) = Vec2(x * s, y * s)
    fun length() = sqrt(x * x + y * y)
    fun normalized() = if (length() > 0) Vec2(x / length(), y / length()) else Vec2(0f, 0f)
}

// ─────────────────────────────────────────────────────────────────────────────
// Level config — mirrors iOS configForLevel()
// ─────────────────────────────────────────────────────────────────────────────

fun tiltMazeSizeForLevel(level: Int): Int = when (level) {
    in 1..2   -> 4
    in 3..4   -> 5
    in 5..6   -> 6
    in 7..8   -> 7
    in 9..10  -> 8
    in 11..13 -> 9
    in 14..16 -> 10
    in 17..20 -> 11
    in 21..34 -> 12
    in 35..59 -> 14
    in 60..99 -> 16
    else      -> 18
}

fun tiltMazeBallRatio(size: Int): Float = when {
    size <= 6  -> 0.38f
    size <= 9  -> 0.35f
    size <= 12 -> 0.33f
    size <= 15 -> 0.31f
    else       -> 0.29f
}

fun tiltMazeHeatMult(level: Int): Float = min(0.7f + (level - 1) * 0.028f, 2.0f)

fun tiltMazeDifficulty(rows: Int): Int = when {
    rows <= 4  -> 1
    rows == 5  -> 2
    rows == 6  -> 3
    rows == 7  -> 4
    rows == 8  -> 5
    rows == 9  -> 6
    rows == 10 -> 7
    rows <= 12 -> 8
    rows <= 14 -> 9
    else       -> 10
}

// ─────────────────────────────────────────────────────────────────────────────
// Maze generator — recursive back-tracker (mirrors iOS generateMaze)
// ─────────────────────────────────────────────────────────────────────────────

fun generateMaze(rows: Int, cols: Int): Array<Array<MazeCell>> {
    val cells = Array(rows) { Array(cols) { MazeCell() } }
    val stack = ArrayDeque<Pair<Int, Int>>()
    cells[0][0].visited = true
    stack.addLast(0 to 0)

    while (stack.isNotEmpty()) {
        val (cr, cc) = stack.last()
        val neighbors = mutableListOf<Triple<Int, Int, Char>>()
        if (cr > 0           && !cells[cr-1][cc].visited) neighbors += Triple(cr-1, cc, 'N')
        if (cr < rows - 1    && !cells[cr+1][cc].visited) neighbors += Triple(cr+1, cc, 'S')
        if (cc > 0           && !cells[cr][cc-1].visited) neighbors += Triple(cr, cc-1, 'W')
        if (cc < cols - 1    && !cells[cr][cc+1].visited) neighbors += Triple(cr, cc+1, 'E')

        if (neighbors.isEmpty()) {
            stack.removeLast()
        } else {
            val (nr, nc, dir) = neighbors.random()
            when (dir) {
                'N' -> { cells[cr][cc].northWall = false; cells[nr][nc].southWall = false }
                'S' -> { cells[cr][cc].southWall = false; cells[nr][nc].northWall = false }
                'E' -> { cells[cr][cc].eastWall  = false; cells[nr][nc].westWall  = false }
                'W' -> { cells[cr][cc].westWall  = false; cells[nr][nc].eastWall  = false }
            }
            cells[nr][nc].visited = true
            stack.addLast(nr to nc)
        }
    }
    return cells
}

// ─────────────────────────────────────────────────────────────────────────────
// Wall rect builder
// ─────────────────────────────────────────────────────────────────────────────

fun buildWallRects(cells: Array<Array<MazeCell>>, cellSize: Float): List<Rect> {
    val rows = cells.size
    val cols = if (rows > 0) cells[0].size else 0
    val wt = 3f
    val hw = wt / 2f
    val bs = cols * cellSize
    val rects = mutableListOf<Rect>()

    // Border
    rects += Rect(0f, 0f, bs, wt)
    rects += Rect(0f, bs - wt, bs, bs)
    rects += Rect(0f, 0f, wt, bs)
    rects += Rect(bs - wt, 0f, bs, bs)

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val cell = cells[r][c]
            val x = c * cellSize
            val y = r * cellSize
            if (cell.southWall && r < rows - 1)
                rects += Rect(x, y + cellSize - hw, x + cellSize, y + cellSize - hw + wt)
            if (cell.eastWall  && c < cols - 1)
                rects += Rect(x + cellSize - hw, y, x + cellSize - hw + wt, y + cellSize)
        }
    }
    return rects
}

// ─────────────────────────────────────────────────────────────────────────────
// TiltMazeEngine — pure physics + heat, no Compose, run off the main thread
// ─────────────────────────────────────────────────────────────────────────────

class TiltMazeEngine(
    val rows: Int,
    val cols: Int,
    val cellSize: Float,
    val ballRadius: Float,
    val heatMultiplier: Float
) {
    // Physics constants — exactly mirrors iOS
    val gravityStrength     = 0.25f
    val friction            = 0.90f
    val wallThickness       = 3.0f
    val rotationDamping     = 0.45f

    // Heat constants
    val baseHeatIncreaseRate = 0.005f
    val baseHeatCoolingRate  = 0.012f
    val heatNaturalCooling   = 0.0008f
    val heatPropagation      = 0.001f
    val meltdownThreshold    = 1.0f
    val idleSpeedThreshold   = 0.3f

    val cells: Array<Array<MazeCell>> = generateMaze(rows, cols)
    val wallRects: List<Rect>         = buildWallRects(cells, cellSize)

    // Mutable state (all read/written from physics coroutine then snapshot to UI state)
    var ballPos  = Vec2(cellSize / 2, cellSize / 2)
    var ballVel  = Vec2(0f, 0f)
    var mazeAngle: Float = 0f // radians

    val heatGrid = Array(rows) { FloatArray(cols) }
    var maxHeat   = 0f
    var flamePhase = 0f
    var shakeX    = 0f
    var shakeY    = 0f
    var idleCount = 0

    // Goal position — bottom-right cell center
    val goalX = (cols - 1 + 0.5f) * cellSize
    val goalY = (rows - 1 + 0.5f) * cellSize

    fun stepPhysics() {
        val gx = sin(mazeAngle) * gravityStrength
        val gy = cos(mazeAngle) * gravityStrength
        ballVel = Vec2(ballVel.x + gx, ballVel.y + gy)
        ballVel = Vec2(ballVel.x * friction, ballVel.y * friction)

        val substeps = 4
        val sx = ballVel.x / substeps
        val sy = ballVel.y / substeps
        for (i in 0 until substeps) {
            ballPos = Vec2(ballPos.x + sx, ballPos.y + sy)
            for (rect in wallRects) resolveCollision(rect)
        }
    }

    private fun resolveCollision(wall: Rect) {
        val cx = ballPos.x.coerceIn(wall.left, wall.right)
        val cy = ballPos.y.coerceIn(wall.top,  wall.bottom)
        val dx = ballPos.x - cx
        val dy = ballPos.y - cy
        val distSq = dx * dx + dy * dy
        if (distSq < ballRadius * ballRadius && distSq > 0f) {
            val d = sqrt(distSq)
            val overlap = ballRadius - d
            val nx = dx / d
            val ny = dy / d
            ballPos = Vec2(ballPos.x + nx * overlap, ballPos.y + ny * overlap)
            val dot = ballVel.x * nx + ballVel.y * ny
            if (dot < 0f) ballVel = Vec2(ballVel.x - 1.1f * dot * nx, ballVel.y - 1.1f * dot * ny)
        } else if (distSq == 0f) {
            ballPos = Vec2(ballPos.x - ballVel.x * 0.1f, ballPos.y - ballVel.y * 0.1f)
        }
    }

    fun stepHeat() {
        val ballR = (ballPos.y / cellSize).toInt().coerceIn(0, rows - 1)
        val ballC = (ballPos.x / cellSize).toInt().coerceIn(0, cols - 1)
        val speed = hypot(ballVel.x.toDouble(), ballVel.y.toDouble()).toFloat()
        val isMoving = speed > idleSpeedThreshold

        if (!isMoving) {
            idleCount++
            val ramp = min(idleCount / 90f, 1f)
            for (r in 0 until rows) for (c in 0 until cols) {
                val dist = abs(r - ballR) + abs(c - ballC)
                if (dist <= 2) {
                    val factor = if (dist == 0) 1f else if (dist == 1) 0.55f else 0.2f
                    heatGrid[r][c] = min(1f, heatGrid[r][c] + baseHeatIncreaseRate * heatMultiplier * factor * ramp)
                }
            }
        } else { idleCount = 0 }

        val cooling = if (isMoving) baseHeatCoolingRate else heatNaturalCooling
        for (r in 0 until rows) for (c in 0 until cols) heatGrid[r][c] = max(0f, heatGrid[r][c] - cooling)

        // Propagation
        val newGrid = Array(rows) { r -> heatGrid[r].copyOf() }
        for (r in 0 until rows) for (c in 0 until cols) {
            val heat = heatGrid[r][c]
            if (heat > 0.15f) {
                val dirs = listOf(
                    Triple(r-1, c, !cells[r][c].northWall),
                    Triple(r+1, c, !cells[r][c].southWall),
                    Triple(r, c-1, !cells[r][c].westWall),
                    Triple(r, c+1, !cells[r][c].eastWall)
                )
                for ((nr, nc, open) in dirs) {
                    if (open && nr in 0 until rows && nc in 0 until cols) {
                        newGrid[nr][nc] = min(1f, newGrid[nr][nc] + heat * heatPropagation)
                    }
                }
            }
        }
        for (r in 0 until rows) for (c in 0 until cols) heatGrid[r][c] = newGrid[r][c]

        var mh = 0f
        for (r in 0 until rows) for (c in 0 until cols) mh = max(mh, heatGrid[r][c])
        maxHeat = mh

        if (mh > 0.85f) {
            val intensity = (mh - 0.85f) / 0.15f * 1.2f
            shakeX = (-intensity..intensity).random()
            shakeY = (-intensity..intensity).random()
        } else { shakeX = 0f; shakeY = 0f }
    }

    fun isWin(): Boolean {
        val dist = hypot((ballPos.x - goalX).toDouble(), (ballPos.y - goalY).toDouble()).toFloat()
        return dist < cellSize * 0.35f
    }

    private fun ClosedFloatingPointRange<Float>.random(): Float =
        start + (endInclusive - start) * kotlin.random.Random.nextFloat()
}

// ─────────────────────────────────────────────────────────────────────────────
// Snapshot passed to UI every frame
// ─────────────────────────────────────────────────────────────────────────────

data class TiltMazeSnapshot(
    val ballX: Float       = 0f,
    val ballY: Float       = 0f,
    val ballVx: Float      = 0f,
    val ballVy: Float      = 0f,
    val mazeAngleDeg: Float = 0f,
    val maxHeat: Float     = 0f,
    val heatGrid: Array<FloatArray> = emptyArray(),
    val flamePhase: Float  = 0f,
    val shakeX: Float      = 0f,
    val shakeY: Float      = 0f,
    val cellSize: Float    = 0f,
    val boardSize: Float   = 0f,
    val ballRadius: Float  = 0f,
    val rows: Int          = 0,
    val cols: Int          = 0
)
