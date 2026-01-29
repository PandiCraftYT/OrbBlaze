package com.example.orbblaze.ui.game

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbblaze.domain.engine.LevelEngine
import com.example.orbblaze.domain.engine.MatchFinder
import com.example.orbblaze.domain.model.Bubble
import com.example.orbblaze.domain.model.BubbleColor
import com.example.orbblaze.domain.model.GridPosition
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.*

data class Projectile(
    val x: Float,
    val y: Float,
    val color: BubbleColor,
    val velocityX: Float,
    val velocityY: Float
)

data class GameParticle(
    val id: Long,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: BubbleColor,
    val size: Float,
    val life: Float
)

data class FloatingText(
    val id: Long,
    val x: Float,
    val y: Float,
    val text: String,
    val life: Float
)

data class BoardMetricsPx(
    val bubbleDiameter: Float,
    val horizontalSpacing: Float,
    val verticalSpacing: Float,
    val boardTopPadding: Float,
    val boardStartPadding: Float,
    val ceilingY: Float
)

enum class GameState {
    PLAYING, WON, LOST
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = LevelEngine()
    private val matchFinder = MatchFinder()
    private val prefs = application.getSharedPreferences("orbblaze_prefs", Context.MODE_PRIVATE)

    // --- ESTADO DEL JUEGO ---
    var bubblesByPosition by mutableStateOf<Map<GridPosition, Bubble>>(emptyMap())
        private set

    var gameState by mutableStateOf(GameState.PLAYING)
        private set

    var isPaused by mutableStateOf(false)
        private set

    var shooterAngle by mutableStateOf(0f)
        private set

    var score by mutableStateOf(0)
        private set

    var highScore by mutableStateOf(0)
        private set

    var shotsFiredCount by mutableStateOf(0)
        private set
    private val DROP_THRESHOLD = 6
    private val COLUMNS_COUNT = 10

    var soundEvent by mutableStateOf<SoundType?>(null)
        private set

    var vibrationEvent by mutableStateOf<Boolean>(false)
        private set

    var nextBubbleColor by mutableStateOf(BubbleColor.values().random())
        private set
    var previewBubbleColor by mutableStateOf(BubbleColor.values().random())
        private set

    val currentBubbleColor: BubbleColor
        get() = nextBubbleColor

    var shotTick by mutableStateOf(0)
        private set
    var activeProjectile by mutableStateOf<Projectile?>(null)
        private set

    val particles = mutableStateListOf<GameParticle>()
    val floatingTexts = mutableStateListOf<FloatingText>()

    private var metrics: BoardMetricsPx? = null
    private var particleIdCounter = 0L
    private var textIdCounter = 0L

    private val bubbleRadius: Float
        get() = (metrics?.bubbleDiameter ?: 44f) / 2f

    init {
        highScore = prefs.getInt("high_score", 0)
        loadLevel()
        startParticleLoop()
    }

    fun setBoardMetrics(metrics: BoardMetricsPx) {
        this.metrics = metrics
    }

    private fun loadLevel() {
        engine.setupInitialLevel(rows = 6, cols = COLUMNS_COUNT)
        bubblesByPosition = engine.gridState
        nextBubbleColor = generateNewBubbleColor()
        previewBubbleColor = generateNewBubbleColor()
        score = 0
        gameState = GameState.PLAYING
        isPaused = false
        shotsFiredCount = 0
        particles.clear()
        floatingTexts.clear()
    }

    fun restartGame() {
        loadLevel()
    }

    fun togglePause() {
        if (gameState == GameState.PLAYING) {
            isPaused = !isPaused
        }
    }

    fun setSfxVolume(volume: Float) {
        prefs.edit().putFloat("sfx_volume", volume).apply()
    }
    fun getSfxVolume(): Float = prefs.getFloat("sfx_volume", 1.0f)

    fun setMusicVolume(volume: Float) {
        prefs.edit().putFloat("music_volume", volume).apply()
    }
    fun getMusicVolume(): Float = prefs.getFloat("music_volume", 0.5f)

    fun resetHighScore() {
        highScore = 0
        prefs.edit().putInt("high_score", 0).apply()
    }

    fun clearSoundEvent() { soundEvent = null }
    fun clearVibrationEvent() { vibrationEvent = false }

    fun updateAngle(touchX: Float, touchY: Float, screenWidth: Float, screenHeight: Float) {
        if (gameState != GameState.PLAYING || isPaused) return
        val centerX = screenWidth / 2f
        val centerY = screenHeight
        val dx = touchX - centerX
        val dy = centerY - touchY
        val angleInRadians = atan2(dx, dy)
        shooterAngle = Math.toDegrees(angleInRadians.toDouble()).toFloat().coerceIn(-80f, 80f)
    }

