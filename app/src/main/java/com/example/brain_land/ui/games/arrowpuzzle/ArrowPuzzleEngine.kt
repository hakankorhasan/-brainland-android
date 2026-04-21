package com.example.brain_land.ui.games.arrowpuzzle

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────────────────────
// Direction
// ─────────────────────────────────────────────────────────────────────────────

enum class PCDirection {
    UP, DOWN, LEFT, RIGHT;

    val dx: Int get() = when (this) { LEFT -> -1; RIGHT -> 1; else -> 0 }
    val dy: Int get() = when (this) { UP -> -1; DOWN -> 1; else -> 0 }

    /** Angle in radians for drawing the arrowhead */
    val angle: Double get() = when (this) {
        RIGHT -> 0.0
        DOWN  -> Math.PI / 2
        LEFT  -> Math.PI
        UP    -> -Math.PI / 2
    }

    companion object {
        fun from(s: String): PCDirection = when (s.lowercase()) {
            "up"    -> UP
            "down"  -> DOWN
            "left"  -> LEFT
            else    -> RIGHT
        }
        val all: List<PCDirection> = listOf(UP, DOWN, LEFT, RIGHT)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Cell
// ─────────────────────────────────────────────────────────────────────────────

data class PCCell(val x: Int, val y: Int) {
    fun advanced(dir: PCDirection) = PCCell(x + dir.dx, y + dir.dy)
    val key: String get() = "$x,$y"
}

// ─────────────────────────────────────────────────────────────────────────────
// Neon palette — 44 colours (mirrors iOS pcNeonPalette)
// ─────────────────────────────────────────────────────────────────────────────

data class PCNeonColor(val label: String, val color: Color, val hex: String)

val pcNeonPalette: List<PCNeonColor> = listOf(
    PCNeonColor("Cyan",          Color(0xFF29ECFF), "29ECFF"),
    PCNeonColor("Magenta",       Color(0xFFFF54DD), "FF54DD"),
    PCNeonColor("Lime",          Color(0xFFB8FF4E), "B8FF4E"),
    PCNeonColor("Orange",        Color(0xFFFF8C3B), "FF8C3B"),
    PCNeonColor("Violet",        Color(0xFF9E5CFF), "9E5CFF"),
    PCNeonColor("Yellow",        Color(0xFFFFE34D), "FFE34D"),
    PCNeonColor("Mint",          Color(0xFF52FFBF), "52FFBF"),
    PCNeonColor("Coral",         Color(0xFFFF6F61), "FF6F61"),
    PCNeonColor("Red",           Color(0xFFFF3366), "FF3366"),
    PCNeonColor("Sky Blue",      Color(0xFF00C9FF), "00C9FF"),
    PCNeonColor("Chartreuse",    Color(0xFFAAFF00), "AAFF00"),
    PCNeonColor("Fuchsia",       Color(0xFFFF00FF), "FF00FF"),
    PCNeonColor("Amber",         Color(0xFFFF9900), "FF9900"),
    PCNeonColor("Spring Green",  Color(0xFF00FF99), "00FF99"),
    PCNeonColor("Hot Pink",      Color(0xFFFF6B9D), "FF6B9D"),
    PCNeonColor("Emerald",       Color(0xFF06D6A0), "06D6A0"),
    PCNeonColor("Cerulean",      Color(0xFF118AB2), "118AB2"),
    PCNeonColor("Crimson",       Color(0xFFEF476F), "EF476F"),
    PCNeonColor("Golden",        Color(0xFFFFD166), "FFD166"),
    PCNeonColor("Deep Purple",   Color(0xFF7400B8), "7400B8"),
    PCNeonColor("Olive",         Color(0xFF80B918), "80B918"),
    PCNeonColor("Ocean Blue",    Color(0xFF0077B6), "0077B6"),
    PCNeonColor("Tangerine",     Color(0xFFF18F01), "F18F01"),
    PCNeonColor("Lavender",      Color(0xFFC77DFF), "C77DFF"),
    PCNeonColor("Imperial Red",  Color(0xFFE63946), "E63946"),
    PCNeonColor("Steel Blue",    Color(0xFF457B9D), "457B9D"),
    PCNeonColor("Persian Green", Color(0xFF2A9D8F), "2A9D8F"),
    PCNeonColor("Saffron",       Color(0xFFE9C46A), "E9C46A"),
    PCNeonColor("Sandy Brown",   Color(0xFFF4A261), "F4A261"),
    PCNeonColor("Charcoal",      Color(0xFF264653), "264653"),
    PCNeonColor("Non-photo Blue",Color(0xFF48BFE3), "48BFE3"),
    PCNeonColor("Sky Crayola",   Color(0xFF56CFE1), "56CFE1"),
    PCNeonColor("Blue Green",    Color(0xFF72EFDD), "72EFDD"),
    PCNeonColor("Cornflower",    Color(0xFF5390D9), "5390D9"),
    PCNeonColor("Purple",        Color(0xFF7B2CBF), "7B2CBF"),
    PCNeonColor("Rose",          Color(0xFFF72585), "F72585"),
    PCNeonColor("Byzantine",     Color(0xFFB5179E), "B5179E"),
    PCNeonColor("Purple 2",      Color(0xFF560BAD), "560BAD"),
    PCNeonColor("Indigo",        Color(0xFF480CA8), "480CA8"),
    PCNeonColor("Persian Blue",  Color(0xFF3A0CA3), "3A0CA3"),
    PCNeonColor("Ultramarine",   Color(0xFF3F37C9), "3F37C9"),
    PCNeonColor("Royal Blue",    Color(0xFF4361EE), "4361EE"),
    PCNeonColor("Cornflower 2",  Color(0xFF4895EF), "4895EF"),
    PCNeonColor("Sky Blue 2",    Color(0xFF4CC9F0), "4CC9F0"),
)

fun findNeonColor(hex: String): PCNeonColor {
    val clean = hex.removePrefix("#").uppercase()
    return pcNeonPalette.firstOrNull { it.hex.uppercase() == clean }
        ?: PCNeonColor("Custom", Color(0xFF29ECFF), clean)
}

// ─────────────────────────────────────────────────────────────────────────────
// PathStream (mutable game piece) — mirrors iOS PCPathStream
// ─────────────────────────────────────────────────────────────────────────────

class PCPathStream(
    val id:           String,
    val label:        String,
    val neonColor:    PCNeonColor,
    val direction:    PCDirection,
    val initialCells: List<PCCell>
) {
    var cells:  List<PCCell> = initialCells.toList()
    var exited: Boolean = false

    val head: PCCell get() = cells.last()
    val color: Color get() = neonColor.color

    fun reset() {
        cells  = initialCells.toList()
        exited = false
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Lightweight snapshot for solver / collision logic — value type
// ─────────────────────────────────────────────────────────────────────────────

data class PCPathData(
    val id:         String,
    val label:      String,
    val colorIndex: Int,
    val direction:  PCDirection,
    val cells:      List<PCCell>,
    val exited:     Boolean
)

// ─────────────────────────────────────────────────────────────────────────────
// Grid
// ─────────────────────────────────────────────────────────────────────────────

data class PCGrid(val cols: Int, val rows: Int, val activeCells: Set<PCCell>? = null) {
    fun isInside(cell: PCCell): Boolean =
        if (activeCells != null) cell in activeCells
        else cell.x in 0 until cols && cell.y in 0 until rows
}

// ─────────────────────────────────────────────────────────────────────────────
// Engine — pure static helpers (mirrors iOS PCEngine)
// ─────────────────────────────────────────────────────────────────────────────

object PCEngine {

    // ── Cell ops ──────────────────────────────────────────────────────────────

    fun nextHeadCell(path: PCPathData): PCCell = path.cells.last().advanced(path.direction)

    fun slitherCells(cells: List<PCCell>, nextHead: PCCell): List<PCCell> =
        cells.drop(1) + nextHead

    fun visibleCells(cells: List<PCCell>, grid: PCGrid): List<PCCell> =
        cells.filter { grid.isInside(it) }

    fun hasVisibleCells(cells: List<PCCell>, grid: PCGrid): Boolean =
        cells.any { grid.isInside(it) }

    // ── Collision check ────────────────────────────────────────────────────────

    fun isCandidateBlocked(
        paths: List<PCPathData>,
        movingIndex: Int,
        nextHead: PCCell,
        grid: PCGrid
    ): Boolean {
        if (!grid.isInside(nextHead)) return false
        val occupied = mutableSetOf<String>()
        for ((i, path) in paths.withIndex()) {
            if (path.exited) continue
            if (i == movingIndex) {
                // Skip tail cell — it will slide away
                path.cells.drop(1).filter { grid.isInside(it) }.forEach { occupied.add(it.key) }
            } else {
                visibleCells(path.cells, grid).forEach { occupied.add(it.key) }
            }
        }
        return nextHead.key in occupied
    }

    // ── Simulate a full move ───────────────────────────────────────────────────

    data class SimResult(val paths: List<PCPathData>, val exitedPathId: String?, val stepCount: Int)

    fun simulateMove(paths: List<PCPathData>, movingIndex: Int, grid: PCGrid): SimResult? {
        if (movingIndex >= paths.size || paths[movingIndex].exited) return null
        val state = paths.toMutableList()
        var moved = 0
        while (true) {
            val nextHead = nextHeadCell(state[movingIndex])
            if (isCandidateBlocked(state, movingIndex, nextHead, grid)) break
            state[movingIndex] = state[movingIndex].copy(
                cells = slitherCells(state[movingIndex].cells, nextHead)
            )
            moved++
            if (!hasVisibleCells(state[movingIndex].cells, grid)) {
                state[movingIndex] = state[movingIndex].copy(exited = true)
                break
            }
        }
        if (moved == 0 || !state[movingIndex].exited) return null
        return SimResult(state, state[movingIndex].id, moved)
    }

    // ── State encoding for DFS memoisation ────────────────────────────────────

    fun encodeState(paths: List<PCPathData>): String =
        paths.joinToString(";") { p ->
            if (p.exited) "${p.id}:X"
            else "${p.id}:" + p.cells.joinToString("|") { "${it.x}.${it.y}" }
        }

    // ── DFS Solver ─────────────────────────────────────────────────────────────

    fun solveLevel(paths: List<PCPathData>, grid: PCGrid, depthLimit: Int = 28): List<String>? {
        val visited = mutableSetOf<String>()

        fun dfs(state: List<PCPathData>, depth: Int): List<String>? {
            if (state.all { it.exited }) return emptyList()
            if (depth >= depthLimit) return null
            val key = encodeState(state)
            if (key in visited) return null
            visited.add(key)

            val moves = mutableListOf<Pair<String, SimResult>>()
            for ((i, path) in state.withIndex()) {
                val result = simulateMove(state, i, grid) ?: continue
                moves.add(path.id to result)
            }
            // Prefer exits first, then by step count
            moves.sortWith(compareByDescending<Pair<String, SimResult>> { it.second.exitedPathId != null }
                .thenByDescending { it.second.stepCount })

            for ((id, result) in moves) {
                val suffix = dfs(result.paths, depth + 1) ?: continue
                return listOf(id) + suffix
            }
            return null
        }

        return dfs(paths, 0)
    }

    // ── Level generation helpers ───────────────────────────────────────────────

    private fun countTurns(cells: List<PCCell>): Int {
        var turns = 0
        for (i in 2 until cells.size) {
            val ab = cells[i - 1].x - cells[i - 2].x to cells[i - 1].y - cells[i - 2].y
            val bc = cells[i].x - cells[i - 1].x to cells[i].y - cells[i - 1].y
            if (ab != bc) turns++
        }
        return turns
    }

    private fun pathSpan(cells: List<PCCell>): Int {
        val xs = cells.map { it.x }
        val ys = cells.map { it.y }
        return (xs.max() - xs.min()) + (ys.max() - ys.min())
    }

    private fun headCandidate(direction: PCDirection, grid: PCGrid): PCCell {
        val edgeInset = 1
        return when (direction) {
            PCDirection.RIGHT -> PCCell(
                (grid.cols - 4).coerceAtLeast(0)..(grid.cols - 2).coerceAtLeast(0),
                edgeInset..(grid.rows - 1 - edgeInset).coerceAtLeast(edgeInset)
            )
            PCDirection.LEFT  -> PCCell(
                1..3.coerceAtMost(grid.cols - 1),
                edgeInset..(grid.rows - 1 - edgeInset).coerceAtLeast(edgeInset)
            )
            PCDirection.UP    -> PCCell(
                edgeInset..(grid.cols - 1 - edgeInset).coerceAtLeast(edgeInset),
                1..3.coerceAtMost(grid.rows - 1)
            )
            PCDirection.DOWN  -> PCCell(
                edgeInset..(grid.cols - 1 - edgeInset).coerceAtLeast(edgeInset),
                (grid.rows - 4).coerceAtLeast(0)..(grid.rows - 2).coerceAtLeast(0)
            )
        }
    }

    private fun IntRange.random(): Int = if (isEmpty()) first else (first..last).random()

    private fun PCCell(xRange: IntRange, yRange: IntRange): PCCell =
        PCCell(xRange.random(), yRange.random())

    private fun respectsHeadExtremity(candidate: PCCell, head: PCCell, direction: PCDirection): Boolean =
        when (direction) {
            PCDirection.RIGHT -> candidate.x < head.x
            PCDirection.LEFT  -> candidate.x > head.x
            PCDirection.UP    -> candidate.y > head.y
            PCDirection.DOWN  -> candidate.y < head.y
        }

    private fun growthOptions(
        path: List<PCCell>, head: PCCell, direction: PCDirection,
        occupied: Set<String>, grid: PCGrid
    ): List<PCCell> {
        val tail = path.first()
        val local = path.map { it.key }.toSet()
        return listOf(
            PCCell(tail.x + 1, tail.y), PCCell(tail.x - 1, tail.y),
            PCCell(tail.x, tail.y + 1), PCCell(tail.x, tail.y - 1)
        ).filter { cell ->
            grid.isInside(cell) &&
                    respectsHeadExtremity(cell, head, direction) &&
                    cell.key !in occupied &&
                    cell.key !in local
        }
    }

    private fun pickGrowth(path: List<PCCell>, options: List<PCCell>): PCCell {
        if (options.size <= 1) return options.first()
        if (path.size < 2) return options.random()
        val tail = path.first()
        val next = path[1]
        val vec = tail.x - next.x to tail.y - next.y
        val straight = options.filter { tail.x - it.x == vec.first && tail.y - it.y == vec.second }
        val turns    = options.filter { it !in straight }
        if (turns.isNotEmpty() && Math.random() < 0.62) return turns.random()
        if (straight.isNotEmpty()) return straight.random()
        return options.random()
    }

    private fun buildHeadRayKeys(head: PCCell, direction: PCDirection, grid: PCGrid): Set<String> {
        val sweep = mutableSetOf<String>()
        var current = head
        while (true) {
            current = current.advanced(direction)
            if (!grid.isInside(current)) break
            sweep.add(current.key)
        }
        return sweep
    }

    private fun generatePathCandidate(
        grid: PCGrid, occupied: Set<String>,
        direction: PCDirection, id: String, label: String, colorIndex: Int
    ): PCPathData? {
        repeat(260) {
            val head = try { headCandidate(direction, grid) } catch (e: Exception) { return@repeat }
            if (head.key in occupied) return@repeat

            val targetLength = (10..19).random()
            val path = mutableListOf(head)
            while (path.size < targetLength) {
                val options = growthOptions(path, head, direction, occupied, grid)
                if (options.isEmpty()) break
                path.add(0, pickGrowth(path, options))
            }

            if (path.size < 8) return@repeat
            if (countTurns(path) < 3) return@repeat
            if (pathSpan(path) < 7) return@repeat

            val testPath = PCPathData(id, label, colorIndex, direction, path, false)
            val result = simulateMove(listOf(testPath), 0, grid) ?: return@repeat
            if (result.exitedPathId == null) return@repeat

            val headRay = buildHeadRayKeys(path.last(), direction, grid)
            val intersects = occupied.any { it in headRay }
            if (occupied.isNotEmpty() && !intersects && Math.random() < 0.68) return@repeat

            return PCPathData(id, label, colorIndex, direction, path, false)
        }
        return null
    }

    private fun immediateMoveCount(paths: List<PCPathData>, grid: PCGrid): Int =
        paths.indices.count { simulateMove(paths, it, grid) != null }

    // ── Public: generate a verified level ─────────────────────────────────────

    data class PCLevelData(val levelNumber: Int, val solution: List<String>, val paths: List<PCPathData>)

    suspend fun generateVerifiedLevel(grid: PCGrid, levelNumber: Int): PCLevelData? =
        withContext(Dispatchers.Default) {
            repeat(420) {
                val pathCount = (7..8).random()
                val shuffledColors = pcNeonPalette.indices.shuffled().take(pathCount)
                val occupied = mutableSetOf<String>()
                val placedInReverse = mutableListOf<PCPathData>()
                var failed = false

                for (i in 0 until pathCount) {
                    val streamIndex = pathCount - i
                    val colorIdx = shuffledColors[i]
                    var candidate: PCPathData? = null
                    for (dir in PCDirection.all.shuffled()) {
                        candidate = generatePathCandidate(
                            grid, occupied, dir,
                            "stream-$streamIndex", "Stream $streamIndex", colorIdx
                        )
                        if (candidate != null) break
                    }
                    if (candidate == null) { failed = true; return@repeat }
                    candidate.cells.forEach { occupied.add(it.key) }
                    placedInReverse.add(candidate)
                }

                if (failed) return@repeat

                val levelPaths = placedInReverse.reversed().mapIndexed { i, p ->
                    PCPathData("stream-${i + 1}", "Stream ${i + 1}", p.colorIndex, p.direction, p.cells, false)
                }

                val totalCells = levelPaths.sumOf { it.cells.size }
                if (totalCells < (grid.cols * grid.rows * 0.46).toInt()) return@repeat

                val solution = solveLevel(levelPaths, grid, depthLimit = 40) ?: return@repeat
                val immediateMoves = immediateMoveCount(levelPaths, grid)
                val hasDependency = immediateMoves < levelPaths.size
                val meaningfulSolution = solution.size >= maxOf(5, levelPaths.size - 1)

                if (meaningfulSolution && hasDependency) {
                    return@withContext PCLevelData(levelNumber, solution, levelPaths)
                }
            }
            null
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// TrailDot — original cell positions of exited streams (for ghost dots)
// ─────────────────────────────────────────────────────────────────────────────

data class TrailDot(val cells: List<PCCell>, val color: Color)

// ─────────────────────────────────────────────────────────────────────────────
// API models — mirrors iOS PathClearingGame inner structs
// ─────────────────────────────────────────────────────────────────────────────

data class APILevelResponse(val success: Boolean, val level: ArrowPuzzleAPILevel)
data class ArrowPuzzleAPILevel(
    val levelNumber: Int,
    val gameType: String,
    val difficulty: String,
    val difficultyScore: Int,
    val grid: APIGridSize,
    val shapeName: String?,
    val activeCells: List<APICellData>?,
    val streams: List<APIStreamData>,
    val solution: List<String>
)
data class APIGridSize(val cols: Int, val rows: Int)
data class APIStreamData(val id: String, val label: String, val color: String, val direction: String, val cells: List<APICellData>)
data class APICellData(val x: Int, val y: Int)
