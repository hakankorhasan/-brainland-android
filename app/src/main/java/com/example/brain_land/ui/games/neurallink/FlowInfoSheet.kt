package com.example.brain_land.ui.games.neurallink

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowInfoSheet(onDismiss: () -> Unit) {
    val bgColor = Color(0xFF10131B)
    val accentCyan = Color(0xFF00E5FF)
    val accentPurple = Color(0xFFB24DFF)

    val flowColors = listOf(
        Color(0xFF00E5FF), // cyan
        Color(0xFFFF3399), // magenta/pink
        Color(0xFF33FF66)  // neon green
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = bgColor,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(0.2f)) },
        modifier = Modifier.fillMaxHeight(0.95f) // Large detent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            // Close Button over header
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(0.4f))
                }
            }

            // Hero Icon
            Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            Brush.radialGradient(listOf(accentCyan.copy(0.2f), Color.Transparent)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(12.dp, CircleShape, spotColor = accentCyan.copy(0.3f))
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF0A1628), Color(0xFF050A14))),
                                CircleShape
                            )
                            .border(
                                1.5.dp,
                                Brush.linearGradient(listOf(accentCyan.copy(0.5f), accentPurple.copy(0.3f))),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Timeline,
                            contentDescription = null,
                            tint = accentCyan,
                            modifier = Modifier.align(Alignment.Center).size(28.dp).shadow(6.dp, CircleShape, spotColor = accentCyan)
                        )
                    }
                }
            }

            Text(
                text = "How to Play",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp).align(Alignment.CenterHorizontally)
            )

            // Description card
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Info, null, tint = accentCyan.copy(0.8f), modifier = Modifier.size(18.dp))
                    Text("What is Neural Link?", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(0.9f))
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Connect matching dots with a continuous line. Different colors cannot cross each other. Find the right path to link all synapses.",
                    fontSize = 13.sp, color = Color.White.copy(0.6f), lineHeight = 18.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // Gameplay Details
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.TouchApp, null, tint = accentCyan.copy(0.8f), modifier = Modifier.size(18.dp))
                    Text("Gameplay", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(0.9f))
                }
                Spacer(Modifier.height(12.dp))
                StepItem(flowColors[0], Icons.Default.Circle, "Select Dot", "Tap and hold on any colored dot.")
                Spacer(Modifier.height(8.dp))
                StepItem(flowColors[1], Icons.Default.Draw, "Drag", "Drag your finger to its matching pair without lifting.")
                Spacer(Modifier.height(8.dp))
                StepItem(flowColors[2], Icons.Default.GridOn, "Fill the Grid", "You must cover every empty cell to win.")
                
                Spacer(Modifier.height(16.dp))
                // Blocker cell info
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color.Red.copy(0.1f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Red.copy(0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.Red.copy(0.6f), modifier = Modifier.size(18.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Blocker Cell", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Red.copy(0.7f))
                        Text("You cannot pass through cells marked with an X.", fontSize = 11.sp, color = Color.White.copy(0.45f))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Solved Example Card
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckBox, null, tint = Color.Green.copy(0.9f), modifier = Modifier.size(18.dp))
                    Text("Solved Example", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(0.9f))
                }
                Spacer(Modifier.height(8.dp))
                Text("Notice how every cell is filled and colors never cross.", fontSize = 12.sp, color = Color.White.copy(0.45f))
                Spacer(Modifier.height(14.dp))
                
                // Mini 5x5 Grid
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(10.dp, RoundedCornerShape(10.dp), spotColor = accentCyan.copy(0.08f))
                        .background(Color(0xFF080A14), RoundedCornerShape(10.dp))
                        .border(0.5.dp, accentCyan.copy(0.1f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ExampleSolvedGrid(flowColors = flowColors)
                }

                Spacer(Modifier.height(14.dp))
                // Legends
                LegendRow(flowColors[0], "Path 1", "Short direct connection")
                Spacer(Modifier.height(6.dp))
                LegendRow(flowColors[1], "Path 2", "Wraps around the outside")
                Spacer(Modifier.height(6.dp))
                LegendRow(flowColors[2], "Path 3", "Fills the bottom gaps")
                Spacer(Modifier.height(6.dp))
                LegendRow(Color.Red.copy(0.5f), "Obstacle", "Cannot be crossed", isDead = true)
            }

            Spacer(Modifier.height(16.dp))

            // Tips
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.TipsAndUpdates, null, tint = Color(0xFFE8845C), modifier = Modifier.size(18.dp))
                    Text("Strategies", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(0.9f))
                }
                Spacer(Modifier.height(12.dp))
                TipRow("1", "Start by connecting dots that are right next to each other or along the wall.")
                Spacer(Modifier.height(8.dp))
                TipRow("2", "Run paths along the edges to prevent blocking other colors in the middle.")
                Spacer(Modifier.height(8.dp))
                TipRow("3", "If a cell is trapped and impossible to enter and exit, your current paths are wrong.")
                Spacer(Modifier.height(8.dp))
                TipRow("4", "Use the undo or reset buttons if you feel stuck.")
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(0.03f), RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun StepItem(color: Color, icon: ImageVector, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(color.copy(0.12f), RoundedCornerShape(8.dp))
                .border(1.dp, color.copy(0.25f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color.copy(0.9f))
            Text(desc, fontSize = 11.sp, color = Color.White.copy(0.5f), lineHeight = 14.sp)
        }
    }
}

