package com.example.orbblaze.ui.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.orbblaze.domain.engine.LevelEngine
import com.example.orbblaze.domain.engine.MatchFinder
import com.example.orbblaze.domain.model.Bubble
import com.example.orbblaze.domain.model.BubbleColor
import com.example.orbblaze.domain.model.GridPosition
import kotlin.math.atan2

class GameViewModel : ViewModel() {

    private val engine = LevelEngine()
    private val matchFinder = MatchFinder()

    // 1. El estado de la grilla (burbujas estáticas)
    var bubblesByPosition by mutableStateOf<Map<GridPosition, Bubble>>(emptyMap())
        private set

    // 2. El ángulo del cañón (en grados)
    var shooterAngle by mutableStateOf(0f)
        private set

    // 3. La burbuja cargada en el cañón
    var nextBubbleColor by mutableStateOf(BubbleColor.values().random())
        private set

    init {
        loadLevel()
    }

    private fun loadLevel() {
        engine.setupInitialLevel(rows = 10, cols = 8)
        bubblesByPosition = engine.gridState
    }

    /**
     * Actualiza el ángulo del cañón basándose en la posición del toque.
     * El cañón está centrado en la parte inferior de la pantalla.
     */
    fun updateAngle(touchX: Float, touchY: Float, screenWidth: Float, screenHeight: Float) {
        val centerX = screenWidth / 2f
        val centerY = screenHeight // El cañón está en la base

        val dx = touchX - centerX
        val dy = centerY - touchY // Invertimos Y porque en pantallas 0 es arriba

        // Calculamos el ángulo en grados
        val angleInRadians = atan2(dx, dy)
        shooterAngle = Math.toDegrees(angleInRadians.toDouble()).toFloat().coerceIn(-80f, 80f)
    }

    /**
     * Lógica de disparo (Versión inicial: coloca la burbuja y busca matches)
     */
    fun onShoot(targetPos: GridPosition) {
        val currentGrid = bubblesByPosition.toMutableMap()

        // Colocamos la burbuja en la posición de impacto
        val newBubble = Bubble(color = nextBubbleColor)
        currentGrid[targetPos] = newBubble

        // Buscamos si hay 3 o más conectadas del mismo color
        val matches = matchFinder.findMatches(targetPos, currentGrid)

        if (matches.size >= 3) {
            // ¡BOOM! Eliminamos las burbujas del combo
            matches.forEach { currentGrid.remove(it) }
        }

        // Actualizamos la UI y preparamos la siguiente burbuja
        bubblesByPosition = currentGrid
        nextBubbleColor = BubbleColor.values().random()
    }
}