    fun onShoot(screenWidth: Float, screenHeight: Float) {
        if (gameState != GameState.PLAYING || isPaused) return
        if (activeProjectile != null) return
        val m = metrics ?: return

        shotTick++
        soundEvent = SoundType.SHOOT
        triggerVibration()

        val angleRad = Math.toRadians(shooterAngle.toDouble())
        val speed = 25f
        val startX = screenWidth / 2f
        val startY = screenHeight - 140f

        activeProjectile = Projectile(
            x = startX,
            y = startY,
            color = nextBubbleColor,
            velocityX = (sin(angleRad) * speed).toFloat(),
            velocityY = (-cos(angleRad) * speed).toFloat()
        )

        nextBubbleColor = previewBubbleColor
        previewBubbleColor = generateNewBubbleColor()
        startPhysicsLoop(screenWidth)
    }

    fun swapBubbles() {
        if (activeProjectile == null && gameState == GameState.PLAYING && !isPaused) {
            val temp = nextBubbleColor
            nextBubbleColor = previewBubbleColor
            previewBubbleColor = temp
            soundEvent = SoundType.SWAP
            triggerVibration()
        }
    }

    private fun triggerVibration() {
        val isVibrationEnabled = prefs.getBoolean("vibration_enabled", true)
        if (isVibrationEnabled) {
            vibrationEvent = true
        }
    }

    // ✅ CORREGIDO: Probabilidades ajustadas para Modo Normal
    private fun generateNewBubbleColor(): BubbleColor {
        val rand = Math.random()
        return when {
            rand < 0.01 -> BubbleColor.RAINBOW // 1% Probabilidad (Muy rara)
            rand < 0.04 -> BubbleColor.BOMB    // 3% Probabilidad (Poco común)
            else -> BubbleColor.values().filter { it != BubbleColor.BOMB && it != BubbleColor.RAINBOW }.random()
        }
    }

    private fun updateHighScore() {
        if (score > highScore) {
            highScore = score
            prefs.edit().putInt("high_score", highScore).apply()
        }
    }

    private fun startPhysicsLoop(screenWidth: Float) {
        viewModelScope.launch {
            while (activeProjectile != null) {
                if (isPaused) {
                    delay(100)
                    continue
                }

                val m = metrics ?: break
                val p = activeProjectile ?: break
                val prevX = p.x; val prevY = p.y
                var nextX = p.x + p.velocityX; var nextY = p.y + p.velocityY; var nextVx = p.velocityX

                if (nextX - bubbleRadius <= 0f || nextX + bubbleRadius >= screenWidth) {
                    nextVx *= -1f
                    nextX = p.x + nextVx
                    nextY = p.y + p.velocityY
                }

                if (nextY - bubbleRadius <= m.ceilingY) { snapToGrid(nextX, nextY, p.color); break }
                if (checkSweepCollision(prevX, prevY, nextX, nextY)) { snapToGrid(nextX, nextY, p.color); break }

                activeProjectile = p.copy(x = nextX, y = nextY, velocityX = nextVx)
                delay(16)
            }
        }
    }

    private fun startParticleLoop() {
        viewModelScope.launch {
            while (isActive) {
                if (isPaused) {
                    delay(100)
                    continue
                }

                if (particles.isNotEmpty()) {
                    val iterator = particles.iterator()
                    while (iterator.hasNext()) {
                        val p = iterator.next()
                        val newX = p.x + p.vx
                        val newY = p.y + p.vy + 1.5f
                        val newLife = p.life - 0.04f
                        if (newLife <= 0f) iterator.remove()
                        else {
                            val index = particles.indexOf(p)
                            if (index != -1) particles[index] = p.copy(x = newX, y = newY, life = newLife)
                        }
                    }
                }

                if (floatingTexts.isNotEmpty()) {
                    val txtIterator = floatingTexts.iterator()
                    while (txtIterator.hasNext()) {
                        val t = txtIterator.next()
                        val newY = t.y - 2.0f
                        val newLife = t.life - 0.02f
                        if (newLife <= 0f) txtIterator.remove()
                        else {
                            val idx = floatingTexts.indexOf(t)
                            if (idx != -1) floatingTexts[idx] = t.copy(y = newY, life = newLife)
                        }
                    }
                }

                delay(16)
            }
        }
    }

    private fun spawnExplosion(cx: Float, cy: Float, color: BubbleColor) {
        repeat(12) {
            val angle = Random.nextDouble(0.0, 2 * Math.PI)
            val speed = Random.nextDouble(5.0, 15.0)
            val size = Random.nextDouble(10.0, 25.0).toFloat()
            particles.add(GameParticle(particleIdCounter++, cx, cy, (cos(angle) * speed).toFloat(), (sin(angle) * speed).toFloat(), color, size, 1.0f))
        }
    }