@Composable
private fun TipRow(number: String, desc: String) {
    val accentCyan = Color(0xFF00E5FF)
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(accentCyan.copy(0.12f), CircleShape)
                .border(0.5.dp, accentCyan.copy(0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(number, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentCyan)
        }
        Text(desc, fontSize = 12.sp, color = Color.White.copy(0.55f), lineHeight = 16.sp)
    }
}

@Composable
private fun LegendRow(color: Color, label: String, desc: String, isDead: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isDead) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color(0xFF0F0508), RoundedCornerShape(3.dp))
                    .border(0.5.dp, Color.Red.copy(0.2f), RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, null, tint = Color.Red.copy(0.4f), modifier = Modifier.size(10.dp))
            }
        } else {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .shadow(3.dp, CircleShape, spotColor = color.copy(0.6f))
                    .background(color, CircleShape)
            )
        }
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        Text("—", fontSize = 10.sp, color = Color.White.copy(0.2f))
        Text(desc, fontSize = 10.sp, color = Color.White.copy(0.4f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hardcoded 5x5 Grid Definition
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExampleSolvedGrid(flowColors: List<Color>) {
    val gridSize = 5
    // -1 = dead, 0/1/2 = flow index
    val grid = listOf(
        listOf(0, 0, -1, 1, 1),
        listOf(0, 0, 1, 1, 1),
        listOf(0, 0, 1, 1, 1),
        listOf(2, 2, -1, 1, 1),
        listOf(2, 2, 2, 2, 2)
    )
    val endpoints = listOf(
        listOf(2 to 0, 2 to 1),
        listOf(1 to 4, 2 to 4),
        listOf(4 to 0, 4 to 4)
    )
    val paths = listOf(
        listOf(2 to 0, 1 to 0, 0 to 0, 0 to 1, 1 to 1, 2 to 1),
        listOf(1 to 4, 0 to 4, 0 to 3, 1 to 3, 1 to 2, 2 to 2, 2 to 3, 3 to 3, 3 to 4, 2 to 4),
        listOf(4 to 0, 3 to 0, 3 to 1, 4 to 1, 4 to 2, 4 to 3, 4 to 4)
    )

    fun isConnected(path: List<Pair<Int, Int>>, r1: Int, c1: Int, r2: Int, c2: Int): Boolean {
        for (i in 0 until path.size - 1) {
            val a = path[i]; val b = path[i+1]
            if ((a.first == r1 && a.second == c1 && b.first == r2 && b.second == c2) ||
                (a.first == r2 && a.second == c2 && b.first == r1 && b.second == c1)) {
                return true
            }
        }
        return false
    }

    val spacing = 2.dp
    val cellSize = 38.dp

    Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
        for (r in 0 until gridSize) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                for (c in 0 until gridSize) {
                    val fi = grid[r][c]
                    val isDead = fi == -1
                    val isEp = fi >= 0 && endpoints.getOrNull(fi)?.contains(r to c) == true
                    
                    val up = r > 0 && fi >= 0 && isConnected(paths[fi], r, c, r-1, c)
                    val down = r < gridSize-1 && fi >= 0 && isConnected(paths[fi], r, c, r+1, c)
                    val left = c > 0 && fi >= 0 && isConnected(paths[fi], r, c, r, c-1)
                    val right = c < gridSize-1 && fi >= 0 && isConnected(paths[fi], r, c, r, c+1)

                    ExampleCell(
                        fi = fi, isDead = isDead, isEndpoint = isEp,
                        up = up, down = down, left = left, right = right,
                        flowColors = flowColors, size = cellSize
                    )
                }
            }
        }
    }
}

