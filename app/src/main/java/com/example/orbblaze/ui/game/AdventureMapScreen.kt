package com.example.orbblaze.ui.game

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
import kotlin.math.sin
import kotlin.random.Random

// --- Estructuras de Datos ---
private data class LevelNodeData(
    val id: Int,
    val position: Offset,
    val state: LevelState
)

private enum class LevelState { LOCKED, CURRENT, COMPLETED }

// --- Colores ---
private val OrbBlueBg = Color(0xFF81D4FA)
private val OrbYellow = Color(0xFFFFD600)
private val OrbWhite = Color.White
private val OrbTextBlue = Color(0xFF0D47A1)

private val BubbleColors = listOf(
    Color(0xFFFF5252).copy(alpha = 0.6f),
    Color(0xFF448AFF).copy(alpha = 0.6f),
    Color(0xFF69F0AE).copy(alpha = 0.6f),
    Color(0xFFFFD740).copy(alpha = 0.6f)
)

@Composable
fun AdventureMapScreen(
    onLevelSelect: (Int) -> Unit,
    onBackClick: () -> Unit,
    settingsManager: SettingsManager
) {
    val scrollState = rememberScrollState()
    val currentProgress by settingsManager.adventureProgressFlow.collectAsState(initial = 0)
    val density = LocalDensity.current

    val nodeSpacing = 160.dp
    val topPadding = 220.dp
    val bottomPadding = 150.dp // Margen inferior para que el nivel 1 no pegue abajo
    val curveAmplitude = 0.30f

    val levels = AdventureLevels.levels

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(OrbBlueBg)
    ) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val centerXPx = screenWidthPx / 2
        val screenHeightDp = maxHeight

        // Calculamos la altura total del mapa
        val totalHeightDp = topPadding + (nodeSpacing * (levels.size - 1)) + bottomPadding
        val totalHeightPx = with(density) { totalHeightDp.toPx() }
        val bottomPaddingPx = with(density) { bottomPadding.toPx() }
        val nodeSpacingPx = with(density) { nodeSpacing.toPx() }

        // --- CÁLCULO DE POSICIONES (INVERTIDO: DE ABAJO HACIA ARRIBA) ---
        val cachedNodes = remember(levels, currentProgress, screenWidthPx, totalHeightPx) {
            levels.mapIndexed { index, level ->
                // LÓGICA CLAVE: Empezamos desde abajo (TotalHeight) y restamos padding y espaciado
                // Index 0 (Nivel 1) estará abajo. Index N (Nivel 10) estará arriba.
                val yPosPx = totalHeightPx - bottomPaddingPx - (index * nodeSpacingPx)

                val xOffset = sin(index * 2.5) * (screenWidthPx * curveAmplitude)
                val xPosPx = centerXPx + xOffset.toFloat()

                val state = when {
                    level.id < currentProgress + 1 -> LevelState.COMPLETED
                    level.id == currentProgress + 1 -> LevelState.CURRENT
                    else -> LevelState.LOCKED
                }

                LevelNodeData(level.id, Offset(xPosPx, yPosPx), state)
            }
        }

        // Auto-scroll al nivel actual (Centrado en pantalla)
        LaunchedEffect(currentProgress, totalHeightPx) {
            val targetNode = cachedNodes.find { it.state == LevelState.CURRENT }
            if (targetNode != null) {
                // Calculamos para que el nodo quede en el centro de la pantalla
                val screenHalfHeight = with(density) { screenHeightDp.toPx() / 2 }
                val scrollPos = (targetNode.position.y - screenHalfHeight).toInt().coerceAtLeast(0)
                scrollState.animateScrollTo(scrollPos)
            } else {
                // Si es el inicio (Nivel 1), hacemos scroll al fondo automáticamente
                scrollState.scrollTo(scrollState.maxValue)
            }
        }

        // Fondo de Burbujas
        FloatingBubblesBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeightDp)
            ) {
                PathDrawer(nodes = cachedNodes)

                cachedNodes.forEach { node ->
                    val xDp = with(density) { node.position.x.toDp() }
                    val yDp = with(density) { node.position.y.toDp() }

                    LevelNodeItem(
                        data = node,
                        xOffset = xDp,
                        yOffset = yDp,
                        onClick = { onLevelSelect(node.id) }
                    )
                }
            }
        }

        HeaderOverlay(onBackClick)
    }
}

