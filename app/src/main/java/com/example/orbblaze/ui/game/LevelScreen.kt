package com.example.orbblaze.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.orbblaze.domain.model.BubbleColor
import com.example.orbblaze.ui.components.Shooter
import com.example.orbblaze.ui.components.VisualBubble
import com.example.orbblaze.ui.theme.*

@Composable
fun LevelScreen(viewModel: GameViewModel = viewModel()) {
    val bubbles = viewModel.bubblesByPosition
    val bubbleSize = 44.dp
    val horizontalSpacing = 40.dp
    val verticalSpacing = 36.dp

    // BoxWithConstraints nos permite saber el ancho y alto disponible (maxWidth, maxHeight)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground)
            .pointerInput(Unit) {
                // Detecta cuando arrastras el dedo para mover el cañón
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
                // Detecta un toque rápido para disparar
                detectTapGestures {
                    // Por ahora, simulamos un disparo al centro (esto evolucionará con la física)
                    // viewModel.onShoot(...)
                }
            }
    ) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }

        bubbles.entries.forEach { entry ->
            val pos = entry.key
            val bubble = entry.value

            val xOffset = if (pos.row % 2 != 0) (bubbleSize / 2) else 0.dp
            val xPos = (pos.col.toFloat() * horizontalSpacing.value).dp + xOffset + 16.dp
            val yPos = (pos.row.toFloat() * verticalSpacing.value).dp + 50.dp

            VisualBubble(
                color = mapBubbleColor(bubble.color),
                modifier = Modifier.offset(x = xPos, y = yPos)
            )
        }

        Shooter(
            angle = viewModel.shooterAngle,
            currentBubbleColor = mapBubbleColor(viewModel.nextBubbleColor)
        )
    }
}

fun mapBubbleColor(type: BubbleColor): Color = when(type) {
    BubbleColor.RED -> BubbleRed
    BubbleColor.BLUE -> BubbleBlue
    BubbleColor.GREEN -> BubbleGreen
    BubbleColor.PURPLE -> BubblePurple
    BubbleColor.YELLOW -> Color.Yellow
    BubbleColor.CYAN -> Color.Cyan
}