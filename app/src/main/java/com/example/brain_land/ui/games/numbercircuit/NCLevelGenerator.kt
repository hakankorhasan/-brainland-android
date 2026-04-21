package com.example.brain_land.ui.games.numbercircuit

import kotlin.math.*

// MARK: - Mulberry32 Seeded PRNG (mirrors iOS)

class Mulberry32(seed: UInt) {
    private var state: UInt = seed

    fun next(): Double {
        state += 0x6D2B79F5u
        var t = state
        t = (t xor (t shr 15)) * (t or 1u)
        t = t xor (t + (t xor (t shr 7)) * (t or 61u))
        val result = t xor (t shr 14)
        return result.toLong().and(0xFFFFFFFFL).toDouble() / 0xFFFFFFFFL.toDouble()
    }

    fun nextInt(min: Int, max: Int): Int {
        val range = max - min + 1
        return min + (next() * range).toInt().coerceIn(0, range - 1)
    }

    fun <T> shuffled(array: List<T>): List<T> {
        val arr = array.toMutableList()
        for (i in arr.size - 1 downTo 1) {
            val j = nextInt(0, i)
            val tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp
        }
        return arr
    }
}

// MARK: - Level Config

data class LevelConfig(
    val gridSize: Int,
    val allowedOps: List<NCOperator>,
    val pathLengthMin: Int,
    val pathLengthMax: Int,
    val specialTileTypes: List<NCSpecialTileType>
)

// MARK: - Level Generator

object NCLevelGenerator {

    fun getLevelConfig(levelNumber: Int): LevelConfig = when {
        levelNumber <= 10 -> LevelConfig(3, listOf(NCOperator.ADD, NCOperator.SUBTRACT), 2, 3, emptyList())
        levelNumber <= 25 -> LevelConfig(4, listOf(NCOperator.ADD, NCOperator.SUBTRACT, NCOperator.MULTIPLY), 2, 4, listOf(NCSpecialTileType.LOCKED))
        levelNumber <= 60 -> LevelConfig(5, listOf(NCOperator.ADD, NCOperator.SUBTRACT, NCOperator.MULTIPLY, NCOperator.DIVIDE), 3, 5, listOf(NCSpecialTileType.LOCKED, NCSpecialTileType.MULTIPLIER, NCSpecialTileType.BOMB))
        else -> LevelConfig(6, NCOperator.values().toList(), 3, 6, NCSpecialTileType.values().toList())
    }

    fun generateLevel(levelNumber: Int): NCLevel {
        val seed = hashSeed(levelNumber)
        val rand = Mulberry32(seed)
        val config = getLevelConfig(levelNumber)

        repeat(100) {
            val grid = generateGrid(config.gridSize, rand)
            tryGenerateLevel(grid, config, levelNumber, rand)?.let { return it }
        }
        return fallbackLevel(levelNumber, config)
    }

    private fun hashSeed(levelNumber: Int): UInt {
        val str = "level-$levelNumber"
        var hash: UInt = 0u
        for (c in str) {
            hash = hash * 31u + c.code.toUInt()
        }
        return if (hash == 0u) 1u else hash
    }

    fun generateGrid(size: Int, rand: Mulberry32): List<List<Int>> =
        List(size) { List(size) { rand.nextInt(1, 9) } }

