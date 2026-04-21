package com.example.brain_land.ui.games.pipeconnect

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

fun DrawScope.drawPipeShape(connections: Set<PipeDirection>, color: Color, backgroundColor: Color) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val pipeWidth = size.width * 0.42f
    val innerWidth = pipeWidth * 0.55f
    
    val bgDark = backgroundColor
    val bgLight = backgroundColor.copy(alpha = 0.6f)
    val fgDark = color
    val fgLight = color.copy(alpha = 0.5f)

    // Helper to get cylinder gradient for a specific area (horizontal or vertical)
    fun getCasingBrush(isVertical: Boolean): androidx.compose.ui.graphics.Brush {
        val start = if (isVertical) Offset(center.x - pipeWidth/2f, 0f) else Offset(0f, center.y - pipeWidth/2f)
        val end = if (isVertical) Offset(center.x + pipeWidth/2f, 0f) else Offset(0f, center.y + pipeWidth/2f)
        return androidx.compose.ui.graphics.Brush.linearGradient(
            0.0f to bgDark,
            0.2f to bgLight,
            0.5f to bgDark.copy(alpha = 0.8f),
            0.8f to bgLight,
            1.0f to bgDark,
            start = start, end = end
        )
    }

    fun getInnerBrush(isVertical: Boolean): androidx.compose.ui.graphics.Brush {
        val start = if (isVertical) Offset(center.x - innerWidth/2f, 0f) else Offset(0f, center.y - innerWidth/2f)
        val end = if (isVertical) Offset(center.x + innerWidth/2f, 0f) else Offset(0f, center.y + innerWidth/2f)
        return androidx.compose.ui.graphics.Brush.linearGradient(
            0.0f to fgDark,
            0.5f to fgLight,
            1.0f to fgDark,
            start = start, end = end
        )
    }

    // 1. Draw Junction Outer Base
    val junctionSize = pipeWidth * 1.1f
    drawOval(
        brush = androidx.compose.ui.graphics.Brush.radialGradient(
            colors = listOf(bgLight, bgDark)
        ),
        topLeft = Offset(center.x - junctionSize / 2f, center.y - junctionSize / 2f),
        size = Size(junctionSize, junctionSize)
    )

    // 2. Draw Outer Arms
    connections.forEach { dir ->
        val isVertical = dir == PipeDirection.UP || dir == PipeDirection.DOWN
        val tleft: Offset
        val tsize: Size

        when (dir) {
            PipeDirection.UP -> {
                tleft = Offset(center.x - pipeWidth / 2f, 0f)
                tsize = Size(pipeWidth, center.y)
            }
            PipeDirection.DOWN -> {
                tleft = Offset(center.x - pipeWidth / 2f, center.y)
                tsize = Size(pipeWidth, center.y)
            }
            PipeDirection.LEFT -> {
                tleft = Offset(0f, center.y - pipeWidth / 2f)
                tsize = Size(center.x, pipeWidth)
            }
            PipeDirection.RIGHT -> {
                tleft = Offset(center.x, center.y - pipeWidth / 2f)
                tsize = Size(center.x, pipeWidth)
            }
        }
        drawRect(
            brush = getCasingBrush(isVertical),
            topLeft = tleft,
            size = tsize
        )

        // Draw joint rim at the edge of the tile for realistic look
        val rimThickness = size.width * 0.08f
        val rimStart = when(dir) {
            PipeDirection.UP -> Offset(center.x - pipeWidth/2f - 2f, 0f)
            PipeDirection.DOWN -> Offset(center.x - pipeWidth/2f - 2f, size.height - rimThickness)
            PipeDirection.LEFT -> Offset(0f, center.y - pipeWidth/2f - 2f)
            PipeDirection.RIGHT -> Offset(size.width - rimThickness, center.y - pipeWidth/2f - 2f)
        }
        val rimSize = when(dir) {
            PipeDirection.UP, PipeDirection.DOWN -> Size(pipeWidth + 4f, rimThickness)
            PipeDirection.LEFT, PipeDirection.RIGHT -> Size(rimThickness, pipeWidth + 4f)
        }
        drawRoundRect(
            color = bgDark,
            topLeft = rimStart,
            size = rimSize,
            cornerRadius = CornerRadius(4f)
        )
    }

    // 3. Draw Junction Connector Ring
    val ringSize = pipeWidth * 0.9f
    drawOval(
        color = Color(0xFF1E212B),
        topLeft = Offset(center.x - ringSize / 2f, center.y - ringSize / 2f),
        size = Size(ringSize, ringSize)
    )

    // 4. Draw Inner Arms
    connections.forEach { dir ->
        val isVertical = dir == PipeDirection.UP || dir == PipeDirection.DOWN
        val tleft: Offset
        val tsize: Size

        when (dir) {
            PipeDirection.UP -> {
                tleft = Offset(center.x - innerWidth / 2f, 0f)
                tsize = Size(innerWidth, center.y)
            }
            PipeDirection.DOWN -> {
                tleft = Offset(center.x - innerWidth / 2f, center.y)
                tsize = Size(innerWidth, center.y)
            }
            PipeDirection.LEFT -> {
                tleft = Offset(0f, center.y - innerWidth / 2f)
                tsize = Size(center.x, innerWidth)
            }
            PipeDirection.RIGHT -> {
                tleft = Offset(center.x, center.y - innerWidth / 2f)
                tsize = Size(center.x, innerWidth)
            }
        }

        drawRect(
            brush = getInnerBrush(isVertical),
            topLeft = tleft,
            size = tsize
        )
    }

    // 5. Draw Junction Inner Base
    val junctionInnerSize = innerWidth * 1.05f
    drawOval(
        brush = androidx.compose.ui.graphics.Brush.radialGradient(
            colors = listOf(fgLight, fgDark)
        ),
        topLeft = Offset(center.x - junctionInnerSize / 2f, center.y - junctionInnerSize / 2f),
        size = Size(junctionInnerSize, junctionInnerSize)
    )
}