@Composable
private fun ExampleCell(
    fi: Int, isDead: Boolean, isEndpoint: Boolean,
    up: Boolean, down: Boolean, left: Boolean, right: Boolean,
    flowColors: List<Color>, size: Dp
) {
    val color = if (fi >= 0) flowColors[fi % flowColors.size] else Color.Transparent

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(3.dp))
            .background(
                when {
                    isDead -> Color(0xFF0F0508)
                    fi >= 0 && !isEndpoint -> color.copy(alpha = 0.10f)
                    else -> Color(0xFF080A12)
                }
            )
            .border(
                0.5f.dp,
                if (!isDead && fi < 0) Color.White.copy(0.04f) else Color.Transparent,
                RoundedCornerShape(3.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isDead) {
            Icon(Icons.Default.Close, null, tint = Color.Red.copy(0.2f), modifier = Modifier.fillMaxSize(0.6f))
        } else if (fi >= 0) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val thick = size.toPx() * 0.3f
                val half = size.toPx() / 2f
                val center = Offset(half, half)

                if (!isEndpoint) {
                    drawCircle(color.copy(0.8f), thick * 0.6f, center)
                }
                
                val pipeBrush = Brush.linearGradient(listOf(color.copy(0.7f), color.copy(0.9f), color.copy(0.7f)))
                
                if (up) drawRect(pipeBrush, topLeft = Offset(center.x - thick/2f, 0f), size = androidx.compose.ui.geometry.Size(thick, half + 1f))
                if (down) drawRect(pipeBrush, topLeft = Offset(center.x - thick/2f, half), size = androidx.compose.ui.geometry.Size(thick, half + 1f))
                if (left) drawRect(pipeBrush, topLeft = Offset(0f, center.y - thick/2f), size = androidx.compose.ui.geometry.Size(half + 1f, thick))
                if (right) drawRect(pipeBrush, topLeft = Offset(half, center.y - thick/2f), size = androidx.compose.ui.geometry.Size(half + 1f, thick))

                if (isEndpoint) {
                    drawCircle(Brush.radialGradient(listOf(color.copy(0.5f), color.copy(0.15f), Color.Transparent)), half * 0.9f, center)
                    drawCircle(color.copy(0.5f), half * 0.6f, center, style = androidx.compose.ui.graphics.drawscope.Stroke(4f))
                    drawCircle(Brush.radialGradient(listOf(Color.White.copy(0.9f), color, color.copy(0.8f))), half * 0.45f, center)
                    drawCircle(Color.White.copy(0.5f), half * 0.45f, center, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                }
            }
        }
    }
}
