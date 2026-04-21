package com.example.brain_land.ui.games.pipeconnect

import java.util.UUID

enum class PipeDirection(val rawValue: Int) {
    UP(0), RIGHT(1), DOWN(2), LEFT(3);

    val opposite: PipeDirection
        get() = when (this) {
            UP -> DOWN
            DOWN -> UP
            LEFT -> RIGHT
            RIGHT -> LEFT
        }

    fun rotated(steps: Int): PipeDirection {
        val newRaw = (this.rawValue + steps) % 4
        return values().first { it.rawValue == (newRaw + 4) % 4 }
    }

    val dr: Int
        get() = when (this) {
            UP -> -1
            DOWN -> 1
            else -> 0
        }

    val dc: Int
        get() = when (this) {
            LEFT -> -1
            RIGHT -> 1
            else -> 0
        }

    companion object {
        fun from(string: String): PipeDirection {
            return when (string.lowercase()) {
                "up" -> UP
                "down" -> DOWN
                "left" -> LEFT
                "right" -> RIGHT
                else -> LEFT
            }
        }
    }
}

enum class PipeType {
    STRAIGHT,   // ─
    ELBOW,      // ┘
    T_PIPE,     // ┬
    CROSS;      // +

    val baseConnections: Set<PipeDirection>
        get() = when (this) {
            STRAIGHT -> setOf(PipeDirection.LEFT, PipeDirection.RIGHT)
            ELBOW    -> setOf(PipeDirection.UP, PipeDirection.RIGHT)
            T_PIPE   -> setOf(PipeDirection.UP, PipeDirection.LEFT, PipeDirection.RIGHT)
            CROSS    -> setOf(PipeDirection.UP, PipeDirection.DOWN, PipeDirection.LEFT, PipeDirection.RIGHT)
        }

    fun connections(rotation: Int): Set<PipeDirection> {
        return baseConnections.map { it.rotated(rotation) }.toSet()
    }

    companion object {
        fun from(string: String): PipeType {
            return when (string.lowercase()) {
                "straight" -> STRAIGHT
                "elbow"    -> ELBOW
                "tpipe"    -> T_PIPE
                "cross"    -> CROSS
                else       -> STRAIGHT
            }
        }
    }
}

data class PipeCell(
    val id: String = UUID.randomUUID().toString(),
    val row: Int,
    val col: Int,
    var pipeType: PipeType,
    var rotation: Int,               // 0..3 (*90 deg)
    var isSource: Boolean = false,
    var isSink: Boolean = false,
    var isBlocked: Boolean = false,
    var isLocked: Boolean = false,
    // Deterministic wall variant (1-4) based on position — stable across recompose
    val wallVariant: Int = ((row * 3 + col * 7) % 4) + 1,
    var isFilled: Boolean = false,
    var isLeaking: Boolean = false,
    var fillOrder: Int = -1,
    var waterDirections: Set<PipeDirection> = emptySet(),
    var waterEntry: PipeDirection? = null
) {
    val connections: Set<PipeDirection>
        get() = if (isBlocked) emptySet() else pipeType.connections(rotation)

    fun connects(to: PipeDirection): Boolean {
        return connections.contains(to)
    }
}

enum class FlowResult {
    SUCCESS,
    FAILED,
    NO_CONNECTION
}
