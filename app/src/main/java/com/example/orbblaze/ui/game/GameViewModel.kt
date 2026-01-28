package com.example.orbblaze.ui.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbblaze.domain.engine.LevelEngine
import com.example.orbblaze.domain.engine.MatchFinder
import com.example.orbblaze.domain.model.Bubble
import com.example.orbblaze.domain.model.BubbleColor
import com.example.orbblaze.domain.model.GridPosition
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

// Asegúrate de tener esta data class en domain/model/Bubble.kt, si no, pégala al final de este archivo temporalmente
data class Projectile(
    val x: Float,
    val y: Float,
    val color: BubbleColor,
    val velocityX: Float,
    val velocityY: Float
)

class GameViewModel : ViewModel() {

    private val engine = LevelEngine()
    private val matchFinder = MatchFinder()

    // --- ESTADOS DE LA UI ---
    var bubblesByPosition by mutableStateOf<Map<GridPosition, Bubble>>(emptyMap())
        private set

    var shooterAngle by mutableStateOf(0f)
        private set

    var nextBubbleColor by mutableStateOf(BubbleColor.values().random())
        private set

    // El proyectil activo (nulo si no hay disparo)
    var activeProjectile by mutableStateOf<Projectile?>(null)
        private set

    // Constantes físicas (Ajustadas para la escala de tu pantalla)
    private val bubbleRadius = 22.dpToPx() // 44.dp / 2
    private val bubbleDiameter = 44.dpToPx()

    init {
        loadLevel()
    }

    private fun loadLevel() {
        engine.setupInitialLevel(rows = 10, cols = 8)
        bubblesByPosition = engine.gridState
    }

    // --- LÓGICA DE APUNTADO ---
    fun updateAngle(touchX: Float, touchY: Float, screenWidth: Float, screenHeight: Float) {
        val centerX = screenWidth / 2f
        val centerY = screenHeight
        val dx = touchX - centerX
        val dy = centerY - touchY

        // Convertimos a grados y limitamos el giro
        val angleInRadians = atan2(dx, dy)
        shooterAngle = Math.toDegrees(angleInRadians.toDouble()).toFloat().coerceIn(-80f, 80f)
    }

    // --- LÓGICA DE DISPARO Y FÍSICA ---
    fun onShoot(screenWidth: Float, screenHeight: Float) {
        if (activeProjectile != null) return // Evitar ametralladora

        // Calculamos velocidad basada en el ángulo
        val angleRad = Math.toRadians(shooterAngle.toDouble())
        val speed = 25f // Velocidad del orbe

        activeProjectile = Projectile(
            x = screenWidth / 2f,
            y = screenHeight - 100f, // Sale un poco arriba de la base
            color = nextBubbleColor,
            velocityX = (sin(angleRad) * speed).toFloat(),
            velocityY = (-cos(angleRad) * speed).toFloat()
        )

        nextBubbleColor = BubbleColor.values().random()
        startPhysicsLoop(screenWidth)
    }

    private fun startPhysicsLoop(screenWidth: Float) {
        viewModelScope.launch {
            while (activeProjectile != null) {
                val p = activeProjectile ?: break

                // 1. Mover proyectil
                var nextX = p.x + p.velocityX
                var nextY = p.y + p.velocityY
                var nextVx = p.velocityX

                // 2. Rebote en paredes (Izquierda/Derecha)
                // Restamos el radio para que rebote en el borde de la burbuja, no en el centro
                if (nextX - bubbleRadius <= 0 || nextX + bubbleRadius >= screenWidth) {
                    nextVx *= -1
                    // Corrección simple para que no se pegue a la pared
                    nextX = p.x
                }

                // 3. Detección de Colisión
                // A) Si toca el techo
                if (nextY - bubbleRadius <= 50f) { // 50f es un margen superior arbitrario
                    snapToGrid(nextX, nextY, p.color)
                    break
                }

                // B) Si choca con otra burbuja
                if (checkCollisionWithBubbles(nextX, nextY)) {
                    snapToGrid(nextX, nextY, p.color)
                    break
                }

                // Actualizar estado del proyectil
                activeProjectile = p.copy(x = nextX, y = nextY, velocityX = nextVx)

                // 16ms aprox = 60 FPS
                delay(16)
            }
        }
    }

    // --- DETECCIÓN DE COLISIONES ---
    private fun checkCollisionWithBubbles(x: Float, y: Float): Boolean {
        // Distancia mínima para colisión (un poco menos del diámetro para que se sienta bien)
        val collisionDist = bubbleDiameter * 0.85f

        // Revisamos contra todas las burbujas activas (ineficiente pero funciona para <100 burbujas)
        // Para optimizar, en el futuro solo revisaríamos las cercanas
        return bubblesByPosition.keys.any { pos ->
            val bubbleCenter = getBubbleCenter(pos)
            val dist = hypot(x - bubbleCenter.first, y - bubbleCenter.second)
            dist < collisionDist
        }
    }

    // --- ANCLAJE Y COMBOS ---
    private fun snapToGrid(x: Float, y: Float, color: BubbleColor) {
        // Convertimos coordenadas (x,y) a la fila/columna más cercana
        // Nota: Estos valores (40f, 36f) deben coincidir con tu LevelScreen.kt
        // Si usas density dinámico, deberías pasarlo aquí. Por ahora uso valores aproximados en px.
        val verticalSpacing = 36f * 2.625f // 36.dp a px aprox (asumiendo xxhdpi)
        val horizontalSpacing = 40f * 2.625f // 40.dp a px aprox

        // Algoritmo simple de conversión Hexagonal
        val row = ((y - 130f) / verticalSpacing).roundToInt().coerceAtLeast(0)
        val xOffset = if (row % 2 != 0) (bubbleDiameter / 2) else 0f
        val col = ((x - xOffset) / horizontalSpacing).roundToInt().coerceAtLeast(0)

        val newPos = GridPosition(row, col)
        val newGrid = bubblesByPosition.toMutableMap()

        // Agregamos la burbuja
        newGrid[newPos] = Bubble(color = color)

        // BUSCAR COMBOS (MatchFinder)
        val matches = matchFinder.findMatches(newPos, newGrid)
        if (matches.size >= 3) {
            matches.forEach { newGrid.remove(it) }
        }

        bubblesByPosition = newGrid
        activeProjectile = null // Destruir proyectil volador
    }

    // Auxiliar para saber dónde está una burbuja de la grilla en pixeles
    private fun getBubbleCenter(pos: GridPosition): Pair<Float, Float> {
        val verticalSpacing = 36f * 2.625f
        val horizontalSpacing = 40f * 2.625f

        val xOffset = if (pos.row % 2 != 0) (bubbleDiameter / 2) else 0f
        val x = (pos.col * horizontalSpacing) + xOffset + 40f // +40f margen aprox
        val y = (pos.row * verticalSpacing) + 130f // +130f margen superior aprox
        return Pair(x, y)
    }

    // Utilidad simple para simular DP a PX si no tienes contexto
    private fun Int.dpToPx(): Float = this * 2.625f
}