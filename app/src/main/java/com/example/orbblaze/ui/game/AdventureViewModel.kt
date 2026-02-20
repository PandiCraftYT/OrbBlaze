package com.example.orbblaze.ui.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.orbblaze.data.SettingsManager
import com.example.orbblaze.domain.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AdventureViewModel(
    application: Application,
    settingsManager: SettingsManager
) : GameViewModel(application, settingsManager) {
    
    var currentLevelId by mutableIntStateOf(1)
        private set
    
    var shotsRemaining by mutableIntStateOf(0)
        private set

    var starsEarned by mutableIntStateOf(0)
        private set

    var showReviveAlert by mutableStateOf(false)

    private var currentLevelObj: Level? = null
    private var cascadeJob: Job? = null

    init {
        changeGameMode(GameMode.ADVENTURE)
    }

    override fun restartGame() {
        loadAdventureLevel(currentLevelId)
    }

    fun loadAdventureLevel(levelId: Int) {
        val level = AdventureLevels.levels.find { it.id == levelId } ?: return
        currentLevelId = levelId
        currentLevelObj = level

        val layout = level.layout.filter { it.isNotEmpty() }
        val maxLayoutWidth = layout.maxOfOrNull { it.trimEnd().length } ?: 10
        columnsCount = maxLayoutWidth.coerceAtLeast(10)

        val newGrid = mutableMapOf<GridPosition, Bubble>()
        layout.forEachIndexed { row, rowText ->
            rowText.forEachIndexed { charIndex, char ->
                val color = charToColor(char)
                if (color != null && charIndex < columnsCount) {
                    newGrid[GridPosition(row, charIndex)] = Bubble(color = color)
                }
            }
        }
        
        if (levelId >= 31) {
            val patternText = layout[0]
            patternText.forEachIndexed { charIndex, char ->
                val color = charToColor(char)
                if (color != null && charIndex < columnsCount) {
                    newGrid[GridPosition(-1, charIndex)] = Bubble(color = color)
                }
            }
        }
        
        bubblesByPosition = newGrid
        shotsRemaining = level.maxShots
        score = 0
        starsEarned = 0
        gameState = GameState.IDLE
        isPaused = false
        showReviveAlert = false
        rowsDroppedCount = 0
        visualScrollOffset = 0f
        
        // ✅ AVENTURA: No permitir bombas ni arcoíris
        nextBubbleColor = engine.getSmartProjectileColor(bubblesByPosition, allowSpecials = false)
        previewBubbleColor = engine.getSmartProjectileColor(bubblesByPosition, allowSpecials = false)
        cascadeJob?.cancel()
    }

    private fun charToColor(char: Char): BubbleColor? = when(char) {
        'R' -> BubbleColor.RED; 'B' -> BubbleColor.BLUE; 'G' -> BubbleColor.GREEN
        'Y' -> BubbleColor.YELLOW; 'P' -> BubbleColor.PURPLE; 'C' -> BubbleColor.CYAN
        else -> null
    }

    override fun startGame() {
        super.startGame()
        if (currentLevelId >= 31) startCascadeLoop()
    }

    private fun startCascadeLoop() {
        cascadeJob?.cancel()
        cascadeJob = viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            while (isActive) {
                val currentTime = System.currentTimeMillis()
                val deltaTime = (currentTime - lastTime) / 1000f
                lastTime = currentTime

                if (gameState == GameState.PLAYING && !isPaused && !showReviveAlert) {
                    val m = metrics
                    if (m != null) {
                        visualScrollOffset += 80f * deltaTime
                        checkAdventureDefeat(m)
                        if (visualScrollOffset >= m.verticalSpacing && gameState == GameState.PLAYING) {
                            anchorVisualOffset(m.verticalSpacing)
                        }
                    }
                }
                delay(16)
            }
        }
    }

    private fun anchorVisualOffset(rowHeight: Float) {
        val level = currentLevelObj ?: return
        val layout = level.layout.filter { it.isNotEmpty() }
        val newGrid = mutableMapOf<GridPosition, Bubble>()
        
        bubblesByPosition.forEach { (pos, bubble) ->
            if (pos.row < 30) newGrid[GridPosition(pos.row + 1, pos.col)] = bubble
        }
        
        val nextRowIndex = (rowsDroppedCount + 1) % layout.size
        val patternText = layout[nextRowIndex]
        val colors = listOf('R', 'B', 'G', 'Y', 'P', 'C')
        val colorOffset = (rowsDroppedCount / layout.size) % colors.size
        val horizontalShift = rowsDroppedCount % patternText.length

        patternText.forEachIndexed { charIndex, char ->
            val shiftedIdx = (charIndex + horizontalShift) % patternText.length
            val colorChar = patternText[shiftedIdx]
            val finalColorChar = if (colorChar in colors) {
                val cIdx = colors.indexOf(colorChar)
                colors[(cIdx + colorOffset) % colors.size]
            } else colorChar

            val color = charToColor(finalColorChar)
            if (color != null && charIndex < columnsCount) {
                newGrid[GridPosition(-1, charIndex)] = Bubble(color = color)
            }
        }

        removeFloatingBubbles(newGrid)
        bubblesByPosition = newGrid
        rowsDroppedCount++
        visualScrollOffset -= rowHeight
    }

    override fun onShoot(spawnX: Float, spawnY: Float) {
        if (showReviveAlert || isPaused) return
        super.onShoot(spawnX, spawnY)
        // ✅ AVENTURA: No permitir bombas ni arcoíris en la vista previa
        previewBubbleColor = engine.getSmartProjectileColor(bubblesByPosition, allowSpecials = false)
    }

    override fun onPostSnap() {
        shotsRemaining--
        if (shotsRemaining <= 0) {
            evaluateFinishingStatus()
        } else {
            if (bubblesByPosition.isEmpty() && currentLevelId <= 30) {
                starsEarned = 0
                gameState = GameState.WON
                soundEvent = SoundType.WIN
                saveProgress()
            } else {
                if (!bubblesByPosition.values.any { it.color == nextBubbleColor }) {
                    // ✅ AVENTURA: No permitir bombas ni arcoíris al validar colores
                    nextBubbleColor = engine.getSmartProjectileColor(bubblesByPosition, allowSpecials = false)
                }
            }
        }
    }

    private fun evaluateFinishingStatus() {
        val level = currentLevelObj ?: return
        if (currentLevelId > 30) {
            if (score >= level.star1Threshold) {
                starsEarned = calculateStars(score, level)
                gameState = GameState.WON
                soundEvent = SoundType.WIN
                saveProgress()
            } else {
                gameState = GameState.LOST
                soundEvent = SoundType.LOSE
            }
        } else {
            if (bubblesByPosition.isEmpty()) {
                gameState = GameState.WON
                soundEvent = SoundType.WIN
                saveProgress()
            } else {
                gameState = GameState.LOST
                soundEvent = SoundType.LOSE
            }
        }
        cascadeJob?.cancel()
    }

    private fun checkAdventureDefeat(m: BoardMetricsPx) {
        val dangerY = m.boardTopPadding + (m.verticalSpacing * 13)
        if (bubblesByPosition.keys.any { getBubbleCenter(it).second + (m.bubbleDiameter/2f) >= dangerY }) { 
            val level = currentLevelObj
            if (level != null && currentLevelId > 30 && score >= level.star1Threshold) {
                evaluateFinishingStatus()
            } else {
                gameState = GameState.LOST 
                soundEvent = SoundType.LOSE 
                cascadeJob?.cancel()
            }
        }
    }

    private fun calculateStars(finalScore: Int, level: Level): Int {
        if (currentLevelId <= 30) return 0
        return when {
            finalScore >= level.star3Threshold -> 3
            finalScore >= level.star2Threshold -> 2
            finalScore >= level.star1Threshold -> 1
            else -> 0
        }
    }

    private fun saveProgress() {
        viewModelScope.launch {
            val completedLevels = settingsManager.adventureProgressFlow.first()
            if (currentLevelId > completedLevels) {
                settingsManager.setAdventureProgress(currentLevelId)
            }
            if (currentLevelId > 30) {
                settingsManager.setLevelStars(currentLevelId, starsEarned)
            }
        }
    }

    fun reviveWithAd() {
        val currentBubbles = bubblesByPosition.toMutableMap()
        val maxRow = currentBubbles.keys.maxOfOrNull { it.row } ?: 0
        
        val toRemove = currentBubbles.keys.filter { it.row >= maxRow - 5 }
        toRemove.forEach { currentBubbles.remove(it) }
        
        shotsRemaining = 15 
        bubblesByPosition = currentBubbles
        removeFloatingBubbles(currentBubbles)
        
        gameState = GameState.PLAYING
        isPaused = true 
        showReviveAlert = true
        
        if (currentLevelId >= 31) startCascadeLoop()
    }
}
