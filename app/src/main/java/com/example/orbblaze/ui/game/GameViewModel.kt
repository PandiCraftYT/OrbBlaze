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
data class BoardMetricsPx(
    val bubbleDiameter: Float,
    val horizontalSpacing: Float,
    val verticalSpacing: Float,
    val boardTopPadding: Float,     // donde empieza el tablero (PX)
    val boardStartPadding: Float,   // padding izquierdo real (PX)
    val ceilingY: Float             // “techo” donde se pega (PX)
)

class GameViewModel : ViewModel() {

    private val engine = LevelEngine()
    private val matchFinder = MatchFinder()

    var bubblesByPosition by mutableStateOf<Map<GridPosition, Bubble>>(emptyMap())
        private set

    var shooterAngle by mutableStateOf(0f)
        private set

    var nextBubbleColor by mutableStateOf(BubbleColor.values().random())
        private set
    var shotTick by mutableStateOf(0)
        private set
    var activeProjectile by mutableStateOf<Projectile?>(null)
        private set

    // ✅ NUEVO: métricas reales que vienen de Compose
    private var metrics: BoardMetricsPx? = null

    // ✅ helper
    private val bubbleRadius: Float
        get() = (metrics?.bubbleDiameter ?: 44f) / 2f

    init { loadLevel() }

    fun setBoardMetrics(metrics: BoardMetricsPx) {
        this.metrics = metrics
    }

    private fun loadLevel() {
        engine.setupInitialLevel(rows = 10, cols = 8)
        bubblesByPosition = engine.gridState
    }

    fun updateAngle(touchX: Float, touchY: Float, screenWidth: Float, screenHeight: Float) {
        val centerX = screenWidth / 2f
        val centerY = screenHeight
        val dx = touchX - centerX
        val dy = centerY - touchY
        val angleInRadians = atan2(dx, dy)
        shooterAngle = Math.toDegrees(angleInRadians.toDouble()).toFloat().coerceIn(-80f, 80f)
    }

    fun onShoot(screenWidth: Float, screenHeight: Float) {
        if (activeProjectile != null) return
        val m = metrics ?: return // ✅ sin métricas aún, no dispares

        val angleRad = Math.toRadians(shooterAngle.toDouble())
        val speed = 25f

        // ✅ Origen del disparo: arriba de la base (ajústalo si quieres)
        val startX = screenWidth / 2f
        val startY = screenHeight - 140f  // tu base mide 140.dp aprox; esto en px “a ojo”
        // (si quieres exactitud total, también pásame shooterY desde UI)

        activeProjectile = Projectile(
            x = startX,
            y = startY,
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
                val m = metrics ?: break
                val p = activeProjectile ?: break

                val prevX = p.x
                val prevY = p.y

                var nextX = p.x + p.velocityX
                var nextY = p.y + p.velocityY
                var nextVx = p.velocityX

                // Rebote en paredes (con radio)
                if (nextX - bubbleRadius <= 0f || nextX + bubbleRadius >= screenWidth) {
                    nextVx *= -1f
                    nextX = p.x + nextVx
                    nextY = p.y + p.velocityY
                }

                // Techo
                if (nextY - bubbleRadius <= m.ceilingY) {
                    snapToGrid(nextX, nextY, p.color)
                    break
                }

                // ✅ COLISIÓN REAL: segmento prev->next contra burbujas
                val hit = checkSweepCollision(prevX, prevY, nextX, nextY)
                if (hit) {
                    snapToGrid(nextX, nextY, p.color)
                    break
                }

                activeProjectile = p.copy(x = nextX, y = nextY, velocityX = nextVx)
                delay(16)
            }
        }
    }

    private fun checkSweepCollision(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        val m = metrics ?: return false

        // distancia para choque: radio proyectil + radio burbuja
        val collideDist = m.bubbleDiameter * 0.95f // ~diametro (ajusta 0.9-1.0)

        // revisa todas (luego optimizamos)
        return bubblesByPosition.keys.any { pos ->
            val (cx, cy) = getBubbleCenter(pos)
            distancePointToSegment(cx, cy, x1, y1, x2, y2) <= collideDist
        }
    }

    // Distancia de un punto C al segmento AB
    private fun distancePointToSegment(
        cx: Float, cy: Float,
        ax: Float, ay: Float,
        bx: Float, by: Float
    ): Float {
        val abx = bx - ax
        val aby = by - ay
        val acx = cx - ax
        val acy = cy - ay

        val abLen2 = abx * abx + aby * aby
        if (abLen2 == 0f) return hypot(cx - ax, cy - ay)

        var t = (acx * abx + acy * aby) / abLen2
        t = t.coerceIn(0f, 1f)

        val px = ax + t * abx
        val py = ay + t * aby
        return hypot(cx - px, cy - py)
    }

    private fun checkCollisionWithBubbles(x: Float, y: Float): Boolean {
        val m = metrics ?: return false
        val collisionDist = m.bubbleDiameter * 0.92f

        return bubblesByPosition.keys.any { pos ->
            val (bx, by) = getBubbleCenter(pos)
            hypot(x - bx, y - by) < collisionDist
        }
    }

    private fun snapToGrid(x: Float, y: Float, color: BubbleColor) {
        val m = metrics ?: return

        // ✅ conversión consistente con tu dibujo
        val row = ((y - m.boardTopPadding) / m.verticalSpacing)
            .roundToInt()
            .coerceAtLeast(0)

        val xOffset = if (row % 2 != 0) (m.bubbleDiameter / 2f) else 0f

        val col = ((x - (m.boardStartPadding + xOffset)) / m.horizontalSpacing)
            .roundToInt()
            .coerceAtLeast(0)

        val newPos = GridPosition(row, col)
        val newGrid = bubblesByPosition.toMutableMap()

        // ✅ si cae encima de una ocupada, busca una vecina libre cercana
        val finalPos = if (newGrid.containsKey(newPos)) {
            findNearestFreeNeighbor(newPos, newGrid) ?: newPos
        } else newPos

        newGrid[finalPos] = Bubble(color = color)

        val matches = matchFinder.findMatches(finalPos, newGrid)
        if (matches.size >= 3) matches.forEach { newGrid.remove(it) }

        bubblesByPosition = newGrid
        activeProjectile = null
    }

    private fun findNearestFreeNeighbor(
        pos: GridPosition,
        grid: Map<GridPosition, Bubble>
    ): GridPosition? {
        // vecinos en grid hex “simple”
        val candidates = listOf(
            GridPosition(pos.row, pos.col - 1),
            GridPosition(pos.row, pos.col + 1),
            GridPosition(pos.row - 1, pos.col),
            GridPosition(pos.row + 1, pos.col),
            GridPosition(pos.row - 1, pos.col + if (pos.row % 2 != 0) 1 else -1),
            GridPosition(pos.row + 1, pos.col + if (pos.row % 2 != 0) 1 else -1),
        )
        return candidates.firstOrNull { !grid.containsKey(it) && it.row >= 0 && it.col >= 0 }
    }

    private fun getBubbleCenter(pos: GridPosition): Pair<Float, Float> {
        val m = metrics ?: return 0f to 0f

        val xOffset = if (pos.row % 2 != 0) (m.bubbleDiameter / 2f) else 0f
        val x = m.boardStartPadding + xOffset + (pos.col * m.horizontalSpacing)
        val y = m.boardTopPadding + (pos.row * m.verticalSpacing)
        return x to y
    }
}
