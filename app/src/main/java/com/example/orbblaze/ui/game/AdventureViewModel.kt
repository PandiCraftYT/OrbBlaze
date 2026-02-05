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

    // ✅ Sobrescribimos restartGame para que recargue el nivel de aventura actual
    override fun restartGame() {
        loadAdventureLevel(currentLevelId)
    }

    fun loadAdventureLevel(levelId: Int) {
        val level = AdventureLevels.levels.find { it.id == levelId } ?: return
        currentLevelId = levelId

        val newGrid = mutableMapOf<GridPosition, Bubble>()
        val layout = level.layout.filter { it.isNotEmpty() }
        
        // Encontrar el ancho máximo del bloque para centrarlo
        val maxLayoutWidth = layout.maxOfOrNull { it.length } ?: 0

        layout.forEachIndexed { row, rowText ->
            val isOffsetRow = row % 2 != 0
            val totalGridCols = if (isOffsetRow) 10 else 11
            
            // Calcular un margen único para centrar el bloque de texto completo
            val startCol = ((totalGridCols - maxLayoutWidth) / 2).coerceAtLeast(0)

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
                if (color != null) {
                    // Se usa charIndex para respetar los espacios internos del layout
                    val finalCol = startCol + charIndex
                    if (finalCol < totalGridCols) {
                        newGrid[GridPosition(row, finalCol)] = Bubble(color = color)
                    }
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
