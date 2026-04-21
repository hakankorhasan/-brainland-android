package com.example.brain_land.ui.games.tiltmaze

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.brain_land.data.GameResultRepository
import com.example.brain_land.data.GameResultResponse
import com.example.brain_land.data.GameType
import kotlinx.coroutines.launch
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────────────────────
// Colors — iOS GameResultView palette
// ─────────────────────────────────────────────────────────────────────────────

private val BgResult  = Color(0xFF10131B)
private val GreenOrb  = Color(0xFF4FFFB0)
private val PinkOrb   = Color(0xFFFF6EB4)
private val AccCyan   = Color(0xFF6FE4CF)
private val AccPurple = Color(0xFFB88AE8)
private val AccGreen  = Color(0xFF4FFFB0)
private val AccGold   = Color(0xFFFFBF24)

// ─────────────────────────────────────────────────────────────────────────────
// Confetti
// ─────────────────────────────────────────────────────────────────────────────

private data class Confetti(val x: Float, val y: Float, val size: Float, val alpha: Float, val ci: Int)

private val confColors = listOf(
    GreenOrb, PinkOrb, Color(0xFF85CAE0),
    GreenOrb.copy(.5f), PinkOrb.copy(.5f), Color.White.copy(.4f)
)

// ─────────────────────────────────────────────────────────────────────────────
// GameSuggestion — mirrors iOS MiniGameInfo.randomSuggestions()
// ─────────────────────────────────────────────────────────────────────────────

private data class GameSuggestion(
    val type:  GameType,
    val name:  String,
    val emoji: String,
    val cardColor: Color
)

/** All games that can appear as suggestions — excludes TILT_MAZE (current game) */
private val suggestablGames: List<GameSuggestion> = listOf(
    GameSuggestion(GameType.PIPE_CONNECT,     "Pipe Connect",     "🔧", Color(0xFFAFABE5)),
    GameSuggestion(GameType.LASER_PUZZLE,     "Laser Puzzle",     "🔦", Color(0xFF7E7DDC)),
    GameSuggestion(GameType.HIDDEN_PAIR,      "Hidden Pair",      "🃏", Color(0xFFD495BB)),
    GameSuggestion(GameType.BINARY_PUZZLE,    "Binary Puzzle",    "01", Color(0xFFF2F0F7)),
    GameSuggestion(GameType.NONOGRAM,         "Pixel Excavation", "🖼", Color(0xFF12141F)),
    GameSuggestion(GameType.SLITHERLINK,      "Slitherlink",      "🐍", Color(0xFF1D1B29)),
    GameSuggestion(GameType.BLOCK_FIT,        "Block Fit",        "🧩", Color(0xFFAFABE5)),
    GameSuggestion(GameType.CRYPTO_CAGE,      "Crypto-Cage",      "🔐", Color(0xFF0D2137)),
    GameSuggestion(GameType.NEURAL_LINK,      "Neural Link",      "🧠", Color(0xFF0A1628)),
    GameSuggestion(GameType.GALACTIC_BEACONS, "Galactic Beacons", "🛸", Color(0xFF0D0B2E)),
    GameSuggestion(GameType.NUMBER_CIRCUIT,   "Number Circuit",   "🔢", Color(0xFF0A1A2E)),
    GameSuggestion(GameType.WORD_PUZZLE,      "Word Puzzle",      "📝", Color(0xFF0D3B2E)),
)

private fun randomSuggestions(excludingId: String = "tiltMaze", count: Int = 2): List<GameSuggestion> =
    suggestablGames
        .filter { it.type.gameId != excludingId }
        .shuffled()
        .take(count)

