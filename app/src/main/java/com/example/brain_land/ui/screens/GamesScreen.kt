package com.example.brain_land.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brain_land.data.GameItem
import com.example.brain_land.data.GameType
import com.example.brain_land.viewmodel.GamesViewModel

// ──────────────────────────────────────────────────────────
// Game filter categories — mirrors iOS GameFilter enum
// ──────────────────────────────────────────────────────────

enum class GameFilter(val label: String) {
    ALL("All"),
    PUZZLE("Puzzle"),
    LOGIC("Logic"),
    MATH("Math"),
    VISUAL("Visual");

    fun matches(game: GameType): Boolean = when (this) {
        ALL    -> true
        PUZZLE -> game in listOf(
            GameType.PIPE_CONNECT, GameType.HIDDEN_PAIR, GameType.BLOCK_FIT,
            GameType.NONOGRAM, GameType.LIQUID_SORT, GameType.TILT_MAZE
        )
        LOGIC  -> game in listOf(
            GameType.BINARY_PUZZLE, GameType.SLITHERLINK,
            GameType.CRYPTO_CAGE, GameType.NEURAL_LINK
        )
        MATH   -> game in listOf(GameType.NUMBER_CIRCUIT)
        VISUAL -> game in listOf(
            GameType.LASER_PUZZLE, GameType.GALACTIC_BEACONS, GameType.PATH_CLEARING
        )
    }
}

// Category label per game — mirrors iOS categoryLabel()
fun categoryLabel(game: GameType): String = when (game) {
    GameType.PIPE_CONNECT,
    GameType.HIDDEN_PAIR,
    GameType.BLOCK_FIT,
    GameType.NONOGRAM,
    GameType.TILT_MAZE,
    GameType.LIQUID_SORT   -> "Puzzle"
    GameType.BINARY_PUZZLE,
    GameType.SLITHERLINK,
    GameType.CRYPTO_CAGE,
    GameType.NEURAL_LINK   -> "Logic"
    GameType.NUMBER_CIRCUIT -> "Math"
    GameType.LASER_PUZZLE,
    GameType.GALACTIC_BEACONS,
    GameType.PATH_CLEARING -> "Visual"
    GameType.WORD_PUZZLE   -> "Wordly"
    else                   -> "Puzzle"
}

private val accentCyan   = Color(0xFF00E5FF)
private val accentPurple = Color(0xFFA78BFA)
private val bgDark       = Color(0xFF10131B)

// ──────────────────────────────────────────────────────────
// GamesScreen — fetches from backend, mirrors iOS GamesView
// ──────────────────────────────────────────────────────────

@Composable
fun GamesScreen(
    vm: GamesViewModel = viewModel(),
    onSelectGame: (GameType) -> Unit = {}
) {
    val allGames   by vm.games.collectAsState()
    val isLoading  by vm.isLoading.collectAsState()

    var searchText     by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(GameFilter.ALL) }
    val focusManager   = LocalFocusManager.current

    // Filter: first resolve GameType, then apply filter + search
    // Mirrors iOS: visibleGames.reversed().filter { matchesFilter && matchesSearch }
    val filteredGames = remember(allGames, searchText, selectedFilter) {
        allGames
            .reversed()
            .filter { item ->
                val gt = item.resolvedType ?: return@filter false
                val matchesFilter = selectedFilter.matches(gt)
                val matchesSearch = searchText.isEmpty() ||
                    item.displayName.contains(searchText, ignoreCase = true) ||
                    item.gameType.contains(searchText, ignoreCase = true)
                matchesFilter && matchesSearch
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgDark)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Games",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${allGames.size} games",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.35f)
                )
            }

            // ── Search bar ──
            GameSearchBar(
                searchText = searchText,
                onSearchChange = { searchText = it },
                onClear = {
                    searchText = ""
                    focusManager.clearFocus()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp)
            )

            // ── Filter chips ──
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                items(GameFilter.entries) { filter ->
                    val count = allGames.count { item ->
                        val gt = item.resolvedType ?: return@count false
                        filter.matches(gt)
                    }
                    GameFilterChip(
                        filter = filter,
                        count = count,
                        isSelected = filter == selectedFilter,
                        onClick = { selectedFilter = filter }
                    )
                }
            }

            // ── Game grid or loading / empty ──
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading && allGames.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = accentCyan,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    filteredGames.isEmpty() -> GameEmptyState()
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                top = 0.dp,
                                bottom = 140.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredGames) { item ->
                                val gt = item.resolvedType
                                if (gt != null) {
                                    GameCard(
                                        game = gt,
                                        backendName = item.displayName,
                                        onClick = { onSelectGame(gt) }
                                    )
                                }
                            }
                        }

                        // Bottom fade — mirrors iOS ZStack overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        0f    to Color.Transparent,
                                        0.3f  to bgDark.copy(alpha = 0.4f),
                                        0.65f to bgDark.copy(alpha = 0.85f),
                                        1f    to bgDark
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
// Search bar
// ──────────────────────────────────────────────────────────

