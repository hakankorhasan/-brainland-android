package com.example.brain_land.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.brain_land.data.GameType
import com.example.brain_land.data.LeaderboardPlayer
import com.example.brain_land.data.LeaderboardResponse
import com.example.brain_land.ui.theme.*
import com.example.brain_land.viewmodel.HomeViewModel

// ──────────────────────────────────────────────────────────────
// Tab enum — mirrors iOS TabItem
// ──────────────────────────────────────────────────────────────

enum class HomeTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    GAMES("Games", Icons.Filled.GridView, Icons.Outlined.GridView),
    LEADERBOARD("Ranks", Icons.Filled.Leaderboard, Icons.Outlined.Leaderboard),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

// ──────────────────────────────────────────────────────────────
// Root HomeScreen with custom tab bar
// ──────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(vm: HomeViewModel = viewModel()) {
    val leaderboard by vm.leaderboard.collectAsState()
    val isLoadingLeader by vm.isLoadingLeader.collectAsState()
    val suggested by vm.suggestedGames.collectAsState()
    val nickname by vm.nickname.collectAsState()
    val avatarUrl by vm.avatarUrl.collectAsState()
    val daily by vm.completedPuzzles.collectAsState()
    val streak by vm.dailyStreak.collectAsState()
    val playerProfile by vm.playerProfile.collectAsState()
    val isLoadingProfile by vm.isLoadingProfile.collectAsState()

    var selectedTab  by remember { mutableStateOf(HomeTab.HOME) }
    var activeGame   by remember { mutableStateOf<GameType?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCard)
    ) {
        // ── Tab content (always below game overlay) ──
        when (selectedTab) {
            HomeTab.HOME ->
                HomeTabContent(
                    nickname         = nickname,
                    avatarUrl        = avatarUrl,
                    leaderboard      = leaderboard,
                    isLoadingLeader  = isLoadingLeader,
                    playerProfile    = playerProfile,
                    isLoadingProfile = isLoadingProfile,
                    suggested        = suggested,
                    completedPuzzles = daily,
                    streak           = streak,
                    onTabChange      = { selectedTab = it }
                )
            HomeTab.GAMES ->
                GamesTabContent(
                    suggested     = GameType.allTypes(),
                    onSelectGame  = { game -> activeGame = game }
                )
            HomeTab.LEADERBOARD ->
                LeaderboardTabContent(leaderboard = leaderboard, isLoading = isLoadingLeader)
            HomeTab.PROFILE ->
                ProfileTabContent(nickname = nickname, avatarUrl = avatarUrl)
        }

        // ── Tab Bar — hidden when a game is active ──
        if (activeGame == null) {
            BrainLandTabBar(
                selectedTab   = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier      = Modifier.align(Alignment.BottomCenter)
            )
        }

        // ── Game full-screen overlay (covers tab bar + everything) ──
        if (activeGame == GameType.TILT_MAZE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF10131B))
            ) {
                com.example.brain_land.ui.games.tiltmaze.TiltMazePuzzleView(
                    onHome           = { activeGame = null },
                    onNavigateToGame = { targetGame -> activeGame = targetGame }
                )
            }
        }

        if (activeGame == GameType.LIQUID_SORT) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF10131B))
            ) {
                com.example.brain_land.ui.games.liquidsort.LiquidSortPuzzleView(
                    onHome           = { activeGame = null },
                    onNavigateToGame = { targetGame -> activeGame = targetGame }
                )
            }
        }

        if (activeGame == GameType.PATH_CLEARING) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF10131B))
            ) {
                com.example.brain_land.ui.games.arrowpuzzle.ArrowPuzzlePuzzleView(
                    onHome           = { activeGame = null },
                    onNavigateToGame = { targetGame -> activeGame = targetGame }
                )
            }
        }

        if (activeGame == GameType.WORD_PUZZLE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF10131B))
            ) {
                com.example.brain_land.ui.games.wordpuzzle.WordPuzzlePuzzleView(
                    onHome           = { activeGame = null },
                    onNavigateToGame = { targetGame -> activeGame = targetGame }
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Home Tab  (mirrors iOS homeView)
// ──────────────────────────────────────────────────────────────

@Composable
private fun HomeTabContent(
    nickname: String,
    avatarUrl: String,
    leaderboard: LeaderboardResponse?,
    isLoadingLeader: Boolean,
    playerProfile: com.example.brain_land.data.PlayerProfileData?,
    isLoadingProfile: Boolean,
    suggested: List<GameType>,
    completedPuzzles: Set<Int>,
    streak: Int,
    onTabChange: (HomeTab) -> Unit
) {
    val bgColor = BgCard

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
        ) {
            // ── Header ──
            HomeHeader(nickname = nickname, avatarUrl = avatarUrl)

            Spacer(Modifier.height(20.dp))

            // ── Player Rank section ──
            SectionTitle(title = "Player Rank")
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RatingCard(
                    playerProfile    = playerProfile,
                    isLoadingProfile = isLoadingProfile,
                    modifier         = Modifier.weight(1f)
                )
                DailyChallengeCard(
                    completedPuzzles = completedPuzzles,
                    streak = streak,
                    modifier = Modifier.width(140.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Suggested Games ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle(title = "Suggested Games")
                Spacer(Modifier.weight(1f))
                Text(
                    text = "See All",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.clickable { onTabChange(HomeTab.GAMES) }
                )
            }
            Spacer(Modifier.height(14.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(suggested) { game ->
                    HomeGameCard(game = game)
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Story Mode Banner ──
            StoryModeBanner(modifier = Modifier.padding(horizontal = 16.dp))

            Spacer(Modifier.height(28.dp))

            // ── Leaderboard section ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle(title = "Leaderboard")
                Spacer(Modifier.weight(1f))
                Text(
                    text = "See All",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.clickable { onTabChange(HomeTab.LEADERBOARD) }
                )
            }
            Spacer(Modifier.height(14.dp))

            LeaderboardCard(
                leaderboard = leaderboard,
                isLoading = isLoadingLeader,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Space for tab bar
            Spacer(Modifier.height(120.dp))
        }

        // Bottom fade gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.4f to bgColor.copy(alpha = 0.5f),
                        1f to bgColor
                    )
                )
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Header (Avatar + Nickname + Settings)
// ──────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(nickname: String, avatarUrl: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        if (avatarUrl.isNotEmpty()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF6FE4CF), Color(0xFFB88AE8))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (nickname.isEmpty()) "P"
                    else nickname.first().uppercaseChar().toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = if (nickname.isEmpty()) "Player" else nickname,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(Modifier.weight(1f))

        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Settings",
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(22.dp)
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Rating Card (mirrors iOS ratingCard)
// ──────────────────────────────────────────────────────────────

@Composable
private fun RatingCard(
    playerProfile: com.example.brain_land.data.PlayerProfileData?,
    isLoadingProfile: Boolean,
    modifier: Modifier = Modifier
) {
    // Prefer weightedGlobalScore (mirrors iOS displayScore)
    val myScore = playerProfile?.weightedGlobalScore ?: playerProfile?.globalScore ?: 0
    val myRank  = playerProfile?.rank ?: 0
    val myTier  = playerProfile?.tier ?: "bronze"

    val (tierEmoji, tierClr) = tierInfo(myScore, myTier)

    Box(
        modifier = modifier
            .height(200.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF9C5B9E), Color(0xFF4C528C), Color(0xFF9A60AA))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Rating", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.height(4.dp))

                if (isLoadingProfile && playerProfile == null) {
                    // Loading skeleton
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(0.12f))
                    )
                } else {
                    Text(
                        text = "$myScore",
                        fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White
                    )
                }

                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Cyan.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "#${if (myRank > 0) myRank else "-"} Global",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tierEmoji, fontSize = 28.sp)
                Spacer(Modifier.width(6.dp))
                Text(myTier.uppercase(),
                    fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = tierClr)
                Text(" League", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Daily Challenge Card (mirrors iOS dailyChallengeCard)
// ──────────────────────────────────────────────────────────────

@Composable
private fun DailyChallengeCard(
    completedPuzzles: Set<Int>,
    streak: Int,
    modifier: Modifier = Modifier
) {
    val allCompleted = completedPuzzles.size >= 5

    Box(
        modifier = modifier
            .height(200.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    if (allCompleted)
                        listOf(Color(0xFF1A3A2A), Color(0xFF383754), Color(0xFF252530))
                    else
                        listOf(Color(0xFF4A4C8F), Color(0xFF383754), Color(0xFF252530))
                )
            )
            .border(
                1.dp,
                if (allCompleted) Color(0xFF4FFFB0).copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(18.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Daily Challenge",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (allCompleted) "✅ Completed!" else "Solve 5 puzzles",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            // Progress dots
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                (1..5).forEach { i ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (completedPuzzles.contains(i)) Color(0xFF4FFFB0)
                                else Color.White.copy(alpha = 0.1f)
                            )
                    )
                }
            }

            // Streak or play now
            if (streak > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔥", fontSize = 14.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "$streak day streak",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            } else {
                Text(
                    "Play now →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Home Game Card (130×130 card with game colour & emoji icon)
// mirrors iOS homeGameCard
// ──────────────────────────────────────────────────────────────

@Composable
private fun HomeGameCard(game: GameType) {
    val cardColor = hexToColor(game.cardColorHex)
    val fg = if (game.isLightCard) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "card_scale"
    )

    Column(
        modifier = Modifier
            .width(130.dp)
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.03f),
                        Color.Transparent
                    )
                ),
                RoundedCornerShape(14.dp)
            )
            .clickable { pressed = !pressed }
    ) {
        // Icon area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(
                    Brush.linearGradient(
                        listOf(cardColor, cardColor.copy(alpha = 0.5f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Decorative circle
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .offset(30.dp, (-15).dp)
                    .background(fg.copy(alpha = 0.04f), CircleShape)
            )
            Text(
                text = if (game == GameType.BINARY_PUZZLE) "01" else game.emoji,
                fontSize = if (game == GameType.BINARY_PUZZLE) 22.sp else 30.sp,
                fontWeight = FontWeight.Bold,
                color = fg
            )
        }

        // Name bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.03f))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = game.displayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Story Mode Banner  (mirrors iOS Story Mode promo ZStack)
// ──────────────────────────────────────────────────────────────

@Composable
private fun StoryModeBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF6B3FA0), Color(0xFF4A2D7A), Color(0xFF2E1B5E))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Stars decoration
        StarsDecoration()

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "STORY MODE",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Immersive story-driven puzzles",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun StarsDecoration() {
    val positions = remember {
        listOf(
            Pair(0.1f, 0.2f), Pair(0.8f, 0.15f), Pair(0.9f, 0.7f),
            Pair(0.15f, 0.8f), Pair(0.5f, 0.1f), Pair(0.65f, 0.85f)
        )
    }
    Box(modifier = Modifier.fillMaxSize()) {
        positions.forEach { (xFrac, yFrac) ->
            Text(
                text = "✦",
                fontSize = 8.sp,
                color = Color.White.copy(alpha = 0.3f),
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
                    .offset(
                        x = ((xFrac - 0.5f) * 300).dp,
                        y = ((yFrac - 0.5f) * 80).dp
                    )
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Leaderboard Card — top 3 players
// ──────────────────────────────────────────────────────────────

@Composable
private fun LeaderboardCard(
    leaderboard: LeaderboardResponse?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val players = leaderboard?.players?.take(3) ?: listOf(
        null, null, null
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
    ) {
        if (isLoading && leaderboard == null) {
            repeat(3) { i ->
                LeaderboardRow(
                    rank = i + 1,
                    name = "—",
                    score = 0,
                    avatarUrl = null,
                    isLoading = true
                )
                if (i < 2) HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
            }
        } else {
            val displayPlayers = if (players.isEmpty()) listOf(null, null, null) else players
            displayPlayers.forEachIndexed { idx, player ->
                if (player != null) {
                    LeaderboardRow(
                        rank = player.rank,
                        name = player.displayName,
                        score = player.displayScore,
                        avatarUrl = player.avatarUrl,
                        isLoading = false
                    )
                } else {
                    LeaderboardRow(rank = idx + 1, name = "—", score = 0, avatarUrl = null, isLoading = false)
                }
                if (idx < displayPlayers.size - 1) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(
    rank: Int,
    name: String,
    score: Int,
    avatarUrl: String?,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Rank badge
        val badgeEmoji = when (rank) {
            1 -> "🥇"
            2 -> "🥈"
            3 -> "🥉"
            else -> "#$rank"
        }
        if (rank <= 3) {
            Text(badgeEmoji, fontSize = 20.sp, modifier = Modifier.width(28.dp))
        } else {
            Text(
                text = badgeEmoji,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.width(28.dp)
            )
        }

        // Avatar
        if (!avatarUrl.isNullOrEmpty()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(32.dp).clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF6FE4CF).copy(alpha = 0.3f),
                                Color(0xFFB88AE8).copy(alpha = 0.3f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                val initial = if (name == "—") "" else name.firstOrNull()?.uppercaseChar()?.toString() ?: ""
                Text(initial, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // Name
        Text(
            text = name,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f)
        )

        // Score
        Text(
            text = "$score",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AccentCyan
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Section title helper
// ──────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

// ──────────────────────────────────────────────────────────────
// Placeholder tabs (Games / Leaderboard / Profile)
// These will be fully implemented in the next step
// ──────────────────────────────────────────────────────────────

@Composable
private fun GamesTabContent(
    suggested: List<GameType>,
    onSelectGame: (GameType) -> Unit
) {
    GamesScreen(
        onSelectGame = { game -> onSelectGame(game) }
    )
}

@Composable
private fun LeaderboardTabContent(leaderboard: LeaderboardResponse?, isLoading: Boolean) {
    LeaderboardScreen()
}

@Composable
private fun ProfileTabContent(nickname: String, avatarUrl: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCard)
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Text("👤 Profile\n(Coming soon)", color = Color.White, textAlign = TextAlign.Center, fontSize = 18.sp)
    }
}

// ──────────────────────────────────────────────────────────────
// Custom Tab Bar — mirrors iOS CustomTabBar exactly
// ──────────────────────────────────────────────────────────────

@Composable
private fun BrainLandTabBar(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1A1D2E).copy(alpha = 0.95f))
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.08f),
                    RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            HomeTab.entries.forEach { tab ->
                TabItem(
                    tab = tab,
                    isSelected = tab == selectedTab,
                    onClick = { onTabSelected(tab) }
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: HomeTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.9f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "tab_scale_${tab.name}"
    )

    Column(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) AccentPurple.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
            contentDescription = tab.label,
            tint = if (isSelected) AccentPurple else Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = tab.label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) AccentPurple else Color.White.copy(alpha = 0.4f)
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Utility helpers
// ──────────────────────────────────────────────────────────────

fun hexToColor(hex: String): Color {
    val cleaned = hex.trimStart('#')
    val value = cleaned.toLongOrNull(16) ?: return Color.Gray
    return when (cleaned.length) {
        6 -> Color(
            red   = ((value shr 16) and 0xFF) / 255f,
            green = ((value shr 8)  and 0xFF) / 255f,
            blue  =  (value         and 0xFF) / 255f
        )
        else -> Color.Gray
    }
}

private fun tierInfo(score: Int, tier: String): Pair<String, Color> {
    val t = tier.lowercase()
    return when {
        t == "legend"   || score >= 15000 -> "👑" to Color(0xFFFFD700)
        t == "diamond"  || score >= 10000 -> "💎" to Color(0xFF00D4FF)
        t == "platinum" || score >= 6000  -> "⚡" to Color(0xFFE5E4E2)
        t == "gold"     || score >= 3000  -> "🔱" to Color(0xFFFFAA00)
        t == "silver"   || score >= 1000  -> "🌙" to Color(0xFFC0C0C0)
        else                              -> "🛡️" to Color(0xFFCD7F32)
    }
}