    private fun spawnFloatingText(x: Float, y: Float, text: String) {
        floatingTexts.add(
            FloatingText(
                id = textIdCounter++,
                x = x,
                y = y,
                text = text,
                life = 1.0f
            )
        )
    }

    private fun dropCeiling() {
        val newGrid = mutableMapOf<GridPosition, Bubble>()

        bubblesByPosition.forEach { (pos, bubble) ->
            newGrid[GridPosition(pos.row + 1, pos.col)] = bubble
        }

        // Rellenar Fila 0 con 10 columnas
        for (col in 0 until COLUMNS_COUNT) {
            newGrid[GridPosition(0, col)] = Bubble(color = generateNewBubbleColor())
        }

        bubblesByPosition = newGrid
        soundEvent = SoundType.STICK
        triggerVibration()

        removeFloatingBubbles(newGrid)
        metrics?.let { checkGameConditions(it) }
    }

    private fun snapToGrid(x: Float, y: Float, color: BubbleColor) {
        val m = metrics ?: return
        val row = ((y - m.boardTopPadding) / m.verticalSpacing).roundToInt().coerceAtLeast(0)
        val xOffset = if (row % 2 != 0) (m.bubbleDiameter / 2f) else 0f
        val col = ((x - (m.boardStartPadding + xOffset)) / m.horizontalSpacing).roundToInt().coerceAtLeast(0)
        val newPos = GridPosition(row, col)

        val newGrid = bubblesByPosition.toMutableMap()
        val finalPos = if (newGrid.containsKey(newPos)) findNearestFreeNeighbor(newPos, newGrid) ?: newPos else newPos

        when (color) {
            BubbleColor.BOMB -> {
                explodeAt(finalPos, newGrid)
            }
            BubbleColor.RAINBOW -> {
                handleRainbowAt(finalPos, newGrid, x, y)
            }
            else -> {
                newGrid[finalPos] = Bubble(color = color)
                val matches = matchFinder.findMatches(finalPos, newGrid)
                if (matches.size >= 3) {
                    processMatches(matches, newGrid, x, y, color)
                } else {
                    soundEvent = SoundType.STICK
                }
            }
        }

        bubblesByPosition = newGrid
        activeProjectile = null

        shotsFiredCount++
        if (shotsFiredCount >= DROP_THRESHOLD) {
            shotsFiredCount = 0
            dropCeiling()
        } else {
            checkGameConditions(m)
        }
    }

    private fun processMatches(matches: Set<GridPosition>, grid: MutableMap<GridPosition, Bubble>, x: Float, y: Float, visualColor: BubbleColor) {
        soundEvent = SoundType.POP
        triggerVibration()
        val count = matches.size

        // Puntos
        val points = (count * 10) + ((count - 3) * 20)
        score += points
        spawnFloatingText(x, y, "+$points")
        updateHighScore()

        // Explosiones
        matches.forEach { pos ->
            val bubbleColor = grid[pos]?.color ?: visualColor
            grid.remove(pos)
            val (bx, by) = getBubbleCenter(pos)
            spawnExplosion(bx, by, bubbleColor)
        }

        removeFloatingBubbles(grid)
    }

    private fun handleRainbowAt(pos: GridPosition, grid: MutableMap<GridPosition, Bubble>, fx: Float, fy: Float) {
        val neighbors = getNeighbors(pos, grid)
        val adjacentColors = neighbors.mapNotNull { grid[it]?.color }.distinct()
            .filter { it != BubbleColor.BOMB && it != BubbleColor.RAINBOW }

        if (adjacentColors.isEmpty()) {
            val fallbackColor = generateNewBubbleColor().takeIf { it != BubbleColor.RAINBOW && it != BubbleColor.BOMB } ?: BubbleColor.RED
            grid[pos] = Bubble(color = fallbackColor)
            soundEvent = SoundType.STICK
            return
        }

        val bubblesToPop = mutableSetOf<GridPosition>()
        bubblesToPop.add(pos)

        var comboTriggered = false
        adjacentColors.forEach { neighborColor ->
            grid[pos] = Bubble(color = neighborColor)
            val matches = matchFinder.findMatches(pos, grid)
            if (matches.size >= 3) {
                bubblesToPop.addAll(matches)
                comboTriggered = true
            }
        }

        if (comboTriggered) {
            processMatches(bubblesToPop, grid, fx, fy, BubbleColor.RAINBOW)
        } else {
            grid[pos] = Bubble(color = adjacentColors.first())
            soundEvent = SoundType.STICK
        }
    }

