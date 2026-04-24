package com.example.brain_land.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.brain_land.R
import com.example.brain_land.data.*
import com.example.brain_land.ui.theme.*
import com.example.brain_land.viewmodel.HomeViewModel

// ── Colors (iOS ProfileView exact match) ──
private val ProfileBg = Color(0xFF10131B)
private val PCyan = Color(0xFF00E5FF)
private val PPurple = Color(0xFFA78BFA)
private val CardBgColor = Color.White.copy(alpha = 0.04f)
private val CardStroke = Color.White.copy(alpha = 0.06f)

// ── Tier definitions (mirrors iOS allTiers) ──
data class TierDef(val name: String, val color: Color, val iconRes: Int, val minScore: Int)

val allTiers = listOf(
    TierDef("bronze",   Color(0xFFCD7F32), R.drawable.bronze,   0),
    TierDef("silver",   Color(0xFFC0C0C0), R.drawable.silver,   1000),
    TierDef("gold",     Color(0xFFFFD700), R.drawable.gold,     3000),
    TierDef("platinum", Color(0xFF50C9CE), R.drawable.platinum, 6000),
    TierDef("diamond",  Color(0xFFB9F2FF), R.drawable.diamond,  10000),
    TierDef("legend",   Color(0xFFFF6B6B), R.drawable.legend,   15000),
)

// ── Achievement model ──
data class Achievement(
    val id: String, val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String, val desc: String,
    val requirement: Int, val current: Int, val color: Color
) {
    val isUnlocked get() = current >= requirement
    val progress get() = if (requirement > 0) (current.toFloat() / requirement).coerceIn(0f, 1f) else 0f
}

// ═══════════════════════════════════════════════════════════════
// ProfileScreen — main composable
// ═══════════════════════════════════════════════════════════════

