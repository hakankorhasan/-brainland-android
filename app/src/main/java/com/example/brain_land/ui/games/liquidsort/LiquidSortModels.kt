package com.example.brain_land.ui.games.liquidsort

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────────────────────
// LiquidColor — mirrors iOS LiquidColor enum
// ─────────────────────────────────────────────────────────────────────────────

enum class LiquidColor {
    RUBY, SAPPHIRE, EMERALD, AMBER, VIOLET, CORAL, CYAN, MAGENTA,
    LIME, ROSE, SKY, MINT, PEACH, LAVENDER, GOLD;

    val topColor: Color get() = when (this) {
        RUBY     -> Color(0xFFFF2020)
        CORAL    -> Color(0xFFFF7800)
        AMBER    -> Color(0xFFFFCC00)
        LIME     -> Color(0xFF88FF00)
        EMERALD  -> Color(0xFF00CC55)
        MINT     -> Color(0xFF007766)
        CYAN     -> Color(0xFF00DDFF)
        SAPPHIRE -> Color(0xFF1155FF)
        SKY      -> Color(0xFF4400CC)
        VIOLET   -> Color(0xFF9900FF)
        MAGENTA  -> Color(0xFFFF0099)
        ROSE     -> Color(0xFFFF4488)
        PEACH    -> Color(0xFFBB6600)
        LAVENDER -> Color(0xFF00BB88)
        GOLD     -> Color(0xFF880022)
    }

    val bottomColor: Color get() = when (this) {
        RUBY     -> Color(0xFFCC0000)
        CORAL    -> Color(0xFFCC4C00)
        AMBER    -> Color(0xFFCC9900)
        LIME     -> Color(0xFF55BB00)
        EMERALD  -> Color(0xFF008833)
        MINT     -> Color(0xFF004433)
        CYAN     -> Color(0xFF0099CC)
        SAPPHIRE -> Color(0xFF0033CC)
        SKY      -> Color(0xFF220077)
        VIOLET   -> Color(0xFF6600CC)
        MAGENTA  -> Color(0xFFCC0066)
        ROSE     -> Color(0xFFCC2255)
        PEACH    -> Color(0xFF884400)
        LAVENDER -> Color(0xFF008855)
        GOLD     -> Color(0xFF550011)
    }

