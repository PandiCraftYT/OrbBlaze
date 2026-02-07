package com.example.orbblaze.ui.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.orbblaze.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AdventureViewModel(application: Application) : GameViewModel(application) {
    
    var currentLevelId by mutableIntStateOf(1)
        private set
    
    var shotsRemaining by mutableIntStateOf(0)
        private set

    init {
        changeGameMode(GameMode.ADVENTURE)
    }

    override fun restartGame() {
        loadAdventureLevel(currentLevelId)
    }

    fun loadAdventureLevel(levelId: Int) {
        val level = AdventureLevels.levels.find { it.id == levelId } ?: return
        currentLevelId = levelId

        val layout = level.layout.filter { it.isNotEmpty() }
        
        // ✅ CALCULAR ANCHO DINÁMICO
        // Buscamos la línea más larga del diseño para determinar el número de columnas
        val maxLayoutWidth = layout.maxOfOrNull { it.trimEnd().length } ?: 10
        // Ajustamos columnsCount (mínimo 10 para mantener consistencia con el modo clásico)
        columnsCount = maxLayoutWidth.coerceAtLeast(10)

        val newGrid = mutableMapOf<GridPosition, Bubble>()
        layout.forEachIndexed { row, rowText ->
            val isOffsetRow = (row + rowsDroppedCount) % 2 != 0
            val rowMaxCols = if (isOffsetRow) columnsCount else columnsCount + 1
            
            rowText.forEachIndexed { charIndex, char ->
                val color = when(char) {
                    'R' -> BubbleColor.RED
                    'B' -> BubbleColor.BLUE
                    'G' -> BubbleColor.GREEN
                    'Y' -> BubbleColor.YELLOW
                    'P' -> BubbleColor.PURPLE
                    'C' -> BubbleColor.CYAN
                    else -> null
                }
                if (color != null && charIndex < rowMaxCols) {
                    newGrid[GridPosition(row, charIndex)] = Bubble(color = color)
                }
            }
        }
        
        bubblesByPosition = newGrid
        shotsRemaining = level.maxShots
        score = 0
        gameState = GameState.IDLE
        isPaused = false
        rowsDroppedCount = 0
        
        nextBubbleColor = generateSmartColor()
        previewBubbleColor = generateSmartColor()
    }

    private fun generateSmartColor(): BubbleColor {
        val availableColors = bubblesByPosition.values
            .map { it.color }
            .filter { it != BubbleColor.BOMB && it != BubbleColor.RAINBOW }
            .distinct()
        
        return if (availableColors.isNotEmpty()) {
            availableColors.random()
        } else {
            BubbleColor.entries.filter { it != BubbleColor.BOMB && it != BubbleColor.RAINBOW }.random()
        }
    }

    override fun onShoot(spawnX: Float, spawnY: Float) {
        super.onShoot(spawnX, spawnY)
        previewBubbleColor = generateSmartColor()
    }

    override fun onPostSnap() {
        shotsRemaining--
        
        if (bubblesByPosition.isEmpty()) {
            gameState = GameState.WON
            soundEvent = SoundType.WIN
            saveProgress()
        } else if (shotsRemaining <= 0) {
            gameState = GameState.LOST
            soundEvent = SoundType.LOSE
        } else {
            if (!bubblesByPosition.values.any { it.color == nextBubbleColor }) {
                nextBubbleColor = generateSmartColor()
            }
            metrics?.let { checkGameConditions(it) }
        }
    }

    private fun saveProgress() {
        viewModelScope.launch {
            val completedLevels = settingsManager.adventureProgressFlow.first()
            if (currentLevelId > completedLevels) {
                settingsManager.setAdventureProgress(currentLevelId)
            }
        }
    }
}
