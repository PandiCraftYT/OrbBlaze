package com.example.orbblaze.ui.game

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbblaze.domain.engine.LevelEngine
import com.example.orbblaze.domain.engine.MatchFinder
import com.example.orbblaze.domain.model.Achievement
import com.example.orbblaze.domain.model.Bubble
import com.example.orbblaze.domain.model.BubbleColor
import com.example.orbblaze.domain.model.GridPosition
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.*

// --- CLASES DE DATOS Y ENUMS ---
data class Projectile(val x: Float, val y: Float, val color: BubbleColor, val velocityX: Float, val velocityY: Float, val isFireball: Boolean = false)
data class GameParticle(val id: Long, val x: Float, val y: Float, val vx: Float, val vy: Float, val color: BubbleColor, val size: Float, val life: Float)
data class FloatingText(val id: Long, val x: Float, val y: Float, val text: String, val life: Float)
data class BoardMetricsPx(val bubbleDiameter: Float, val horizontalSpacing: Float, val verticalSpacing: Float, val boardTopPadding: Float, val boardStartPadding: Float, val ceilingY: Float)

enum class GameState { IDLE, PLAYING, WON, LOST }
enum class GameMode { CLASSIC, TIME_ATTACK }
enum class SoundType { SHOOT, POP, EXPLODE, STICK, WIN, LOSE, SWAP }

open class GameViewModel(application: Application) : AndroidViewModel(application) {

    protected val engine = LevelEngine()
    protected val matchFinder = MatchFinder()
    protected val prefs = application.getSharedPreferences("orbblaze_prefs", Context.MODE_PRIVATE)

    var bubblesByPosition by mutableStateOf<Map<GridPosition, Bubble>>(emptyMap())
        protected set

    var gameState by mutableStateOf(GameState.IDLE)
        protected set

    var gameMode by mutableStateOf(GameMode.CLASSIC)
        protected set

    var isPaused by mutableStateOf(false)
        protected set

    var shooterAngle by mutableStateOf(0f)
        protected set

    var score by mutableIntStateOf(0)
        protected set

    var highScore by mutableIntStateOf(0)
        protected set

    var coins by mutableIntStateOf(0)
        protected set

    var timeLeft by mutableIntStateOf(90)
        protected set

    protected var timerJob: Job? = null

    var shotsFiredCount by mutableIntStateOf(0)
        protected set

    var rowsDroppedCount by mutableIntStateOf(0)
        protected set

    var joyTick by mutableIntStateOf(0)
        protected set

    var currentRewardDay by mutableIntStateOf(1)
        private set

    protected val dropThreshold = 8
    protected val columnsCount = 10

    var soundEvent by mutableStateOf<SoundType?>(null)
        protected set

    var vibrationEvent by mutableStateOf<Boolean>(false)
        protected set

    val achievements = mutableStateListOf<Achievement>()

    var activeAchievement by mutableStateOf<Achievement?>(null)
        protected set

    // Usamos el generador de proyectiles para el cañón
    var nextBubbleColor by mutableStateOf(generateProjectileColor())
        protected set
    var previewBubbleColor by mutableStateOf(generateProjectileColor())
        protected set

    var isFireballQueued by mutableStateOf(false)
        protected set

    val currentBubbleColor: BubbleColor get() = nextBubbleColor

    var shotTick by mutableIntStateOf(0)
        protected set
    var activeProjectile by mutableStateOf<Projectile?>(null)
        protected set

    val particles = mutableStateListOf<GameParticle>()
    val floatingTexts = mutableStateListOf<FloatingText>()

    protected var metrics: BoardMetricsPx? = null
    private var particleIdCounter = 0L
    private var textIdCounter = 0L

    protected val bubbleRadius: Float get() = (metrics?.bubbleDiameter ?: 44f) / 2f

    init {
        highScore = prefs.getInt("high_score", 0)
        coins = prefs.getInt("coins", 0)
        currentRewardDay = prefs.getInt("current_reward_day", 1)
        checkRewardDayReset()
        setupAchievements()
        loadAchievements()
        startParticleLoop()
    }

