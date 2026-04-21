package com.example.brain_land.ui.games.nonogram

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class NonogramEngine(
    private val scope: CoroutineScope,
    initialLevel: Int = 1
) {
    var levelNumber by mutableStateOf(initialLevel)
    var gridSize    by mutableStateOf(5)
    var cells       by mutableStateOf<List<List<NonogramCell>>>(emptyList())
    var rowClues    by mutableStateOf<List<List<Int>>>(emptyList())
    var colClues    by mutableStateOf<List<List<Int>>>(emptyList())
    var isSolved    by mutableStateOf(false)
    var isLoading   by mutableStateOf(false)
    var moveCount   by mutableStateOf(0)
    var fillFraction by mutableStateOf(0.4)

    private val baseURL = "https://us-central1-mini-games-9a4e1.cloudfunctions.net"

    // MARK: - Load Level

    fun loadLevel() {
        if (isLoading) return
        isLoading = true
        isSolved  = false
        moveCount = 0
        cells     = emptyList()
        rowClues  = emptyList()
        colClues  = emptyList()

        scope.launch {
            val ok = fetchAndParseLevel()
            if (!ok) {
                // Fallback: local 5×5 puzzle
                withContext(Dispatchers.Main) {
                    buildFallback()
                    isLoading = false
                }
            }
        }
    }

    fun nextLevel() {
        levelNumber++
        loadLevel()
    }

    fun resetPuzzle() {
        isSolved  = false
        moveCount = 0
        cells = cells.map { row -> row.map { c -> c.copy(state = NonogramCellState.EMPTY) } }
    }

    // MARK: - Tap Cell

    fun tapCell(row: Int, col: Int) {
        if (isSolved) return
        val newCells = cells.map { it.toMutableList() }.toMutableList()
        val cell = newCells[row][col]
        newCells[row][col] = cell.copy(state = cell.state.next)
        cells = newCells
        moveCount++
        checkWin()
    }

    // MARK: - Reveal Hint (fix one mistake, then reveal a missing cell)

    fun revealHint(): Pair<Int, Int>? {
        val wrongFilled = mutableListOf<Pair<Int,Int>>()
        val wrongMarked = mutableListOf<Pair<Int,Int>>()
        val missing     = mutableListOf<Pair<Int,Int>>()

        for (r in cells.indices) {
            for (c in cells[r].indices) {
                val cell = cells[r][c]
                when {
                    !cell.isSolution && cell.state == NonogramCellState.FILLED -> wrongFilled.add(r to c)
                    cell.isSolution  && cell.state == NonogramCellState.MARKED -> wrongMarked.add(r to c)
                    cell.isSolution  && cell.state != NonogramCellState.FILLED -> missing.add(r to c)
                }
            }
        }

        val target = (wrongFilled + wrongMarked).randomOrNull() ?: missing.randomOrNull() ?: return null
        val (r, c) = target
        val newCells = cells.map { it.toMutableList() }.toMutableList()
        val fix = if (wrongFilled.contains(target) || wrongMarked.contains(target))
            NonogramCellState.EMPTY else NonogramCellState.FILLED
        newCells[r][c] = newCells[r][c].copy(state = fix)
        cells = newCells
        moveCount++
        checkWin()
        return target
    }

    // MARK: - Clue Completion checks

    fun isRowComplete(row: Int): Boolean {
        if (rowClues.size <= row) return false
        val fills = cells[row].map { it.state == NonogramCellState.FILLED }
        return getFilledRuns(fills) == rowClues[row]
    }

    fun isColComplete(col: Int): Boolean {
        if (colClues.size <= col) return false
        val fills = cells.indices.map { cells[it][col].state == NonogramCellState.FILLED }
        return getFilledRuns(fills) == colClues[col]
    }

    // MARK: - Internals

    private fun getFilledRuns(line: List<Boolean>): List<Int> {
        val runs = mutableListOf<Int>()
        var count = 0
        for (filled in line) {
            if (filled) count++ else { if (count > 0) { runs.add(count); count = 0 } }
        }
        if (count > 0) runs.add(count)
        return if (runs.isEmpty()) listOf(0) else runs
    }

    private fun checkWin() {
        for (r in cells.indices) {
            for (c in cells[r].indices) {
                val cell = cells[r][c]
                if (cell.isSolution && cell.state != NonogramCellState.FILLED) return
                if (!cell.isSolution && cell.state == NonogramCellState.FILLED) return
            }
        }
        isSolved = true
    }

    // MARK: - Network fetch

    private suspend fun fetchAndParseLevel(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseURL/getNonogramLevels?level=$levelNumber")
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
                Log.w("NonogramEngine", "API error: $err")
                return@withContext false
            }

            val lvl = json.getJSONObject("level")
            val num  = lvl.optInt("levelNumber", levelNumber)
            val size = lvl.optInt("gridSize", 5)
            val fill = lvl.optDouble("fillFraction", 0.4)

            val solArray   = lvl.optJSONArray("solution")
            val rowArray   = lvl.optJSONArray("rowClues")
            val colArray   = lvl.optJSONArray("colClues")

            if (solArray == null || rowArray == null || colArray == null) {
                Log.w("NonogramEngine", "Missing fields in level $num")
                return@withContext false
            }

            val solution = parseBoolGrid(solArray, size)
            val rClues   = parseIntGrid(rowArray)
            val cClues   = parseIntGrid(colArray)

            val newCells = (0 until size).map { r ->
                (0 until size).map { c ->
                    NonogramCell(row = r, col = c, isSolution = solution[r][c])
                }
            }

            withContext(Dispatchers.Main) {
                levelNumber  = num
                gridSize     = size
                fillFraction = fill
                cells        = newCells
                rowClues     = rClues
                colClues     = cClues
                isLoading    = false
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e("NonogramEngine", "Fetch error: ${e.message}")
            withContext(Dispatchers.Main) { isLoading = false }
            return@withContext false
        }
    }

    private fun parseBoolGrid(arr: JSONArray, size: Int): List<List<Boolean>> {
        return (0 until size).map { r ->
            val row = arr.optJSONArray(r)
            (0 until size).map { c ->
                row?.optBoolean(c, false) ?: false
            }
        }
    }

    private fun parseIntGrid(arr: JSONArray): List<List<Int>> {
        return (0 until arr.length()).map { i ->
            val row = arr.optJSONArray(i)
            if (row == null) listOf(0)
            else (0 until row.length()).map { j -> row.optInt(j, 0) }
        }
    }

    // MARK: - Fallback puzzle (offline 5×5)

    private fun buildFallback() {
        val n = 5
        val sol = listOf(
            listOf(true, false, true, false, true),
            listOf(false, true, false, true, false),
            listOf(true, true, false, false, true),
            listOf(false, false, true, true, false),
            listOf(true, false, false, false, true)
        )
        gridSize     = n
        fillFraction = 0.4
        cells = sol.mapIndexed { r, row -> row.mapIndexed { c, sol -> NonogramCell(r, c, sol) } }
        rowClues = sol.map { row -> getFilledRuns(row) }
        colClues = (0 until n).map { c -> getFilledRuns(sol.map { it[c] }) }
    }
}
