package com.example.orbblaze.ui.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.data.SettingsManager
import com.example.orbblaze.domain.model.AdventureLevels
import kotlin.math.sin

@Composable
fun AdventureMapScreen(
    onLevelSelect: (Int) -> Unit,
    onBackClick: () -> Unit,
    settingsManager: SettingsManager
) {
    val scrollState = rememberScrollState(Int.MAX_VALUE)
    val density = LocalDensity.current
    
    // Observar progreso real del usuario
    val currentProgress by settingsManager.adventureProgressFlow.collectAsState(initial = 0)
    
    val mapHeight = 2500.dp
    val nodes = AdventureLevels.levels
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF2E7D32), Color(0xFF1B5E20))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Box(modifier = Modifier.height(mapHeight).fillMaxWidth()) {
                
                // CAMINO
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path()
                    val startY = size.height
                    val amplitude = size.width * 0.25f
                    val frequency = 0.003f
                    
                    path.moveTo(size.width / 2f, startY)
                    for (y in startY.toInt() downTo 0) {
                        val x = (size.width / 2f) + sin(y * frequency) * amplitude
                        path.lineTo(x, y.toFloat())
                    }
                    
                    drawPath(
                        path = path,
                        color = Color(0xFF5D4037),
                        style = Stroke(width = 90.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = path,
                        color = Color(0xFFD7CCC8),
                        style = Stroke(width = 70.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                AdventureDecorations()

                // NODOS DINÁMICOS
                nodes.forEachIndexed { index, level ->
                    val nodeY = mapHeight - 150.dp - (index * 200).dp
                    
                    // Cálculo de X sincronizado con el camino
                    val xOffset = with(density) {
                        val yPx = nodeY.toPx()
                        val screenWidthPx = 1080f 
                        val amplitude = screenWidthPx * 0.15f
                        sin(yPx * 0.003f) * amplitude
                    }

                    // Lógica de desbloqueo: Nivel 1 siempre abierto, otros si completaste el anterior
                    val isUnlocked = level.id == 1 || level.id <= (currentProgress + 1)
                    val isCompleted = level.id <= currentProgress

                    LevelNodeCustom(
                        id = level.id,
                        isUnlocked = isUnlocked,
                        isCompleted = isCompleted,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(x = xOffset.dp, y = nodeY),
                        onClick = { if (isUnlocked) onLevelSelect(level.id) }
                    )
                }
            }
        }

        // UI FIJA
        IconButton(
            onClick = onBackClick, 
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp).statusBarsPadding()
                .background(Color.Black.copy(0.3f), CircleShape)
        ) {
            Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
        }
        
        Surface(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp).statusBarsPadding(),
            color = Color.Black.copy(0.5f),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                "NIVEL ACTUAL: ${currentProgress + 1}",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                color = Color.White, fontWeight = FontWeight.Black, letterSpacing = 2.sp
            )
        }
    }
}

@Composable
fun AdventureDecorations() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val bambooColor = Color(0xFF689F38)
        val bambooDark = Color(0xFF33691E)
        for (i in 0..15) {
            val y = i * (size.height / 15)
            drawBamboo(Offset((i % 3) * 20f + 20f, y), 25f, bambooColor, bambooDark)
        }
        for (i in 0..15) {
            val y = i * (size.height / 15) + 200f
            drawBamboo(Offset(size.width - 40f - (i % 2) * 30f, y), 30f, bambooColor, bambooDark)
        }
        for (i in 0..5) {
            drawCircle(color = Color.White.copy(0.4f), radius = 150f, center = Offset(i * 300f, 100f))
        }
    }
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBamboo(pos: Offset, width: Float, color: Color, dark: Color) {
    drawRect(color, pos.copy(x = pos.x - width/2), size = androidx.compose.ui.geometry.Size(width, 400f))
    for (j in 0..4) {
        drawLine(dark, pos.copy(y = pos.y + j * 80f), pos.copy(x = pos.x + width, y = pos.y + j * 80f), strokeWidth = 4f)
    }
}

@Composable
fun LevelNodeCustom(id: Int, isUnlocked: Boolean, isCompleted: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "node_fx")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = if (isUnlocked && !isCompleted) 1.15f else 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .shadow(if (isUnlocked) 12.dp else 0.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    when {
                        isCompleted -> Brush.radialGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA000))) // Oro
                        isUnlocked -> Brush.radialGradient(listOf(Color(0xFFFBC02D), Color(0xFFF57F17)))  // Amarillo
                        else -> Brush.radialGradient(listOf(Color(0xFF757575), Color(0xFF424242)))        // Gris (Bloqueado)
                    }
                )
                .clickable(enabled = isUnlocked) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(if (isUnlocked) Color(0xFF8D6E63) else Color(0xFF212121), radius = size.minDimension / 2, style = Stroke(8f))
            }
            
            if (isUnlocked) {
                Text("$id", color = if (isCompleted) Color.White else Color(0xFF5D4037), fontWeight = FontWeight.Black, fontSize = 24.sp)
            } else {
                Icon(Icons.Default.Lock, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
            }
        }
    }
}