    private fun checkRewardDayReset() {
        val lastClaim = prefs.getLong("last_reward_claim", 0L)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClaim > 48 * 60 * 60 * 1000L && lastClaim != 0L) {
            currentRewardDay = 1; prefs.edit().putInt("current_reward_day", 1).apply()
        }
    }

    fun canClaimReward(): Boolean {
        val lastClaim = prefs.getLong("last_reward_claim", 0L)
        return (System.currentTimeMillis() - lastClaim) >= 24 * 60 * 60 * 1000L
    }

    fun claimDailyReward() {
        if (canClaimReward()) {
            val rewards = listOf(50, 100, 150, 200, 300, 500, 1000)
            addCoins(rewards[(currentRewardDay - 1) % 7])
            prefs.edit().putLong("last_reward_claim", System.currentTimeMillis()).apply()
            currentRewardDay = if (currentRewardDay >= 7) 1 else currentRewardDay + 1
            prefs.edit().putInt("current_reward_day", currentRewardDay).apply()
        }
    }

    fun addCoins(amount: Int) {
        coins += amount
        prefs.edit().putInt("coins", coins).apply()
    }

    fun spendCoins(amount: Int): Boolean {
        if (coins >= amount) {
            coins -= amount; prefs.edit().putInt("coins", coins).apply(); return true
        }
        return false
    }

    fun buyFireball(): Boolean {
        if (!isFireballQueued && spendCoins(1)) {
            isFireballQueued = true
            soundEvent = SoundType.SWAP
            return true
        }
        return false
    }

    open fun changeGameMode(mode: GameMode) { this.gameMode = mode }
    open fun startGame() { gameState = GameState.PLAYING; startTimer() }

    open fun loadLevel(initialRows: Int = 6) {
        engine.setupInitialLevel(rows = initialRows, cols = columnsCount)
        // REGENERACIÓN DE TABLERO: Nos aseguramos de limpiar especiales si hubieran quedado
        val cleanGrid = engine.gridState.toMutableMap()
        cleanGrid.forEach { (pos, bubble) ->
            if (bubble.color == BubbleColor.RAINBOW || bubble.color == BubbleColor.BOMB) {
                cleanGrid[pos] = bubble.copy(color = generateBoardBubbleColor())
            }
        }
        bubblesByPosition = cleanGrid

        nextBubbleColor = generateProjectileColor()
        previewBubbleColor = generateProjectileColor()
        score = 0; gameState = GameState.IDLE
        isPaused = false; shotsFiredCount = 0; rowsDroppedCount = 0; timeLeft = 90
        isFireballQueued = false
        particles.clear(); floatingTexts.clear(); timerJob?.cancel()
    }

    fun restartGame() { loadLevel(if (gameMode == GameMode.TIME_ATTACK) 3 else 6) }
    fun swapBubbles() { if (activeProjectile == null && gameState == GameState.PLAYING && !isPaused) { val temp = nextBubbleColor; nextBubbleColor = previewBubbleColor; previewBubbleColor = temp; soundEvent = SoundType.SWAP; if (prefs.getBoolean("vibration_enabled", true)) vibrationEvent = true } }
    fun setBoardMetrics(metrics: BoardMetricsPx) { this.metrics = metrics }
    fun togglePause() { if (gameState == GameState.PLAYING) isPaused = !isPaused }
    fun setSfxVolume(volume: Float) { prefs.edit().putFloat("sfx_volume", volume).apply() }
    fun getSfxVolume(): Float = prefs.getFloat("sfx_volume", 1.0f)
    fun clearSoundEvent() { soundEvent = null }
    fun clearVibrationEvent() { vibrationEvent = false }

    fun updateAngle(touchX: Float, touchY: Float, screenWidth: Float, screenHeight: Float) {
        if (gameState != GameState.PLAYING || isPaused) return
        val centerX = screenWidth / 2f
        val dx = touchX - centerX; val dy = (screenHeight * 0.82f) - touchY
        shooterAngle = Math.toDegrees(atan2(dx, dy).toDouble()).toFloat().coerceIn(-80f, 80f)
    }

    open fun onShoot(spawnX: Float, spawnY: Float) {
        if (gameState != GameState.PLAYING || isPaused || activeProjectile != null) return
        shotTick++
        soundEvent = SoundType.SHOOT
        if (prefs.getBoolean("vibration_enabled", true)) vibrationEvent = true
        val angleRad = Math.toRadians(shooterAngle.toDouble())
        val speed = 40f

        val isFire = isFireballQueued
        activeProjectile = Projectile(
            x = spawnX,
            y = spawnY,
            color = nextBubbleColor,
            velocityX = (sin(angleRad) * speed).toFloat(),
            velocityY = (-cos(angleRad) * speed).toFloat(),
            isFireball = isFire
        )

        if (isFire) isFireballQueued = false

        nextBubbleColor = previewBubbleColor; previewBubbleColor = generateProjectileColor()
        startPhysicsLoop(metrics?.horizontalSpacing?.times(columnsCount) ?: 1080f)
    }

    // --- LÓGICA DE COLORES SEPARADA ---

    // Solo colores básicos para el tablero (NO Rainbow, NO Bombas)
    protected fun generateBoardBubbleColor(): BubbleColor {
        return BubbleColor.entries.filter { it != BubbleColor.BOMB && it != BubbleColor.RAINBOW }.random()
    }

    // Colores especiales permitidos solo en el cañón
    protected fun generateProjectileColor(): BubbleColor {
        val rand = Math.random()
        return when {
            rand < 0.025 -> BubbleColor.RAINBOW
            rand < 0.08 -> BubbleColor.BOMB
            else -> BubbleColor.entries.filter { it != BubbleColor.BOMB && it != BubbleColor.RAINBOW }.random()
        }
    }

    protected open fun updateHighScore() {
        val key = if (gameMode == GameMode.TIME_ATTACK) "high_score_time" else "high_score"
        val currentHigh = prefs.getInt(key, 0)
        if (score > currentHigh) { prefs.edit().putInt(key, score).apply(); if (gameMode == GameMode.CLASSIC) highScore = score; addCoins(10) }
        if (score >= 100) unlockAchievement("first_blood")
        if (score >= 1000) unlockAchievement("score_1000")
    }

    // --- FÍSICAS DE ALTA PRECISIÓN (SUB-STEPPING) ---
    protected fun startPhysicsLoop(screenWidth: Float) {
        viewModelScope.launch {
            val physicsSteps = 10

            while (activeProjectile != null) {
                if (isPaused) { delay(100); continue }
                val m = metrics ?: break

                var currentP = activeProjectile ?: break
                var collisionDetected = false

                repeat(physicsSteps) {
                    if (collisionDetected) return@repeat

                    val stepVx = currentP.velocityX / physicsSteps.toFloat()
                    val stepVy = currentP.velocityY / physicsSteps.toFloat()

                    var nextX = currentP.x + stepVx
                    var nextY = currentP.y + stepVy
                    var nextVx = currentP.velocityX

                    // 1. REBOTE EN PAREDES (NERF FUEGO)
                    if (nextX - bubbleRadius <= 0f) {
                        // Si es FUEGO, se destruye al tocar pared (NO rebota)
                        if (currentP.isFireball) {
                            activeProjectile = null; spawnExplosion(nextX, nextY, BubbleColor.RED); return@repeat
                        }
                        nextX = bubbleRadius
                        nextVx = abs(currentP.velocityX)
                    } else if (nextX + bubbleRadius >= screenWidth) {
                        // Si es FUEGO, se destruye al tocar pared (NO rebota)
                        if (currentP.isFireball) {
                            activeProjectile = null; spawnExplosion(nextX, nextY, BubbleColor.RED); return@repeat
                        }
                        nextX = screenWidth - bubbleRadius
                        nextVx = -abs(currentP.velocityX)
                    }

                    // 2. COLISIONES
                    if (currentP.isFireball) {
                        checkFireballDestruction(nextX, nextY)
                        // Destruir si sale por arriba
                        if (nextY < -bubbleRadius * 2) {
                            activeProjectile = null; return@repeat
                        }
                    } else {
                        if (nextY - bubbleRadius <= m.ceilingY) {
                            snapToGrid(nextX, nextY, currentP.color)
                            collisionDetected = true
                        } else if (checkSweepCollision(currentP.x, currentP.y, nextX, nextY)) {
                            snapToGrid(nextX, nextY, currentP.color)
                            collisionDetected = true
                        }
                    }

                    if (!collisionDetected && activeProjectile != null) {
                        currentP = currentP.copy(x = nextX, y = nextY, velocityX = nextVx)
                    }
                }

                if (!collisionDetected && activeProjectile != null) {
                    activeProjectile = currentP
                }

                delay(16)
            }
        }
    }

    private fun checkFireballDestruction(x: Float, y: Float) {
        val newGrid = bubblesByPosition.toMutableMap()
        val toRemove = mutableListOf<GridPosition>()
        var hitAny = false

        newGrid.forEach { (pos, _) ->
            val (bx, by) = getBubbleCenter(pos)
            // RADIO REDUCIDO A 1.1f para balancear
            if (hypot(x - bx, y - by) < bubbleRadius * 1.1f) {
                toRemove.add(pos)
            }
        }

        if (toRemove.isNotEmpty()) {
            hitAny = true
            soundEvent = SoundType.POP
            score += (toRemove.size * 10)

            toRemove.forEach { pos ->
                newGrid.remove(pos)
                val (cx, cy) = getBubbleCenter(pos)
                spawnExplosion(cx, cy, BubbleColor.entries.random())
            }

            bubblesByPosition = newGrid
            removeFloatingBubbles(newGrid)
        }
    }

    private fun startParticleLoop() {
        viewModelScope.launch {
            while (isActive) {
                if (!isPaused) {
                    particles.removeAll { it.life <= 0f }
                    for (i in particles.indices) { particles[i] = particles[i].copy(x = particles[i].x + particles[i].vx, y = particles[i].y + particles[i].vy + 1.5f, life = particles[i].life - 0.04f) }
                    floatingTexts.removeAll { it.life <= 0f }
                    for (i in floatingTexts.indices) { floatingTexts[i] = floatingTexts[i].copy(y = floatingTexts[i].y - 2.0f, life = floatingTexts[i].life - 0.02f) }
                }
                delay(16)
            }
        }
    }

    protected open fun startTimer() {
        timerJob?.cancel()
        timeLeft = 90
        timerJob = viewModelScope.launch {
            while (gameState == GameState.PLAYING) {
                delay(1000)
                if (!isPaused && gameMode == GameMode.TIME_ATTACK) {
                    timeLeft--
                    if (timeLeft <= 0) {
                        addRows(3); timeLeft = 90
                    }
                }
            }
        }
    }

    protected fun addRows(count: Int) {
        rowsDroppedCount += count
        val newGrid = mutableMapOf<GridPosition, Bubble>()
        bubblesByPosition.forEach { (pos, bubble) -> newGrid[GridPosition(pos.row + count, pos.col)] = bubble }
        for (r in 0 until count) { for (c in 0 until columnsCount) { newGrid[GridPosition(r, c)] = Bubble(color = generateBoardBubbleColor()) } }
        bubblesByPosition = newGrid; soundEvent = SoundType.STICK; removeFloatingBubbles(newGrid); metrics?.let { checkGameConditions(it) }
    }

    private fun snapToGrid(x: Float, y: Float, color: BubbleColor) {
        val m = metrics ?: return

        // --- SNAP CLÁSICO ---
        val estimatedRow = ((y - m.boardTopPadding) / m.verticalSpacing).roundToInt().coerceAtLeast(0)
        val xOffsetEst = if ((estimatedRow + rowsDroppedCount) % 2 != 0) (m.bubbleDiameter / 2f) else 0f
        val estimatedCol = ((x - (m.boardStartPadding + xOffsetEst)) / m.horizontalSpacing).roundToInt().coerceAtLeast(0)
        val estimatedPos = GridPosition(estimatedRow, estimatedCol)

        val finalPos = (getNeighborsAll(estimatedPos) + estimatedPos)
            .filter { pos -> pos.row >= 0 && pos.col >= 0 && pos.col < columnsCount && !bubblesByPosition.containsKey(pos) }
            .minByOrNull { pos ->
                val (cx, cy) = getBubbleCenter(pos)
                (x - cx)*(x - cx) + (y - cy)*(y - cy)
            } ?: estimatedPos

        val newGrid = bubblesByPosition.toMutableMap()
        if (color == BubbleColor.BOMB) { unlockAchievement("bomb_squad"); explodeAt(finalPos, newGrid) }
        else if (color == BubbleColor.RAINBOW) handleRainbowAt(finalPos, newGrid, x, y)
        else {
            newGrid[finalPos] = Bubble(color = color)
            if (matchFinder.findMatches(finalPos, newGrid, rowsDroppedCount).size >= 3) { joyTick++; processMatches(matchFinder.findMatches(finalPos, newGrid, rowsDroppedCount), newGrid, x, y, color) } else soundEvent = SoundType.STICK
        }
        bubblesByPosition = newGrid; activeProjectile = null; onPostSnap()
    }

    protected open fun onPostSnap() { }

    private fun processMatches(matches: Set<GridPosition>, grid: MutableMap<GridPosition, Bubble>, x: Float, y: Float, visualColor: BubbleColor) {
        soundEvent = SoundType.POP; val count = matches.size
        if (count >= 6) unlockAchievement("combo_master")
        val points = (count * 10) + ((count - 3) * 20); score += points; if (count >= 5) addCoins(count / 2)
        spawnFloatingText(x, y, "+$points"); updateHighScore()
        matches.forEach { pos -> val color = grid[pos]?.color ?: visualColor; grid.remove(pos); val (bx, by) = getBubbleCenter(pos); spawnExplosion(bx, by, color) }
        removeFloatingBubbles(grid)
    }

    private fun handleRainbowAt(pos: GridPosition, grid: MutableMap<GridPosition, Bubble>, fx: Float, fy: Float) {
        val adjacentColors = getNeighborsAll(pos).mapNotNull { grid[it]?.color }.distinct().filter { it != BubbleColor.BOMB && it != BubbleColor.RAINBOW }
        if (adjacentColors.isEmpty()) { grid[pos] = Bubble(color = generateBoardBubbleColor()); return }

        val toPop = mutableSetOf<GridPosition>(); toPop.add(pos)
        adjacentColors.forEach { c ->
            grid[pos] = Bubble(color = c);
            toPop.addAll(matchFinder.findMatches(pos, grid, rowsDroppedCount))
        }

        if (toPop.size >= 2) {
            joyTick++
            unlockAchievement("rainbow_power")
            processMatches(toPop, grid, fx, fy, BubbleColor.RAINBOW)
        }
        else {
            grid[pos] = Bubble(color = adjacentColors.first()); soundEvent = SoundType.STICK
        }
    }

    private fun explodeAt(center: GridPosition, grid: MutableMap<GridPosition, Bubble>) {
        soundEvent = SoundType.EXPLODE; val affected = getNeighborsAll(center).filter { grid.containsKey(it) } + center
        val (cx, cy) = getBubbleCenter(center)
        affected.forEach { pos -> grid[pos]?.let { spawnExplosion(getBubbleCenter(pos).first, getBubbleCenter(pos).second, it.color); grid.remove(pos) } }
        val points = affected.size * 50
        score += points; spawnFloatingText(cx, cy, "+$points"); updateHighScore(); joyTick++; removeFloatingBubbles(grid)
    }

    protected fun removeFloatingBubbles(grid: MutableMap<GridPosition, Bubble>) {
        val visited = mutableSetOf<GridPosition>(); val queue = ArrayDeque<GridPosition>()
        val ceiling = grid.keys.filter { it.row == 0 }; queue.addAll(ceiling); visited.addAll(ceiling)
        while (queue.isNotEmpty()) { val current = queue.removeFirst(); getNeighborsAll(current).filter { grid.containsKey(it) && it !in visited }.forEach { visited.add(it); queue.add(it) } }
        grid.keys.filter { it !in visited }.forEach { pos -> val b = grid[pos]; grid.remove(pos); val (bx, by) = getBubbleCenter(pos); spawnExplosion(bx, by, b?.color ?: BubbleColor.BLUE); addCoins(1); score += 20 }
    }

    protected fun getNeighborsAll(pos: GridPosition): List<GridPosition> {
        val offsets = if ((pos.row + rowsDroppedCount) % 2 == 0) listOf(-1 to -1, -1 to 0, 0 to -1, 0 to 1, 1 to -1, 1 to 0) else listOf(-1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to 0, 1 to 1)
        return offsets.map { GridPosition(pos.row + it.first, pos.col + it.second) }
    }

    protected fun checkGameConditions(m: BoardMetricsPx) {
        if (bubblesByPosition.isEmpty()) { gameState = GameState.WON; soundEvent = SoundType.WIN; addCoins(100) }
        val dangerY = m.boardTopPadding + (m.verticalSpacing * 12)
        if (bubblesByPosition.keys.any { getBubbleCenter(it).second + (m.bubbleDiameter/2f) >= dangerY }) { gameState = GameState.LOST; soundEvent = SoundType.LOSE }
    }

    private fun checkSweepCollision(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        val m = metrics ?: return false;
        val collideDist = m.bubbleDiameter * 0.65f
        return bubblesByPosition.keys.any { pos -> val (cx, cy) = getBubbleCenter(pos); distancePointToSegment(cx, cy, x1, y1, x2, y2) <= collideDist }
    }

    private fun distancePointToSegment(cx: Float, cy: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val abx = bx - ax; val aby = by - ay; val acx = cx - ax; val acy = cy - ay; val abLen2 = abx * abx + aby * aby
        if (abLen2 == 0f) return hypot(cx - ax, cy - ay)
        var t = (acx * abx + acy * aby) / abLen2; t = t.coerceIn(0f, 1f)
        return hypot(cx - (ax + t * abx), cy - (ay + t * aby))
    }

    protected fun getBubbleCenter(pos: GridPosition): Pair<Float, Float> {
        val m = metrics ?: return 0f to 0f
        val xOffset = if ((pos.row + rowsDroppedCount) % 2 != 0) (m.bubbleDiameter / 2f) else 0f
        return (m.boardStartPadding + xOffset + (pos.col * m.horizontalSpacing)) to (m.boardTopPadding + (pos.row * m.verticalSpacing))
    }

    fun spawnExplosion(cx: Float, cy: Float, color: BubbleColor) {
        repeat(12) {
            val angle = (Math.random() * 2 * Math.PI)
            val speed = 5.0 + (Math.random() * 10.0); val size = 10.0 + (Math.random() * 15.0)
            particles.add(GameParticle(particleIdCounter++, cx, cy, (cos(angle) * speed).toFloat(), (sin(angle) * speed).toFloat(), color, size.toFloat(), 1.0f))
        }
    }

    fun spawnFloatingText(x: Float, y: Float, text: String) {
        floatingTexts.add(FloatingText(id = textIdCounter++, x = x, y = y, text = text, life = 1.0f))
    }

    fun unlockAchievement(id: String) {
        val achievement = achievements.find { it.id == id }
        if (achievement != null && !achievement.isUnlocked) {
            achievement.isUnlocked = true; prefs.edit().putBoolean("ach_${id}", true).apply(); addCoins(50)
            viewModelScope.launch { activeAchievement = achievement; soundEvent = SoundType.WIN; delay(4000); activeAchievement = null }
        }
    }

    private fun setupAchievements() {
        achievements.addAll(listOf(
            Achievement("first_blood", "¡Primeros Pasos!", "Consigue tus primeros 100 puntos"),
            Achievement("combo_master", "¡Combo Brutal!", "Explota 6 o más burbujas a la vez"),
            Achievement("rainbow_power", "Poder Prismático", "Usa una burbuja Arcoíris con éxito"),
            Achievement("bomb_squad", "¡Boom!", "Detona una bomba"),
            Achievement("score_1000", "Leyenda", "Alcanza los 1000 puntos en una partida"),
            Achievement("secret_popper", "¡Curioso!", "Explotaste una burbuja del menú principal", isHidden = true)
        ))
    }

    private fun loadAchievements() {
        achievements.forEach { achievement ->
            if (prefs.getBoolean("ach_${achievement.id}", false)) {
                achievement.isUnlocked = true
            }
        }
    }
}
object Random { fun nextDouble(min: Double, max: Double): Double = min + (Math.random() * (max - min)) }