// ─────────────────────────────────────────────────────────────────────────────
// GameResultSheet — fullscreen Dialog (covers nav bar)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GameResultSheet(
    visible:           Boolean,
    level:             Int,
    elapsed:           Int,
    difficulty:        Int,
    gridSize:          Int,
    onNextPuzzle:      () -> Unit,
    onPlayAgain:       () -> Unit,
    onBackToGames:     () -> Unit,
    onNavigateToGame:  (GameType) -> Unit = {}
) {
    if (!visible) return

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows  = false
        )
    ) {
        GameResultContent(
            level            = level,
            elapsed          = elapsed,
            difficulty       = difficulty,
            gridSize         = gridSize,
            onNextPuzzle     = onNextPuzzle,
            onPlayAgain      = onPlayAgain,
            onBackToGames    = onBackToGames,
            onNavigateToGame = onNavigateToGame
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GameResultContent
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GameResultContent(
    level:            Int,
    elapsed:          Int,
    difficulty:       Int,
    gridSize:         Int,
    onNextPuzzle:     () -> Unit,
    onPlayAgain:      () -> Unit,
    onBackToGames:    () -> Unit,
    onNavigateToGame: (GameType) -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val repo    = remember { GameResultRepository(context) }

    var resultResponse by remember { mutableStateOf<GameResultResponse?>(null) }
    var apiCompleted   by remember { mutableStateOf(false) }
    var showContent    by remember { mutableStateOf(false) }

    // Stable suggestions — mirrors iOS suggestedGames (computed once on appear)
    val suggestions = remember { randomSuggestions(excludingId = "tiltMaze", count = 2) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(80)
        showContent = true
        scope.launch {
            resultResponse = repo.submitGameResult(
                gameId       = "tiltMaze",
                level        = level,
                difficulty   = difficulty,
                correct      = true,
                responseTime = elapsed.toDouble()
            )
            apiCompleted = true
        }
    }

    // Orb pulse
    val orbInf   = rememberInfiniteTransition(label = "orb")
    val orbPulse by orbInf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "op"
    )

    // Confetti
    val confetti = remember {
        (0 until 20).map { i ->
            Confetti(
                x     = Random.nextFloat() * 300f - 150f,
                y     = Random.nextFloat() * 90f  - 40f,
                size  = Random.nextFloat() * 4f + 3f,
                alpha = Random.nextFloat() * 0.4f + 0.2f,
                ci    = i % confColors.size
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgResult)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // ── Orbs + confetti + title ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .graphicsLayer(
                        alpha        = if (showContent) 1f else 0f,
                        translationY = if (showContent) 0f else 40f
                    ),
                contentAlignment = Alignment.Center
            ) {
                OrbsArea(orbPulse, confetti)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Puzzle Complete", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Great job!", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(.45f))
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Rating section ──
            RatingSection(
                response     = resultResponse,
                apiCompleted = apiCompleted,
                modifier     = Modifier.fillMaxWidth().padding(horizontal = 20.dp).animateContentSize()
            )

            Spacer(Modifier.height(20.dp))

            // ── Performance Stats ──
            AnimatedVisibility(
                visible = showContent,
                enter   = fadeIn(tween(500, delayMillis = 200)) + slideInVertically(tween(500, delayMillis = 200)) { 30 }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text("Performance Stats",
                        fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        StreakCard(response = resultResponse, apiCompleted = apiCompleted, modifier = Modifier.weight(1f))
                        SolveTimeCard(elapsed = elapsed, difficulty = difficulty, modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Options section (2 random games) — mirrors iOS optionsSection ──
            AnimatedVisibility(
                visible = showContent,
                enter   = fadeIn(tween(500, delayMillis = 350)) + slideInVertically(tween(500, delayMillis = 350)) { 30 }
            ) {
                OptionsSection(
                    suggestions      = suggestions,
                    onNavigateToGame = onNavigateToGame,
                    modifier         = Modifier.padding(horizontal = 20.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Buttons ──
            AnimatedVisibility(
                visible = showContent,
                enter   = fadeIn(tween(400, delayMillis = 450)) + slideInVertically(tween(400, delayMillis = 450)) { 20 }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Next Puzzle — gradient capsule
                    Button(
                        onClick = onNextPuzzle,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(listOf(Color(0xFF6FE4CF), Color(0xFFB88AE8))),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Next Puzzle", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    // Back to Games — ghost
                    TextButton(onClick = onBackToGames, modifier = Modifier.fillMaxWidth()) {
                        Text("Back to Games", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(.6f))
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// OptionsSection — mirrors iOS optionsSection
// 2 game cards side-by-side, square aspect ratio
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OptionsSection(
    suggestions: List<GameSuggestion>,
    onNavigateToGame: (GameType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            suggestions.forEachIndexed { index, game ->
                OptionCard(
                    game             = game,
                    isFirst          = (index == 0),
                    onClick          = { onNavigateToGame(game.type) },
                    modifier         = Modifier.weight(1f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// OptionCard — mirrors iOS optionCard exactly:
//   • "Option" label at top
//   • Game emoji icon centered
//   • First card → radial purple glow bottom-left + angular gradient border
//   • Second card → plain border
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OptionCard(
    game:     GameSuggestion,
    isFirst:  Boolean,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(0.04f))
            .clickable { onClick() }
    ) {
        // First card: radial glow at bottom-left (mirrors iOS)
        if (isFirst) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF9D6FE8).copy(0.30f), Color.Transparent),
                            center  = androidx.compose.ui.geometry.Offset(0f, Float.MAX_VALUE),
                            radius  = 400f
                        )
                    )
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // "Option" label (top)
            Text(
                "Option",
                fontSize   = 16.sp, fontWeight = FontWeight.SemiBold,
                color      = Color.White.copy(0.65f),
                modifier   = Modifier.padding(top = 16.dp)
            )

            // Game icon — centered
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    game.emoji,
                    fontSize = 40.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Game name label
            Text(
                game.name,
                fontSize   = 11.sp,
                fontWeight = FontWeight.Medium,
                color      = Color.White.copy(0.45f),
                textAlign  = TextAlign.Center,
                modifier   = Modifier.padding(bottom = 12.dp, start = 8.dp, end = 8.dp),
                maxLines   = 1
            )
        }

        // Border — first card gets angular gradient, second gets plain
        if (isFirst) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 2.5.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xFFE18FD6).copy(0.6f),
                                Color(0xFFC47FE8).copy(0.35f),
                                Color.Transparent,
                                Color.Transparent,
                                Color(0xFF9D91E8).copy(0.3f),
                                Color(0xFFE18FD6).copy(0.6f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, Color.White.copy(0.08f), RoundedCornerShape(16.dp))
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Rating Section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RatingSection(
    response:     GameResultResponse?,
    apiCompleted: Boolean,
    modifier:     Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (response?.improved == true) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .background(AccGold.copy(.15f), CircleShape)
                    .border(1.dp, AccGold.copy(.3f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Icon(Icons.Default.Star, null, tint = AccGold, modifier = Modifier.size(10.dp))
                Text("NEW BEST", fontSize = 12.sp, fontWeight = FontWeight.Black, color = AccGold)
            }
        }

        response?.let { r ->
            val tierClr = tierColor(r.tier)
            Text(
                r.tier.uppercase(),
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tierClr,
                modifier = Modifier
                    .background(tierClr.copy(.15f), CircleShape)
                    .border(1.dp, tierClr.copy(.3f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        Text("RATING", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(.4f))

        when {
            response != null -> {
                val oldRating = response.previousRating ?: (response.newRating - response.ratingChange)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("$oldRating", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("~", fontSize = 24.sp, color = Color.White.copy(.25f))
                    Text("${response.newRating}", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    if (response.ratingChange != 0) {
                        val sign = if (response.ratingChange > 0) "+" else ""
                        Text(
                            "$sign${response.ratingChange}",
                            fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            color = if (response.ratingChange > 0) Color(0xFF4FFFB0) else Color(0xFFFF6B6B)
                        )
                    }
                }
            }
            apiCompleted -> Text("Could not load", fontSize = 14.sp, color = Color.White.copy(.3f))
            else         -> CircularProgressIndicator(color = Color.White.copy(.3f), modifier = Modifier.size(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Streak Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StreakCard(
    response:     GameResultResponse?,
    apiCompleted: Boolean,
    modifier:     Modifier = Modifier
) {
    val streak      = response?.newStreak   ?: 0
    val scoreGained = response?.scoreGained ?: 0

    Box(
        modifier = modifier
            .height(90.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(.05f))
            .border(2.dp, Color.White.copy(.08f), RoundedCornerShape(14.dp))
    ) {
        Box(
            modifier = Modifier.size(50.dp).offset(x = (-10).dp, y = 20.dp)
                .blur(18.dp).background(Color(0xFF9D6FE8).copy(.35f), CircleShape)
                .align(Alignment.BottomStart)
        )
        Box(
            modifier = Modifier.fillMaxWidth(.5f).height(3.dp).align(Alignment.BottomStart)
                .background(Brush.horizontalGradient(listOf(Color(0xFFE18FD6), Color(0xFF9D91E8), Color.Transparent)))
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.LocalFireDepartment, null,
                    tint = if (streak > 0) Color(0xFFFF9500) else Color.White.copy(.3f),
                    modifier = Modifier.size(16.dp))
                if (apiCompleted || response != null) {
                    Text("$streak× Streak", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(.9f))
                } else {
                    CircularProgressIndicator(color = Color.White.copy(.3f), modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                }
            }
            Text("+$scoreGained pts", fontSize = 16.sp, fontWeight = FontWeight.Medium,
                color = AccGreen.copy(if (scoreGained > 0) .8f else .3f))
            if (apiCompleted && scoreGained == 0) {
                Text("Beat your best to earn points", fontSize = 11.sp, color = Color.White.copy(.3f))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Solve Time Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SolveTimeCard(elapsed: Int, difficulty: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(90.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(.05f))
            .border(1.dp, Color.White.copy(.10f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(fmtTime(elapsed), fontSize = 26.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, color = Color.White)
            Text("Difficulty $difficulty/10", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                color = Color.White.copy(.45f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Orbs + confetti
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OrbsArea(orbPulse: Float, confetti: List<Confetti>) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(70.dp).offset(x = (-90).dp, y = (20f + (12f - 20f) * orbPulse).dp)
            .blur(12.dp).background(Brush.radialGradient(listOf(GreenOrb.copy(.9f), GreenOrb.copy(.3f), Color.Transparent)), CircleShape))
        Box(Modifier.size(14.dp).offset(x = (-90).dp, y = (20f + (12f - 20f) * orbPulse).dp).background(GreenOrb, CircleShape))

        Box(Modifier.size(70.dp).offset(x = 90.dp, y = (15f + (5f - 15f) * orbPulse).dp)
            .blur(12.dp).background(Brush.radialGradient(listOf(PinkOrb.copy(.9f), PinkOrb.copy(.3f), Color.Transparent)), CircleShape))
        Box(Modifier.size(14.dp).offset(x = 90.dp, y = (15f + (5f - 15f) * orbPulse).dp).background(PinkOrb, CircleShape))

        confetti.forEach { p ->
            val yOff = p.y + if (orbPulse > 0.5f) -4f else 4f
            Box(Modifier.size(p.size.dp, (p.size * 0.6f).dp).offset(x = p.x.dp, y = yOff.dp)
                .rotate(p.x * 5f).background(confColors[p.ci].copy(p.alpha), RoundedCornerShape(1.dp)))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun fmtTime(s: Int): String {
    val m = s / 60; val sec = s % 60
    return if (m > 0) "${m}m ${sec}s" else "${sec}s"
}

private fun tierColor(tier: String): Color = when (tier.lowercase()) {
    "diamond"  -> Color(0xFF00E5FF)
    "platinum" -> Color(0xFFE5E4E2)
    "gold"     -> Color(0xFFFFD700)
    "silver"   -> Color(0xFFC0C0C0)
    else       -> Color(0xFFCD7F32)
}