    val highlightColor: Color get() = when (this) {
        RUBY     -> Color(0xFFFF7777)
        CORAL    -> Color(0xFFFFBB66)
        AMBER    -> Color(0xFFFFEE77)
        LIME     -> Color(0xFFCCFF66)
        EMERALD  -> Color(0xFF66FF99)
        MINT     -> Color(0xFF44CCAA)
        CYAN     -> Color(0xFF66EEFF)
        SAPPHIRE -> Color(0xFF6699FF)
        SKY      -> Color(0xFF9966FF)
        VIOLET   -> Color(0xFFCC66FF)
        MAGENTA  -> Color(0xFFFF66CC)
        ROSE     -> Color(0xFFFF88BB)
        PEACH    -> Color(0xFFDDAA66)
        LAVENDER -> Color(0xFF55DDBB)
        GOLD     -> Color(0xFFCC4466)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottle — mirrors iOS Bottle struct
// layers = bottom → top order
// ─────────────────────────────────────────────────────────────────────────────

data class Bottle(
    val id: Int,
    val layers: MutableList<LiquidColor> = mutableListOf(),
    val capacity: Int = 4
) {
    val isEmpty:   Boolean get() = layers.isEmpty()
    val isFull:    Boolean get() = layers.size >= capacity
    val freeSlots: Int     get() = capacity - layers.size
    val topColor:  LiquidColor? get() = layers.lastOrNull()

    val topGroupCount: Int get() {
        val top = topColor ?: return 0
        var count = 0
        for (layer in layers.reversed()) {
            if (layer == top) count++ else break
        }
        return count
    }

    val isComplete: Boolean get() = isFull && layers.all { it == layers.first() }
    val isUniform:  Boolean get() = layers.isEmpty() || layers.all { it == layers.first() }
}

fun Bottle.copy(): Bottle = Bottle(
    id       = id,
    layers   = layers.toMutableList(),
    capacity = capacity
)

// ─────────────────────────────────────────────────────────────────────────────
// PourMove — for undo
// ─────────────────────────────────────────────────────────────────────────────

data class PourMove(
    val sourceIndex: Int,
    val targetIndex: Int,
    val layerCount:  Int,
    val color:       LiquidColor
)

// ─────────────────────────────────────────────────────────────────────────────
// LiquidSortGenerator — mirrors iOS LiquidSortGenerator exactly
// ─────────────────────────────────────────────────────────────────────────────

object LiquidSortGenerator {

    private val allColors = LiquidColor.entries

    fun generate(levelNumber: Int): List<Bottle> {
        val config = configuration(levelNumber)
        val colors = allColors.take(config.colorCount)

        // Start with solved state
        val bottles = mutableListOf<Bottle>()
        colors.forEachIndexed { i, color ->
            bottles.add(Bottle(id = i, layers = MutableList(4) { color }))
        }
        // Add empty bottles
        repeat(config.emptyBottles) { j ->
            bottles.add(Bottle(id = colors.size + j))
        }

        // Shuffle by random reverse pours
        val shuffled = shuffle(bottles, config.shuffleMoves)
        return shuffled
    }

    fun configuration(level: Int): LevelConfig {
        val bottleCount  = bottlesForLevel(level)
        val colorCount   = bottleCount - 2
        val shuffleMoves = minOf(20 + colorCount * 12, 200)
        return LevelConfig(colorCount, 2, shuffleMoves)
    }

    fun bottlesForLevel(level: Int): Int = when (level) {
        in 1..3   -> 5
        in 4..6   -> 6
        in 7..10  -> 7
        in 11..15 -> 8
        in 16..22 -> 9
        in 23..30 -> 10
        in 31..40 -> 11
        in 41..52 -> 12
        in 53..66 -> 13
        in 67..82 -> 14
        else      -> 15
    }

    fun difficultyLevel(level: Int): Int = when (level) {
        in 1..20   -> 2
        in 21..50  -> 4
        in 51..100 -> 6
        in 101..200 -> 8
        else        -> 10
    }

    fun calculateScore(level: Int, timeElapsed: Int, undoCount: Int): Int {
        val bottles  = bottlesForLevel(level)
        val basePts  = (bottles * 15).toDouble()
        val timePen  = minOf(timeElapsed * 0.5, basePts * 0.60)
        val undoPen  = undoCount * 10.0
        val raw      = basePts - timePen - undoPen
        return maxOf(raw.toInt(), (basePts * 0.15).toInt())
    }

    private fun shuffle(bottles: MutableList<Bottle>, moves: Int): MutableList<Bottle> {
        val result = bottles.map { it.copy() }.toMutableList()
        var lastSource = -1
        var lastTarget = -1

        repeat(moves) {
            val validPairs = mutableListOf<Pair<Int, Int>>()
            for (s in result.indices) {
                if (result[s].isEmpty) continue  // skip empty sources, don't abort the whole move
                for (t in result.indices) {
                    if (s == t) continue
                    if (result[t].isFull) continue
                    if (s == lastTarget && t == lastSource) continue
                    validPairs.add(s to t)
                }
            }
            if (validPairs.isEmpty()) return@repeat

            val (source, target) = validPairs.random()
            val layer = result[source].layers.removeLast()
            result[target].layers.add(layer)
            lastSource = source; lastTarget = target
        }

        // Re-shuffle if accidentally solved
        if (result.all { it.isEmpty || it.isComplete }) {
            return shuffle(bottles, moves + 20)
        }
        return result
    }

    data class LevelConfig(val colorCount: Int, val emptyBottles: Int, val shuffleMoves: Int)
}