@Composable
fun ProfileScreen(vm: HomeViewModel = viewModel()) {
    val profileData by vm.playerProfileFull.collectAsState()
    val isLoading by vm.isLoadingProfileFull.collectAsState()
    val nickname by vm.nickname.collectAsState()
    val avatarUrl by vm.avatarUrl.collectAsState()

    LaunchedEffect(Unit) { vm.fetchFullProfile() }

    val displayName = profileData?.profile?.nickname?.ifEmpty { null } ?: nickname.ifEmpty { "Player" }
    val myScore = profileData?.stats?.weightedGlobalScore ?: 0
    val myTier = profileData?.stats?.tier ?: "Bronze"
    val myRank = profileData?.stats?.rank ?: 0
    val totalPlayed = profileData?.stats?.gamesPlayed ?: 0
    val uniqueGames = profileData?.stats?.uniqueGamesPlayed ?: 0
    val winRate = profileData?.stats?.winRate ?: 0.0
    val bestStreak = profileData?.stats?.bestStreak ?: 0
    val tierIdx = allTiers.indexOfFirst { it.name.equals(myTier, true) }.coerceAtLeast(0)
    val currentTier = allTiers[tierIdx]

    Column(
        modifier = Modifier.fillMaxSize().background(ProfileBg).statusBarsPadding()
    ) {
        // ── Header ──
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Profile", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.weight(1f))
        }

        if (isLoading && profileData == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PCyan, strokeWidth = 2.dp)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1) Profile Header Card
                ProfileHeaderCard(displayName, avatarUrl, myRank, currentTier)

                // 2) Tier Progress Card
                TierProgressCard(myScore, myTier, tierIdx, profileData?.tierProgress)

                // 3) Quick Stats
                QuickStatsRow(totalPlayed, winRate, bestStreak, myRank)

                // 4) Daily Challenge
                profileData?.dailyChallenge?.let { dc ->
                    if (dc.totalDaysCompleted > 0 || dc.currentStreak > 0) {
                        DailyChallengeCard(dc)
                    }
                }

                // 5) Achievements
                AchievementsSection(totalPlayed, uniqueGames, myScore)

                // 6) Game Stats
                profileData?.let { GameStatsSection(it) }

                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 1) Profile Header Card
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ProfileHeaderCard(name: String, avatarUrl: String, rank: Int, tier: TierDef) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBgColor)
            .border(1.dp, Brush.linearGradient(listOf(tier.color.copy(0.15f), CardStroke)), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        if (avatarUrl.isNotEmpty()) {
            AsyncImage(
                model = avatarUrl, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp).clip(CircleShape)
                    .border(2.dp, Brush.linearGradient(listOf(Color(0xFF6FE4CF).copy(0.5f), Color(0xFFB88AE8).copy(0.5f))), CircleShape)
            )
        } else {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF6FE4CF), Color(0xFFB88AE8))))
                    .border(2.dp, Brush.linearGradient(listOf(Color(0xFF6FE4CF).copy(0.5f), Color(0xFFB88AE8).copy(0.5f))), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(name.take(1).uppercase(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(Modifier.width(16.dp))

        Column {
            Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (rank > 0) {
                    Box(
                        modifier = Modifier.clip(CircleShape)
                            .background(Color(0xFF6FE4CF).copy(0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("#$rank Global", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6FE4CF))
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 2) Tier Progress Card
// ═══════════════════════════════════════════════════════════════

@Composable
private fun TierProgressCard(score: Int, tier: String, tierIdx: Int, tp: PlayerProfileTierProgress?) {
    val currentTier = allTiers[tierIdx]
    val nextTier = if (tierIdx < allTiers.size - 1) allTiers[tierIdx + 1] else null
    val progress = tp?.progress?.toFloat() ?: run {
        if (nextTier == null) 1f
        else {
            val range = nextTier.minScore - currentTier.minScore
            if (range > 0) ((score - currentTier.minScore).toFloat() / range).coerceIn(0f, 1f) else 1f
        }
    }
    val ptsToNext = tp?.pointsToNext ?: (nextTier?.let { it.minScore - score } ?: 0)

    Column(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)).background(CardBgColor)
            .border(1.dp, Brush.linearGradient(listOf(currentTier.color.copy(0.2f), CardStroke)), RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.TrendingUp, null, tint = PCyan, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Tier Progress", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.weight(1f))
            Text("$score pts", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PCyan, fontFamily = FontFamily.Monospace)
        }

        // Tier icons row — weight(1f) so all 6 share space equally (no squishing)
        Row(Modifier.fillMaxWidth()) {
            allTiers.forEachIndexed { idx, t ->
                val isUnlocked = idx <= tierIdx
                val isActive   = idx == tierIdx
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
                        // Radial glow for active tier
                        if (isActive) {
                            Box(
                                modifier = Modifier.size(44.dp).background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            t.color.copy(alpha = 0.45f),
                                            t.color.copy(alpha = 0.1f),
                                            Color.Transparent
                                        )
                                    )
                                )
                            )
                        }
                        // Circle background — neutral, no color bleed through transparent PNG pixels
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                                .background(Color.White.copy(alpha = if (isUnlocked) 0.07f else 0.03f))
                                .then(if (isActive) Modifier.border(2.dp, t.color, CircleShape) else Modifier)
                        )
                        // Use AsyncImage (Coil) — handles density & color space correctly
                        AsyncImage(
                            model = t.iconRes,
                            contentDescription = t.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(34.dp),
                            alpha = if (isUnlocked) 1f else 0.3f
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        t.name.replaceFirstChar { it.uppercase() },
                        fontSize = 9.sp,
                        fontWeight = if (isUnlocked) FontWeight.Bold else FontWeight.Normal,
                        color = if (isUnlocked) t.color else Color.White.copy(0.3f)
                    )
                }
            }
        }

        // Progress bar
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth().height(12.dp)
                    .clip(RoundedCornerShape(6.dp)).background(Color.White.copy(0.06f))
            ) {
                Box(
                    modifier = Modifier.fillMaxHeight()
                        .fillMaxWidth(progress.coerceAtLeast(0.02f))
                        .clip(RoundedCornerShape(6.dp))
                        .background(Brush.horizontalGradient(listOf(currentTier.color, nextTier?.color ?: currentTier.color)))
                )
            }
            Row {
                Text(currentTier.name.replaceFirstChar { it.uppercase() }, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = currentTier.color)
                Spacer(Modifier.weight(1f))
                if (nextTier != null) {
                    Text("$ptsToNext pts to ${nextTier.name.replaceFirstChar { it.uppercase() }}", fontSize = 10.sp, color = Color.White.copy(0.4f))
                } else {
                    Text("MAX TIER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 3) Quick Stats Row
// ═══════════════════════════════════════════════════════════════

@Composable
private fun QuickStatsRow(totalPlayed: Int, winRate: Double, bestStreak: Int, rank: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MiniStatCard(Icons.Default.Whatshot, "$totalPlayed", "Total Played", Color(0xFFFF6B6B), Modifier.weight(1f))
        MiniStatCard(Icons.Default.Percent, if (winRate > 0) "%.1f%%".format(winRate) else "—", "Win Rate", Color(0xFF4FFFB0), Modifier.weight(1f))
        MiniStatCard(Icons.Default.Bolt, if (bestStreak > 0) "$bestStreak" else "—", "Best Streak", PCyan, Modifier.weight(1f))
        MiniStatCard(Icons.Default.EmojiEvents, if (rank > 0) "#$rank" else "—", "Rank", Color(0xFFFFD700), Modifier.weight(1f))
    }
}

@Composable
private fun MiniStatCard(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(CardBgColor)
            .border(1.dp, CardStroke, RoundedCornerShape(14.dp)).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color.copy(0.1f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
        }
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(0.35f))
    }
}

