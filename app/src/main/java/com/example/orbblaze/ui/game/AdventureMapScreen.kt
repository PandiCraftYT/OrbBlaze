package com.example.orbblaze.ui.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.data.SettingsManager
import com.example.orbblaze.domain.model.AdventureLevels
import kotlinx.coroutines.flow.first
import kotlin.math.sin
import kotlin.random.Random

private data class LevelNodeData(
    val id: Int,
    val position: Offset = Offset.Zero,
    val state: LevelState,
    val stars: Int = 0
)

private enum class LevelState { LOCKED, CURRENT, COMPLETED }

private val OrbBlueBg = Color(0xFF81D4FA)
private val OrbYellow = Color(0xFFFFD600)
private val OrbWhite = Color.White
private val OrbTextBlue = Color(0xFF0D47A1)

@Composable
fun AdventureMapScreen(
    onLevelSelect: (Int) -> Unit,
    onBackClick: () -> Unit,
    settingsManager: SettingsManager
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val levels = AdventureLevels.levels

    val currentProgress by settingsManager.adventureProgressFlow.collectAsState(initial = 0)
    val nodesState = remember { mutableStateListOf<LevelNodeData>() }
    var isLoading by remember { mutableStateOf(true) }

    // Carga de datos y estados de niveles
    LaunchedEffect(currentProgress) {
        val starsMap = settingsManager.allStarsFlow.first()
        val list = levels.map { level ->
            val state = when {
                level.id <= currentProgress -> LevelState.COMPLETED
                level.id == currentProgress + 1 -> LevelState.CURRENT
                else -> LevelState.LOCKED
            }
            LevelNodeData(id = level.id, stars = starsMap[level.id] ?: 0, state = state)
        }
        nodesState.clear()
        nodesState.addAll(list)
        isLoading = false
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(Modifier.fillMaxSize().background(OrbBlueBg), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            val screenWidthPx = with(density) { maxWidth.toPx() }
            val screenHeightPx = with(density) { maxHeight.toPx() }
            val centerXPx = screenWidthPx / 2
            
            val nodeSpacing = 160.dp
            val topPadding = 220.dp
            val bottomPadding = 150.dp
            val totalHeightDp = topPadding + (nodeSpacing * (levels.size - 1)) + bottomPadding
            val totalHeightPx = with(density) { totalHeightDp.toPx() }
            val bottomPaddingPx = with(density) { bottomPadding.toPx() }
            val nodeSpacingPx = with(density) { nodeSpacing.toPx() }

            val finalNodes = remember(screenWidthPx, totalHeightPx, nodesState.size) {
                nodesState.mapIndexed { index, node ->
                    val yPosPx = totalHeightPx - bottomPaddingPx - (index * nodeSpacingPx)
                    val xOffset = sin(index * 2.5) * (screenWidthPx * 0.30f)
                    node.copy(position = Offset(centerXPx + xOffset.toFloat(), yPosPx))
                }
            }

            // ✅ CORRECCIÓN SCROLL: Centrar el nivel actual (CURRENT)
            LaunchedEffect(finalNodes) {
                val currentNode = finalNodes.find { it.state == LevelState.CURRENT }
                if (currentNode != null) {
                    val targetY = currentNode.position.y - (screenHeightPx / 2)
                    scrollState.scrollTo(targetY.toInt().coerceIn(0, scrollState.maxValue))
                } else if (currentProgress >= levels.size) {
                    // Si completó todo, mostrar el final (arriba)
                    scrollState.scrollTo(0)
                } else {
                    // Inicio
                    scrollState.scrollTo(scrollState.maxValue)
                }
            }

            val currentViewY = totalHeightPx - scrollState.value - (screenHeightPx / 2)
            val estimatedIndex = ((totalHeightPx - bottomPaddingPx - currentViewY) / nodeSpacingPx)
                .coerceIn(0f, (levels.size - 1).toFloat()).toInt()
            val currentLevelInView = levels[estimatedIndex].id

            val (targetBgTop, targetBgBottom) = when {
                currentLevelInView <= 30 -> Color(0xFF81D4FA) to Color(0xFF4FC3F7) 
                currentLevelInView <= 50 -> Color(0xFF1B5E20) to Color(0xFF4DB6AC) 
                currentLevelInView <= 70 -> Color(0xFF3E2723) to Color(0xFFBF360C) 
                currentLevelInView <= 90 -> Color(0xFF0277BD) to Color(0xFFE1F5FE) 
                else -> Color(0xFF0D47A1) to Color(0xFF000000) 
            }

            val animatedBgTop by animateColorAsState(targetValue = targetBgTop, animationSpec = tween(1000), label = "bgTop")
            val animatedBgBottom by animateColorAsState(targetValue = targetBgBottom, animationSpec = tween(1000), label = "bgBottom")

            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(animatedBgTop, animatedBgBottom)))) {
                FloatingBubblesBackground(animatedBgTop)
                Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                    Box(modifier = Modifier.fillMaxWidth().height(totalHeightDp)) {
                        PathDrawer(nodes = finalNodes)
                        finalNodes.forEach { node ->
                            LevelNodeItem(
                                data = node,
                                xOffset = with(density) { node.position.x.toDp() },
                                yOffset = with(density) { node.position.y.toDp() },
                                onClick = { onLevelSelect(node.id) }
                            )
                        }
                    }
                }
            }
        }
        HeaderOverlay(onBackClick)
    }
}

