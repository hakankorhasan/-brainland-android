package com.example.brain_land.ui.games.neurallink

import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// ─────────────────────────────────────────────────────────────────────────────
// Data Models
// ─────────────────────────────────────────────────────────────────────────────

data class NeuralCell(
    val row: Int,
    val col: Int,
    var flowIndex: Int? = null,   // which flow this cell belongs to
    var isEndpoint: Boolean = false,
    var isDead: Boolean = false
)

data class GridSnapshot(
    val cells: List<List<NeuralCell>>,
    val paths: List<List<Pair<Int, Int>>>,
    val completedFlows: Set<Int>,
    val moveCount: Int,
    val lockedPathLengths: List<Int>
)

// Neon color palette — mirrors iOS neuralColors
val neuralColors = listOf(
    0xFF00E5FF.toLong(),   // cyan
    0xFFFF3399.toLong(),   // magenta/pink
    0xFF33FF66.toLong(),   // neon green
    0xFFFF9900.toLong(),   // electric orange
    0xFFBB44FF.toLong(),   // purple
    0xFFFFE633.toLong(),   // gold/yellow
    0xFF33B3E6.toLong(),   // teal
    0xFFFF6666.toLong(),   // coral
)

// ─────────────────────────────────────────────────────────────────────────────
// NeuralLinkEngine — mirrors NeuralLinkGrid.swift
// ─────────────────────────────────────────────────────────────────────────────

