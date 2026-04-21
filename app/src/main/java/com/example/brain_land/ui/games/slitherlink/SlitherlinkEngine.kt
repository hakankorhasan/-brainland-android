package com.example.brain_land.ui.games.slitherlink

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.brain_land.data.GameType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SlitherlinkEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    var rows by mutableIntStateOf(4)
    var cols by mutableIntStateOf(4)
    var cells by mutableStateOf<List<List<SlitherlinkCell>>>(emptyList())
    
    // horizontalEdges[r][c] = edge between dots (r,c) and (r,c+1). Size: (rows+1) x cols
    var horizontalEdges by mutableStateOf<List<List<SlitherlinkEdgeState>>>(emptyList())
    // verticalEdges[r][c] = edge between dots (r,c) and (r+1,c). Size: rows x (cols+1)
    var verticalEdges by mutableStateOf<List<List<SlitherlinkEdgeState>>>(emptyList())

    var isSolved by mutableStateOf(false)
    var moveCount by mutableIntStateOf(0)
    var isLoading by mutableStateOf(false)
    
    var levelNumber by mutableIntStateOf(1)
        private set
        
    var difficultyValue by mutableIntStateOf(1)
    var difficultyName by mutableStateOf("beginner")

    init {
        val prefs = context.getSharedPreferences("slitherlink_prefs", Context.MODE_PRIVATE)
        levelNumber = prefs.getInt("currentLevel", 1)
        loadLevel()
    }

    fun loadLevel() {
        isLoading = true
        isSolved = false
        moveCount = 0

        scope.launch {
            withContext(Dispatchers.Main) {
                // Generate a hardcoded offline fallback level here exactly as we do for others
                buildOfflineLevel()
                isLoading = false
            }
        }
    }

    fun nextLevel() {
        val completed = levelNumber
        levelNumber++
        val prefs = context.getSharedPreferences("slitherlink_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("currentLevel", levelNumber).apply()
        loadLevel()
    }

    fun resetPuzzle() {
        horizontalEdges = List(rows + 1) { List(cols) { SlitherlinkEdgeState.NONE } }
        verticalEdges = List(rows) { List(cols + 1) { SlitherlinkEdgeState.NONE } }
        cells = cells.map { rowList ->
            rowList.map { it.copy(isError = false) }
        }
        moveCount = 0
        isSolved = false
    }

    fun tapHorizontalEdge(r: Int, c: Int) {
        if (r !in 0..rows || c !in 0 until cols) return
        if (isSolved) return
        
        val mut = horizontalEdges.map { it.toMutableList() }.toMutableList()
        mut[r][c] = mut[r][c].next
        horizontalEdges = mut
        
        moveCount++
        recalculateErrors()
        checkWinCondition()
    }

    fun tapVerticalEdge(r: Int, c: Int) {
        if (r !in 0 until rows || c !in 0..cols) return
        if (isSolved) return
        
        val mut = verticalEdges.map { it.toMutableList() }.toMutableList()
        mut[r][c] = mut[r][c].next
        verticalEdges = mut
        
        moveCount++
        recalculateErrors()
        checkWinCondition()
    }

    fun linesAroundCell(r: Int, c: Int): Int {
        var count = 0
        if (horizontalEdges[r][c] == SlitherlinkEdgeState.LINE) count++
        if (horizontalEdges[r + 1][c] == SlitherlinkEdgeState.LINE) count++
        if (verticalEdges[r][c] == SlitherlinkEdgeState.LINE) count++
        if (verticalEdges[r][c + 1] == SlitherlinkEdgeState.LINE) count++
        return count
    }

    fun linesAtDot(r: Int, c: Int): Int {
        var count = 0
        if (c > 0 && horizontalEdges[r][c - 1] == SlitherlinkEdgeState.LINE) count++
        if (c < cols && horizontalEdges[r][c] == SlitherlinkEdgeState.LINE) count++
        if (r > 0 && verticalEdges[r - 1][c] == SlitherlinkEdgeState.LINE) count++
        if (r < rows && verticalEdges[r][c] == SlitherlinkEdgeState.LINE) count++
        return count
    }

    private fun recalculateErrors() {
        cells = cells.mapIndexed { r, rowList ->
            rowList.mapIndexed { c, cell ->
                var error = false
                val clue = cell.clue
                if (clue != null) {
                    val lines = linesAroundCell(r, c)
                    if (lines > clue) {
                        error = true
                    } else {
                        val edgeStates = listOf(
                            horizontalEdges[r][c],
                            horizontalEdges[r + 1][c],
                            verticalEdges[r][c],
                            verticalEdges[r][c + 1]
                        )
                        val undecided = edgeStates.count { it == SlitherlinkEdgeState.NONE }
                        if (lines + undecided < clue) {
                            error = true
                        }
                        if (undecided == 0 && lines != clue) {
                            error = true
                        }
                    }
                }
                cell.copy(isError = error)
            }
        }
    }

    private fun checkWinCondition() {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val clue = cells[r][c].clue
                if (clue != null) {
                    if (linesAroundCell(r, c) != clue) return
                }
            }
        }

        var hasAnyLine = false
        for (r in 0..rows) {
            for (c in 0..cols) {
                val degree = linesAtDot(r, c)
                if (degree == 1 || degree == 3) return
                if (degree > 0) hasAnyLine = true
            }
        }

        if (!hasAnyLine) return
        if (!isConnectedLoop()) return

        isSolved = true
    }

    private fun isConnectedLoop(): Boolean {
        var startDot: Pair<Int, Int>? = null
        for (r in 0..rows) {
            for (c in 0..cols) {
                if (linesAtDot(r, c) > 0) {
                    startDot = Pair(r, c)
                    break
                }
            }
            if (startDot != null) break
        }

        if (startDot == null) return false

        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(startDot)
        visited.add("${startDot.first},${startDot.second}")

        while (queue.isNotEmpty()) {
            val (r, c) = queue.removeFirst()
            if (c > 0 && horizontalEdges[r][c - 1] == SlitherlinkEdgeState.LINE) {
                val key = "$r,${c - 1}"
                if (!visited.contains(key)) { visited.add(key); queue.add(Pair(r, c - 1)) }
            }
            if (c < cols && horizontalEdges[r][c] == SlitherlinkEdgeState.LINE) {
                val key = "$r,${c + 1}"
                if (!visited.contains(key)) { visited.add(key); queue.add(Pair(r, c + 1)) }
            }
            if (r > 0 && verticalEdges[r - 1][c] == SlitherlinkEdgeState.LINE) {
                val key = "${r - 1},$c"
                if (!visited.contains(key)) { visited.add(key); queue.add(Pair(r - 1, c)) }
            }
            if (r < rows && verticalEdges[r][c] == SlitherlinkEdgeState.LINE) {
                val key = "${r + 1},$c"
                if (!visited.contains(key)) { visited.add(key); queue.add(Pair(r + 1, c)) }
            }
        }

        var totalLineDots = 0
        for (r in 0..rows) {
            for (c in 0..cols) {
                if (linesAtDot(r, c) > 0) { totalLineDots++ }
            }
        }

        return visited.size == totalLineDots
    }

    private fun buildOfflineLevel() {
        val gridSize = 5
        rows = gridSize
        cols = gridSize
        difficultyValue = 1
        difficultyName = "beginner"

        // Example Level Clues (5x5):
        // 0 2 - - 3
        // 3 - 3 - -
        // - 1 - 2 2
        // - 2 3 - 2
        // 3 1 - 3 -
        val cluesRaw = listOf(
            listOf(0, 2, null, null, 3),
            listOf(3, null, 3, null, null),
            listOf(null, 1, null, 2, 2),
            listOf(null, 2, 3, null, 2),
            listOf(3, 1, null, 3, null)
        )

        cells = (0 until rows).map { r ->
            (0 until cols).map { c ->
                val clue = if (r < cluesRaw.size && c < cluesRaw[r].size) cluesRaw[r][c] else null
                SlitherlinkCell(r, c, clue)
            }
        }

        horizontalEdges = List(rows + 1) { List(cols) { SlitherlinkEdgeState.NONE } }
        verticalEdges = List(rows) { List(cols + 1) { SlitherlinkEdgeState.NONE } }
    }
}