@Composable
private fun GameSearchBar(
    searchText: String,
    onSearchChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(18.dp)
        )
        BasicTextField(
            value = searchText,
            onValueChange = onSearchChange,
            singleLine = true,
            cursorBrush = SolidColor(accentCyan),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { }),
            textStyle = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            ),
            decorationBox = { innerTextField ->
                Box {
                    if (searchText.isEmpty()) {
                        Text(
                            "Search games...",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.25f)
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier.weight(1f)
        )
        AnimatedVisibility(visible = searchText.isNotEmpty()) {
            Icon(
                Icons.Default.Cancel,
                contentDescription = "Clear",
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onClear() }
            )
        }
    }
}

// ──────────────────────────────────────────────────────────
// Filter chip
// ──────────────────────────────────────────────────────────

@Composable
private fun GameFilterChip(
    filter: GameFilter,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgBrush = if (isSelected) {
        Brush.horizontalGradient(listOf(accentCyan.copy(alpha = 0.2f), accentPurple.copy(alpha = 0.1f)))
    } else {
        Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.04f), Color.White.copy(alpha = 0.04f)))
    }
    val borderColor = if (isSelected) accentCyan.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.06f)
    val textColor   = if (isSelected) Color.White else Color.White.copy(alpha = 0.45f)

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(bgBrush)
            .border(1.dp, borderColor, CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(filter.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textColor)
        if (filter != GameFilter.ALL) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    "$count",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = textColor
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
// Game Card — mirrors iOS gameCard() exactly
//
// • 160dp tall, 2-column grid
// • cardColor linear gradient background
// • 2 decorative semi-transparent circles
// • emoji icon centered, offset y:-14dp
// • bottom 40% black gradient overlay
// • bottom row: BackendName · Level N · Category | ▶
// ──────────────────────────────────────────────────────────

@Composable
fun GameCard(
    game: GameType,
    backendName: String = game.displayName,
    level: Int = 1,
    onClick: () -> Unit = {}
) {
    val cardColor = hexToColor(game.cardColorHex)
    val isLight   = game.isLightCard
    val fg        = if (isLight) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_scale_${game.name}",
        finishedListener = { pressed = false }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .scale(scale)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = cardColor.copy(alpha = 0.15f),
                spotColor = cardColor.copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                pressed = true
                onClick()
            }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.03f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        // Background gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(cardColor, cardColor.copy(alpha = 0.6f))))
        )

        // Decorative circle 1 — offset(x:40, y:-20)
        Box(
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.TopEnd)
                .offset(x = (-10).dp, y = (-10).dp)
                .background(fg.copy(alpha = 0.04f), CircleShape)
        )
        // Decorative circle 2 — offset(x:-30, y:25)
        Box(
            modifier = Modifier
                .size(50.dp)
                .align(Alignment.TopStart)
                .offset(x = (-10).dp, y = 40.dp)
                .background(fg.copy(alpha = 0.03f), CircleShape)
        )

        // Emoji icon — centered, shifted up
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (game == GameType.BINARY_PUZZLE) "01" else game.emoji,
                fontSize = if (game == GameType.BINARY_PUZZLE) 30.sp else 44.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
                modifier = Modifier.offset(y = (-14).dp)
            )
        }

        // Bottom black gradient (40% of 160dp = 64dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        0f    to Color.Black.copy(0f),
                        0.35f to Color.Black.copy(0.55f),
                        0.7f  to Color.Black.copy(0.85f),
                        1f    to Color.Black.copy(0.95f)
                    )
                )
        )

        // Bottom text row
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = backendName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val levelColor = if (isLight) cardColor else accentCyan
                    Text("Level $level", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = levelColor)
                    Text("•", fontSize = 8.sp, color = Color.White.copy(alpha = 0.4f))
                    Text(categoryLabel(game), fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.55f))
                }
            }
            Icon(
                imageVector = Icons.Filled.PlayCircleFilled,
                contentDescription = "Play",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────
// Empty / loading states
// ──────────────────────────────────────────────────────────

@Composable
private fun GameEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.SearchOff,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "No results",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.3f),
                textAlign = TextAlign.Center
            )
        }
    }
}