class NeuralLinkEngine(
    private val scope: CoroutineScope,
    initialLevel: Int = 1
) {
    var levelNumber  by mutableStateOf(initialLevel)
    var gridSize     by mutableStateOf(5)
    var flowCount    by mutableStateOf(3)
    var cells        by mutableStateOf<List<List<NeuralCell>>>(emptyList())
    var paths        by mutableStateOf<List<List<Pair<Int, Int>>>>(emptyList())
    var endpoints    by mutableStateOf<List<Pair<Pair<Int,Int>, Pair<Int,Int>>>>(emptyList())
    var completedFlows by mutableStateOf<Set<Int>>(emptySet())
    var lockedPathLengths by mutableStateOf<List<Int>>(emptyList())
    var activeFlow   by mutableStateOf<Int?>(null)
    var isSolved     by mutableStateOf(false)
    var moveCount    by mutableStateOf(0)
    var canUndo      by mutableStateOf(false)
    var isLoading    by mutableStateOf(false)

    private var undoStack = mutableListOf<GridSnapshot>()
    private var preGestureSnapshot: GridSnapshot? = null
    private var stateChangedDuringGesture = false
    private var dragLocked = false

    private val baseURL = "https://us-central1-mini-games-9a4e1.cloudfunctions.net"

    // ── Public API ────────────────────────────────────────────────────────────

    fun loadLevel() {
        if (isLoading) return
        isLoading  = true
        isSolved   = false
        moveCount  = 0
        activeFlow = null
        completedFlows = emptySet()
        undoStack.clear()
        canUndo    = false

        scope.launch {
            val ok = fetchLevel()
            if (!ok) {
                withContext(Dispatchers.Main) { buildFallback(); isLoading = false }
            }
        }
    }

    fun nextLevel() { levelNumber++; loadLevel() }

    fun resetPuzzle() {
        // Rebuild from endpoints — set endpoint cells, clear everything else
        val newCells = cells.map { row ->
            row.map { c ->
                val epFi = if (c.isEndpoint) {
                    endpoints.indexOfFirst { e ->
                        (e.first.first == c.row && e.first.second == c.col) ||
                        (e.second.first == c.row && e.second.second == c.col)
                    }.takeIf { it >= 0 }
                } else null
                c.copy(flowIndex = epFi)
            }
        }
        cells = newCells
        paths = List(flowCount) { emptyList() }
        completedFlows = emptySet()
        lockedPathLengths = List(flowCount) { 0 }
        moveCount = 0
        isSolved  = false
        activeFlow = null
        undoStack.clear()
        canUndo = false
    }

    fun undoLastMove() {
        val snapshot = undoStack.removeLastOrNull() ?: return
        cells              = snapshot.cells
        paths              = snapshot.paths
        completedFlows     = snapshot.completedFlows
        moveCount          = snapshot.moveCount
        lockedPathLengths  = snapshot.lockedPathLengths
        isSolved           = false
        activeFlow         = null
        dragLocked         = false
        canUndo            = undoStack.isNotEmpty()
    }

    // ── Drag Handling ─────────────────────────────────────────────────────────

    fun beginDrag(row: Int, col: Int) {
        if (row < 0 || row >= gridSize || col < 0 || col >= gridSize) return
        if (isSolved || dragLocked) return
        val cell = cells[row][col]
        if (cell.isDead) return
        val fi = cell.flowIndex ?: return

        preGestureSnapshot = createSnapshot()
        stateChangedDuringGesture = false

        val newCells = cells.mutableCopy()
        val newPaths = paths.map { it.toMutableList() }.toMutableList()
        val newCompleted = completedFlows.toMutableSet()
        val newLocked = lockedPathLengths.toMutableList()

        when {
            completedFlows.contains(fi) -> {
                if (!cell.isEndpoint) return
                clearFlow(fi, newCells, newPaths)
                newCompleted.remove(fi)
                newPaths[fi] = mutableListOf(row to col)
                newCells[row][col] = newCells[row][col].copy(flowIndex = fi)
                newLocked[fi] = 0
                activeFlow = fi
                moveCount++
                stateChangedDuringGesture = true
            }
            newPaths[fi].isEmpty() && cell.isEndpoint -> {
                newPaths[fi] = mutableListOf(row to col)
                newLocked[fi] = 0
                activeFlow = fi
                moveCount++
                stateChangedDuringGesture = true
            }
            else -> { activeFlow = fi }
        }
        cells = newCells; paths = newPaths; completedFlows = newCompleted; lockedPathLengths = newLocked
    }

    fun continueDrag(row: Int, col: Int) {
        val fi = activeFlow ?: return
        if (row < 0 || row >= gridSize || col < 0 || col >= gridSize) return
        if (isSolved) return
        val targetCell = cells[row][col]
        if (targetCell.isDead) return

        val curPath = paths[fi]
        val last = curPath.lastOrNull() ?: return
        val dr = kotlin.math.abs(row - last.first)
        val dc = kotlin.math.abs(col - last.second)
        if (!((dr == 1 && dc == 0) || (dr == 0 && dc == 1))) return

        val newCells = cells.mutableCopy()
        val newPaths = paths.map { it.toMutableList() }.toMutableList()
        val newCompleted = completedFlows.toMutableSet()
        val newLocked = lockedPathLengths.toMutableList()

        // Backtrack
        if (newPaths[fi].size >= 2) {
            val prev = newPaths[fi][newPaths[fi].size - 2]
            if (prev.first == row && prev.second == col) {
                if (newPaths[fi].size > newLocked[fi]) {
                    val removed = newPaths[fi].removeLast()
                    if (!newCells[removed.first][removed.second].isEndpoint) {
                        newCells[removed.first][removed.second] = newCells[removed.first][removed.second].copy(flowIndex = null)
                    }
                    stateChangedDuringGesture = true
                }
                cells = newCells; paths = newPaths; lockedPathLengths = newLocked
                return
            }
        }

        val pathStart = newPaths[fi].firstOrNull() ?: return

        // Reached other endpoint — complete flow
        if (targetCell.isEndpoint && targetCell.flowIndex == fi &&
            !(row == pathStart.first && col == pathStart.second)) {
            newPaths[fi].add(row to col)
            newCells[row][col] = newCells[row][col].copy(flowIndex = fi)
            activeFlow = null
            dragLocked = true
            moveCount++
            newCompleted.add(fi)
            cells = newCells; paths = newPaths; completedFlows = newCompleted; lockedPathLengths = newLocked
            stateChangedDuringGesture = true
            checkWin()
            return
        }

        if (targetCell.flowIndex != null && targetCell.flowIndex != fi) return
        if (targetCell.isEndpoint && targetCell.flowIndex != fi) return
        if (newPaths[fi].any { it.first == row && it.second == col }) return

        newPaths[fi].add(row to col)
        newCells[row][col] = newCells[row][col].copy(flowIndex = fi)
        moveCount++
        stateChangedDuringGesture = true
        cells = newCells; paths = newPaths; lockedPathLengths = newLocked
    }

    fun endDrag() {
        if (stateChangedDuringGesture) {
            preGestureSnapshot?.let { undoStack.add(it) }
            if (undoStack.size > 20) undoStack.removeFirst()
            canUndo = true
        }
        val fi = activeFlow
        if (fi != null) {
            val newLocked = lockedPathLengths.toMutableList()
            newLocked[fi] = paths[fi].size
            lockedPathLengths = newLocked
        }
        activeFlow = null
        dragLocked = false
        preGestureSnapshot = null
        stateChangedDuringGesture = false
    }

    // ── Connection helpers ─────────────────────────────────────────────────────

    fun isConnected(r1: Int, c1: Int, r2: Int, c2: Int, fi: Int): Boolean {
        val path = paths.getOrNull(fi) ?: return false
        for (i in 0 until path.size - 1) {
            if ((path[i].first == r1 && path[i].second == c1 && path[i+1].first == r2 && path[i+1].second == c2) ||
                (path[i].first == r2 && path[i].second == c2 && path[i+1].first == r1 && path[i+1].second == c1))
                return true
        }
        return false
    }

    data class Directions(val up: Boolean, val down: Boolean, val left: Boolean, val right: Boolean)

    fun connectedDirections(row: Int, col: Int): Directions {
        val fi = cells.getOrNull(row)?.getOrNull(col)?.flowIndex ?: return Directions(false, false, false, false)
        val n  = gridSize
        return Directions(
            up    = row > 0   && isConnected(row, col, row-1, col, fi),
            down  = row < n-1 && isConnected(row, col, row+1, col, fi),
            left  = col > 0   && isConnected(row, col, row, col-1, fi),
            right = col < n-1 && isConnected(row, col, row, col+1, fi)
        )
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun clearFlow(fi: Int, cells: MutableList<MutableList<NeuralCell>>, paths: MutableList<MutableList<Pair<Int,Int>>>) {
        for ((r, c) in paths[fi]) {
            if (!cells[r][c].isEndpoint) cells[r][c] = cells[r][c].copy(flowIndex = null)
        }
        paths[fi].clear()
    }

    private fun checkWin() {
        val n = gridSize
        for (r in 0 until n) for (c in 0 until n) {
            if (!cells[r][c].isDead && cells[r][c].flowIndex == null) return
        }
        for (fi in 0 until flowCount) {
            val path = paths.getOrNull(fi) ?: return
            if (path.size < 2) return
            val ep = endpoints.getOrNull(fi) ?: return
            val first = path.first(); val last = path.last()
            val startsOk = (first == ep.first) || (first == ep.second)
            val endsOk   = (last  == ep.first) || (last  == ep.second)
            if (!startsOk || !endsOk || first == last) return
        }
        isSolved = true
    }

    private fun createSnapshot() = GridSnapshot(
        cells = cells.map { row -> row.map { it.copy() } },
        paths = paths.map { it.toList() },
        completedFlows = completedFlows.toSet(),
        moveCount = moveCount,
        lockedPathLengths = lockedPathLengths.toList()
    )

    private fun List<List<NeuralCell>>.mutableCopy() =
        this.map { it.map { c -> c.copy() }.toMutableList() }.toMutableList()

    // ── Network fetch ─────────────────────────────────────────────────────────

    private suspend fun fetchLevel(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseURL/getNeuralLinkLevels?level=$levelNumber")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"; connectTimeout = 10_000; readTimeout = 15_000
            }
            val raw = try { conn.inputStream.bufferedReader().readText() }
            catch (e: Exception) { conn.errorStream?.bufferedReader()?.readText() ?: throw e }
            finally { conn.disconnect() }

            val json = JSONObject(raw)
            if (!json.optBoolean("success", false)) {
                Log.w("NeuralLinkEngine", "API error: ${json.optString("error")}")
                return@withContext false
            }

            val lvl  = json.getJSONObject("level")
            val num  = lvl.optInt("levelNumber", levelNumber)
            val size = lvl.optInt("gridSize", 5)
            val fc   = lvl.optInt("flowCount", 3)

            val endpointsArr = lvl.optJSONArray("endpoints") ?: return@withContext false
            val deadArr      = lvl.optJSONArray("deadCells")

            val newCells = (0 until size).map { r -> (0 until size).map { c -> NeuralCell(r, c) }.toMutableList() }.toMutableList()

            // dead cells
            if (deadArr != null) {
                for (i in 0 until deadArr.length()) {
                    val d = deadArr.getJSONArray(i)
                    val dr = d.optInt(0); val dc = d.optInt(1)
                    if (dr in 0 until size && dc in 0 until size) newCells[dr][dc] = newCells[dr][dc].copy(isDead = true)
                }
            }

            // endpoints
            val epList = mutableListOf<Pair<Pair<Int,Int>, Pair<Int,Int>>>()
            for (fi in 0 until minOf(fc, endpointsArr.length())) {
                val pair = endpointsArr.getJSONArray(fi)
                if (pair.length() < 2) continue
                val a = pair.getJSONArray(0); val b = pair.getJSONArray(1)
                val r1 = a.optInt(0); val c1 = a.optInt(1)
                val r2 = b.optInt(0); val c2 = b.optInt(1)
                if (r1 in 0 until size && c1 in 0 until size) {
                    newCells[r1][c1] = newCells[r1][c1].copy(flowIndex = fi, isEndpoint = true)
                }
                if (r2 in 0 until size && c2 in 0 until size) {
                    newCells[r2][c2] = newCells[r2][c2].copy(flowIndex = fi, isEndpoint = true)
                }
                epList.add((r1 to c1) to (r2 to c2))
            }

            withContext(Dispatchers.Main) {
                levelNumber = num; gridSize = size; flowCount = fc
                cells = newCells; endpoints = epList
                paths = List(fc) { emptyList() }
                completedFlows = emptySet()
                lockedPathLengths = List(fc) { 0 }
                isLoading = false
            }
            true
        } catch (e: Exception) {
            Log.e("NeuralLinkEngine", "Fetch error: ${e.message}")
            withContext(Dispatchers.Main) { isLoading = false }
            false
        }
    }

    // ── Offline fallback ──────────────────────────────────────────────────────

    private fun buildFallback() {
        val n  = 5; val fc = 3
        gridSize = n; flowCount = fc
        val eps = listOf(
            (0 to 0) to (4 to 4),
            (0 to 4) to (4 to 0),
            (2 to 0) to (2 to 4)
        )
        val newCells = (0 until n).map { r -> (0 until n).map { c -> NeuralCell(r, c) }.toMutableList() }.toMutableList()
        for ((fi, ep) in eps.withIndex()) {
            val (a, b) = ep
            newCells[a.first][a.second] = newCells[a.first][a.second].copy(flowIndex = fi, isEndpoint = true)
            newCells[b.first][b.second] = newCells[b.first][b.second].copy(flowIndex = fi, isEndpoint = true)
        }
        cells = newCells; endpoints = eps
        paths = List(fc) { emptyList() }
        completedFlows = emptySet()
        lockedPathLengths = List(fc) { 0 }
    }
}