@Composable
private fun PathDrawer(nodes: List<LevelNodeData>) {
    val infiniteTransition = rememberInfiniteTransition(label = "cascade")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 400f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "phase"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val path = Path()
        if (nodes.isNotEmpty()) {
            path.moveTo(nodes.first().position.x, nodes.first().position.y)
            for (i in 0 until nodes.size - 1) {
                val current = nodes[i]; val next = nodes[i + 1]
                val controlY = (current.position.y + next.position.y) / 2
                path.cubicTo(current.position.x, controlY, next.position.x, controlY, next.position.x, next.position.y)
            }
        }
        drawPath(path, Color.White.copy(alpha = 0.2f), style = Stroke(26.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, Color.White.copy(alpha = 0.4f), style = Stroke(18.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, Color.White.copy(alpha = 0.6f), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(60f, 120f), phase)))
    }
}

@Composable
private fun LevelNodeItem(data: LevelNodeData, xOffset: Dp, yOffset: Dp, onClick: () -> Unit) {
    val nodeSize = 64.dp
    val bgColor = when(data.state) {
        LevelState.LOCKED -> Color.Gray.copy(alpha = 0.5f)
        LevelState.CURRENT -> OrbYellow
        LevelState.COMPLETED -> OrbWhite
    }
    val textColor = if(data.state == LevelState.LOCKED) Color.DarkGray else OrbTextBlue

    Box(modifier = Modifier.offset(x = xOffset - (nodeSize / 2), y = yOffset - (nodeSize / 2)).size(nodeSize + 30.dp), contentAlignment = Alignment.TopCenter) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(nodeSize)) {
                Box(modifier = Modifier.matchParentSize().offset(y = 4.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.15f)))
                Box(modifier = Modifier.matchParentSize().clip(CircleShape).background(bgColor).clickable(enabled = data.state != LevelState.LOCKED) { onClick() }, contentAlignment = Alignment.Center) {
                    if (data.state == LevelState.LOCKED) {
                        Icon(Icons.Default.Lock, null, tint = textColor, modifier = Modifier.size(24.dp))
                    } else {
                        Text("${data.id}", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            if (data.id > 30 && data.state != LevelState.LOCKED) {
                Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.Center) {
                    repeat(3) { i ->
                        Icon(
                            imageVector = Icons.Default.Star, contentDescription = null,
                            tint = if (i < data.stars) OrbYellow else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingBubblesBackground(baseColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "bubbles")
    val time by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(10000, easing = LinearEasing)), label = "time")
    Canvas(modifier = Modifier.fillMaxSize()) {
        val random = Random(42)
        repeat(12) {
            val baseX = random.nextFloat() * size.width; val baseY = random.nextFloat() * size.height
            val radius = random.nextFloat() * 80.dp.toPx() + 30.dp.toPx()
            val color = baseColor.copy(alpha = 0.15f)
            val movement = (time * 150.dp.toPx()); val finalY = (baseY - movement + size.height) % size.height
            drawCircle(color, radius, Offset(baseX, finalY))
            drawCircle(Color.White.copy(alpha = 0.1f), radius * 0.8f, Offset(baseX, finalY), style = Stroke(2.dp.toPx()))
        }
    }
}

@Composable
private fun HeaderOverlay(onBackClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding()) {
        IconButton(onClick = onBackClick, modifier = Modifier.shadow(4.dp, CircleShape).background(Color.White, CircleShape).size(48.dp).align(Alignment.CenterStart)) { Icon(Icons.Default.ArrowBack, null, tint = OrbTextBlue) }
        Text("AVENTURA", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Center))
    }
}
