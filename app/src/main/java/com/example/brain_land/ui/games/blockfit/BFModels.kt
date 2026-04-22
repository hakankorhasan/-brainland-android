package com.example.brain_land.ui.games.blockfit

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Block shape templates — mirrors iOS blockShapeTemplates
// ─────────────────────────────────────────────────────────────────────────────

val blockShapeTemplates: List<List<Pair<Int, Int>>> = listOf(
    // Single
    listOf(0 to 0),
    // Domino
    listOf(0 to 0, 0 to 1),
    listOf(0 to 0, 1 to 0),
    // Triominos
    listOf(0 to 0, 0 to 1, 0 to 2),
    listOf(0 to 0, 1 to 0, 2 to 0),
    listOf(0 to 0, 0 to 1, 1 to 0),
    listOf(0 to 0, 0 to 1, 1 to 1),
    listOf(0 to 0, 1 to 0, 1 to 1),
    listOf(0 to 1, 1 to 0, 1 to 1),
    // Tetrominoes
    listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3),   // I horizontal
    listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0),   // I vertical
    listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1),   // O
    listOf(0 to 0, 0 to 1, 0 to 2, 1 to 0),   // L
    listOf(0 to 0, 0 to 1, 0 to 2, 1 to 2),   // J
    listOf(0 to 0, 1 to 0, 1 to 1, 2 to 1),   // S
    listOf(0 to 1, 1 to 0, 1 to 1, 2 to 0),   // Z
    listOf(0 to 0, 0 to 1, 0 to 2, 1 to 1),   // T
    // Pentominoes
    listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3, 0 to 4),   // I5
    listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0, 4 to 0),   // I5 vertical
    listOf(0 to 0, 0 to 1, 0 to 2, 1 to 0, 2 to 0),   // L5
    listOf(0 to 0, 0 to 1, 0 to 2, 1 to 2, 2 to 2),   // J5
    listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2, 2 to 2),   // S5
    listOf(0 to 0, 1 to 0, 1 to 1, 2 to 1, 2 to 2),   // S5b
    // 2×2 block
    listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1),
    // 3×3 block
    listOf(0 to 0, 0 to 1, 0 to 2, 1 to 0, 1 to 1, 1 to 2, 2 to 0, 2 to 1, 2 to 2),
    // Corner shapes
    listOf(0 to 0, 0 to 1, 0 to 2, 1 to 0, 2 to 0),
    listOf(0 to 0, 0 to 1, 0 to 2, 1 to 2, 2 to 2),
    listOf(0 to 0, 1 to 0, 2 to 0, 2 to 1, 2 to 2),
    listOf(0 to 2, 1 to 2, 2 to 0, 2 to 1, 2 to 2),
)

// ─────────────────────────────────────────────────────────────────────────────
// Block colors — mirrors iOS blockColors
// ─────────────────────────────────────────────────────────────────────────────

val blockColors: List<Color> = listOf(
    Color(0xFFE74C3C),   // Red
    Color(0xFF3498DB),   // Blue
    Color(0xFF2ECC71),   // Green
    Color(0xFFF1C40F),   // Yellow
    Color(0xFF9B59B6),   // Purple
    Color(0xFFE67E22),   // Orange
    Color(0xFF1ABC9C),   // Teal
    Color(0xFFE84393),   // Pink
)

// ─────────────────────────────────────────────────────────────────────────────
// BlockShape — mirrors iOS BlockShape
// ─────────────────────────────────────────────────────────────────────────────

data class BlockShape(
    val id: Int,          // slot index
    val cells: List<Pair<Int, Int>>,   // (row, col) offsets from top-left
    val color: Color
) {
    val width: Int  get() = (cells.maxOfOrNull { it.second } ?: 0) + 1
    val height: Int get() = (cells.maxOfOrNull { it.first  } ?: 0) + 1
}

fun randomBlockShape(slotId: Int): BlockShape {
    val template = blockShapeTemplates.random()
    val color    = blockColors.random()
    return BlockShape(id = slotId, cells = template, color = color)
}
