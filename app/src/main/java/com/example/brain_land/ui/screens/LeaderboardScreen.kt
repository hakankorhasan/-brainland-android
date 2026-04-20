package com.example.brain_land.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.brain_land.data.LeaderboardPlayer
import com.example.brain_land.ui.theme.AccentCyan
import com.example.brain_land.ui.theme.AccentPurple
import com.example.brain_land.ui.theme.BgCard
import com.example.brain_land.viewmodel.LeaderboardViewModel
import com.example.brain_land.viewmodel.SmartItem

// Cyan & purple accent colours matching iOS
private val Cyan   = Color(0xFF00E5FF)
private val Purple = Color(0xFFA78BFA)
private val BgDark = Color(0xFF10131B)

@Composable
fun LeaderboardScreen(vm: LeaderboardViewModel = viewModel()) {
    val allPlayers by vm.allPlayers.collectAsState()
    val response   by vm.response.collectAsState()
    val isLoading  by vm.isLoading.collectAsState()
    val myDeviceId by vm.myDeviceId.collectAsState()

    val myRank  = response?.myRank  ?: 0
    val myScore = response?.myScore ?: 0
    val total   = allPlayers.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
    ) {
        // ── Header ──
        LeaderboardHeader(total = total, onRefresh = { vm.fetchLeaderboard() })

        when {
            isLoading && response == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Cyan, modifier = Modifier.size(36.dp))
                }
            }
            allPlayers.isEmpty() -> EmptyState()
            else -> {
                val listItems = vm.buildSmartList(allPlayers)

                // Determine scroll-to anchor (iOS auto-scroll to self)
                val myScrollId = remember(allPlayers, myDeviceId) {
                    if (myRank < 4) null  // in podium
                    else listItems.filterIsInstance<SmartItem.Player>()
                        .firstOrNull { vm.isMe(it.player) }?.id
                }

                val listState = rememberLazyListState()

                // Auto-scroll once to user's position (mirrors iOS asyncAfter 0.4s)
                LaunchedEffect(myScrollId) {
                    if (myScrollId != null) {
                        kotlinx.coroutines.delay(400)
                        val idx = listItems.indexOfFirst { it.id == myScrollId }
                        if (idx >= 0) {
                            // +3 offset: podium (1 item) + subheader (1) = 2, approx
                            listState.animateScrollToItem(idx + 3, scrollOffset = -200)
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // ── Podium (top 3) ──
                    if (allPlayers.size >= 2) {
                        item(key = "podium") {
                            PodiumSection(
                                players = allPlayers,
                                isMe = { vm.isMe(it) },
                                modifier = Modifier.padding(bottom = 24.dp)
                            )
                        }
                    }

                    // ── Sub-header "All Players | Your rank" ──
                    item(key = "subheader") {
                        ListSubheader(myRank = myRank)
                    }

                    // ── Smart list ──
                    items(items = listItems, key = { it.id }) { item ->
                        when (item) {
                            is SmartItem.Player -> {
                                val player = item.player
                                val isSelf = vm.isMe(player)
                                LeaderboardRowCard(
                                    player    = player,
                                    isCurrentUser = isSelf,
                                    accentColor = when {
                                        isSelf          -> Cyan
                                        player.rank % 7 == 1 -> Color(0xFFFFD700)
                                        player.rank % 7 == 4 -> Purple
                                        else            -> null
                                    },
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                                )
                            }
                            is SmartItem.Ellipsis -> {
                                EllipsisDivider(
                                    skipped = item.skippedCount,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // ── Footer ──
                    item(key = "footer") {
                        LeaderboardFooter(total = total, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
// Header — "Leaderboard" title + player count badge
// ──────────────────────────────────────────────────────────

@Composable
private fun LeaderboardHeader(total: Int, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = "Leaderboard",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.weight(1f))
        if (total > 0) {
            // Player count badge
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Cyan.copy(alpha = 0.1f))
                    .border(1.dp, Cyan.copy(alpha = 0.25f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Group,
                    contentDescription = null,
                    tint = Cyan.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
                Text("$total", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Cyan)
                Text("players", fontSize = 12.sp, color = Cyan.copy(alpha = 0.6f))
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
// Podium — top 3 players (mirrors iOS topPlayersPodium)
// Order: 2nd | 1st | 3rd  (iOS HStack alignment .bottom)
// ──────────────────────────────────────────────────────────

@Composable
private fun PodiumSection(
    players: List<LeaderboardPlayer>,
    isMe: (LeaderboardPlayer) -> Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Top Players",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // 2nd place
            if (players.size > 1) {
                PodiumCard(
                    player = players[1], rank = 2,
                    cardColor = Color(0xFFA78BFA),
                    cardHeight = 110.dp,
                    isSelf = isMe(players[1]),
                    modifier = Modifier.weight(1f)
                )
            }
            // 1st place (tallest)
            if (players.isNotEmpty()) {
                PodiumCard(
                    player = players[0], rank = 1,
                    cardColor = Color(0xFFFFD700),
                    cardHeight = 140.dp,
                    isSelf = isMe(players[0]),
                    modifier = Modifier.weight(1f)
                )
            }
            // 3rd place
            if (players.size > 2) {
                PodiumCard(
                    player = players[2], rank = 3,
                    cardColor = Color(0xFFFF8A65),
                    cardHeight = 100.dp,
                    isSelf = isMe(players[2]),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PodiumCard(
    player: LeaderboardPlayer,
    rank: Int,
    cardColor: Color,
    cardHeight: androidx.compose.ui.unit.Dp,
    isSelf: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar (sticks out above card)
        Box(modifier = Modifier.offset(y = 16.dp).zIndex(1f)) {
            AvatarCircle(
                player = player,
                size = if (rank == 1) 48.dp else 40.dp,
                isSelf = isSelf
            )
            if (isSelf) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .clip(CircleShape)
                        .background(Cyan)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("YOU", fontSize = 7.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }
        }

        // Card body
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        if (isSelf) listOf(Cyan.copy(alpha = 0.9f), cardColor.copy(alpha = 0.7f))
                        else listOf(cardColor, cardColor.copy(alpha = 0.6f))
                    )
                )
                .then(
                    if (isSelf) Modifier.border(2.dp, Cyan, RoundedCornerShape(18.dp))
                    else Modifier
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Spacer for avatar overlap
            Spacer(Modifier.height(20.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = player.displayName,
                    fontSize = if (rank == 1) 14.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${player.displayScore}",
                    fontSize = if (rank == 1) 16.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            // Rank circle at the bottom
            Box(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun AvatarCircle(
    player: LeaderboardPlayer,
    size: androidx.compose.ui.unit.Dp,
    isSelf: Boolean
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .border(3.dp, BgDark, CircleShape)
    ) {
        if (!player.avatarUrl.isNullOrEmpty()) {
            AsyncImage(
                model = player.avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            if (isSelf)
                                listOf(Cyan.copy(alpha = 0.6f), Purple.copy(alpha = 0.6f))
                            else
                                listOf(Color(0xFF6FE4CF).copy(alpha = 0.6f), Color(0xFFB88AE8).copy(alpha = 0.6f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = player.displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    fontSize = (size.value * 0.4f).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        if (isSelf) {
            Box(modifier = Modifier.fillMaxSize().clip(CircleShape).border(2.dp, Cyan, CircleShape))
        }
    }
}

// ──────────────────────────────────────────────────────────
// Sub-header row ("All Players" | "Your rank: #N")
// ──────────────────────────────────────────────────────────

@Composable
private fun ListSubheader(myRank: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "All Players",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.weight(1f))
        if (myRank > 0) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Cyan.copy(alpha = 0.1f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Your rank: #$myRank",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Cyan.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
// Leaderboard Row Card (mirrors iOS LeaderboardRowView)
// ──────────────────────────────────────────────────────────

@Composable
fun LeaderboardRowCard(
    player: LeaderboardPlayer,
    isCurrentUser: Boolean,
    accentColor: Color?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isCurrentUser)
                    Brush.horizontalGradient(listOf(Cyan.copy(alpha = 0.12f), Purple.copy(alpha = 0.07f)))
                else
                    Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.04f), Color.White.copy(alpha = 0.04f)))
            )
            .border(
                width = if (isCurrentUser) 1.5.dp else 1.dp,
                color = when {
                    isCurrentUser -> Cyan.copy(alpha = 0.45f)
                    accentColor != null -> accentColor.copy(alpha = 0.4f)
                    else -> Color.White.copy(alpha = 0.06f)
                },
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Rank label
        Text(
            text = "#${player.rank}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isCurrentUser) Cyan else Color.White.copy(alpha = 0.4f),
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center
        )

        // Avatar
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .border(2.dp, if (isCurrentUser) Cyan else Color.Transparent, CircleShape)
        ) {
            if (!player.avatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = player.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                if (isCurrentUser)
                                    listOf(Cyan.copy(alpha = 0.7f), Purple.copy(alpha = 0.7f))
                                else
                                    listOf(Color(0xFF6FE4CF).copy(alpha = 0.5f), Color(0xFFB88AE8).copy(alpha = 0.5f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = player.displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Name + tier column
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = player.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCurrentUser) Cyan else Color.White,
                    maxLines = 1
                )
                if (isCurrentUser) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Cyan)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("YOU", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }
            Text(
                text = player.displayTier,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrentUser) Cyan.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.35f)
            )
        }

        // Score
        Text(
            text = "${player.displayScore}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (isCurrentUser) Cyan else Color.White.copy(alpha = 0.85f)
        )
    }
}

// ──────────────────────────────────────────────────────────
// Ellipsis divider (mirrors iOS ellipsisDivider)
// ──────────────────────────────────────────────────────────

@Composable
private fun EllipsisDivider(skipped: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color.White.copy(alpha = 0.07f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    )
                }
            }
            Text(
                text = "$skipped players",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.25f)
            )
        }
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color.White.copy(alpha = 0.07f)
        )
    }
}

// ──────────────────────────────────────────────────────────
// Footer  (mirrors iOS footerSection)
// ──────────────────────────────────────────────────────────

@Composable
private fun LeaderboardFooter(total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.06f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                Icons.Default.Language,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "$total players in ranking",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.2f)
            )
        }
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.06f))
    }
}

// ──────────────────────────────────────────────────────────
// Empty state
// ──────────────────────────────────────────────────────────

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        Brush.radialGradient(listOf(Cyan.copy(alpha = 0.1f), Color.Transparent)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("📊", fontSize = 40.sp)
            }
            Text(
                text = "No players yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = "Play games to climb\nthe leaderboard!",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.3f),
                textAlign = TextAlign.Center
            )
        }
    }
}