    private fun explodeAt(center: GridPosition, grid: MutableMap<GridPosition, Bubble>) {
        soundEvent = SoundType.EXPLODE
        triggerVibration()
        val radius = getNeighbors(center, grid) + center
        var destroyedCount = 0

        radius.forEach { pos ->
            if (grid.containsKey(pos) || pos == center) {
                val targetColor = grid[pos]?.color ?: BubbleColor.BOMB
                if(grid.containsKey(pos)) {
                    val (bx, by) = getBubbleCenter(pos)
                    spawnExplosion(bx, by, targetColor)
                    grid.remove(pos)
                    destroyedCount++
                }
            }
        }

        val points = destroyedCount * 50
        score += points
        val (cx, cy) = getBubbleCenter(center)
        spawnFloatingText(cx, cy, "+$points")
        updateHighScore()

        removeFloatingBubbles(grid)
    }

    private fun removeFloatingBubbles(grid: MutableMap<GridPosition, Bubble>) {
        val visited = mutableSetOf<GridPosition>()
        val queue = ArrayDeque<GridPosition>()

        val ceilingBubbles = grid.keys.filter { it.row == 0 }
        queue.addAll(ceilingBubbles)
        visited.addAll(ceilingBubbles)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val neighbors = getNeighbors(current, grid)
            for (neighbor in neighbors) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor)
                    queue.add(neighbor)
                }
            }
        }

        val floating = grid.keys.filter { !visited.contains(it) }

        if (floating.isNotEmpty()) {
            var droppedPoints = 0
            floating.forEach { pos ->
                val bubble = grid[pos]
                grid.remove(pos)

                val (bx, by) = getBubbleCenter(pos)
                val color = bubble?.color ?: BubbleColor.BLUE
                spawnExplosion(bx, by, color)

                droppedPoints += 20
            }

            if (droppedPoints > 0) {
                score += droppedPoints
                metrics?.let {
                    spawnFloatingText(it.boardStartPadding + 200f, it.boardTopPadding + 400f, "DROP! +$droppedPoints")
                }
                updateHighScore()
            }
        }
    }

    private fun getNeighbors(pos: GridPosition, grid: Map<GridPosition, Bubble>): List<GridPosition> {
        val offsets = if (pos.row % 2 == 0) {
            listOf(-1 to -1, -1 to 0, 0 to -1, 0 to 1, 1 to -1, 1 to 0)
        } else {
            listOf(-1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to 0, 1 to 1)
        }
        return offsets.map { GridPosition(pos.row + it.first, pos.col + it.second) }
            .filter { grid.containsKey(it) }
    }

    private fun checkGameConditions(m: BoardMetricsPx) {
        if (bubblesByPosition.isEmpty()) {
            gameState = GameState.WON
            soundEvent = SoundType.WIN
            return
        }
        val dangerY = m.boardTopPadding + (m.verticalSpacing * 12)
        val reachedBottom = bubblesByPosition.keys.any { pos ->
            val (_, y) = getBubbleCenter(pos)
            (y + bubbleRadius) >= dangerY
        }

        if (reachedBottom) {
            gameState = GameState.LOST
            soundEvent = SoundType.LOSE
        }
    }

    private fun checkSweepCollision(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        val m = metrics ?: return false; val collideDist = m.bubbleDiameter * 0.95f
        return bubblesByPosition.keys.any { pos ->
            val (cx, cy) = getBubbleCenter(pos); distancePointToSegment(cx, cy, x1, y1, x2, y2) <= collideDist
        }
    }
    private fun distancePointToSegment(cx: Float, cy: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val abx = bx - ax; val aby = by - ay; val acx = cx - ax; val acy = cy - ay
        val abLen2 = abx * abx + aby * aby; if (abLen2 == 0f) return hypot(cx - ax, cy - ay)
        var t = (acx * abx + acy * aby) / abLen2; t = t.coerceIn(0f, 1f)
        return hypot(cx - (ax + t * abx), cy - (ay + t * aby))
    }
    private fun findNearestFreeNeighbor(pos: GridPosition, grid: Map<GridPosition, Bubble>): GridPosition? {
        val candidates = listOf(GridPosition(pos.row, pos.col - 1), GridPosition(pos.row, pos.col + 1), GridPosition(pos.row - 1, pos.col), GridPosition(pos.row + 1, pos.col), GridPosition(pos.row - 1, pos.col + if (pos.row % 2 != 0) 1 else -1), GridPosition(pos.row + 1, pos.col + if (pos.row % 2 != 0) 1 else -1))
        return candidates.firstOrNull { !grid.containsKey(it) && it.row >= 0 && it.col >= 0 }
    }
    private fun getBubbleCenter(pos: GridPosition): Pair<Float, Float> {
        val m = metrics ?: return 0f to 0f; val xOffset = if (pos.row % 2 != 0) (m.bubbleDiameter / 2f) else 0f
        val x = m.boardStartPadding + xOffset + (pos.col * m.horizontalSpacing); val y = m.boardTopPadding + (pos.row * m.verticalSpacing)
        return x to y
    }
}
object Random { fun nextDouble(min: Double, max: Double): Double = min + (Math.random() * (max - min)) }