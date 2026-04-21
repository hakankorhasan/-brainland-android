package com.example.brain_land.ui.games.numbercircuit

import android.content.Context
import androidx.compose.runtime.*
import kotlinx.coroutines.*

class NCGameState(
    private val context: Context,
    private val scope: CoroutineScope
) {
    var level by mutableStateOf(NCLevelGenerator.generateLevel(1))
        private set
    var levelNumber by mutableIntStateOf(1)
        private set
    var selectedPath by mutableStateOf<List<NCPosition>>(emptyList())
        private set
    var chosenOperators by mutableStateOf<List<NCOperator>>(emptyList())
        private set
    var moveCount by mutableIntStateOf(0)
        private set
    var isSolved by mutableStateOf(false)
        private set
    var isWrong by mutableStateOf(false)
        private set
    var hintLevel by mutableIntStateOf(0)
        private set
    var needsOperator by mutableStateOf(false)
        private set

    init {
        val prefs = context.getSharedPreferences("number_circuit_prefs", Context.MODE_PRIVATE)
        levelNumber = prefs.getInt("currentLevel", 1)
        level = NCLevelGenerator.generateLevel(levelNumber)
    }

    val gridSize: Int get() = level.gridSize

    val expressionString: String get() {
        if (selectedPath.isEmpty()) return ""
        val parts = mutableListOf<String>()
        selectedPath.forEachIndexed { i, pos ->
            parts.add("${level.grid[pos.row][pos.col]}")
            if (i < chosenOperators.size) parts.add(chosenOperators[i].symbol)
        }
        return parts.joinToString(" ")
    }

    val currentResult: Int? get() {
        if (selectedPath.size < 2) return null
        if (chosenOperators.size != selectedPath.size - 1) return null
        val values = selectedPath.map { level.grid[it.row][it.col] }
        val result = NCLevelGenerator.evaluateExpression(values, chosenOperators) ?: return null
        val intResult = result.toInt()
        return if (result == intResult.toDouble()) intResult else null
    }

    val isExpressionComplete: Boolean get() =
        selectedPath.size >= 2 && chosenOperators.size == selectedPath.size - 1

    // MARK: - Actions

    fun selectTile(row: Int, col: Int) {
        val pos = NCPosition(row, col)
        if (needsOperator) return

        val existingIdx = selectedPath.indexOf(pos)
        if (existingIdx >= 0) {
            val newPath = selectedPath.take(existingIdx)
            selectedPath = newPath
            val opsToKeep = maxOf(0, newPath.size - 1)
            chosenOperators = chosenOperators.take(opsToKeep)
            needsOperator = false
            return
        }

        if (selectedPath.isEmpty()) {
            selectedPath = listOf(pos)
            moveCount++
            return
        }

        val last = selectedPath.last()
        if (!pos.isAdjacent(last)) return

        selectedPath = selectedPath + pos
        moveCount++
        needsOperator = true
    }

    fun setOperator(op: NCOperator) {
        if (!needsOperator) return
        if (!level.allowedOperators.contains(op)) return
        chosenOperators = chosenOperators + op
        needsOperator = false
    }

    fun undoLast() {
        if (needsOperator && selectedPath.size > 1) {
            selectedPath = selectedPath.dropLast(1)
            needsOperator = false
        } else if (chosenOperators.isNotEmpty()) {
            chosenOperators = chosenOperators.dropLast(1)
            if (selectedPath.size > 1) selectedPath = selectedPath.dropLast(1)
        } else if (selectedPath.isNotEmpty()) {
            selectedPath = selectedPath.dropLast(1)
        }
    }

    fun submit(): Boolean {
        if (!isExpressionComplete) return false
        val result = currentResult ?: run { triggerWrong(); return false }
        return if (result == level.target) {
            isSolved = true
            val prefs = context.getSharedPreferences("number_circuit_prefs", Context.MODE_PRIVATE)
            prefs.edit().putInt("currentLevel", levelNumber + 1).apply()
            true
        } else {
            triggerWrong()
            false
        }
    }

    fun resetSelection() {
        selectedPath = emptyList()
        chosenOperators = emptyList()
        needsOperator = false
        isWrong = false
        hintLevel = 0
    }

    fun resetPuzzle() {
        resetSelection()
        moveCount = 0
        isSolved = false
        level = NCLevelGenerator.generateLevel(levelNumber)
    }

    fun nextLevel() {
        levelNumber++
        isSolved = false
        resetSelection()
        moveCount = 0
        level = NCLevelGenerator.generateLevel(levelNumber)
        val prefs = context.getSharedPreferences("number_circuit_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("currentLevel", levelNumber).apply()
    }

    fun showNextHint() {
        if (hintLevel < 3) hintLevel++
    }

    private fun triggerWrong() {
        isWrong = true
        scope.launch {
            delay(800)
            isWrong = false
        }
    }
}
