package com.example.orbblaze.ui.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbblaze.data.SettingsManager
import com.example.orbblaze.domain.engine.HexGridHelper
import com.example.orbblaze.domain.engine.LevelEngine
import com.example.orbblaze.domain.engine.MatchFinder
import com.example.orbblaze.domain.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.*

enum class GameState { IDLE, PLAYING, WON, LOST }
enum class GameMode { CLASSIC, TIME_ATTACK, ADVENTURE } 
enum class SoundType { SHOOT, POP, EXPLODE, STICK, WIN, LOSE, SWAP, ACHIEVEMENT }

open class GameViewModel(
    application: Application,
    protected val settingsManager: SettingsManager
) : AndroidViewModel(application) {

    protected val engine = LevelEngine()
    protected val matchFinder = MatchFinder()

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
    
    var highScore by mutableIntStateOf(0)
        protected set

    var coins by mutableIntStateOf(0)
        protected set

    var timeLeft by mutableIntStateOf(90) 
        protected set

    protected var timerJob: Job? = null
    private var physicsJob: Job? = null

    var shotsFiredCount by mutableIntStateOf(0)
        protected set

    var rowsDroppedCount by mutableIntStateOf(0)
        protected set

    var visualScrollOffset by mutableStateOf(0f)
    
    var joyTick by mutableIntStateOf(0)
        protected set

    var columnsCount by mutableIntStateOf(10)
        protected set

    var soundEvent by mutableStateOf<SoundType?>(null)
        protected set

    var vibrationEvent by mutableStateOf<Boolean>(false)
        protected set

    var shakeIntensity by mutableStateOf(0f)
        protected set

    val achievements = mutableStateListOf<Achievement>()

    var activeAchievement by mutableStateOf<Achievement?>(null)
        protected set

    var nextBubbleColor by mutableStateOf(BubbleColor.BLUE)
        protected set
    var previewBubbleColor by mutableStateOf(BubbleColor.RED)
        protected set

    var isFireballQueued by mutableStateOf(false)
        protected set

    val currentBubbleColor: BubbleColor get() = nextBubbleColor

    var shotTick by mutableIntStateOf(0)
        protected set
    var activeProjectile by mutableStateOf<Projectile?>(null)
        protected set

    // ✅ Listas de partículas y textos (Fix para recomposición fluida)
    var particles by mutableStateOf<List<GameParticle>>(emptyList())
        protected set
    var floatingTexts by mutableStateOf<List<FloatingText>>(emptyList())
        protected set

    var metrics: BoardMetricsPx? = null
        protected set
        
    private var particleIdCounter = 0L
    private var textIdCounter = 0L

    protected val bubbleRadius: Float get() = (metrics?.bubbleDiameter ?: 44f) / 2f

    var dynamicDangerRow by mutableIntStateOf(12)

    var comboMultiplier by mutableIntStateOf(1)
        protected set

    var trajectoryPoints by mutableStateOf<List<Offset>>(emptyList())
        protected set

    init {
        setupAchievements()
        observeData()
        startParticleLoop()
        startShakeDecayLoop()
    }

    private fun observeData() {
        viewModelScope.launch {
            settingsManager.highScoreFlow.collectLatest { if (gameMode == GameMode.CLASSIC) highScore = it }
        }
        viewModelScope.launch {
            settingsManager.coinsFlow.collectLatest { coins = it }
        }
        viewModelScope.launch {
            achievements.forEach { achievement ->
                launch {
                    settingsManager.isAchievementUnlocked(achievement.id).collectLatest { unlocked ->
                        achievement.isUnlocked = unlocked
                    }
                }
            }
        }
    }

    fun triggerShake(intensity: Float) {
        shakeIntensity = (shakeIntensity + intensity).coerceAtMost(15f)
    }

    private fun startShakeDecayLoop() {
        viewModelScope.launch {
            while (isActive) {
                if (shakeIntensity > 0) {
                    shakeIntensity = (shakeIntensity - 0.8f).coerceAtLeast(0f)
                }
                delay(16)
            }
        }
    }

    fun addCoins(amount: Int) { 
        viewModelScope.launch { settingsManager.setCoins(coins + amount) }
    }
    
    fun spendCoins(amount: Int, onResult: (Boolean) -> Unit) { 
        if (coins >= amount) { 
            viewModelScope.launch { 
                settingsManager.setCoins(coins - amount)
                onResult(true)
            }
        } else {
            onResult(false)
        }
    }

    fun buyFireball() {
        spendCoins(1000) { success ->
            if (success) {
                isFireballQueued = true
                soundEvent = SoundType.SWAP
            }
        }
    }

    open fun changeGameMode(mode: GameMode) { this.gameMode = mode }
    open fun startGame() { 
        gameState = GameState.PLAYING
        comboMultiplier = 1
        nextBubbleColor = engine.getSmartProjectileColor(bubblesByPosition)
        previewBubbleColor = engine.getSmartProjectileColor(bubblesByPosition)
        startTimer() 
    }

    open fun loadLevel(initialRows: Int = 6) {
        columnsCount = 10 
        engine.setupInitialLevel(rows = initialRows, cols = columnsCount)
        val cleanGrid = engine.gridState.toMutableMap()
        cleanGrid.forEach { (pos, bubble) ->
            if (bubble.color == BubbleColor.RAINBOW || bubble.color == BubbleColor.BOMB) {
                cleanGrid[pos] = bubble.copy(color = engine.generateBaseColor())
            }
        }
        bubblesByPosition = cleanGrid
        
        score = 0
        comboMultiplier = 1
        gameState = GameState.IDLE
        isPaused = false
        shotsFiredCount = 0
        rowsDroppedCount = 0
        visualScrollOffset = 0f
        timeLeft = 90 
        isFireballQueued = false
        physicsJob?.cancel()
        activeProjectile = null
        shooterAngle = 0f
        shotTick = 0
        joyTick = 0
        shakeIntensity = 0f
        trajectoryPoints = emptyList()
        
        particles = emptyList()
        floatingTexts = emptyList()
        timerJob?.cancel()
    }

    open fun restartGame() { loadLevel(if (gameMode == GameMode.TIME_ATTACK) 3 else 6) }
    
    fun swapBubbles() { 
        if (activeProjectile == null && gameState == GameState.PLAYING && !isPaused) { 
            val temp = nextBubbleColor
            nextBubbleColor = previewBubbleColor
            previewBubbleColor = temp
            soundEvent = SoundType.SWAP
            viewModelScope.launch {
                if (settingsManager.vibrationEnabledFlow.first()) vibrationEvent = true
            }
            updateTrajectory()
        } 
    }
    
    fun setBoardMetrics(metrics: BoardMetricsPx) { 
        this.metrics = metrics 
        updateTrajectory() 
    }

    fun togglePause() { if (gameState == GameState.PLAYING) isPaused = !isPaused }
    fun setSfxVolume(volume: Float) { viewModelScope.launch { settingsManager.setSfxVolume(volume) } }
    fun clearSoundEvent() { soundEvent = null }
    fun clearVibrationEvent() { vibrationEvent = false }

    fun updateAngle(touchX: Float, touchY: Float, screenWidth: Float, screenHeight: Float) {
        if (gameState != GameState.PLAYING || isPaused) return
        val dx = touchX - (screenWidth / 2f)
        val pivotY = metrics?.pivotY ?: (screenHeight * 0.82f)
        val dy = pivotY - touchY
        
        shooterAngle = Math.toDegrees(atan2(dx, dy).toDouble()).toFloat().coerceIn(-80f, 80f)
        updateTrajectory()
    }

    fun updateTrajectory() {
        val m = metrics ?: return
        val angleRad = Math.toRadians(shooterAngle.toDouble())
        
        val pivotX = m.screenWidth / 2f
        val pivotY = m.pivotY
        var curX = pivotX + (sin(angleRad) * m.barrelLength).toFloat()
        var curY = pivotY - (cos(angleRad) * m.barrelLength).toFloat()
        
        val speed = 35f 
        var vx = sin(angleRad).toFloat() * speed
        var vy = -cos(angleRad).toFloat() * speed
        
        val points = mutableListOf<Offset>()
        val leftWall = m.boardStartPadding - bubbleRadius
        val rightWall = m.screenWidth - (m.boardStartPadding - bubbleRadius)
        
        val collisionThreshold = m.bubbleDiameter * GameConstants.BUBBLE_COLLISION_SCALE
        
        for (i in 0 until 120) {
            val prevX = curX
            val prevY = curY
            curX += vx
            curY += vy
            
            if (curX <= leftWall || curX >= rightWall) {
                vx = -vx
                curX = curX.coerceIn(leftWall, rightWall)
            }
            
            val hit = bubblesByPosition.keys.any { pos ->
                val (bx, by) = getBubbleCenter(pos)
                distancePointToSegment(bx, by, prevX, prevY, curX, curY) < collisionThreshold
            }
            
            val ceiling = m.ceilingY + (if(gameMode == GameMode.ADVENTURE) visualScrollOffset else 0f)
            
            if (hit || curY < ceiling) {
                points.add(Offset(curX, curY))
                break
            }
            
            if (i % 3 == 0) points.add(Offset(curX, curY))
        }
        trajectoryPoints = points
    }

    open fun onShoot(spawnX: Float, spawnY: Float) {
        if (gameState != GameState.PLAYING || isPaused || activeProjectile != null) return
        shotTick++; soundEvent = SoundType.SHOOT
        viewModelScope.launch {
            if (settingsManager.vibrationEnabledFlow.first()) vibrationEvent = true
        }
        val angleRad = Math.toRadians(shooterAngle.toDouble())
        val speed = GameConstants.PROJECTILE_SPEED 
        
        activeProjectile = Projectile(spawnX, spawnY, nextBubbleColor, (sin(angleRad) * speed).toFloat(), (-cos(angleRad) * speed).toFloat(), isFireballQueued)
        
        isFireballQueued = false
        nextBubbleColor = previewBubbleColor
        previewBubbleColor = engine.getSmartProjectileColor(bubblesByPosition)
        trajectoryPoints = emptyList() 
        startPhysicsLoop()
    }

    protected open fun updateHighScore() {
        viewModelScope.launch {
            val isTimeMode = gameMode == GameMode.TIME_ATTACK
            val currentHigh = if (isTimeMode) settingsManager.highScoreTimeFlow.first() else settingsManager.highScoreFlow.first()
            
            if (score > currentHigh) { 
                if (isTimeMode) settingsManager.setHighScoreTime(score) else settingsManager.setHighScore(score)
                addCoins(10) 
            }
            if (score >= 100) unlockAchievement("first_blood")
            if (score >= 1000) unlockAchievement("score_1000")
        }
    }

    protected fun startPhysicsLoop() {
        physicsJob?.cancel()
        physicsJob = viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            while (isActive && activeProjectile != null) {
                if (isPaused) { 
                    delay(100)
                    lastTime = System.currentTimeMillis()
                    continue 
                }
                val m = metrics ?: break
                val currentTime = System.currentTimeMillis()
                val deltaTime = (currentTime - lastTime) / 1000f
                lastTime = currentTime

                val leftWall = m.boardStartPadding - bubbleRadius
                val rightWall = m.screenWidth - (m.boardStartPadding - bubbleRadius)
                
                var currentP = activeProjectile ?: break
                var collisionDetected = false

                val subSteps = GameConstants.PHYSICS_STEPS
                val stepDelta = deltaTime / subSteps

                repeat(subSteps) {
                    if (collisionDetected || !isActive) return@repeat
                    
                    var nextX = currentP.x + currentP.velocityX * stepDelta
                    var nextY = currentP.y + currentP.velocityY * stepDelta
                    var nextVx = currentP.velocityX
                    
                    if (nextX - bubbleRadius <= leftWall || nextX + bubbleRadius >= rightWall) {
                        if (currentP.isFireball) { 
                            activeProjectile = null
                            spawnExplosion(nextX, nextY, BubbleColor.RED)
                            triggerShake(10f)
                            collisionDetected = true
                            return@repeat 
                        }
                        nextX = if (nextX - bubbleRadius <= leftWall) leftWall + bubbleRadius else rightWall - bubbleRadius
                        nextVx = -currentP.velocityX
                    }

                    if (currentP.isFireball) {
                        checkFireballDestruction(nextX, nextY)
                        if (nextY < -bubbleRadius * 2) { 
                            activeProjectile = null
                            collisionDetected = true
                            return@repeat 
                        }
                    } else {
                        val ceiling = m.ceilingY + if (gameMode == GameMode.ADVENTURE) visualScrollOffset else 0f
                        
                        // ✅ FIX "TRASPASO": Detección de techo y burbujas con snap corregido
                        if (nextY - bubbleRadius <= ceiling) {
                            // Si golpea el techo, forzamos la posición para evitar que traspase el UI
                            snapToGrid(nextX, ceiling + bubbleRadius, currentP.color)
                            collisionDetected = true
                        } else if (checkSweepCollision(currentP.x, currentP.y, nextX, nextY)) {
                            snapToGrid(nextX, nextY, currentP.color)
                            collisionDetected = true
                        }
                    }
                    if (!collisionDetected) {
                        currentP = currentP.copy(x = nextX, y = nextY, velocityX = nextVx)
                    }
                }
                
                if (isActive) {
                    if (!collisionDetected) activeProjectile = currentP else activeProjectile = null
                }
                delay(8) 
            }
        }
    }

    private fun checkFireballDestruction(x: Float, y: Float) {
        val newGrid = bubblesByPosition.toMutableMap(); val toRemove = mutableListOf<GridPosition>()
        newGrid.forEach { (pos, _) ->
            val (bx, by) = getBubbleCenter(pos)
            if (hypot(x - bx, y - by) < bubbleRadius * 1.1f) toRemove.add(pos)
        }
        if (toRemove.isNotEmpty()) {
            soundEvent = SoundType.POP; 
            val pts = (toRemove.size * 10) * comboMultiplier
            score += pts
            triggerShake(pts.toFloat() / 50f)
            toRemove.forEach { pos -> newGrid.remove(pos); val (cx, cy) = getBubbleCenter(pos); spawnExplosion(cx, cy, BubbleColor.entries.random()) }
            bubblesByPosition = newGrid; removeFloatingBubbles(newGrid)
        }
    }

    private fun startParticleLoop() {
        viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            while (isActive) {
                val currentTime = System.currentTimeMillis()
                val deltaTime = ((currentTime - lastTime) / 1000f).coerceIn(0.001f, 0.05f)
                lastTime = currentTime

                if (!isPaused) {
                    // ✅ Actualización de partículas
                    if (particles.isNotEmpty()) {
                        particles = particles.mapNotNull { p ->
                            if (p.life <= 0f) null
                            else p.copy(
                                x = p.x + p.vx * deltaTime * 60f,
                                y = p.y + (p.vy + GameConstants.PARTICLE_GRAVITY * deltaTime) * deltaTime * 60f,
                                life = p.life - GameConstants.PARTICLE_LIFE_DECAY * deltaTime
                            )
                        }
                    }

                    // ✅ Actualización de textos flotantes (Fix: No se quedan pegados)
                    if (floatingTexts.isNotEmpty()) {
                        floatingTexts = floatingTexts.mapNotNull { t ->
                            if (t.life <= 0f) null
                            else t.copy(
                                y = t.y - GameConstants.TEXT_FLOAT_SPEED * deltaTime,
                                life = t.life - GameConstants.TEXT_LIFE_DECAY * deltaTime
                            )
                        }
                    }
                }
                delay(16)
            }
        }
    }

    protected open fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (gameState == GameState.PLAYING) {
                delay(1000)
                if (!isPaused && gameMode == GameMode.TIME_ATTACK) {
                    timeLeft--; if (timeLeft <= 0) { 
                        addRows(GameConstants.TIME_ATTACK_PENALTY_ROWS)
                        timeLeft = GameConstants.TIME_ATTACK_INITIAL 
                    }
                }
            }
        }
    }

    protected fun addRows(count: Int) {
        rowsDroppedCount += count; val newGrid = mutableMapOf<GridPosition, Bubble>()
        bubblesByPosition.forEach { (pos, bubble) -> newGrid[GridPosition(pos.row + count, pos.col)] = bubble }
        for (r in 0 until count) for (c in 0 until columnsCount) newGrid[GridPosition(r, c)] = Bubble(color = engine.generateBaseColor())
        bubblesByPosition = newGrid; soundEvent = SoundType.STICK; removeFloatingBubbles(newGrid); metrics?.let { checkGameConditions(it) }
    }

    private fun snapToGrid(x: Float, y: Float, color: BubbleColor) {
        val m = metrics ?: return
        val newGrid = bubblesByPosition.toMutableMap()

        val offset = if (gameMode == GameMode.ADVENTURE) visualScrollOffset else 0f
        
        // ✅ FIX "TRASPASO": Forzamos a que la fila sea como mínimo 0
        val estRow = ((y - m.boardTopPadding - offset) / m.verticalSpacing).roundToInt().coerceAtLeast(0)
        
        val candidates = mutableListOf<GridPosition>()
        // ✅ Aumentamos rango de búsqueda para evitar que burbujas rápidas se pierdan
        for (r in (estRow - 1)..(estRow + 2)) {
            if (r < 0) continue // Nunca permitir burbujas por encima de la fila 0
            val actualCols = if ((r + rowsDroppedCount) % 2 == 0) columnsCount else columnsCount - 1
            for (c in 0 until actualCols) {
                val p = GridPosition(r, c)
                if (!newGrid.containsKey(p)) candidates.add(p)
            }
        }

        if (candidates.isEmpty()) { activeProjectile = null; return }

        val finalPos = candidates.minByOrNull { pos ->
            val (cx, cy) = getBubbleCenter(pos)
            val dx = x - cx
            val dy = y - cy
            val magneticBias = if (dy > 0) GameConstants.MAGNETIC_BIAS_LOW else GameConstants.MAGNETIC_BIAS_HIGH
            dx * dx + (dy * dy * magneticBias)
        }!!

        var matched = false
        when (color) {
            BubbleColor.BOMB -> { 
                unlockAchievement("bomb_squad")
                explodeAt(finalPos, newGrid) 
                triggerShake(8f)
                matched = true
            }
            BubbleColor.RAINBOW -> {
                matched = handleRainbowAt(finalPos, newGrid, x, y)
            }
            else -> {
                newGrid[finalPos] = Bubble(color = color)
                val matches = matchFinder.findMatches(finalPos, newGrid, rowsDroppedCount)
                if (matches.size >= 3) { 
                    matched = true
                    joyTick++
                    if (matches.size >= 6) triggerShake(matches.size * 0.8f)
                    processMatches(matches, newGrid, x, y, color) 
                } else {
                    soundEvent = SoundType.STICK
                }
            }
        }
        
        if (matched) {
            comboMultiplier++
        } else {
            comboMultiplier = 1
        }
        
        bubblesByPosition = newGrid
        if (matched) {
            removeFloatingBubbles(newGrid.toMutableMap())
        }
        
        activeProjectile = null; onPostSnap()
    }

    protected open fun onPostSnap() {
        validateProjectileColors()
        metrics?.let { checkGameConditions(it) }
    }

    protected fun validateProjectileColors() {
        if (bubblesByPosition.isEmpty()) return
        
        val boardColors = bubblesByPosition.values.map { it.color }.distinct()
            .filter { it != BubbleColor.RAINBOW && it != BubbleColor.BOMB }
        
        if (boardColors.isNotEmpty()) {
            if (nextBubbleColor !in boardColors && nextBubbleColor != BubbleColor.RAINBOW && nextBubbleColor != BubbleColor.BOMB) {
                nextBubbleColor = engine.getSmartProjectileColor(bubblesByPosition)
            }
            if (previewBubbleColor !in boardColors && previewBubbleColor != BubbleColor.RAINBOW && previewBubbleColor != BubbleColor.BOMB) {
                previewBubbleColor = engine.getSmartProjectileColor(bubblesByPosition)
            }
        }
    }

    private fun processMatches(matches: Set<GridPosition>, grid: MutableMap<GridPosition, Bubble>, x: Float, y: Float, visualColor: BubbleColor) {
        soundEvent = SoundType.POP; val count = matches.size
        if (count >= 6) unlockAchievement("combo_master")
        
        val basePoints = (count * 10) + ((count - 3) * 20)
        val points = basePoints * comboMultiplier
        
        score += points
        if (count >= 5) addCoins(count / 2)
        
        val comboText = if (comboMultiplier > 1) " COMBO x$comboMultiplier!" else ""
        spawnFloatingText(x, y, "+$points$comboText")
        
        updateHighScore()
        matches.forEach { pos -> val color = grid[pos]?.color ?: visualColor; grid.remove(pos); val (bx, by) = getBubbleCenter(pos); spawnExplosion(bx, by, color) }
    }

    private fun handleRainbowAt(pos: GridPosition, grid: MutableMap<GridPosition, Bubble>, fx: Float, fy: Float): Boolean {
        val adjacentColors = HexGridHelper.getNeighbors(pos, rowsDroppedCount).mapNotNull { grid[it]?.color }.distinct().filter { it != BubbleColor.BOMB && it != BubbleColor.RAINBOW }
        if (adjacentColors.isEmpty()) { grid[pos] = Bubble(color = engine.generateBaseColor()); return false }
        val toPop = mutableSetOf<GridPosition>().apply { add(pos) }
        adjacentColors.forEach { c -> grid[pos] = Bubble(color = c); toPop.addAll(matchFinder.findMatches(pos, grid, rowsDroppedCount)) }
        return if (toPop.size >= 2) { 
            joyTick++
            if (toPop.size >= 6) triggerShake(toPop.size * 0.8f)
            unlockAchievement("rainbow_power")
            processMatches(toPop, grid, fx, fy, BubbleColor.RAINBOW) 
            true
        }
        else { 
            grid[pos] = Bubble(color = adjacentColors.first())
            soundEvent = SoundType.STICK
            false
        }
    }

    private fun explodeAt(center: GridPosition, grid: MutableMap<GridPosition, Bubble>) {
        soundEvent = SoundType.EXPLODE; val affected = HexGridHelper.getNeighbors(center, rowsDroppedCount).filter { grid.containsKey(it) } + center
        val (cx, cy) = getBubbleCenter(center)
        affected.forEach { pos -> grid[pos]?.let { spawnExplosion(getBubbleCenter(pos).first, getBubbleCenter(pos).second, it.color); grid.remove(pos) } }
        
        val points = (affected.size * 50) * comboMultiplier
        score += points
        spawnFloatingText(cx, cy, "+$points")
        updateHighScore()
        joyTick++
    }

    protected fun removeFloatingBubbles(grid: MutableMap<GridPosition, Bubble>) {
        val floating = engine.findFloatingBubbles(grid, rowsDroppedCount)
        if (floating.isEmpty()) return
        
        if (floating.size >= 10) triggerShake(floating.size * 0.3f)
        floating.forEach { pos -> 
            val b = grid[pos]
            grid.remove(pos)
            val (bx, by) = getBubbleCenter(pos)
            spawnExplosion(bx, by, b?.color ?: BubbleColor.BLUE)
            addCoins(1)
            score += (20 * comboMultiplier)
        }
        bubblesByPosition = grid.toMap()
    }

    protected fun checkGameConditions(m: BoardMetricsPx) {
        if (gameState != GameState.PLAYING) return
        
        if (bubblesByPosition.isEmpty()) { 
            gameState = GameState.WON; soundEvent = SoundType.WIN; addCoins(100)
            return
        }
        
        val dangerY = m.boardTopPadding + (m.verticalSpacing * dynamicDangerRow)
        
        val hasLost = bubblesByPosition.keys.any { pos ->
            val center = getBubbleCenter(pos)
            val bubbleBottomY = center.second + (m.bubbleDiameter / 2.2f)
            bubbleBottomY >= dangerY
        }

        if (hasLost) {
            gameState = GameState.LOST 
            soundEvent = SoundType.LOSE 
        }
    }

    private fun checkSweepCollision(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        val m = metrics ?: return false
        val threshold = m.bubbleDiameter * GameConstants.BUBBLE_COLLISION_SCALE
        
        val minY = min(y1, y2) - threshold
        val maxY = max(y1, y2) + threshold

        return bubblesByPosition.keys.any { pos -> 
            val (bx, by) = getBubbleCenter(pos)
            if (by in minY..maxY) {
                distancePointToSegment(bx, by, x1, y1, x2, y2) <= threshold
            } else false
        }
    }

    private fun distancePointToSegment(cx: Float, cy: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val abx = bx - ax; val aby = by - ay; val acx = cx - ax; val acy = cy - ay; val abLen2 = abx * abx + aby * aby
        if (abLen2 == 0f) return hypot(cx - ax, cy - ay)
        var t = (acx * abx + acy * aby) / abLen2; t = t.coerceIn(0f, 1f)
        return hypot(cx - (ax + t * abx), cy - (ay + t * aby))
    }

    fun getBubbleCenter(pos: GridPosition): Pair<Float, Float> {
        return metrics?.let { 
            val base = HexGridHelper.getBubbleCenter(pos, it, rowsDroppedCount)
            val offset = if (gameMode == GameMode.ADVENTURE) visualScrollOffset else 0f
            base.first to (base.second + offset)
        } ?: (0f to 0f)
    }

    fun spawnExplosion(cx: Float, cy: Float, color: BubbleColor) {
        repeat(12) {
            val angle = Math.random() * 2 * Math.PI; val speed = 5.0 + (Math.random() * 10.0)
            particles = particles + GameParticle(particleIdCounter++, cx, cy, (cos(angle) * speed).toFloat(), (sin(angle) * speed).toFloat(), color, (10.0 + Math.random() * 15.0).toFloat(), 1.0f)
        }
    }

    fun spawnFloatingText(x: Float, y: Float, text: String) { 
        floatingTexts = floatingTexts + FloatingText(textIdCounter++, x, y, text, 1.0f) 
    }

    fun unlockAchievement(id: String) {
        val achievement = achievements.find { it.id == id }
        if (achievement != null && !achievement.isUnlocked) {
            viewModelScope.launch {
                settingsManager.unlockAchievement(id)
                addCoins(50)
                activeAchievement = achievement; soundEvent = SoundType.ACHIEVEMENT; delay(4000); activeAchievement = null
            }
        }
    }

    private fun setupAchievements() {
        achievements.clear()
        achievements.addAll(listOf(
            Achievement("first_blood", "¡Primeros Pasos!", "Consigue tus primeros 100 puntos"),
            Achievement("combo_master", "¡Combo Brutal!", "Explota 6 o más burbujas a la vez"),
            Achievement("rainbow_power", "Poder Prismático", "Usa una burbuja Arcoíris con éxito"),
            Achievement("bomb_squad", "¡Boom!", "Detona una bomba"),
            Achievement("score_1000", "Leyenda", "Alcanza los 1000 puntos en una partida"),
            Achievement("secret_popper", "¡Curioso!", "Explotaste una burbuja del menú principal", isHidden = true)
        ))
    }
}
