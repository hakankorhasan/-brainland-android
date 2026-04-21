package com.example.brain_land.ui.games.starbattle

import androidx.compose.runtime.*

// ─────────────────────────────────────────────────────────────────────────────
// SBGameState — mirrors iOS GalacticBeaconsGrid (ObservableObject)
// ─────────────────────────────────────────────────────────────────────────────

class SBGameState(val level: SBLevel) {

    private val n = level.gridSize
    private val b = level.beaconsPerUnit

    // 2-D state grid — each cell is individually observable
    var cells by mutableStateOf(buildCells())
        private set

    var isSolved  by mutableStateOf(false)     ; private set
    var moveCount by mutableIntStateOf(0)       ; private set
    var errorMessage by mutableStateOf<String?>(null) ; private set

    // ── Public actions ────────────────────────────────────────────────────────

    fun tapCell(row: Int, col: Int) {
        if (isSolved) return
        val grid = cells.map { it.toMutableList() }.toMutableList()
        grid[row][col] = grid[row][col].copy(state = grid[row][col].state.next)
        moveCount++
        recalcErrors(grid)
        cells = grid
        checkWin()
    }

    fun resetPuzzle() {
        cells = buildCells()
        isSolved = false
        moveCount = 0
        errorMessage = null
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun buildCells(): List<List<GBCell>> =
        (0 until n).map { r ->
            (0 until n).map { c ->
                GBCell(row = r, col = c, regionId = level.regions[r][c])
            }
        }

    /** Mirrors iOS recalculateErrors() */
    private fun recalcErrors(grid: MutableList<MutableList<GBCell>>) {
        // Reset
        for (r in 0 until n) for (c in 0 until n) grid[r][c] = grid[r][c].copy(isError = false)
        errorMessage = null

        var rowErr = false; var colErr = false; var regErr = false; var adjErr = false

        // 1. Row count
        for (r in 0 until n) {
            val stars = (0 until n).filter { grid[r][it].state == GBCellState.STAR }
            if (stars.size > b) {
                stars.forEach { c -> grid[r][c] = grid[r][c].copy(isError = true) }
                rowErr = true
            }
        }
        // 2. Column count
        for (c in 0 until n) {
            val stars = (0 until n).filter { grid[it][c].state == GBCellState.STAR }
            if (stars.size > b) {
                stars.forEach { r -> grid[r][c] = grid[r][c].copy(isError = true) }
                colErr = true
            }
        }
        // 3. Region count
        val regionCells = mutableMapOf<Int, MutableList<Pair<Int,Int>>>()
        for (r in 0 until n) for (c in 0 until n)
            regionCells.getOrPut(grid[r][c].regionId) { mutableListOf() }.add(r to c)
        for ((_, positions) in regionCells) {
            val stars = positions.filter { (r, c) -> grid[r][c].state == GBCellState.STAR }
            if (stars.size > b) {
                stars.forEach { (r, c) -> grid[r][c] = grid[r][c].copy(isError = true) }
                regErr = true
            }
        }
        // 4. Adjacency (8-dir)
        val dirs = listOf(-1 to -1,-1 to 0,-1 to 1,0 to -1,0 to 1,1 to -1,1 to 0,1 to 1)
        for (r in 0 until n) for (c in 0 until n) {
            if (grid[r][c].state != GBCellState.STAR) continue
            for ((dr, dc) in dirs) {
                val nr = r + dr; val nc = c + dc
                if (nr in 0 until n && nc in 0 until n && grid[nr][nc].state == GBCellState.STAR) {
                    grid[r][c]   = grid[r][c].copy(isError = true)
                    grid[nr][nc] = grid[nr][nc].copy(isError = true)
                    adjErr = true
                }
            }
        }

        errorMessage = when {
            adjErr -> "Stars cannot touch each other"
            rowErr -> "Too many stars in a row"
            colErr -> "Too many stars in a column"
            regErr -> "Too many stars in a region"
            else   -> null
        }
    }

    /** Mirrors iOS checkWinCondition() */
    private fun checkWin() {
        val total = b * n
        var starCount = 0
        for (r in 0 until n) for (c in 0 until n)
            if (cells[r][c].state == GBCellState.STAR) starCount++
        if (starCount != total) return
        if (cells.any { row -> row.any { it.isError } }) return
        // Verify exact per-row, per-col, per-region
        for (r in 0 until n) {
            val cnt = (0 until n).count { cells[r][it].state == GBCellState.STAR }
            if (cnt != b) return
        }
        for (c in 0 until n) {
            val cnt = (0 until n).count { cells[it][c].state == GBCellState.STAR }
            if (cnt != b) return
        }
        val regionCounts = mutableMapOf<Int, Int>()
        for (r in 0 until n) for (c in 0 until n)
            if (cells[r][c].state == GBCellState.STAR)
                regionCounts[cells[r][c].regionId] = (regionCounts[cells[r][c].regionId] ?: 0) + 1
        for ((_, cnt) in regionCounts) if (cnt != b) return
        isSolved = true
    }
}
