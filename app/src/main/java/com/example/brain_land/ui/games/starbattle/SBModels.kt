package com.example.brain_land.ui.games.starbattle

// ─────────────────────────────────────────────────────────────────────────────
// Cell State — mirrors iOS GBCellState (empty → star → empty)
// ─────────────────────────────────────────────────────────────────────────────

enum class GBCellState {
    EMPTY, STAR;
    val next get() = if (this == EMPTY) STAR else EMPTY
}

// ─────────────────────────────────────────────────────────────────────────────
// Cell
// ─────────────────────────────────────────────────────────────────────────────

data class GBCell(
    val row: Int,
    val col: Int,
    val regionId: Int,
    var state: GBCellState = GBCellState.EMPTY,
    var isError: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// Level — mirrors iOS GalacticBeaconsAPILevel
// ─────────────────────────────────────────────────────────────────────────────

data class SBLevel(
    val levelNumber: Int,
    val gridSize: Int,
    val beaconsPerUnit: Int,
    val difficulty: String,
    val difficultyValue: Int,
    val regions: List<List<Int>>,        // [row][col] → regionId
    val solution: List<List<Boolean>>,   // [row][col] → isBeacon
    val regionColors: List<Int>          // regionId → colorIndex
)

// ─────────────────────────────────────────────────────────────────────────────
// Nebula Color Palette — mirrors iOS nebulaColorPairs
// ─────────────────────────────────────────────────────────────────────────────

data class NebulaColors(val bg: Long, val accent: Long)

val nebulaColorPairs: List<NebulaColors> = listOf(
    NebulaColors(0xFF3B1578, 0xFF9B59FF), // purple
    NebulaColors(0xFF0E4D6E, 0xFF38BDF8), // blue
    NebulaColors(0xFF6B1D4A, 0xFFF472B6), // pink
    NebulaColors(0xFF0D5C4E, 0xFF2DD4BF), // teal
    NebulaColors(0xFF7C2D12, 0xFFFB923C), // orange
    NebulaColors(0xFF1E3A5F, 0xFF60A5FA), // sky
    NebulaColors(0xFF5B1A1A, 0xFFF87171), // red
    NebulaColors(0xFF3B3080, 0xFFA78BFA), // indigo
    NebulaColors(0xFF064E3B, 0xFF34D399), // emerald
    NebulaColors(0xFF713F12, 0xFFFFBF24), // amber
)