@Composable
private fun PathDrawer(nodes: List<LevelNodeData>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val path = Path()
        if (nodes.isNotEmpty()) {
            path.moveTo(nodes.first().position.x, nodes.first().position.y)
            for (i in 0 until nodes.size - 1) {
                val current = nodes[i]
                val next = nodes[i + 1]
                val controlY = (current.position.y + next.position.y) / 2
                val control1 = Offset(current.position.x, controlY)
                val control2 = Offset(next.position.x, controlY)
                path.cubicTo(control1.x, control1.y, control2.x, control2.y, next.position.x, next.position.y)
            }
        }

        // Camino base (semi-transparente)
        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.5f),
            style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Línea central punteada
        drawPath(
            path = path,
            color = Color.White,
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(40f, 40f), 0f))
        )
    }
}

@Composable
private fun LevelNodeItem(
    data: LevelNodeData,
    xOffset: Dp,
    yOffset: Dp,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by if (data.state == LevelState.CURRENT) {
        infiniteTransition.animateFloat(
            initialValue = 1f, targetValue = 1.15f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "pulse"
        )
    } else remember { mutableStateOf(1f) }

    val nodeSize = if (data.state == LevelState.CURRENT) 80.dp else 64.dp

    val (bgColor, textColor) = when (data.state) {
        LevelState.COMPLETED -> OrbWhite to OrbTextBlue
        LevelState.CURRENT -> OrbYellow to OrbTextBlue
        LevelState.LOCKED -> Color.White.copy(alpha = 0.3f) to Color.White.copy(0.6f)
    }

    Box(
        modifier = Modifier
            .offset(x = xOffset - (nodeSize / 2), y = yOffset - (nodeSize / 2))
            .size(nodeSize)
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
            }
    ) {
        if (data.state != LevelState.LOCKED) {
            Box(modifier = Modifier
                .matchParentSize()
                .offset(y = 4.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.15f)))
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(bgColor)
                .clickable(enabled = data.state != LevelState.LOCKED) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (data.state == LevelState.LOCKED) {
                Icon(Icons.Default.Lock, null, tint = textColor)
            } else {
                Text(
                    text = "${data.id}",
                    color = textColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Box(modifier = Modifier
                .align(Alignment.TopStart).offset(x = 10.dp, y = 10.dp)
                .size(nodeSize / 4).clip(CircleShape)
                .background(Color.White.copy(alpha = 0.4f)))
        }

        if (data.state == LevelState.CURRENT) {
            Icon(Icons.Default.Star, null, tint = Color.White,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp).size(24.dp))
        }
    }
}

@Composable
private fun FloatingBubblesBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bubbles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Restart),
        label = "time"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val random = Random(42)
        repeat(15) {
            val baseX = random.nextFloat() * size.width
            val baseY = random.nextFloat() * size.height
            val radius = random.nextFloat() * 60.dp.toPx() + 20.dp.toPx()
            val color = BubbleColors[random.nextInt(BubbleColors.size)]

            // Hacemos que floten hacia arriba (restando Y) para coincidir con el tema de "subir de nivel"
            val movement = (time * 200.dp.toPx()) // Velocidad
            val animatedY = (baseY - movement) % size.height
            // Ajuste para que cuando salga por arriba entre por abajo
            val finalY = if (animatedY < 0) animatedY + size.height else animatedY

            drawCircle(color = color, radius = radius, center = Offset(baseX, finalY))
            drawCircle(color = Color.White.copy(alpha = 0.3f), radius = radius * 0.3f,
                center = Offset(baseX - radius*0.3f, finalY - radius*0.3f))
        }
    }
}

@Composable
private fun HeaderOverlay(onBackClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .statusBarsPadding()
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .shadow(4.dp, CircleShape)
                .background(Color.White, CircleShape)
                .size(48.dp)
                .align(Alignment.CenterStart)
        ) {
            Icon(Icons.Default.ArrowBack, "Volver", tint = OrbTextBlue)
        }

        Text(
            text = "AVENTURA",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.3f),
                    offset = Offset(4f, 4f),
                    blurRadius = 8f
                )
            ),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}