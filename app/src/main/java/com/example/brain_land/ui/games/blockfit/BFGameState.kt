package com.example.brain_land.ui.games.blockfit

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL

// ─────────────────────────────────────────────────────────────────────────────
// BFGameState — mirrors iOS BlockFitGrid (ObservableObject)
// ─────────────────────────────────────────────────────────────────────────────

class BFGameState(
    initialLevel: Int = 1,
    private val scope: CoroutineScope
) {
    companion object {
        const val GRID_SIZE = 9
        private const val BASE_URL = "https://us-central1-mini-games-9a4e1.cloudfunctions.net"
        private val GRAY = Color(0xFF404052)
    }

    // ── Published state ───────────────────────────────────────────────────────

    var levelNumber   by mutableIntStateOf(initialLevel)         ; private set
    var targetScore   by mutableIntStateOf(50)                   ; private set
    var difficultyValue by mutableIntStateOf(1)                  ; private set
    var difficultyName by mutableStateOf("beginner")             ; private set

    // grid: null = empty, non-null = filled with Color
    var grid by mutableStateOf(emptyGrid())                      ; private set

    var availableBlocks by mutableStateOf<List<BlockShape?>>(listOf(null, null, null)) ; private set
    var score           by mutableIntStateOf(0)                  ; private set
    var isGameOver      by mutableStateOf(false)                 ; private set
    var isWon           by mutableStateOf(false)                 ; private set
    var isLoading       by mutableStateOf(false)                 ; private set

    // Preview — which cells to highlight as drop hint
    var previewCells    by mutableStateOf<List<Pair<Int,Int>>>(emptyList()) ; private set
    var previewValid    by mutableStateOf(false)                 ; private set

    // Highlight cells (turn white when cleared)
    var highlightCells  by mutableStateOf<Set<String>>(emptySet()) ; private set

    // Shake trigger (invalid placement)
    var invalidPlacementTick by mutableIntStateOf(0)             ; private set

    // Pool of template indices (from backend)
    private var blockPool: List<Int> = (0..8).toList()

    init { loadLevel() }

    // ── Loading ───────────────────────────────────────────────────────────────

    fun loadLevel() {
        scope.launch {
            withContext(Dispatchers.Main) {
                isLoading = true
                isGameOver = false
                isWon = false
                score = 0
                highlightCells = emptySet()
                previewCells = emptyList()
                grid = emptyGrid()
                availableBlocks = listOf(null, null, null)
            }
            try {
                val raw  = withContext(Dispatchers.IO) {
                    URL("$BASE_URL/getBlockFitLevels?level=$levelNumber").readText()
                }
                val json = JSONObject(raw)
                val ld   = json.optJSONObject("level")
                if (ld != null) {
                    val ts      = ld.optInt("targetScore", 50)
                    val dv      = ld.optInt("difficultyValue", 1)
                    val dn      = ld.optString("difficulty", "beginner")
                    val poolArr = ld.optJSONArray("blockPool")
                    val pool    = if (poolArr != null) (0 until poolArr.length()).map { poolArr.getInt(it) }
                                  else (0..8).toList()
                    val prefill = ld.optJSONArray("prefill")

                    withContext(Dispatchers.Main) {
                        targetScore   = ts
                        difficultyValue = dv
                        difficultyName  = dn
                        blockPool = pool

                        // Apply prefill
                        val newGrid = emptyGrid()
                        if (prefill != null) {
                            for (i in 0 until prefill.length()) {
                                val item  = prefill.getJSONObject(i)
                                val row   = item.optInt("row", -1)
                                val col   = item.optInt("col", -1)
                                val cidx  = item.optInt("colorIndex", -1)
                                if (row in 0 until GRID_SIZE && col in 0 until GRID_SIZE) {
                                    newGrid[row][col] = if (cidx == -1) GRAY
                                                        else blockColors.getOrElse(cidx) { GRAY }
                                }
                            }
                        }
                        grid = newGrid
                        spawnNewBlocks()
                        isLoading = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        // fallback: start with empty grid
                        spawnNewBlocks()
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // fallback local
                    spawnNewBlocks()
                    isLoading = false
                }
            }
        }
    }

    fun nextLevel() {
        val completed = levelNumber
        levelNumber++
        loadLevel()
    }

    fun resetPuzzle() { loadLevel() }

    // ── Block placement ───────────────────────────────────────────────────────

    fun canPlace(block: BlockShape, row: Int, col: Int): Boolean {
        for ((dr, dc) in block.cells) {
            val r = row + dr; val c = col + dc
            if (r !in 0 until GRID_SIZE || c !in 0 until GRID_SIZE) return false
            if (grid[r][c] != null) return false
        }
        return true
    }

    fun updatePreview(block: BlockShape, row: Int, col: Int) {
        val valid = canPlace(block, row, col)
        previewValid = valid
        previewCells = block.cells.mapNotNull { (dr, dc) ->
            val r = row + dr; val c = col + dc
            if (r in 0 until GRID_SIZE && c in 0 until GRID_SIZE) r to c else null
        }
    }

    fun clearPreview() {
        previewCells = emptyList()
        previewValid = false
    }

    fun placeBlock(block: BlockShape, row: Int, col: Int, blockIndex: Int) {
        if (!canPlace(block, row, col)) {
            invalidPlacementTick++
            return
        }
        val newGrid = grid.map { it.copyOf() }.toTypedArray()
        for ((dr, dc) in block.cells) { newGrid[row + dr][col + dc] = block.color }
        score += block.cells.size
        grid = newGrid
        val newBlocks = availableBlocks.toMutableList()
        newBlocks[blockIndex] = null
        availableBlocks = newBlocks
        clearPreview()

        clearCompleted()
    }

    // ── Row/Column clearing ───────────────────────────────────────────────────

    private fun clearCompleted() {
        val rowsToClear = (0 until GRID_SIZE).filter { r -> (0 until GRID_SIZE).all { c -> grid[r][c] != null } }
        val colsToClear = (0 until GRID_SIZE).filter { c -> (0 until GRID_SIZE).all { r -> grid[r][c] != null } }

        if (rowsToClear.isEmpty() && colsToClear.isEmpty()) {
            afterClear()
            return
        }

        val cells = mutableSetOf<String>()
        rowsToClear.forEach { r -> (0 until GRID_SIZE).forEach { c -> cells.add("$r,$c") } }
        colsToClear.forEach { c -> (0 until GRID_SIZE).forEach { r -> cells.add("$r,$c") } }

        val lines = rowsToClear.size + colsToClear.size
        score += lines * GRID_SIZE
        if (lines > 1) score += (lines - 1) * GRID_SIZE

        highlightCells = cells

        scope.launch {
            delay(350)
            withContext(Dispatchers.Main) {
                val newGrid = grid.map { it.copyOf() }.toTypedArray()
                rowsToClear.forEach { r -> (0 until GRID_SIZE).forEach { c -> newGrid[r][c] = null } }
                colsToClear.forEach { c -> (0 until GRID_SIZE).forEach { r -> newGrid[r][c] = null } }
                grid = newGrid
                highlightCells = emptySet()

                if (rowsToClear.isNotEmpty()) {
                    delay(250)
                    applyGravity()
                    delay(350)
                }
                afterClear()
            }
        }
    }

    private fun afterClear() {
        if (score >= targetScore && !isWon) {
            isWon = true
            return
        }
        if (availableBlocks.all { it == null }) spawnNewBlocks()
        checkGameOver()
    }

    // ── Gravity ───────────────────────────────────────────────────────────────

    private fun applyGravity() {
        val newGrid = emptyGrid()
        for (c in 0 until GRID_SIZE) {
            val filled = (0 until GRID_SIZE).reversed().mapNotNull { r -> grid[r][c] }
            filled.forEachIndexed { i, color ->
                newGrid[GRID_SIZE - 1 - i][c] = color
            }
        }
        grid = newGrid
    }

    // ── Spawn ─────────────────────────────────────────────────────────────────

    private fun spawnNewBlocks() {
        val pool = blockPool.ifEmpty { (0..8).toList() }
        availableBlocks = (0 until 3).map { i ->
            val tIdx  = pool.random()
            val safe  = tIdx.coerceIn(0, blockShapeTemplates.size - 1)
            BlockShape(id = i, cells = blockShapeTemplates[safe], color = blockColors.random())
        }
    }

    // ── Game over ─────────────────────────────────────────────────────────────

    private fun checkGameOver() {
        val anyCanPlace = availableBlocks.filterNotNull().any { block ->
            (0 until GRID_SIZE).any { r ->
                (0 until GRID_SIZE).any { c -> canPlace(block, r, c) }
            }
        }
        if (!anyCanPlace) isGameOver = true
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun emptyGrid(): Array<Array<Color?>> =
        Array(GRID_SIZE) { Array<Color?>(GRID_SIZE) { null } }
}