// ═══════════════════════════════════════════════════════════════
// 4) Daily Challenge Card
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DailyChallengeCard(dc: PlayerProfileDailyChallenge) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)).background(CardBgColor)
            .border(1.dp, Brush.linearGradient(listOf(Color(0xFFFBBF24).copy(0.2f), CardStroke)), RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarMonth, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Daily Challenge", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            DcStat(Icons.Default.Whatshot, "${dc.currentStreak}", "Current", Color(0xFFFF6B6B))
            DcStat(Icons.Default.EmojiEvents, "${dc.bestStreak}", "Best", Color(0xFFFFD700))
            DcStat(Icons.Default.CheckCircle, "${dc.totalDaysCompleted}", "Days", Color(0xFF4FFFB0))
            DcStat(Icons.Default.Extension, "${dc.totalPuzzlesSolved}", "Solved", PCyan)
        }
    }
}

@Composable
private fun DcStat(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(0.35f))
    }
}

// ═══════════════════════════════════════════════════════════════
// 5) Achievements Section
// ═══════════════════════════════════════════════════════════════

@Composable
private fun AchievementsSection(totalPlayed: Int, uniqueGames: Int, score: Int) {
    val achievements = listOf(
        Achievement("first", Icons.Default.PlayCircle, "First Steps", "Play your first game", 1, totalPlayed, Color(0xFF4FFFB0)),
        Achievement("10g", Icons.Default.Whatshot, "Getting Warmed Up", "Play 10 games", 10, totalPlayed, Color(0xFFFF6B6B)),
        Achievement("50g", Icons.Default.Bolt, "Dedicated Player", "Play 50 games", 50, totalPlayed, Color(0xFFFBBF24)),
        Achievement("100g", Icons.Default.Star, "Century", "Play 100 games", 100, totalPlayed, Color(0xFFFF8A65)),
        Achievement("3dif", Icons.Default.Casino, "Versatile", "Play 3 different games", 3, uniqueGames, PPurple),
        Achievement("5dif", Icons.Default.SportsEsports, "Game Explorer", "Play 5 different games", 5, uniqueGames, Color(0xFF50C9CE)),
        Achievement("silver", Icons.Default.Shield, "Silver League", "Reach Silver tier", 500, score, Color(0xFFC0C0C0)),
        Achievement("gold", Icons.Default.MilitaryTech, "Gold League", "Reach Gold tier", 1500, score, Color(0xFFFFD700)),
        Achievement("s100", Icons.Default.GpsFixed, "Triple Digits", "Reach 100 score", 100, score, Color(0xFF6FE4CF)),
        Achievement("s1000", Icons.Default.EmojiEvents, "Score Master", "Reach 1,000 score", 1000, score, Color(0xFFFFD700)),
    )
    val unlocked = achievements.filter { it.isUnlocked }
    val locked = achievements.filter { !it.isUnlocked }.take(3)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Title
        Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MilitaryTech, null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Achievements", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.weight(1f))
            Text("${unlocked.size}/${achievements.size}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(0.4f))
        }

        // Unlocked horizontal scroll
        if (unlocked.isNotEmpty()) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(unlocked, key = { it.id }) { a ->
                    AchievementCard(a)
                }
            }
        }

        // Locked rows
        locked.forEach { a -> LockedAchievementRow(a) }
    }
}

