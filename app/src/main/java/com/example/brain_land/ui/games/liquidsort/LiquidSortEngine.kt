package com.example.brain_land.ui.games.liquidsort

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// LiquidSortEngine — mirrors iOS LiquidSortViewModel
// All animation timing values are identical to the Swift version.
// ─────────────────────────────────────────────────────────────────────────────

class LiquidSortEngine(private val scope: CoroutineScope) {

    // ── Published State ──
    var bottles        by mutableStateOf<List<Bottle>>(emptyList())
    var selected       by mutableStateOf<Int?>(null)
    var moveCount      by mutableStateOf(0)
    var isSolved       by mutableStateOf(false)
    var invalidShakeIndex by mutableStateOf<Int?>(null)
    var levelNumber    by mutableStateOf(1)
    var undoCount      by mutableStateOf(0)

    // ── Pour Animation State ──
    var isAnimating        by mutableStateOf(false)
    var pourSourceIndex    by mutableStateOf<Int?>(null)
    var pourTargetIndex    by mutableStateOf<Int?>(null)
    var sourceOffsetX      by mutableStateOf(0f)
    var sourceOffsetY      by mutableStateOf(0f)
    var sourceTilt         by mutableStateOf(0f)         // degrees
    var sourceScale        by mutableStateOf(1f)
    var streamProgress     by mutableStateOf(0f)
    var isStreamVisible    by mutableStateOf(false)
    var pourStartPoint     by mutableStateOf(Offset.Zero)
    var pourEndPoint       by mutableStateOf(Offset.Zero)
    var drainProgress      by mutableStateOf(0f)
    var fillProgress       by mutableStateOf(0f)
    var pourColor          by mutableStateOf<LiquidColor?>(null)
    var currentPourSegment by mutableStateOf(0)
    var totalPourSegments  by mutableStateOf(0)
    var liquidTiltFactor   by mutableStateOf(0f)
    var flowBias           by mutableStateOf(0f)
    var splashProgress     by mutableStateOf(0f)

    // ── Pour Queue ──
    var pendingSourceIndex by mutableStateOf<Int?>(null)
    private var pendingPour: Pair<Int, Int>? = null

    // ── Undo ──
    private val undoStack = mutableListOf<PourMove>()

    // ── Bottle Frames (set by views for animation math) ──
    val bottleFrames = mutableStateMapOf<Int, Rect>()

    // ── Timing ──
    private var levelStartMs = System.currentTimeMillis()

    val elapsedSeconds: Int get() = ((System.currentTimeMillis() - levelStartMs) / 1000).toInt()

    // ─────────────────────────────────────────────────────────────────────────
    // Level Management
    // ─────────────────────────────────────────────────────────────────────────

    fun loadLevel(level: Int) {
        levelNumber = level
        bottles = LiquidSortGenerator.generate(level)
        resetAllState()
    }

    fun restart() = loadLevel(levelNumber)
    fun nextLevel() = loadLevel(levelNumber + 1)

