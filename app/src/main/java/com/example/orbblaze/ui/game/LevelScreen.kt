package com.example.orbblaze.ui.game

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.orbblaze.domain.model.BubbleColor
import com.example.orbblaze.ui.components.Shooter
import com.example.orbblaze.ui.components.VisualBubble
import com.example.orbblaze.ui.theme.*

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun LevelScreen(viewModel: GameViewModel = viewModel()) {
    val bubbles = viewModel.bubblesByPosition
    val activeProjectile = viewModel.activeProjectile // ¡No olvides esto para ver el disparo!

    val bubbleSize = 44.dp
    val horizontalSpacing = 40.dp
    val verticalSpacing = 36.dp
    val density = LocalDensity.current

    // 1. FONDO PREMIUM (Pastel Gradient)
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE0F7FA), // Cyan muy pálido (Cielo)
            Color(0xFFF3E5F5), // Púrpura pálido
            Color(0xFFFFF3E0)  // Naranja pálido (Suelo)
        )
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            // GESTOS
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    viewModel.updateAngle(
                        touchX = change.position.x,
                        touchY = change.position.y,
                        screenWidth = size.width.toFloat(),
                        screenHeight = size.height.toFloat()
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures {
                    viewModel.onShoot(
                        screenWidth = size.width.toFloat(),
                        screenHeight = size.height.toFloat()
                    )
                }
            }
    ) {
        // 2. DECORACIÓN DE FONDO (Nubes/Formas abstractas)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = size.width * 0.6f,
                center = Offset(size.width * 0.1f, size.height * 0.2f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = size.width * 0.4f,
                center = Offset(size.width * 0.9f, size.height * 0.6f)
            )
        }

        // 3. TABLERO DE JUEGO (Marco sutil)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp, start = 8.dp, end = 8.dp) // Margen de seguridad
        ) {
            bubbles.entries.forEach { entry ->
                val pos = entry.key
                val bubble = entry.value

                val xOffset = if (pos.row % 2 != 0) (bubbleSize / 2) else 0.dp
                val xPos = (pos.col.toFloat() * horizontalSpacing.value).dp + xOffset + 16.dp // Ajuste lateral
                val yPos = (pos.row.toFloat() * verticalSpacing.value).dp

                VisualBubble(
                    color = mapBubbleColor(bubble.color),
                    modifier = Modifier.offset(x = xPos, y = yPos)
                )
            }
        }

        // 4. PROYECTIL EN VUELO
        activeProjectile?.let { projectile ->
            VisualBubble(
                color = mapBubbleColor(projectile.color),
                modifier = Modifier.offset(
                    x = with(density) { projectile.x.toDp() } - (bubbleSize / 2),
                    y = with(density) { projectile.y.toDp() } - (bubbleSize / 2)
                )
            )
        }

        // 5. UI DEL CAÑÓN (Base moderna curva)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(140.dp)
        ) {
            // Dibujamos una base curva blanca semitransparente
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path().apply {
                    moveTo(0f, size.height)
                    lineTo(0f, size.height * 0.4f)
                    quadraticBezierTo(
                        size.width / 2, size.height * 0.1f, // Punto de control (curva hacia arriba)
                        size.width, size.height * 0.4f
                    )
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.6f) // Efecto cristal
                )
            }

            Shooter(
                angle = viewModel.shooterAngle,
                currentBubbleColor = mapBubbleColor(viewModel.nextBubbleColor)
            )
        }
    }
}

// 6. COLORES VIBRANTES (Para fondo claro)
// Ajustamos los colores para que no se pierdan en el fondo blanco
fun mapBubbleColor(type: BubbleColor): Color = when(type) {
    BubbleColor.RED -> Color(0xFFFF453A)    // Rojo Intenso
    BubbleColor.BLUE -> Color(0xFF0A84FF)   // Azul Vivo
    BubbleColor.GREEN -> Color(0xFF32D74B)  // Verde Brillante
    BubbleColor.PURPLE -> Color(0xFFBF5AF2) // Púrpura Neón Suave
    BubbleColor.YELLOW -> Color(0xFFFFD60A) // Amarillo Oro (Se ve mejor en blanco)
    BubbleColor.CYAN -> Color(0xFF64D2FF)   // Cyan Cielo
}