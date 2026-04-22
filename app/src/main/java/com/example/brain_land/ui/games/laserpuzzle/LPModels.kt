package com.example.brain_land.ui.games.laserpuzzle

// ─────────────────────────────────────────────────────────────────────────────
// LaserDirection — mirrors iOS LaserDirection enum
// ─────────────────────────────────────────────────────────────────────────────

enum class LaserDirection(val rawValue: Int) {
    UP(0), RIGHT(1), DOWN(2), LEFT(3);

    val opposite: LaserDirection get() = when (this) {
        UP    -> DOWN
        DOWN  -> UP
        LEFT  -> RIGHT
        RIGHT -> LEFT
    }

    val dr: Int get() = when (this) {
        UP   -> -1
        DOWN ->  1
        else ->  0
    }

    val dc: Int get() = when (this) {
        LEFT  -> -1
        RIGHT ->  1
        else  ->  0
    }

    val perpendiculars: List<LaserDirection> get() = when (this) {
        UP, DOWN    -> listOf(LEFT, RIGHT)
        LEFT, RIGHT -> listOf(UP, DOWN)
    }

    /** Reflect off "/" mirror */
    fun reflectSlash(): LaserDirection = when (this) {
        RIGHT -> UP
        DOWN  -> LEFT
        LEFT  -> DOWN
        UP    -> RIGHT
    }

    /** Reflect off "\" mirror */
    fun reflectBackslash(): LaserDirection = when (this) {
        RIGHT -> DOWN
        UP    -> LEFT
        LEFT  -> UP
        DOWN  -> RIGHT
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LaserCellType — mirrors iOS LaserCellType enum
// ─────────────────────────────────────────────────────────────────────────────

sealed class LaserCellType {
    object Empty    : LaserCellType()
    data class Source(val direction: LaserDirection) : LaserCellType()
    object Target   : LaserCellType()
    object Mirror   : LaserCellType()
    object Wall     : LaserCellType()
    data class Portal(val pairId: Int) : LaserCellType()
    object Bomb     : LaserCellType()
    object Splitter : LaserCellType()

    val isSource: Boolean get() = this is Source
    val isMirror: Boolean get() = this is Mirror
    val isSplitter: Boolean get() = this is Splitter
    val isPortal: Boolean get() = this is Portal
    val portalId: Int? get() = (this as? Portal)?.pairId
}

// ─────────────────────────────────────────────────────────────────────────────
// LaserCell — mirrors iOS LaserCell struct
// ─────────────────────────────────────────────────────────────────────────────

data class LaserCell(
    val row: Int,
    val col: Int,
    var cellType: LaserCellType = LaserCellType.Empty,
    var mirrorAngle: Int = 0,       // 0 = "/" (slash), 1 = "\" (backslash)
    var isFixed: Boolean = false,
    var isLit: Boolean = false,
    var isHitTarget: Boolean = false,
    var isHitBomb: Boolean = false
) {
    /** Reflect a direction based on current mirror angle */
    fun reflect(dir: LaserDirection): LaserDirection =
        if (mirrorAngle == 0) dir.reflectSlash() else dir.reflectBackslash()

    /** Splitter: returns (passThrough, reflected) directions */
    fun split(dir: LaserDirection): Pair<LaserDirection, LaserDirection> {
        val reflected = reflect(dir)
        return dir to reflected
    }

    /** Toggle mirror angle (/ ↔ \) */
    fun rotateMirror(): LaserCell {
        if (cellType !is LaserCellType.Mirror && cellType !is LaserCellType.Splitter) return this
        if (isFixed) return this
        return copy(mirrorAngle = (mirrorAngle + 1) % 2)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LaserSegment — mirrors iOS LaserSegment struct
// startX/Y and endX/Y are in grid coordinates (col+0.5, row+0.5)
// ─────────────────────────────────────────────────────────────────────────────

data class LaserSegment(
    val startX: Float,  // grid col + 0.5
    val startY: Float,  // grid row + 0.5
    val endX: Float,
    val endY: Float
)

// ─────────────────────────────────────────────────────────────────────────────
// Backend level data models — mirrors iOS LaserPuzzleLevelManager models
// ─────────────────────────────────────────────────────────────────────────────

data class LaserPuzzleCellData(
    val row: Int,
    val col: Int,
    val type: String,
    val direction: String? = null,
    val mirrorAngle: Int? = null,
    val isFixed: Boolean? = null,
    val portalPairId: Int? = null
)

data class LaserSolutionEntry(
    val row: Int,
    val col: Int,
    val correctAngle: Int
)

data class LaserPuzzleLevel(
    val levelNumber: Int,
    val gridSize: Int,
    val difficulty: String,
    val lives: Int,
    val cells: List<LaserPuzzleCellData>,
    val solution: List<LaserSolutionEntry>
)