    private fun generatePath(grid: List<List<Int>>, config: LevelConfig, rand: Mulberry32): List<NCPosition>? {
        val size = config.gridSize
        val pathLength = rand.nextInt(config.pathLengthMin, config.pathLengthMax)
        val startRow = rand.nextInt(0, size - 1)
        val startCol = rand.nextInt(0, size - 1)
        val path = mutableListOf(NCPosition(startRow, startCol))
        val visited = mutableSetOf(path[0])

        repeat(pathLength - 1) {
            val current = path.last()
            val neighbors = mutableListOf<NCPosition>()
            for (dr in -1..1) for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val nr = current.row + dr; val nc = current.col + dc
                if (nr in 0 until size && nc in 0 until size) {
                    val pos = NCPosition(nr, nc)
                    if (!visited.contains(pos)) neighbors.add(pos)
                }
            }
            if (neighbors.isEmpty()) return@repeat
            val next = rand.shuffled(neighbors)[0]
            path.add(next); visited.add(next)
        }
        return if (path.size >= 2) path else null
    }

    private fun assignOperators(pathLength: Int, allowedOps: List<NCOperator>, rand: Mulberry32): List<NCOperator> =
        List(pathLength - 1) { allowedOps[rand.nextInt(0, allowedOps.size - 1)] }

    fun evaluateExpression(values: List<Int>, operators: List<NCOperator>): Double? {
        if (values.size != operators.size + 1) return null
        val vals = values.map { it.toDouble() }.toMutableList()
        val ops = operators.toMutableList()

        // Pass 1: combine ⊕
        var i = 0
        while (i < ops.size) {
            if (ops[i] == NCOperator.COMBINE) {
                val left = vals[i].toInt(); val right = vals[i + 1].toInt()
                val digits = if (right > 0) (log10(right.toDouble()).toInt() + 1) else 1
                vals[i] = left.toDouble() * 10.0.pow(digits) + right
                vals.removeAt(i + 1); ops.removeAt(i)
            } else i++
        }

        // Pass 2: ^ × ÷
        i = 0
        while (i < ops.size) {
            if (ops[i].precedence == 2) {
                val result = when (ops[i]) {
                    NCOperator.POWER -> vals[i].pow(vals[i + 1])
                    NCOperator.MULTIPLY -> vals[i] * vals[i + 1]
                    NCOperator.DIVIDE -> if (vals[i + 1] != 0.0) vals[i] / vals[i + 1] else return null
                    else -> 0.0
                }
                vals[i] = result; vals.removeAt(i + 1); ops.removeAt(i)
            } else i++
        }

        // Pass 3: + −
        i = 0
        while (i < ops.size) {
            val result = when (ops[i]) {
                NCOperator.ADD -> vals[i] + vals[i + 1]
                NCOperator.SUBTRACT -> vals[i] - vals[i + 1]
                else -> 0.0
            }
            vals[i] = result; vals.removeAt(i + 1); ops.removeAt(i)
        }
        return vals.firstOrNull()
    }

    private fun tryGenerateLevel(grid: List<List<Int>>, config: LevelConfig, levelNumber: Int, rand: Mulberry32): NCLevel? {
        val path = generatePath(grid, config, rand) ?: return null
        val ops = assignOperators(path.size, config.allowedOps, rand)
        val values = path.map { grid[it.row][it.col] }
        val result = evaluateExpression(values, ops) ?: return null
        val target = result.toInt()
        if (result != target.toDouble()) return null
        if (target <= 0 || target > 9999) return null

        val solutionSteps = path.mapIndexed { idx, pos ->
            NCSolutionStep(pos, if (idx > 0) ops[idx - 1] else null)
        }
        val expressionStr = buildExpressionString(values, ops)
        val specials = generateSpecialTiles(grid, path, config, rand)
        val hints = generateHints(path, ops)

        return NCLevel(grid, config.gridSize, target, config.allowedOps, specials, solutionSteps, expressionStr, hints, levelNumber)
    }

    private fun buildExpressionString(values: List<Int>, operators: List<NCOperator>): String {
        val parts = mutableListOf<String>()
        values.forEachIndexed { i, v ->
            parts.add("$v")
            if (i < operators.size) parts.add(operators[i].symbol)
        }
        return parts.joinToString(" ")
    }

    private fun generateSpecialTiles(grid: List<List<Int>>, path: List<NCPosition>, config: LevelConfig, rand: Mulberry32): List<NCSpecialTile> {
        if (config.specialTileTypes.isEmpty()) return emptyList()
        val specials = mutableListOf<NCSpecialTile>()
        val pathSet = path.toSet()

        for (tileType in config.specialTileTypes) {
            when (tileType) {
                NCSpecialTileType.LOCKED -> {
                    if (path.size >= 2) {
                        val idx = rand.nextInt(0, path.size - 1)
                        specials.add(NCSpecialTile(path[idx], NCSpecialTileType.LOCKED))
                    }
                }
                NCSpecialTileType.BOMB -> {
                    val nonPath = allPositions(config.gridSize).filter { !pathSet.contains(it) }
                    if (nonPath.isNotEmpty()) specials.add(NCSpecialTile(rand.shuffled(nonPath)[0], NCSpecialTileType.BOMB))
                }
                NCSpecialTileType.MULTIPLIER -> {
                    val nonPath = allPositions(config.gridSize).filter { !pathSet.contains(it) && specials.none { s -> s.position == it } }
                    if (nonPath.isNotEmpty()) {
                        val mult = rand.nextInt(2, 3)
                        specials.add(NCSpecialTile(rand.shuffled(nonPath)[0], NCSpecialTileType.MULTIPLIER, mult))
                    }
                }
                NCSpecialTileType.FORCED_OP -> {
                    if (path.size >= 3) {
                        val idx = rand.nextInt(1, path.size - 1)
                        val opIdx = rand.nextInt(0, config.allowedOps.size - 1)
                        specials.add(NCSpecialTile(path[idx], NCSpecialTileType.FORCED_OP, forcedOperator = config.allowedOps[opIdx]))
                    }
                }
            }
        }
        return specials
    }

    private fun generateHints(path: List<NCPosition>, operators: List<NCOperator>): NCHints {
        val p1 = path[0]
        val p2 = if (path.size > 1) listOf(path[0], path[1]) else listOf(path[0])
        val op = if (operators.isEmpty()) NCOperator.ADD else operators[0]
        return NCHints(p1, p2, p2, op)
    }

    private fun allPositions(size: Int): List<NCPosition> =
        (0 until size).flatMap { r -> (0 until size).map { c -> NCPosition(r, c) } }

    private fun fallbackLevel(levelNumber: Int, config: LevelConfig): NCLevel {
        val grid = List(config.gridSize) { List(config.gridSize) { (1..9).random() } }
        val a = grid[0][0]; val b = grid[0][1]; val target = a + b
        return NCLevel(
            grid, config.gridSize, target, config.allowedOps, emptyList(),
            listOf(NCSolutionStep(NCPosition(0, 0), null), NCSolutionStep(NCPosition(0, 1), NCOperator.ADD)),
            "$a + $b",
            NCHints(NCPosition(0, 0), listOf(NCPosition(0, 0), NCPosition(0, 1)), listOf(NCPosition(0, 0), NCPosition(0, 1)), NCOperator.ADD),
            levelNumber
        )
    }

    private fun Double.pow(exp: Double): Double = Math.pow(this, exp)
}
