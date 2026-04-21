package com.example.brain_land.ui.games.liquidsort

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import kotlin.math.*

// ─────────────────────────────────────────────────────────────────────────────
// BottleView — mirrors iOS BottleView
// - Drawn entirely on Canvas for performance
// - Wave/tilt liquid surface matching Swift LiquidShape
// - Barrel-curved bottle shape matching Swift BottleGlassShape
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BottleView(
    bottle:          Bottle,
    index:           Int,
    isSelected:      Boolean,
    isPendingSource: Boolean,
    isShaking:       Boolean,
    isPourSource:    Boolean,
    isPourTarget:    Boolean,
    pourColor:       LiquidColor?,
    drainProgress:   Float,
    fillProgress:    Float,
    liquidTilt:      Float,
    flowBias:        Float,
    splashProgress:  Float,
    bottleWidthDp:   Dp = 52.dp,
    bottleHeightDp:  Dp = 150.dp,
    onTap:           () -> Unit
) {
    val density = LocalDensity.current

    // Shake animation
    val shakeAnim = remember { Animatable(0f) }
    LaunchedEffect(isShaking) {
        if (isShaking) {
            shakeAnim.snapTo(0f)
            val steps = listOf(8f, -6f, 5f, -3f, 0f)
            for (step in steps) {
                shakeAnim.animateTo(step, tween(60, easing = LinearEasing))
            }
        }
    }

    // Selection scale / lift — mirrors iOS spring
    val targetScale  = if (isSelected) 1.06f else if (isPendingSource) 1.03f else 1.0f
    val targetLiftDp = if (isSelected) (-10).dp else if (isPendingSource) (-5).dp else 0.dp
    val scale by animateFloatAsState(targetScale, spring(0.7f, 400f), label = "sc$index")
    val lift  by animateDpAsState(targetLiftDp,  spring(0.7f, 400f), label = "lf$index")

    Box(
        modifier = Modifier
            .width(bottleWidthDp + 8.dp)
            .height(bottleHeightDp + 16.dp)
            .offset(x = with(density) { shakeAnim.value.dp }, y = lift)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(onClick = onTap)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bw = bottleWidthDp.toPx()
            val bh = bottleHeightDp.toPx()
            val ox = (size.width  - bw) / 2f
            val oy = (size.height - bh) / 2f

            drawBottle(
                ox = ox, oy = oy, bw = bw, bh = bh,
                bottle          = bottle,
                isSelected      = isSelected,
                isPendingSource = isPendingSource,
                isPourSource    = isPourSource,
                isPourTarget    = isPourTarget,
                pourColor       = pourColor,
                drainProgress   = drainProgress,
                fillProgress    = fillProgress,
                liquidTilt      = liquidTilt,
                flowBias        = flowBias,
                splashProgress  = splashProgress
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Core drawing — runs inside Canvas DrawScope
// ─────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawBottle(
    ox: Float, oy: Float, bw: Float, bh: Float,
    bottle:          Bottle,
    isSelected:      Boolean,
    isPendingSource: Boolean,
    isPourSource:    Boolean,
    isPourTarget:    Boolean,
    pourColor:       LiquidColor?,
    drainProgress:   Float,
    fillProgress:    Float,
    liquidTilt:      Float,
    flowBias:        Float,
    splashProgress:  Float
) {
    // Build both paths: glass outline and liquid clip (inner shape without rim flare)
    val glassPath = bottleGlassPath(ox, oy, bw, bh)
    val clipPath  = liquidClipPath(ox, oy, bw, bh)

    // ── Frosted glass fill ──────────────────────────────────────────────────
    drawPath(
        glassPath,
        brush = Brush.linearGradient(
            listOf(Color.White.copy(0.05f), Color.White.copy(0.01f), Color.Transparent),
            start = Offset(ox, oy), end = Offset(ox + bw, oy + bh)
        )
    )

    // ── Liquid layers ────────────────────────────────────────────────────────
    val capacity    = bottle.capacity
    // iOS: neckPortion = 0.30 → bodyHeight = bottleHeight * 0.70
    val bodyH       = bh * 0.70f
    val segH        = bodyH / capacity.toFloat()
    val bottleBottom = oy + bh

    // ── Target: filling layer (drawn first, underneath) ─────────────────────
    if (isPourTarget && fillProgress > 0f && pourColor != null) {
        val baseH  = segH * bottle.layers.size
        val addedH = segH * fillProgress
        val topY   = bottleBottom - baseH - addedH
        val rectH  = baseH + addedH

        drawClipped(clipPath) {
            val liqPath = buildLiquidPath(ox, bw, topY, bottleBottom, 0f, 0f, 0f)
            drawPath(
                liqPath,
                brush = Brush.verticalGradient(
                    listOf(pourColor.topColor, pourColor.bottomColor),
                    startY = topY, endY = bottleBottom
                )
            )
        }

        // Splash ripple on target liquid surface
        if (splashProgress > 0f && splashProgress < 1f) {
            val rippleY  = topY + 4.dp.toPx()
            val rippleR  = (bw * 0.3f) * splashProgress
            val rAlpha   = 0.5f * (1f - splashProgress)
            drawCircle(
                color  = pourColor.highlightColor.copy(rAlpha),
                radius = rippleR,
                center = Offset(ox + bw / 2f, rippleY),
                style  = Stroke(width = 1.5.dp.toPx())
            )
            val rippleR2 = rippleR * 0.55f
            if (rippleR2 > 0f) {
                drawCircle(
                    color  = pourColor.highlightColor.copy(rAlpha * 0.5f),
                    radius = rippleR2,
                    center = Offset(ox + bw / 2f, rippleY),
                    style  = Stroke(width = 1.dp.toPx())
                )
            }
        }
    }

    // ── Existing layers — mirrors Swift's reversedIndices approach ────────────
    // Draw from highest index (largest cumulative height) down to 0 (smallest).
    // Each pass paints over the layer below, leaving only its own colour band
    // visible — exactly like SwiftUI ZStack(alignment:.bottom) with reversedIndices.
    for (i in bottle.layers.indices.reversed()) {
        val layer      = bottle.layers[i]
        val isTopLayer = (i == bottle.layers.size - 1)
        val isDraining = isPourSource && isTopLayer && drainProgress > 0f

        // Cumulative height from the bottle bottom
        val layerH = if (isDraining)
            segH * i.toFloat() + segH * (1f - drainProgress)
        else
            segH * (i + 1).toFloat()

        if (layerH <= 0f) continue

        val topY = bottleBottom - layerH

        // Tilt + flowBias only on the top layer while pouring
        val tilt = if (isTopLayer && isPourSource) liquidTilt else 0f
        val bias = if (isTopLayer && isPourSource) flowBias  else 0f
        val wave = if (isPourSource || isPourTarget) 1.5f    else 0f

        drawClipped(clipPath) {
            val liqPath = buildLiquidPath(ox, bw, topY, bottleBottom, tilt, bias, wave)
            drawPath(
                liqPath,
                brush = Brush.verticalGradient(
                    listOf(layer.topColor, layer.bottomColor),
                    startY = topY, endY = bottleBottom
                )
            )
            // Inner sheen
            drawPath(
                liqPath,
                brush = Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(0.12f), Color.Transparent),
                    start = Offset(ox, topY),
                    end   = Offset(ox + bw, topY + layerH)
                ),
                alpha = 0.6f
            )
        }
    }

    // ── Main glass outline stroke ────────────────────────────────────────────
    val strokeAlpha = if (isSelected) 0.70f else 0.45f
    val strokeW     = if (isSelected) 1.8.dp.toPx() else 1.2.dp.toPx()
    drawPath(
        glassPath,
        brush = Brush.linearGradient(
            listOf(
                Color.White.copy(strokeAlpha),
                Color.White.copy(strokeAlpha * 0.6f),
                Color.White.copy(0.10f),
                Color.White.copy(strokeAlpha * 0.2f)
            ),
            start = Offset(ox, oy), end = Offset(ox + bw, oy + bh)
        ),
        style = Stroke(width = strokeW)
    )

    // ── Metallic rim ─────────────────────────────────────────────────────────
    val neckW  = bw * 0.36f
    val neckH  = bh * 0.20f
    val rimH   = bh * 0.04f
    val flair  = neckW * 0.12f
    val rimW   = neckW + flair * 2f
    val neckX  = ox + (bw - neckW) / 2f
    val rimX   = neckX + neckW / 2f - rimW / 2f
    val rimY   = oy - rimH / 2f
    // Outer metallic ring
    drawRoundRect(
        brush = Brush.linearGradient(
            listOf(Color.White.copy(0.55f), Color.White.copy(0.15f), Color.White.copy(0.35f))
        ),
        topLeft      = Offset(rimX, rimY),
        size         = androidx.compose.ui.geometry.Size(rimW, rimH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(rimH / 2f)
    )
    // Inner shadow under rim
    drawRoundRect(
        color = Color.Black.copy(0.25f),
        topLeft      = Offset(rimX + 1f, rimY + rimH * 0.35f),
        size         = androidx.compose.ui.geometry.Size(rimW - 2f, rimH * 0.4f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(rimH * 0.2f)
    )
    // Highlight streak on rim
    drawRoundRect(
        color = Color.White.copy(0.7f),
        topLeft      = Offset(rimX + rimW * 0.12f, rimY - 0.5f),
        size         = androidx.compose.ui.geometry.Size(rimW * 0.55f, rimH * 0.3f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(rimH * 0.15f)
    )

    // ── Glass reflection streaks — clipped to bottle shape (mirrors Swift .clipShape) ──
    val refX = ox + 6.dp.toPx()
    val refY = oy + bh * 0.22f
    val refH = bh * 0.66f
    drawClipped(glassPath) {
        // Primary wide soft streak
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color.White.copy(0.22f), Color.White.copy(0.08f), Color.Transparent),
                startY = refY, endY = refY + refH
            ),
            topLeft      = Offset(refX, refY),
            size         = androidx.compose.ui.geometry.Size(6.dp.toPx(), refH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )
        // Secondary narrow bright streak (upper body only)
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color.White.copy(0.45f), Color.White.copy(0.15f), Color.Transparent),
                startY = refY + bh * 0.02f, endY = refY + refH * 0.5f
            ),
            topLeft      = Offset(refX + 10.dp.toPx(), refY + bh * 0.02f),
            size         = androidx.compose.ui.geometry.Size(2.5.dp.toPx(), refH * 0.48f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
        )
    }

    // ── Selection glow ───────────────────────────────────────────────────────
    if (isSelected) {
        drawPath(glassPath, color = Color.White.copy(0.25f), style = Stroke(width = 8.dp.toPx()))
        drawPath(glassPath, color = Color.White.copy(0.65f), style = Stroke(width = 2.dp.toPx()))
    }

    // ── Complete indicator (green glow) ──────────────────────────────────────
    if (bottle.isComplete) {
        drawPath(glassPath, color = Color(0xFF4ADE80).copy(0.3f),  style = Stroke(width = 7.dp.toPx()))
        drawPath(glassPath, color = Color(0xFF4ADE80).copy(0.75f), style = Stroke(width = 2.dp.toPx()))
        drawPath(path = glassPath, brush = SolidColor(Color(0xFF4ADE80).copy(0.05f)))
    }

    // ── Pending source (amber ring) ──────────────────────────────────────────
    if (isPendingSource) {
        drawPath(glassPath, color = Color(0xFFFF9500).copy(0.3f), style = Stroke(width = 7.dp.toPx()))
        drawPath(glassPath, color = Color(0xFFFF9500).copy(0.7f), style = Stroke(width = 2.dp.toPx()))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Liquid path with wave top surface  — mirrors Swift LiquidShape
// Built from bottom-left → bottom-right → right side up → wavy top → close
// tiltFactor: -1 (left) to +1 (right), flowBias: 0→1 mouth accumulation
// ─────────────────────────────────────────────────────────────────────────────

private fun buildLiquidPath(
    ox:          Float,
    bw:          Float,
    topY:        Float,
    bottomY:     Float,
    tiltFactor:  Float,
    flowBias:    Float,
    waveAmplitude: Float
): Path = Path().apply {
    if (bottomY <= topY) return@apply

    moveTo(ox, bottomY)
    lineTo(ox + bw, bottomY)
    lineTo(ox + bw, topY)

    // Wavy top from right → left (mirrors Swift's x-sweep)
    val step        = 6f
    val maxTiltSlope = 22f
    val tiltSlope   = tiltFactor * maxTiltSlope
    val bulgeAmp    = flowBias * 16f
    val mouthCenter = if (tiltFactor > 0f) 0.82f else 0.18f
    val bulgeW      = 0.35f

    var x = bw
    while (x >= 0f) {
        val relX    = x / bw
        val waveY   = if (waveAmplitude > 0f) waveAmplitude * (1f + sin(relX * PI.toFloat() * 2f)) else 0f
        val tiltY   = tiltSlope * (relX - 0.5f)
        val dist    = relX - mouthCenter
        val gauss   = exp(-(dist * dist) / (2f * bulgeW * bulgeW))
        val bulgeY  = bulgeAmp * gauss
        lineTo(ox + x, topY + waveY - tiltY - bulgeY)
        x -= step
    }
    // Ensure left edge
    val waveL  = if (waveAmplitude > 0f) waveAmplitude * (1f + sin(0f)) else 0f
    val tiltL  = tiltSlope * (0f - 0.5f)
    val distL  = 0f - mouthCenter
    val bulgeL = bulgeAmp * exp(-(distL * distL) / (2f * bulgeW * bulgeW))
    lineTo(ox, topY + waveL - tiltL - bulgeL)

    close()
}

// ─────────────────────────────────────────────────────────────────────────────
// Clip drawing to the given path
// ─────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawClipped(clipPath: Path, block: DrawScope.() -> Unit) {
    drawContext.canvas.save()
    drawContext.canvas.clipPath(clipPath)
    block()
    drawContext.canvas.restore()
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottle glass path — mirrors Swift BottleGlassShape closely
// Elegant barrel curve + S-curve shoulder + rim flare
// ─────────────────────────────────────────────────────────────────────────────

private fun bottleGlassPath(ox: Float, oy: Float, bw: Float, bh: Float): Path = Path().apply {
    val w = bw
    val h = bh

    val rimH      = h * 0.04f
    val neckH     = h * 0.20f
    val neckW     = w * 0.36f
    val neckX     = (w - neckW) / 2f
    val shoulderH = h * 0.14f
    val bodyR     = w * 0.062f
    val botR      = minOf(12f, w * 0.15f)
    val shoulderBot = neckH + shoulderH
    val rimFlair  = neckW * 0.12f

    // Apply ox/oy offset
    val left = ox; val top = oy

    // ── Start bottom-left corner ──
    moveTo(left + botR, top + h)
    lineTo(left + w - botR, top + h)
    quadraticTo(left + w, top + h, left + w, top + h - botR)

    // ── Right barrel body (slight outward curve) ──
    cubicTo(
        left + w + bodyR, top + h * 0.65f,
        left + w + bodyR, top + h * 0.42f,
        left + w, top + shoulderBot
    )

    // ── Right shoulder S-curve into neck ──
    cubicTo(
        left + w,                             top + shoulderBot - shoulderH * 0.25f,
        left + neckX + neckW + w * 0.08f,     top + neckH + shoulderH * 0.4f,
        left + neckX + neckW,                 top + neckH
    )

    // ── Right neck straight up ──
    lineTo(left + neckX + neckW, top + rimH)

    // ── Rim top-right flare ──
    cubicTo(
        left + neckX + neckW + rimFlair * 0.3f, top + rimH,
        left + neckX + neckW + rimFlair,         top + rimH * 0.6f,
        left + neckX + neckW + rimFlair,         top + rimH * 0.5f
    )
    cubicTo(
        left + neckX + neckW + rimFlair,         top + rimH * 0.1f,
        left + neckX + neckW + rimFlair * 0.3f,  top,
        left + neckX + neckW - rimFlair * 0.2f,  top
    )

    // ── Top rim ──
    lineTo(left + neckX + rimFlair * 0.2f, top)

    // ── Rim top-left flare (mirror) ──
    cubicTo(
        left + neckX - rimFlair * 0.3f, top,
        left + neckX - rimFlair,        top + rimH * 0.1f,
        left + neckX - rimFlair,        top + rimH * 0.5f
    )
    cubicTo(
        left + neckX - rimFlair,        top + rimH * 0.6f,
        left + neckX - rimFlair * 0.3f, top + rimH,
        left + neckX,                   top + rimH
    )

    // ── Left neck straight down ──
    lineTo(left + neckX, top + neckH)

    // ── Left shoulder S-curve ──
    cubicTo(
        left + neckX - w * 0.08f, top + neckH + shoulderH * 0.4f,
        left,                     top + shoulderBot - shoulderH * 0.25f,
        left,                     top + shoulderBot
    )

    // ── Left barrel body (mirror) ──
    cubicTo(
        left - bodyR, top + h * 0.42f,
        left - bodyR, top + h * 0.65f,
        left,         top + h - botR
    )

    quadraticTo(left, top + h, left + botR, top + h)
    close()
}

// ─────────────────────────────────────────────────────────────────────────────
// Liquid clip path — mirrors Swift LiquidClipShape
// Same as glass but without rim flare (so liquid clips to bottle interior)
// ─────────────────────────────────────────────────────────────────────────────

private fun liquidClipPath(ox: Float, oy: Float, bw: Float, bh: Float): Path = Path().apply {
    val w = bw
    val h = bh

    val neckH     = h * 0.20f
    val neckW     = w * 0.36f
    val neckX     = (w - neckW) / 2f
    val shoulderH = h * 0.14f
    val bodyR     = w * 0.062f
    val botR      = minOf(12f, w * 0.15f)
    val shoulderBot = neckH + shoulderH

    val left = ox; val top = oy

    moveTo(left + botR, top + h)
    lineTo(left + w - botR, top + h)
    quadraticTo(left + w, top + h, left + w, top + h - botR)

    cubicTo(
        left + w + bodyR, top + h * 0.65f,
        left + w + bodyR, top + h * 0.42f,
        left + w, top + shoulderBot
    )
    cubicTo(
        left + w,                            top + shoulderBot - shoulderH * 0.25f,
        left + neckX + neckW + w * 0.08f,   top + neckH + shoulderH * 0.4f,
        left + neckX + neckW,               top + neckH
    )
    lineTo(left + neckX + neckW, top)
    lineTo(left + neckX,         top)
    lineTo(left + neckX,         top + neckH)

    cubicTo(
        left + neckX - w * 0.08f, top + neckH + shoulderH * 0.4f,
        left,                     top + shoulderBot - shoulderH * 0.25f,
        left,                     top + shoulderBot
    )
    cubicTo(
        left - bodyR, top + h * 0.42f,
        left - bodyR, top + h * 0.65f,
        left,         top + h - botR
    )
    quadraticTo(left, top + h, left + botR, top + h)
    close()
}
