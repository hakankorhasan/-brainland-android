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
    val pipeWidth = size.width * 0.3f
    val cornerRadius = CornerRadius(2f, 2f)

    for (dir in connections) {
        val tw = pipeWidth
        val th = size.height / 2f
        val rectTopLeft: Offset
        val rectSize: Size

        when (dir) {
            PipeDirection.UP -> {
                rectTopLeft = Offset(center.x - pipeWidth / 2f, 0f)
                rectSize = Size(tw, th)
            }
            PipeDirection.DOWN -> {
                rectTopLeft = Offset(center.x - pipeWidth / 2f, center.y)
                rectSize = Size(tw, th)
            }
            PipeDirection.LEFT -> {
                rectTopLeft = Offset(0f, center.y - pipeWidth / 2f)
                rectSize = Size(size.width / 2f, pipeWidth)
            }
            PipeDirection.RIGHT -> {
                rectTopLeft = Offset(center.y, center.y - pipeWidth / 2f)
                rectSize = Size(size.width / 2f, pipeWidth)
            }
        }

        drawRoundRect(
            color = backgroundColor,
            topLeft = rectTopLeft,
            size = rectSize,
            cornerRadius = cornerRadius
        )
    }

    val junctionSize = pipeWidth * 1.15f
    drawOval(
        color = backgroundColor,
        topLeft = Offset(center.x - junctionSize / 2f, center.y - junctionSize / 2f),
        size = Size(junctionSize, junctionSize)
    )

    // Inner pipe 
    for (dir in connections) {
        val rWidth = pipeWidth * 0.7f
        val rLength = size.height / 2f
        val tleft: Offset
        val tsize: Size

        when (dir) {
            PipeDirection.UP -> {
                tleft = Offset(center.x - rWidth / 2f, 0f)
                tsize = Size(rWidth, rLength)
            }
            PipeDirection.DOWN -> {
                tleft = Offset(center.x - rWidth / 2f, center.y)
                tsize = Size(rWidth, rLength)
            }
            PipeDirection.LEFT -> {
                tleft = Offset(0f, center.y - rWidth / 2f)
                tsize = Size(size.width / 2f, rWidth)
            }
            PipeDirection.RIGHT -> {
                tleft = Offset(center.x, center.y - rWidth / 2f)
                tsize = Size(size.width / 2f, rWidth)
            }
        }

        drawRoundRect(
            color = color,
            topLeft = tleft,
            size = tsize,
            cornerRadius = CornerRadius(0f)
        )
    }

    val junctionInnerSize = pipeWidth * 0.85f
    drawOval(
        color = color,
        topLeft = Offset(center.x - junctionInnerSize / 2f, center.y - junctionInnerSize / 2f),
        size = Size(junctionInnerSize, junctionInnerSize)
    )
}

fun DrawScope.drawWaterFlow(
    connections: Set<PipeDirection>,
    waterColor: Color
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val strokeWidth = size.width * 0.15f

    for (dir in connections) {
        val endPoint = when (dir) {
            PipeDirection.UP -> Offset(center.x, 0f)
            PipeDirection.DOWN -> Offset(center.x, size.height)
            PipeDirection.LEFT -> Offset(0f, center.y)
            PipeDirection.RIGHT -> Offset(size.width, center.y)
        }
        drawLine(
            color = waterColor,
            start = center,
            end = endPoint,
            strokeWidth = strokeWidth,
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
        val strokeW = size.width * 0.15f

        val startPt = when (entry) {
            PipeDirection.UP -> Offset(center.x, 0f)
            PipeDirection.DOWN -> Offset(center.x, size.height)
            PipeDirection.LEFT -> Offset(0f, center.y)
            PipeDirection.RIGHT -> Offset(size.width, center.y)
        }

        val path = Path().apply {
            moveTo(startPt.x, startPt.y)
            lineTo(center.x, center.y)
            for ((i, exit) in exits.withIndex()) {
                if (i > 0) moveTo(center.x, center.y)
                val endPt = when (exit) {
                    PipeDirection.UP -> Offset(center.x, 0f)
                    PipeDirection.DOWN -> Offset(center.x, size.height)
                    PipeDirection.LEFT -> Offset(0f, center.y)
                    PipeDirection.RIGHT -> Offset(size.width, center.y)
                }
                lineTo(endPt.x, endPt.y)
            }
        }

        // Draw trimmed path via clipping or partial drawing if possible
        // Actually since we don't have direct PathMeasure.getSegment in Compose easily, we can simulate by drawing over the path
        val fillLength = pathLength(startPt, center, exits, size.width) * trimFraction

        // Simpler approach for directional fill
        // Phase 1: entry to center
        val distToCenter = size.width / 2f
        if (fillLength <= distToCenter) {
            val progress = fillLength / distToCenter
            val currentPt = Offset(
                startPt.x + (center.x - startPt.x) * progress,
                startPt.y + (center.y - startPt.y) * progress
            )
            drawLine(waterColor, startPt, currentPt, strokeW, StrokeCap.Round)
        } else {
            // Already hit center
            drawLine(waterColor, startPt, center, strokeW, StrokeCap.Round)
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
                drawLine(waterColor, center, curPt, strokeW, StrokeCap.Round)
            }
        }
    }
}

private fun pathLength(start: Offset, center: Offset, exits: List<PipeDirection>, width: Float): Float {
    return (width / 2f) + (width / 2f) // Entry to center (w/2), then center to exit (w/2) max length
}
