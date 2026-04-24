package com.example.brain_land.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.example.brain_land.data.*
import com.example.brain_land.ui.theme.*
import com.example.brain_land.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

// ── Colors (match iOS) ──
private val DCBg        = Color(0xFF10131B)
private val DCCyan      = Color(0xFF00E5FF)
private val DCPurple    = Color(0xFFA78BFA)
private val DCGreen     = Color(0xFF4FFFB0)
private val DCRed       = Color(0xFFFF6B6B)
private val DCGold      = Color(0xFFFFD700)
private val DCCardBg    = Color.White.copy(alpha = 0.04f)
private val DCCardStroke= Color.White.copy(alpha = 0.06f)

@Composable
fun DailyChallengeScreen(
    vm: HomeViewModel,
    onBack: () -> Unit,
    onPlayGame: (GameType) -> Unit
) {
    val daily         by vm.dailyChallenge.collectAsState()
    val isLoading     by vm.isLoadingDaily.collectAsState()
    val countdown     by vm.countdownText.collectAsState()
    val showCompletion by vm.showCompletionOverlay.collectAsState()
    val lastResult    by vm.lastSubmitResult.collectAsState()

    var showBonusInfo by remember { mutableStateOf(false) }

    val completedSet  = daily?.completedPuzzles?.toSet() ?: emptySet()
    val allCompleted  = daily?.allCompleted ?: false
    val streak        = daily?.streak ?: DailyStreak(0, 0, 0, 0)
    val totalScore    = daily?.totalScore ?: 0

    LaunchedEffect(Unit) { vm.fetchDaily() }

    Box(
        modifier = Modifier.fillMaxSize().background(DCBg)
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // ── Header ──
            DcHeader(
                dateStr     = daily?.date ?: "",
                streak      = streak.currentStreak,
                onBack      = onBack,
                onBonusInfo = { showBonusInfo = true }
            )

            if (isLoading && daily == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DCCyan)
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(Modifier.height(4.dp))

                    // ── Streak + Timer card ──
                    StreakTimerCard(streak = streak, countdown = countdown)

                    // ── Progress ──
                    ProgressSection(completedSet = completedSet, allCompleted = allCompleted)

                    // ── Puzzle Cards ──
                    daily?.puzzles?.forEach { puzzle ->
                        PuzzleCard(
                            puzzle      = puzzle,
                            isCompleted = completedSet.contains(puzzle.puzzleIndex),
                            score       = daily?.puzzleResults?.get(puzzle.puzzleIndex.toString())?.score,
                            onPlay      = {
                                puzzle.gameType?.let { onPlayGame(it) }
                            }
                        )
                    }

                    // ── Score Summary ──
                    if (totalScore > 0) {
                        ScoreSummary(totalScore = totalScore, lastResult = lastResult, allCompleted = allCompleted)
                    }

                    Spacer(Modifier.height(100.dp))
                }
            }
        }

        // ── Bonus Info Overlay ──
        if (showBonusInfo) {
            BonusInfoOverlay(
                currentStreak = streak.currentStreak,
                onDismiss     = { showBonusInfo = false }
            )
        }

        // ── Completion Overlay ──
        if (showCompletion) {
            CompletionOverlay(
                totalScore  = totalScore,
                streak      = streak.currentStreak,
                lastResult  = lastResult,
                onDismiss   = { vm.showCompletionOverlay.value = false }
            )
        }
    }
}