fun DrawScope.drawWaterFlow(
    connections: Set<PipeDirection>,
    waterColor: Color
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val strokeWidth = size.width * 0.22f

    // To make a glow effect, we draw a thick translucent line, and a thinner solid line
    for (dir in connections) {
        val endPoint = when (dir) {
            PipeDirection.UP -> Offset(center.x, 0f)
            PipeDirection.DOWN -> Offset(center.x, size.height)
            PipeDirection.LEFT -> Offset(0f, center.y)
            PipeDirection.RIGHT -> Offset(size.width, center.y)
        }
        
        // Outer glow
        drawLine(
            color = waterColor.copy(alpha = 0.3f),
            start = center,
            end = endPoint,
            strokeWidth = strokeWidth * 1.6f,
            cap = StrokeCap.Round
        )
        // Inner core
        drawLine(
            color = waterColor,
            start = center,
            end = endPoint,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // Bright center
        drawLine(
            color = Color.White.copy(alpha = 0.8f),
            start = center,
            end = endPoint,
            strokeWidth = strokeWidth * 0.4f,
            cap = StrokeCap.Round
        )
    }
}

// Draw the animated fill using Path measures
@Composable
fun DirectionalWaterFlowView(
    modifier: Modifier,
    entry: PipeDirection,
    exits: List<PipeDirection>,
    waterColor: Color,
    trimFraction: Float // 0f to 1f
) {
    Canvas(modifier = modifier) {
        if (trimFraction <= 0f) return@Canvas

        val center = Offset(size.width / 2f, size.height / 2f)
        val strokeW = size.width * 0.22f
        
        fun drawNeonSegment(start: Offset, end: Offset) {
            // Glow
            drawLine(waterColor.copy(alpha = 0.3f), start, end, strokeW * 1.6f, StrokeCap.Round)
            // Color
            drawLine(waterColor, start, end, strokeW, StrokeCap.Round)
            // Core
            drawLine(Color.White.copy(alpha = 0.8f), start, end, strokeW * 0.4f, StrokeCap.Round)
        }

        val startPt = when (entry) {
            PipeDirection.UP -> Offset(center.x, 0f)
            PipeDirection.DOWN -> Offset(center.x, size.height)
            PipeDirection.LEFT -> Offset(0f, center.y)
            PipeDirection.RIGHT -> Offset(size.width, center.y)
        }

        val fillLength = pathLength(startPt, center, exits, size.width) * trimFraction
        val distToCenter = size.width / 2f

        if (fillLength <= distToCenter) {
            val progress = fillLength / distToCenter
            val currentPt = Offset(
                startPt.x + (center.x - startPt.x) * progress,
                startPt.y + (center.y - startPt.y) * progress
            )
            drawNeonSegment(startPt, currentPt)
        } else {
            drawNeonSegment(startPt, center)
            val distAfterCenter = fillLength - distToCenter
            for (exit in exits) {
                val endPt = when (exit) {
                    PipeDirection.UP -> Offset(center.x, 0f)
                    PipeDirection.DOWN -> Offset(center.x, size.height)
                    PipeDirection.LEFT -> Offset(0f, center.y)
                    PipeDirection.RIGHT -> Offset(size.width, center.y)
                }
                val extDist = size.width / 2f
                val progress = minOf(1f, distAfterCenter / extDist)
                val curPt = Offset(
                    center.x + (endPt.x - center.x) * progress,
                    center.y + (endPt.y - center.y) * progress
                )
                drawNeonSegment(center, curPt)
            }
        }
    }
}

private fun pathLength(start: Offset, center: Offset, exits: List<PipeDirection>, width: Float): Float {
    return (width / 2f) + (width / 2f) // Entry to center (w/2), then center to exit (w/2) max length
}
