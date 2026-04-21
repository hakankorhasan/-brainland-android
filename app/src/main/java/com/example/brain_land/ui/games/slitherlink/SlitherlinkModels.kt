package com.example.brain_land.ui.games.slitherlink

enum class SlitherlinkEdgeState {
    NONE, // untouched
    LINE, // player placed a line
    CROSS; // player marked as no-line (X)

    val next: SlitherlinkEdgeState
        get() = when (this) {
            NONE  -> LINE
            LINE  -> CROSS
            CROSS -> NONE
        }
}

data class SlitherlinkCell(
    val row: Int,
    val col: Int,
    val clue: Int? = null,
    var isError: Boolean = false
)
