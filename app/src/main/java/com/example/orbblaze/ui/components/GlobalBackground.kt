package com.example.orbblaze.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.orbblaze.ui.theme.BgBottom
import com.example.orbblaze.ui.theme.BgTop
import kotlin.math.sin
import kotlin.random.Random

// Estructura para pequeñas partículas de brillo en el fondo
data class BackgroundSparkle(
    val xRel: Float,
    val yRel: Float,
    val size: Float,
    val speed: Float,
    val phase: Float
)

@Composable
fun GlobalBackground(content: @Composable () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "global_bg_anim")
    
    // Animación de desplazamiento continuo
    val backgroundOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bg_scroll"
    )

    // Pulso suave para el resplandor solar
    val sunPulse by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sun_pulse"
    )

    // Generamos las partículas solo una vez para evitar recomposiciones costosas
    val sparkles = remember {
        List(15) {
            BackgroundSparkle(
                xRel = Random.nextFloat(),
                yRel = Random.nextFloat(),
                size = Random.nextFloat() * 2f + 1f,
                speed = Random.nextFloat() * 0.3f + 0.1f,
                phase = Random.nextFloat() * 2 * Math.PI.toFloat()
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. RESPLANDOR SOLAR (Efecto de luz ambiental)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = sunPulse), Color.Transparent),
                    center = Offset(w * 0.9f, 100f),
                    radius = w * 0.7f
                ),
                radius = w * 0.7f,
                center = Offset(w * 0.9f, 100f)
            )

            // 2. ORBES / NUBES DE FONDO
            val cloudColor = Color.White.copy(alpha = 0.12f)
            drawCircle(
                color = cloudColor, 
                radius = 150.dp.toPx(), 
                center = Offset(
                    x = (backgroundOffset % (w + 400.dp.toPx())) - 200.dp.toPx(), 
                    y = h * 0.15f
                )
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.06f), 
                radius = 250.dp.toPx(), 
                center = Offset(
                    x = w - ((backgroundOffset * 0.7f) % (w + 600.dp.toPx())), 
                    y = h * 0.45f
                )
            )

            // 3. PARTÍCULAS FLOTANTES (Sparkles)
            sparkles.forEach { s ->
                val x = (s.xRel * w + (backgroundOffset * s.speed)) % w
                val y = s.yRel * h
                
                // Efecto de parpadeo suave
                val alpha = (0.1f + 0.5f * sin(backgroundOffset * 0.02f + s.phase)).coerceIn(0f, 0.6f)
                
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = s.size.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
        content()
    }
}
