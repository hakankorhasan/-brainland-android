package com.example.brain_land.ui.games.laserpuzzle

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL

// ─────────────────────────────────────────────────────────────────────────────
// LPGameState — mirrors iOS LaserGrid (ObservableObject)
// Backend-driven: loads levels from getLaserPuzzleLevels Cloud Function
// ─────────────────────────────────────────────────────────────────────────────

class LPGameState(
    private val scope: CoroutineScope
) {
    companion object {
        private const val BASE_URL = "https://us-central1-mini-games-9a4e1.cloudfunctions.net"
    }

    // ── Published state ───────────────────────────────────────────────────────

    var levelNumber  by mutableIntStateOf(1)       ; private set
    var gridSize     by mutableIntStateOf(8)       ; private set
    var difficulty   by mutableStateOf("beginner") ; private set
    var lives        by mutableIntStateOf(3)       ; private set
    var moveCount    by mutableIntStateOf(0)       ; private set
    var isSolved     by mutableStateOf(false)      ; private set
    var isGameOver   by mutableStateOf(false)      ; private set
    var isLaserFiring by mutableStateOf(false)     ; private set
    var isLoading    by mutableStateOf(true)       ; private set

    // Grid: 2D array of LaserCell (rebuilt on each level load)
    var cells by mutableStateOf(emptyGrid(8))      ; private set

    // Laser beam segments for canvas drawing
    var laserPath by mutableStateOf<List<LaserSegment>>(emptyList()) ; private set

    // Current level data (needed to reset)
    private var currentLevel: LaserPuzzleLevel? = null
    private var timerSecs by mutableIntStateOf(0)
    var elapsedSeconds: Int get() = timerSecs; set(v) { timerSecs = v }

    init { loadLevel(1) }

    // ── Level Loading ─────────────────────────────────────────────────────────

    fun loadLevel(number: Int) {
        scope.launch {
            withContext(Dispatchers.Main) {
                isLoading     = true
                isSolved      = false
                isGameOver    = false
                isLaserFiring = false
                moveCount     = 0
                laserPath     = emptyList()
            }
            try {
                val raw  = withContext(Dispatchers.IO) {
                    URL("$BASE_URL/getLaserPuzzleLevels?level=$number").readText()
                }
                val json = JSONObject(raw)
                val ok   = json.optBoolean("success", false)
                val ld   = if (ok) json.optJSONObject("level") else null
                if (ld != null) {
                    val level = parseLevel(ld)
                    withContext(Dispatchers.Main) {
                        levelNumber = level.levelNumber
                        gridSize    = level.gridSize
                        difficulty  = level.difficulty
                        lives       = level.lives
                        currentLevel = level
                        cells = buildGrid(level)
                        isLoading = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        // Fallback: local generation
                        gridSize  = 8
                        cells     = emptyGrid(8)
                        lives     = 3
                        isLoading = false
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    gridSize  = 8
                    cells     = emptyGrid(8)
                    lives     = 3
                    isLoading = false
                }
            }
        }
    }

    fun nextLevel() {
        loadLevel(levelNumber + 1)
    }

    fun resetPuzzle() {
        currentLevel?.let { loadLevel(it.levelNumber) } ?: loadLevel(levelNumber)
    }

    // ── JSON Parsing ──────────────────────────────────────────────────────────

    private fun parseLevel(ld: JSONObject): LaserPuzzleLevel {
        val num      = ld.optInt("levelNumber", 1)
        val size     = ld.optInt("gridSize", 8)
        val diff     = ld.optString("difficulty", "beginner")
        val lv       = ld.optInt("lives", 3)
        val cellsArr = ld.optJSONArray("cells")
        val solArr   = ld.optJSONArray("solution")

        val parsedCells = mutableListOf<LaserPuzzleCellData>()
        if (cellsArr != null) {
            for (i in 0 until cellsArr.length()) {
                val c = cellsArr.getJSONObject(i)
                parsedCells += LaserPuzzleCellData(
                    row         = c.optInt("row"),
                    col         = c.optInt("col"),
                    type        = c.optString("type", "empty"),
                    direction   = c.optString("direction").takeIf { it.isNotEmpty() },
                    mirrorAngle = if (c.has("mirrorAngle")) c.optInt("mirrorAngle") else null,
                    isFixed     = if (c.has("isFixed")) c.optBoolean("isFixed") else null,
                    portalPairId = if (c.has("portalPairId")) c.optInt("portalPairId") else null
                )
            }
        }

        val solution = mutableListOf<LaserSolutionEntry>()
        if (solArr != null) {
            for (i in 0 until solArr.length()) {
                val s = solArr.getJSONObject(i)
                solution += LaserSolutionEntry(s.optInt("row"), s.optInt("col"), s.optInt("correctAngle"))
            }
        }
        return LaserPuzzleLevel(num, size, diff, lv, parsedCells, solution)
    }

    private fun buildGrid(level: LaserPuzzleLevel): Array<Array<LaserCell>> {
        val grid = emptyGrid(level.gridSize)
        for (cellData in level.cells) {
            val r = cellData.row
            val c = cellData.col
            if (r !in 0 until level.gridSize || c !in 0 until level.gridSize) continue

            val type: LaserCellType = when (cellData.type) {
                "source" -> LaserCellType.Source(
                    when (cellData.direction) {
                        "up"    -> LaserDirection.UP
                        "down"  -> LaserDirection.DOWN
                        "left"  -> LaserDirection.LEFT
                        "right" -> LaserDirection.RIGHT
                        else    -> LaserDirection.DOWN
                    }
                )
                "target"   -> LaserCellType.Target
                "mirror"   -> LaserCellType.Mirror
                "wall"     -> LaserCellType.Wall
                "portal"   -> LaserCellType.Portal(cellData.portalPairId ?: 0)
                "bomb"     -> LaserCellType.Bomb
                "splitter" -> LaserCellType.Splitter
                else       -> LaserCellType.Empty
            }
            grid[r][c] = grid[r][c].copy(
                cellType    = type,
                mirrorAngle = cellData.mirrorAngle ?: 0,
                isFixed     = cellData.isFixed ?: false
            )
        }
        return grid
    }

    // ── Mirror Rotation ───────────────────────────────────────────────────────

    fun rotateMirror(row: Int, col: Int) {
        if (isSolved || isGameOver) return
        val ct = cells[row][col].cellType
        if (ct !is LaserCellType.Mirror && ct !is LaserCellType.Splitter) return
        if (cells[row][col].isFixed) return

        val newGrid = cells.map { it.copyOf() }.toTypedArray()
        newGrid[row][col] = newGrid[row][col].rotateMirror()
        cells = newGrid
        moveCount++
    }

    // ── Laser Firing ──────────────────────────────────────────────────────────

    fun fireLaser() {
        if (isLaserFiring || isSolved || isGameOver) return

        // Reset previous fire state
        val resetGrid = cells.map { row -> row.map { it.copy(isLit = false, isHitTarget = false, isHitBomb = false) }.toTypedArray() }.toTypedArray()
        cells = resetGrid
        laserPath = emptyList()
        isLaserFiring = true

        val result = traceLaserInternal()
        laserPath = result.segments

        val hitAll   = result.hitTargets.size >= result.totalTargets && !result.hitBomb
        val segments = result.segments

        scope.launch {
            val delay = (segments.size * 250L + 500L).coerceAtMost(3000L)
            delay(delay)
            withContext(Dispatchers.Main) {
                // Apply hit state to cells
                val hitGrid = cells.map { it.copyOf() }.toTypedArray()
                for (key in result.hitTargetKeys) {
                    val parts = key.split(",")
                    if (parts.size == 2) {
                        val r = parts[0].toIntOrNull() ?: continue
                        val c = parts[1].toIntOrNull() ?: continue
                        if (r in 0 until gridSize && c in 0 until gridSize)
                            hitGrid[r][c] = hitGrid[r][c].copy(isHitTarget = true)
                    }
                }
                if (result.hitBomb) {
                    for (key in result.hitBombKeys) {
                        val parts = key.split(",")
                        if (parts.size == 2) {
                            val r = parts[0].toIntOrNull() ?: continue
                            val c = parts[1].toIntOrNull() ?: continue
                            if (r in 0 until gridSize && c in 0 until gridSize)
                                hitGrid[r][c] = hitGrid[r][c].copy(isHitBomb = true)
                        }
                    }
                }
                cells = hitGrid

                if (hitAll) {
                    isSolved = true
                    isLaserFiring = false
                } else {
                    lives--
                    if (lives <= 0) isGameOver = true
                    delay(1200L)
                    withContext(Dispatchers.Main) {
                        isLaserFiring = false
                        laserPath = emptyList()
                        val cleared = cells.map { row -> row.map { it.copy(isLit = false, isHitBomb = false) }.toTypedArray() }.toTypedArray()
                        cells = cleared
                    }
                }
            }
        }
    }

    // ── Static Laser Trace ────────────────────────────────────────────────────

    private data class TraceResult(
        val segments: List<LaserSegment>,
        val hitTargetKeys: Set<String>,
        val hitBombKeys: Set<String>,
        val hitTargets: Set<String>,
        val hitBomb: Boolean,
        val totalTargets: Int
    )

    private fun traceLaserInternal(): TraceResult {
        val grid = cells
        val size  = gridSize

        // Find source
        var sRow = 0; var sCol = 0; var sDir = LaserDirection.RIGHT
        for (r in 0 until size) for (c in 0 until size) {
            if (grid[r][c].cellType is LaserCellType.Source) {
                sRow = r; sCol = c; sDir = (grid[r][c].cellType as LaserCellType.Source).direction
            }
        }

        // Count total targets
        var totalTargets = 0
        for (r in 0 until size) for (c in 0 until size) if (grid[r][c].cellType == LaserCellType.Target) totalTargets++

        data class FireBeam(var row: Int, var col: Int, var dir: LaserDirection, var segStartX: Float, var segStartY: Float)

        val allSegments   = mutableListOf<LaserSegment>()
        val hitTargets    = mutableSetOf<String>()
        val hitBombKeys   = mutableSetOf<String>()
        var hitBomb       = false
        val globalVisited = mutableSetOf<String>()
        val litGrid       = Array(size) { BooleanArray(size) }

        val startX = sCol + 0.5f; val startY = sRow + 0.5f
        litGrid[sRow][sCol] = true

        var beamQueue = ArrayDeque<FireBeam>()
        beamQueue += FireBeam(sRow, sCol, sDir, startX, startY)

        while (beamQueue.isNotEmpty()) {
            var beam = beamQueue.removeFirst()
            val visited = mutableSetOf<String>()

            for (step in 0 until (size * size * 4)) {
                val nr = beam.row + beam.dir.dr
                val nc = beam.col + beam.dir.dc

                if (nr !in 0 until size || nc !in 0 until size) {
                    val ex = nc + 0.5f + beam.dir.dc * 0.5f
                    val ey = nr + 0.5f + beam.dir.dr * 0.5f
                    allSegments += LaserSegment(beam.segStartX, beam.segStartY, ex.coerceIn(0f, size.toFloat()), ey.coerceIn(0f, size.toFloat()))
                    beam.row = -1; break
                }

                val visitKey = "$nr,$nc,${beam.dir.rawValue}"
                if (visited.contains(visitKey) || globalVisited.contains(visitKey)) {
                    allSegments += LaserSegment(beam.segStartX, beam.segStartY, nc + 0.5f, nr + 0.5f)
                    beam.row = -1; break
                }
                visited += visitKey; globalVisited += visitKey

                beam.row = nr; beam.col = nc
                litGrid[nr][nc] = true
                val cellCenter = Pair(nc + 0.5f, nr + 0.5f)

                when (val ct = grid[nr][nc].cellType) {
                    LaserCellType.Wall, is LaserCellType.Source -> {
                        allSegments += LaserSegment(beam.segStartX, beam.segStartY, cellCenter.first, cellCenter.second)
                        beam.row = -1
                    }
                    LaserCellType.Mirror -> {
                        allSegments += LaserSegment(beam.segStartX, beam.segStartY, cellCenter.first, cellCenter.second)
                        beam.dir = grid[nr][nc].reflect(beam.dir)
                        beam.segStartX = cellCenter.first; beam.segStartY = cellCenter.second
                    }
                    LaserCellType.Target -> {
                        allSegments += LaserSegment(beam.segStartX, beam.segStartY, cellCenter.first, cellCenter.second)
                        hitTargets += "$nr,$nc"
                        beam.row = -1
                    }
                    LaserCellType.Bomb -> {
                        allSegments += LaserSegment(beam.segStartX, beam.segStartY, cellCenter.first, cellCenter.second)
                        hitBomb = true; hitBombKeys += "$nr,$nc"
                        beam.row = -1
                    }
                    is LaserCellType.Portal -> {
                        allSegments += LaserSegment(beam.segStartX, beam.segStartY, cellCenter.first, cellCenter.second)
                        val exit = findPortalExit(grid, size, ct.pairId, nr, nc)
                        if (exit != null) {
                            beam.row = exit.first; beam.col = exit.second
                            beam.segStartX = exit.second + 0.5f; beam.segStartY = exit.first + 0.5f
                        } else { beam.row = -1 }
                    }
                    LaserCellType.Splitter -> {
                        allSegments += LaserSegment(beam.segStartX, beam.segStartY, cellCenter.first, cellCenter.second)
                        val (passDir, reflectDir) = grid[nr][nc].split(beam.dir)
                        beamQueue += FireBeam(nr, nc, reflectDir, cellCenter.first, cellCenter.second)
                        beam.dir = passDir
                        beam.segStartX = cellCenter.first; beam.segStartY = cellCenter.second
                    }
                    LaserCellType.Empty -> { /* continue */ }
                }

                if (beam.row == -1) break
            }
        }

        // Apply lit state
        scope.launch(Dispatchers.Main) {
            val newGrid = cells.map { row -> row.copyOf() }.toTypedArray()
            for (r in 0 until size) for (c in 0 until size) if (litGrid[r][c]) newGrid[r][c] = newGrid[r][c].copy(isLit = true)
            cells = newGrid
        }

        return TraceResult(allSegments, hitTargets, hitBombKeys, hitTargets, hitBomb, totalTargets)
    }

    private fun findPortalExit(grid: Array<Array<LaserCell>>, size: Int, pairId: Int, entryRow: Int, entryCol: Int): Pair<Int, Int>? {
        for (r in 0 until size) for (c in 0 until size) {
            val ct = grid[r][c].cellType
            if (ct is LaserCellType.Portal && ct.pairId == pairId && (r != entryRow || c != entryCol)) return r to c
        }
        return null
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun emptyGrid(size: Int): Array<Array<LaserCell>> =
        Array(size) { r -> Array(size) { c -> LaserCell(r, c) } }
}
