package com.example.brain_land.ui.games.nonogram

import androidx.compose.ui.graphics.Color

// MARK: - Cell State (empty → filled → marked → empty)
enum class NonogramCellState {
    EMPTY, FILLED, MARKED;

    val next: NonogramCellState get() = when (this) {
        EMPTY  -> FILLED
        FILLED -> MARKED
        MARKED -> EMPTY
    }
}

// MARK: - Cell Data
data class NonogramCell(
    val row: Int,
    val col: Int,
    val isSolution: Boolean,
    var state: NonogramCellState = NonogramCellState.EMPTY
)

// MARK: - Level Data (from backend)
data class NonogramLevel(
    val levelNumber: Int,
    val gridSize: Int,
    val fillFraction: Double,
    val solution: List<List<Boolean>>,
    val rowClues: List<List<Int>>,
    val colClues: List<List<Int>>
)

// MARK: - Excavation Color Palette (mirrors iOS ExcavationColors)
object ExcavationColors {
    val soil        = Color(0x1F, 0x23, 0x36)   // Dark Indigo (Cavern Rock)
    val soilLight   = Color(0x29, 0x2E, 0x47)   // Lighter Rock
    val excavated   = Color(0x00, 0xE5, 0xFF)   // Glowing Cyan / Neon Gem
    val bone        = Color(0x00, 0xB2, 0xD9)   // Inner Gem color
    val boneGlow    = Color(0x8F, 0xE1, 0xED)   // Bright Cyan Glow
    val sandDark    = Color(0x14, 0x17, 0x29)   // Background Deep Void
    val sandMid     = Color(0x1A, 0x1C, 0x2E)   // Mid Void
    val accent      = Color(0xFF00E5FF)          // Cyan Accent
    val radarGreen  = Color(0xFFA68BFA)          // Purple/Magenta (Clues)
    val flagRed     = Color(0xFFF23D59)          // Rose Red for 'X'
}
