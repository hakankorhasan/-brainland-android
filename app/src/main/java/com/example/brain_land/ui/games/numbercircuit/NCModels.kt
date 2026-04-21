package com.example.brain_land.ui.games.numbercircuit

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

// MARK: - Position

data class NCPosition(val row: Int, val col: Int) {
    fun isAdjacent(other: NCPosition): Boolean {
        val dr = abs(row - other.row)
        val dc = abs(col - other.col)
        return dr <= 1 && dc <= 1 && !(dr == 0 && dc == 0)
    }
}

// MARK: - Operator

enum class NCOperator(val symbol: String) {
    ADD("+"),
    SUBTRACT("−"),
    MULTIPLY("×"),
    DIVIDE("÷"),
    POWER("^"),
    COMBINE("⊕");

    val precedence: Int get() = when (this) {
        COMBINE -> 3
        POWER, MULTIPLY, DIVIDE -> 2
        ADD, SUBTRACT -> 1
    }
}

// MARK: - Special Tile

enum class NCSpecialTileType { LOCKED, BOMB, MULTIPLIER, FORCED_OP }

data class NCSpecialTile(
    val position: NCPosition,
    val type: NCSpecialTileType,
    val multiplierValue: Int = 2,
    val forcedOperator: NCOperator? = null
)

// MARK: - Hints

data class NCHints(
    val hint1Position: NCPosition,
    val hint2Positions: List<NCPosition>,
    val hint3Positions: List<NCPosition>,
    val hint3Operator: NCOperator
)

// MARK: - Solution Step

data class NCSolutionStep(
    val position: NCPosition,
    val operatorValue: NCOperator?  // null for first step
)

// MARK: - Level

data class NCLevel(
    val grid: List<List<Int>>,
    val gridSize: Int,
    val target: Int,
    val allowedOperators: List<NCOperator>,
    val specialTiles: List<NCSpecialTile>,
    val solution: List<NCSolutionStep>,
    val solutionExpression: String,
    val hints: NCHints,
    val levelNumber: Int
)