// ── Header ──
@Composable
private fun DcHeader(dateStr: String, streak: Int, onBack: () -> Unit, onBonusInfo: () -> Unit) {
    val formatted = remember(dateStr) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val d   = sdf.parse(dateStr) ?: return@remember dateStr
            SimpleDateFormat("MMMM d, yyyy", Locale.US).format(d)
        } catch (e: Exception) { dateStr }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ChevronLeft, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(8.dp))

        Column(Modifier.weight(1f)) {
            Text("Daily Challenge", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            if (formatted.isNotEmpty())
                Text(formatted, fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f))
        }

        // Bonus info button
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
                .clickable(onClick = onBonusInfo),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Info, null,
                tint = DCCyan.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
        }

        // Streak badge
        if (streak > 0) {
            Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier.clip(CircleShape)
                    .background(DCRed.copy(alpha = 0.15f))
                    .border(1.dp, DCRed.copy(alpha = 0.3f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocalFireDepartment, null, tint = DCRed, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("$streak", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ── Streak + Timer Card ──
@Composable
private fun StreakTimerCard(streak: DailyStreak, countdown: String) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DCCardBg)
            .border(1.dp, DCCardStroke, RoundedCornerShape(18.dp))
            .padding(vertical = 20.dp)
    ) {
        // Current streak
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(DCRed.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.LocalFireDepartment, null, tint = DCRed, modifier = Modifier.size(22.dp))
            }
            Text("${streak.currentStreak}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White,
                fontFamily = FontFamily.Monospace)
            Text("Day Streak", fontSize = 10.sp, color = Color.White.copy(alpha = 0.35f))
        }
        // Divider
        Box(Modifier.width(1.dp).height(60.dp).background(Color.White.copy(alpha = 0.06f)))
        // Countdown
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(DCCyan.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Timer, null, tint = DCCyan, modifier = Modifier.size(22.dp))
            }
            Text(countdown, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White,
                fontFamily = FontFamily.Monospace)
            Text("Next Reset", fontSize = 10.sp, color = Color.White.copy(alpha = 0.35f))
        }
        // Divider
        Box(Modifier.width(1.dp).height(60.dp).background(Color.White.copy(alpha = 0.06f)))
        // Best streak
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(DCGold.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.EmojiEvents, null, tint = DCGold, modifier = Modifier.size(22.dp))
            }
            Text("${streak.bestStreak}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White,
                fontFamily = FontFamily.Monospace)
            Text("Best Streak", fontSize = 10.sp, color = Color.White.copy(alpha = 0.35f))
        }
    }
}

// ── Progress Section ──
@Composable
private fun ProgressSection(completedSet: Set<Int>, allCompleted: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DCCardBg)
            .border(1.dp, DCCardStroke, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Progress", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.weight(1f))
            Text("${completedSet.size}/5", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (allCompleted) DCGreen else DCCyan)
        }

        // Dots
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..5).forEach { i ->
                val done = completedSet.contains(i)
                Box(
                    modifier = Modifier.size(14.dp).clip(CircleShape)
                        .background(if (done) DCGreen else Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (done) Icon(Icons.Default.Check, null, tint = DCBg, modifier = Modifier.size(8.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            if (allCompleted) Text("Complete!", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DCGreen)
        }

        // Progress bar
        Box(
            Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.06f))
        ) {
            val fraction = (completedSet.size / 5f).coerceIn(0f, 1f)
            Box(
                Modifier.fillMaxHeight().fillMaxWidth(fraction).clip(RoundedCornerShape(3.dp))
                    .background(Brush.horizontalGradient(listOf(DCCyan, DCGreen)))
            )
        }
    }
}

// ── Puzzle Card ──
@Composable
private fun PuzzleCard(
    puzzle: DailyPuzzle,
    isCompleted: Boolean,
    score: Int?,
    onPlay: () -> Unit
) {
    val gameType = puzzle.gameType
    val cardColorHex = gameType?.cardColorHex ?: "A78BFA"
    val cardColor = remember(cardColorHex) {
        try { Color(android.graphics.Color.parseColor("#$cardColorHex")) }
        catch (e: Exception) { DCPurple }
    }
    val isLight = gameType?.isLightCard ?: false
    val fgColor = if (isLight) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)

    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isCompleted) DCGreen.copy(alpha = 0.03f) else DCCardBg)
            .border(1.dp, if (isCompleted) DCGreen.copy(alpha = 0.12f) else DCCardStroke, RoundedCornerShape(16.dp))
            .then(if (!isCompleted) Modifier.clickable(onClick = onPlay) else Modifier)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Icon box
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                .then(
                    if (isCompleted)
                        Modifier.background(DCGreen.copy(alpha = 0.12f))
                    else
                        Modifier.background(Brush.linearGradient(listOf(cardColor, cardColor.copy(alpha = 0.6f))))
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(Icons.Default.CheckCircle, null, tint = DCGreen, modifier = Modifier.size(28.dp))
            } else {
                val iconRes = gameType?.assetIcon
                if (iconRes != null) {
                    AsyncImage(model = iconRes, contentDescription = puzzle.gameName,
                        contentScale = ContentScale.Fit, modifier = Modifier.size(32.dp))
                } else {
                    Text(gameType?.emoji ?: "🎮", fontSize = 24.sp)
                }
            }
        }

        // Name + level
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                puzzle.gameName,
                fontSize = 15.sp, fontWeight = FontWeight.Bold,
                color = if (isCompleted) Color.White.copy(alpha = 0.5f) else Color.White
            )
            Text(
                "Level 1",
                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = if (isCompleted) DCGreen.copy(alpha = 0.6f) else DCCyan
            )
        }

        // Score or Play button
        if (isCompleted && score != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("+$score", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DCGreen)
                Text("pts", fontSize = 9.sp, color = Color.White.copy(alpha = 0.3f))
            }
        } else if (!isCompleted) {
            Row(
                modifier = Modifier.clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(cardColor.copy(alpha = 0.4f), cardColor.copy(alpha = 0.2f))))
                    .border(1.dp, cardColor.copy(alpha = 0.4f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Play", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Icon(Icons.Default.ChevronRight, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ── Score Summary ──
@Composable
private fun ScoreSummary(totalScore: Int, lastResult: DailySubmitResponse?, allCompleted: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DCCardBg)
            .border(1.dp, DCCardStroke, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, null, tint = DCGold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Daily Score", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.weight(1f))
            Text("$totalScore", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, color = DCCyan)
        }

        val bonus = lastResult?.bonusScore
        if (allCompleted && bonus != null && bonus > 0) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CardGiftcard, null, tint = DCPurple, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Streak Bonus", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.weight(1f))
                Text("+$bonus", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace, color = DCPurple)
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Total", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.weight(1f))
                Text("${lastResult.finalScore ?: totalScore}", fontSize = 22.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = DCGreen)
            }
        }
    }
}