    private fun resetAllState() {
        selected           = null
        isAnimating        = false
        moveCount          = 0
        isSolved           = false
        invalidShakeIndex  = null
        undoStack.clear()
        undoCount          = 0
        levelStartMs       = System.currentTimeMillis()
        pourSourceIndex    = null
        pourTargetIndex    = null
        pourStartPoint     = Offset.Zero
        pourEndPoint       = Offset.Zero
        sourceOffsetX      = 0f
        sourceOffsetY      = 0f
        sourceTilt         = 0f
        sourceScale        = 1f
        streamProgress     = 0f
        isStreamVisible    = false
        drainProgress      = 0f
        fillProgress       = 0f
        pourColor          = null
        currentPourSegment = 0
        totalPourSegments  = 0
        liquidTiltFactor   = 0f
        flowBias           = 0f
        splashProgress     = 0f
        pendingSourceIndex = null
        pendingPour        = null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bottle Selection — mirrors iOS selectBottle()
    // ─────────────────────────────────────────────────────────────────────────

    fun selectBottle(index: Int) {
        if (index < 0 || index >= bottles.size) return

        // While animating: handle pre-selection / pour queue
        if (isAnimating) {
            val pending = pendingSourceIndex
            if (pending != null) {
                if (pending == index) {
                    pendingSourceIndex = null
                } else {
                    pendingPour = pending to index
                    pendingSourceIndex = null
                }
            } else {
                if (!bottles[index].isEmpty) pendingSourceIndex = index
            }
            return
        }

        // Normal interaction
        val sel = selected
        if (sel != null) {
            if (sel == index) {
                selected = null
            } else {
                attemptPour(sel, index)
            }
        } else {
            if (!bottles[index].isEmpty) selected = index
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pour Logic
    // ─────────────────────────────────────────────────────────────────────────

    private fun attemptPour(sourceIndex: Int, targetIndex: Int) {
        val source = bottles[sourceIndex]
        val target = bottles[targetIndex]
        val sourceTop = source.topColor ?: run {
            triggerInvalidShake(targetIndex)
            selected = null
            return
        }

        val canPour = target.isEmpty || target.topColor == sourceTop
        val hasSpace = target.freeSlots > 0

        if (!canPour || !hasSpace) {
            triggerInvalidShake(targetIndex)
            selected = null
            return
        }

        val segsToPour = minOf(source.topGroupCount, target.freeSlots)
        selected = null
        executePourAnimation(sourceIndex, targetIndex, segsToPour, sourceTop)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pour Animation — mirrors iOS executePourAnimation() timing exactly
    // ─────────────────────────────────────────────────────────────────────────

    private fun executePourAnimation(
        sourceIndex: Int, targetIndex: Int, count: Int, color: LiquidColor
    ) {
        isAnimating        = true
        pourSourceIndex    = sourceIndex
        pourTargetIndex    = targetIndex
        pourColor          = color
        totalPourSegments  = count
        currentPourSegment = 0

        val sourceFrame = bottleFrames[sourceIndex] ?: Rect.Zero
        val targetFrame = bottleFrames[targetIndex] ?: Rect.Zero

        val sourceCenterX = sourceFrame.center.x
        val sourceCenterY = sourceFrame.center.y
        val sourceMouthX  = sourceFrame.center.x
        val sourceMouthY  = sourceFrame.top

        val targetMouthX  = targetFrame.center.x
        val targetMouthY  = targetFrame.top

        val isLeftPour  = targetMouthX < sourceMouthX
        val tiltAngle   = if (isLeftPour) -85f else 85f
        val rad         = Math.toRadians(tiltAngle.toDouble()).toFloat()

        val mouthVecX = sourceMouthX - sourceCenterX
        val mouthVecY = sourceMouthY - sourceCenterY

        val rotMouthX = mouthVecX * kotlin.math.cos(rad.toDouble()).toFloat() - mouthVecY * kotlin.math.sin(rad.toDouble()).toFloat()
        val rotMouthY = mouthVecX * kotlin.math.sin(rad.toDouble()).toFloat() + mouthVecY * kotlin.math.cos(rad.toDouble()).toFloat()

        val anchorX = targetMouthX + (if (isLeftPour) 10f else -10f)
        val anchorY = targetMouthY - 12f

        val targetOffsetX  = anchorX - (sourceCenterX + rotMouthX)
        val targetOffsetY  = anchorY - (sourceCenterY + rotMouthY)
        val tiltDir        = if (isLeftPour) -1f else 1f

        scope.launch {
            // Phase 1: Lift (85ms)
            sourceScale   = 1.05f
            sourceOffsetY = -30f
            delay(85)

            // Phase 2: Move to target (145ms)
            sourceOffsetX = targetOffsetX
            sourceOffsetY = targetOffsetY
            delay(145)

            // Phase 3: Liquid tilt inside bottle (55ms)
            liquidTiltFactor = tiltDir * 0.5f
            delay(55)

            // Phase 4: Rotate bottle (130ms)
            sourceTilt       = tiltAngle
            liquidTiltFactor = tiltDir * 1.0f
            delay(130)

            // Phase 5: Pre-pour internal flow (120ms)
            flowBias = 1.0f
            delay(120)

            // Phase 6: Compute stream endpoints
            val finalCX = sourceCenterX + targetOffsetX
            val finalCY = sourceCenterY + targetOffsetY
            val finalMX = finalCX + mouthVecX * kotlin.math.cos(rad.toDouble()).toFloat() - mouthVecY * kotlin.math.sin(rad.toDouble()).toFloat()
            val finalMY = finalCY + mouthVecX * kotlin.math.sin(rad.toDouble()).toFloat() + mouthVecY * kotlin.math.cos(rad.toDouble()).toFloat()

            pourStartPoint = Offset(finalMX, finalMY)
            pourEndPoint   = Offset(targetFrame.center.x, targetFrame.top)

            // Phase 7: Start stream (125ms)
            flowBias       = 0.3f
            isStreamVisible = true
            streamProgress  = 0f
            streamProgress  = 1.0f
            delay(125)

            // Phase 8: Continuous drain/fill per segment
            val mutableBottles = bottles.map { it.copy() }.toMutableList()

            for (seg in 0 until count) {
                currentPourSegment = seg
                splashProgress     = 0f
                splashProgress     = 1.0f
                drainProgress      = 0f
                fillProgress       = 0f
                drainProgress      = 1.0f
                fillProgress       = 1.0f
                delay(150)

                // Commit data transfer
                if (mutableBottles[sourceIndex].layers.isNotEmpty()) {
                    val layer = mutableBottles[sourceIndex].layers.removeLast()
                    mutableBottles[targetIndex].layers.add(layer)
                }

                // Update the state list
                bottles = mutableBottles.map { it.copy() }

                drainProgress = 0f
                fillProgress  = 0f

                if (seg < count - 1) delay(40)
            }

            // Phase 9: Retract stream (55ms)
            streamProgress  = 0f
            delay(55)
            isStreamVisible = false
            pourStartPoint  = Offset.Zero
            pourEndPoint    = Offset.Zero
            splashProgress  = 0f

            moveCount++

            undoStack.add(PourMove(sourceIndex, targetIndex, count, color))

            // Phase 10: Un-tilt (85ms)
            sourceTilt       = 0f
            liquidTiltFactor = 0f
            flowBias         = 0f
            delay(85)

            // Phase 11: Return to position (140ms)
            sourceOffsetX = 0f
            sourceOffsetY = 0f
            sourceScale   = 1f
            delay(140)

            // Reset
            pourSourceIndex = null
            pourTargetIndex = null
            pourColor       = null
            isAnimating     = false

            checkWinCondition()
            if (isSolved) return@launch

            // Dequeue
            val next = pendingPour
            if (next != null) {
                pendingPour = null
                pendingSourceIndex = null
                delay(25)
                attemptPour(next.first, next.second)
            } else {
                val nextSrc = pendingSourceIndex
                if (nextSrc != null) {
                    pendingSourceIndex = null
                    selected = nextSrc
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Undo
    // ─────────────────────────────────────────────────────────────────────────

    val canUndo: Boolean get() = undoStack.isNotEmpty() && !isAnimating

    fun undoLastMove() {
        if (!canUndo) return
        val last = undoStack.removeLast()
        val mutable = bottles.map { it.copy() }.toMutableList()
        repeat(last.layerCount) {
            if (mutable[last.targetIndex].layers.isNotEmpty()) {
                val layer = mutable[last.targetIndex].layers.removeLast()
                mutable[last.sourceIndex].layers.add(layer)
            }
        }
        bottles = mutable.map { it.copy() }
        moveCount++
        undoCount++
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Win Check
    // ─────────────────────────────────────────────────────────────────────────

    private fun checkWinCondition() {
        if (bottles.all { it.isEmpty || it.isComplete }) {
            isSolved = true
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Invalid Shake
    // ─────────────────────────────────────────────────────────────────────────

    private fun triggerInvalidShake(index: Int) {
        invalidShakeIndex = index
        scope.launch {
            delay(500)
            invalidShakeIndex = null
        }
    }
}