@Composable
private fun AchievementCard(a: Achievement) {
    Column(
        modifier = Modifier.width(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(a.color.copy(0.08f), CardBgColor)))
            .border(1.dp, a.color.copy(0.2f), RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(a.color.copy(0.15f)), contentAlignment = Alignment.Center) {
            Icon(a.icon, null, tint = a.color, modifier = Modifier.size(22.dp))
        }
        Text(a.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(a.desc, fontSize = 9.sp, color = Color.White.copy(0.35f), maxLines = 2, textAlign = TextAlign.Center)
    }
}

@Composable
private fun LockedAchievementRow(a: Achievement) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)).background(CardBgColor)
            .border(1.dp, CardStroke, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(0.03f)), contentAlignment = Alignment.Center) {
            Icon(a.icon, null, tint = Color.White.copy(0.15f), modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row {
                Text(a.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(0.5f))
                Spacer(Modifier.weight(1f))
                Text("${a.current}/${a.requirement}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(0.3f), fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(0.04f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(a.progress).clip(RoundedCornerShape(2.dp))
                    .background(Brush.horizontalGradient(listOf(a.color.copy(0.5f), a.color.copy(0.2f)))))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 6) Game Stats Section
// ═══════════════════════════════════════════════════════════════

@Composable
private fun GameStatsSection(data: PlayerProfileFullResponse) {
    val sorted = data.games.sortedByDescending { it.weightedScore }
    val maxW = sorted.firstOrNull()?.weightedScore ?: 1

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.BarChart, null, tint = PPurple, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Game Stats", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.weight(1f))
            Text("${data.stats.uniqueGamesPlayed}/${data.stats.totalGames}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(0.4f))
        }

        if (sorted.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.SportsEsports, null, tint = Color.White.copy(0.15f), modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(10.dp))
                Text("Play games to see stats here", fontSize = 13.sp, color = Color.White.copy(0.3f))
            }
        } else {
            sorted.forEach { game -> GameStatRow(game, maxW) }
        }
    }
}

@Composable
private fun GameStatRow(game: PlayerProfileGame, maxW: Int) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)).background(CardBgColor)
            .border(1.dp, CardStroke, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row {
            Text(game.gameName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.weight(1f))
            Box(Modifier.clip(RoundedCornerShape(50)).background(PPurple.copy(0.12f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text("×${"%.1f".format(game.coefficient)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PPurple, fontFamily = FontFamily.Monospace)
            }
        }
        // Bar
        val ratio = if (maxW > 0) (game.weightedScore.toFloat() / maxW).coerceIn(0f, 1f) else 0f
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(0.06f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(ratio.coerceAtLeast(0.02f)).clip(RoundedCornerShape(3.dp))
                .background(Brush.horizontalGradient(listOf(PCyan, PPurple))))
        }
        // Details
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("⭐ ${game.bestScore}", fontSize = 11.sp, color = Color.White.copy(0.45f))
            Text("⚖️ ${game.weightedScore}", fontSize = 11.sp, color = PCyan.copy(0.7f))
            Text("▶ ${game.gamesPlayed} played", fontSize = 11.sp, color = Color.White.copy(0.35f))
            if (game.avgScore > 0) {
                Text("avg ${game.avgScore.toInt()}", fontSize = 11.sp, color = Color.White.copy(0.3f))
            }
        }
    }
}