// ── Bonus Info Overlay ──
@Composable
private fun BonusInfoOverlay(currentStreak: Int, onDismiss: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1A1D2E))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                .clickable(enabled = false) {} // consume clicks
        ) {
            // Header
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CardGiftcard, null,
                    tint = DCCyan, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Streak Bonuses", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    modifier = Modifier.weight(1f))
                Icon(Icons.Default.Cancel, null, tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp).clickable(onClick = onDismiss))
            }

            Text(
                "Complete all 5 puzzles daily to build your streak and earn bonus points!",
                fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp)
            )

            // Bonus rows
            listOf(
                Triple("5/5 Complete", "100", DCGreen),
                Triple("3+ Day Streak", "+25", DCGold),
                Triple("7+ Day Streak", "+50", Color(0xFFFF9500)),
                Triple("14+ Day Streak", "+100", DCRed),
                Triple("30+ Day Streak", "+150", DCPurple)
            ).forEachIndexed { i, (cond, bonus, color) ->
                if (i > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.White.copy(alpha = 0.06f))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (i == 0) Icons.Default.CheckCircle else Icons.Default.LocalFireDepartment,
                        null, tint = color, modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(cond, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.8f), modifier = Modifier.weight(1f))
                    Text(bonus, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace, color = color)
                    Spacer(Modifier.width(8.dp))
                    Text(if (i == 0) "Base" else "Extra", fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.3f))
                }
            }

            // Notes
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Bonuses are NOT cumulative. Only the highest tier applies.",
                        fontSize = 11.sp, color = Color.White.copy(alpha = 0.35f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, null, tint = DCCyan.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Bonus points are added to your Global Score (1.5x)",
                        fontSize = 11.sp, color = Color.White.copy(alpha = 0.35f))
                }
            }

            // Got it button
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 20.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(DCCyan.copy(alpha = 0.3f), DCPurple.copy(alpha = 0.2f))))
                    .border(1.dp, DCCyan.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Got it!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ── Completion Overlay ──
@Composable
private fun CompletionOverlay(
    totalScore: Int,
    streak: Int,
    lastResult: DailySubmitResponse?,
    onDismiss: () -> Unit
) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1A1D2E))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                .padding(32.dp)
                .clickable(enabled = false) {},
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Trophy
            Box(
                Modifier.size(120.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(DCGold.copy(alpha = 0.3f), Color.Transparent))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.EmojiEvents, null, tint = DCGold, modifier = Modifier.size(56.dp))
            }

            Text("Challenge Complete!", fontSize = 26.sp, fontWeight = FontWeight.Bold,
                color = Color.White, textAlign = TextAlign.Center)
            Text("You solved all 5 daily puzzles!", fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center)

            // Stats row
            Row(
                modifier = Modifier.clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                    .padding(vertical = 20.dp, horizontal = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$totalScore", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DCCyan)
                    Text("Score", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$streak", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DCRed)
                    Text("Streak", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                }
                val bonus = lastResult?.bonusScore
                if (bonus != null && bonus > 0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("+$bonus", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DCPurple)
                        Text("Bonus", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                    }
                }
            }

            // Continue
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(DCPurple, Color(0xFF6D28D9))))
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Continue", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
