package com.example.brain_land.ui.games.pipeconnect

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

class PipeConnectEngine(
    private val scope: CoroutineScope,
    initialLevel: Int = 1
) {
    var levelNumber by mutableStateOf(initialLevel)
    var size by mutableStateOf(4)
    var difficulty by mutableStateOf("tutorial")

    var cells by mutableStateOf<List<List<PipeCell>>>(emptyList())
    var isSolved by mutableStateOf(false)
    var isAnimatingWater by mutableStateOf(false)
    var moveCount by mutableStateOf(0)
    var lives by mutableStateOf(5)
    var isGameOver by mutableStateOf(false)
    var lastFlowResult by mutableStateOf<FlowResult?>(null)
    var isLoading by mutableStateOf(false)

    var sourceRow = -1
    var sourceCol = -1
    var sinkRow = -1
    var sinkCol = -1
    var sourceDirection = PipeDirection.LEFT
    var sinkDirection = PipeDirection.RIGHT

    private val baseURL = "https://us-central1-mini-games-9a4e1.cloudfunctions.net"

    fun loadLevel() {
        if (isLoading) return
        isLoading = true
        isSolved = false
        isAnimatingWater = false
        isGameOver = false
        moveCount = 0
        lastFlowResult = null
        cells = emptyList()

        scope.launch {
            val success = fetchAndParseLevel()
            if (!success) {
                // simple fallback
                cells = List(4) { r -> List(4) { c -> PipeCell(row = r, col = c, pipeType = PipeType.STRAIGHT, rotation = 0) } }
                size = 4
                lives = 5
                difficulty = "fallback"
                sourceRow = 2; sourceCol = -1; sinkRow = 2; sinkCol = 4
                sourceDirection = PipeDirection.RIGHT
                sinkDirection = PipeDirection.RIGHT
                isLoading = false
            }
        }
    }

    private suspend fun fetchAndParseLevel(): Boolean = withContext(Dispatchers.IO) {
        try {
            val urlString = "$baseURL/getPipeConnectLevels?level=$levelNumber"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000

            val raw = try {
                conn.inputStream.bufferedReader().readText()
            } catch (e: Exception) {
                conn.errorStream?.bufferedReader()?.readText() ?: throw e
            } finally {
                conn.disconnect()
            }

            val json = JSONObject(raw)
            if (!json.optBoolean("success", false)) {
                val err = json.optString("error")
                Log.w("PipeEngine", "API error: $err")
                return@withContext false
            }

            val level = json.getJSONObject("level")
            val gridSize = level.optInt("gridSize", 4)
            val livesCount = level.optInt("lives", 5)
            val diff = level.optString("difficulty", "tutorial")
            val srcRow = level.optInt("sourceRow", gridSize / 2)
            val srcCol = level.optInt("sourceCol", 0)
            val snkRow = level.optInt("sinkRow", gridSize / 2)
            val snkCol = level.optInt("sinkCol", gridSize - 1)
            val srcDirStr = level.optString("sourceDirection", "left")
            val snkDirStr = level.optString("sinkDirection", "right")

            val grid = MutableList(gridSize) { r ->
                MutableList(gridSize) { c ->
                    PipeCell(row = r, col = c, pipeType = PipeType.STRAIGHT, rotation = 0)
                }
            }

            val cellsArray = level.optJSONArray("cells")
            if (cellsArray != null) {
                for (i in 0 until cellsArray.length()) {
                    val cellData = cellsArray.optJSONObject(i) ?: continue
                    val r = cellData.optInt("row", 0)
                    val c = cellData.optInt("col", 0)
                    if (r < 0 || r >= gridSize || c < 0 || c >= gridSize) continue

                    val item = grid[r][c]
                    item.pipeType = PipeType.from(cellData.optString("pipeType", "straight"))
                    item.rotation = cellData.optInt("rotation", 0)
                    item.isBlocked = cellData.optBoolean("isBlocked", false)
                    item.isSource = cellData.optBoolean("isSource", false)
                    item.isSink = cellData.optBoolean("isSink", false)
                    item.isLocked = cellData.optBoolean("isLocked", false)
                }
            }

            withContext(Dispatchers.Main) {
                size = gridSize
                lives = livesCount
                difficulty = diff
                sourceRow = srcRow
                sourceCol = srcCol
                sinkRow = snkRow
                sinkCol = snkCol
                sourceDirection = PipeDirection.from(srcDirStr)
                sinkDirection = PipeDirection.from(snkDirStr)
                cells = grid.map { it.toList() }
                isLoading = false
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e("PipeEngine", "Parse error: ${e.message}")
            withContext(Dispatchers.Main) { isLoading = false }
            return@withContext false
        }
    }

    fun nextLevel() {
        levelNumber += 1
        loadLevel()
    }

    fun resetPuzzle() {
        loadLevel()
    }

    fun rotatePipe(row: Int, col: Int) {
        if (isSolved || isAnimatingWater || isGameOver) return
        val rowList = cells.getOrNull(row) ?: return
        val cell = rowList.getOrNull(col) ?: return
        if (cell.isBlocked || cell.isLocked) return

        val newGrid = cells.map { it.toMutableList() }.toMutableList()
        val newCell = cell.copy(rotation = (cell.rotation + 1) % 4)
        newGrid[row][col] = newCell
        cells = newGrid
        moveCount++
    }

    fun flowWater() {
        if (isSolved || isAnimatingWater || isGameOver) return

        val newGrid = cells.map { r ->
            r.map { c ->
                c.copy(
                    isFilled = false,
                    isLeaking = false,
                    fillOrder = -1,
                    waterDirections = emptySet(),
                    waterEntry = null
                )
            }.toMutableList()
        }.toMutableList()

        cells = newGrid

        val expectedFirstCellR = sourceRow + sourceDirection.dr
        val expectedFirstCellC = sourceCol + sourceDirection.dc

        // if starting off board
        val actualStartR = if (sourceRow < 0 || sourceRow >= size) sourceRow + sourceDirection.dr else sourceRow
        val actualStartC = if (sourceCol < 0 || sourceCol >= size) sourceCol + sourceDirection.dc else sourceCol

        val firstCell = cells.getOrNull(actualStartR)?.getOrNull(actualStartC)
        if (firstCell == null || !firstCell.connects(sourceDirection.opposite)) {
            lastFlowResult = FlowResult.NO_CONNECTION
            isAnimatingWater = true
            
            // Just fail immediately, no water connection locally available
            scope.launch {
                delay(1500)
                handleFailure()
                delay(500)
                resetWater()
            }
            return
        }

        val traceResult = traceWaterPath(actualStartR, actualStartC, sourceDirection.opposite)
        isAnimatingWater = true
        lastFlowResult = if (traceResult.reachedSink) FlowResult.SUCCESS else FlowResult.FAILED
        
        cells = traceResult.updatedGrid
        animateWaterFlow(traceResult.path, traceResult.reachedSink)
    }

    private data class TraceResult(
        val path: List<Pair<Int, Int>>,
        val reachedSink: Boolean,
        val updatedGrid: List<List<PipeCell>>
    )
    
    private fun traceWaterPath(startR: Int, startC: Int, startCameFrom: PipeDirection): TraceResult {
        val path = mutableListOf<Pair<Int, Int>>()
        val visited = mutableSetOf<String>()
        val cellDirections = mutableMapOf<String, MutableSet<PipeDirection>>()
        val entryDirs = mutableMapOf<String, PipeDirection>()
        
        var currentR = startR
        var currentC = startC
        var cameFrom = startCameFrom
        
        val gridCopy = cells.map { it.map { c -> c.copy() }.toMutableList() }.toMutableList()

        while (true) {
            val key = "$currentR,$currentC"
            if (visited.contains(key)) break
            visited.add(key)
            path.add(currentR to currentC)

            cellDirections.getOrPut(key) { mutableSetOf() }.add(cameFrom)
            entryDirs[key] = cameFrom

            val cell = gridCopy[currentR][currentC]

            val connectsToSink = (currentR + sinkDirection.opposite.dr == sinkRow) && 
                                 (currentC + sinkDirection.opposite.dc == sinkCol) &&
                                 cell.connects(sinkDirection.opposite)

            if (connectsToSink) {
                cellDirections.getOrPut(key) { mutableSetOf() }.add(sinkDirection.opposite)
                applyWaterDirections(gridCopy, cellDirections, entryDirs)
                return TraceResult(path, true, gridCopy)
            }

            var exitDirs = cell.connections.toMutableSet()
            exitDirs.remove(cameFrom)

            val sortedDirs = exitDirs.sortedBy { dir ->
                val ar = currentR + dir.dr
                val ac = currentC + dir.dc
                val aDist = abs(ar - sinkRow) + abs(ac - sinkCol)
                aDist
            }

            var foundNext = false
            for (dir in sortedDirs) {
                val nr = currentR + dir.dr
                val nc = currentC + dir.dc
                if (nr !in 0 until size || nc !in 0 until size) continue
                if (visited.contains("$nr,$nc")) continue

                val neighbor = gridCopy[nr][nc]
                if (!neighbor.connects(dir.opposite)) continue

                cellDirections.getOrPut(key) { mutableSetOf() }.add(dir)
                currentR = nr
                currentC = nc
                cameFrom = dir.opposite
                foundNext = true
                break
            }

            if (!foundNext) break
        }

        applyWaterDirections(gridCopy, cellDirections, entryDirs)
        return TraceResult(path, false, gridCopy)
    }

    private fun applyWaterDirections(
        grid: MutableList<MutableList<PipeCell>>,
        directions: Map<String, Set<PipeDirection>>,
        entries: Map<String, PipeDirection>
    ) {
        for ((key, dirs) in directions) {
            val parts = key.split(",")
            if (parts.size != 2) continue
            val r = parts[0].toIntOrNull() ?: continue
            val c = parts[1].toIntOrNull() ?: continue
            grid[r][c].waterDirections = dirs
            grid[r][c].waterEntry = entries[key]
        }
    }

    private fun animateWaterFlow(path: List<Pair<Int, Int>>, success: Boolean) {
        val delayMs = 450L
        scope.launch {
            for ((index, item) in path.withIndex()) {
                val (r, c) = item
                delay((if (index == 0) 0 else delayMs))
                
                val mut = cells.map { it.toMutableList() }.toMutableList()
                mut[r][c] = mut[r][c].copy(isFilled = true, fillOrder = index)
                cells = mut
                
                if (index == path.lastIndex) {
                    delay(800)
                    if (success) {
                        isSolved = true
                    } else {
                        val failMut = cells.map { it.toMutableList() }.toMutableList()
                        failMut[r][c] = failMut[r][c].copy(isLeaking = true)
                        cells = failMut
                        
                        delay(1200)
                        handleFailure()
                        delay(500)
                        resetWater()
                    }
                }
            }
        }
    }

    private fun handleFailure() {
        lives -= 1
        if (lives <= 0) {
            isGameOver = true
        }
    }

    private fun resetWater() {
        cells = cells.map { row ->
            row.map { c ->
                c.copy(
                    isFilled = false,
                    isLeaking = false,
                    fillOrder = -1,
                    waterDirections = emptySet(),
                    waterEntry = null
                )
            }
        }
        isAnimatingWater = false
    }
}
