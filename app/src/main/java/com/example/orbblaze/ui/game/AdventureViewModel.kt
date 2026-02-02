package com.example.orbblaze.ui.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.example.orbblaze.domain.model.*

class AdventureViewModel(application: Application) : GameViewModel(application) {
    
    var currentLevelId by mutableIntStateOf(1)
        private set
    
    var shotsRemaining by mutableIntStateOf(0)
        private set

    init {
        changeGameMode(GameMode.CLASSIC) // Usaremos una base clásica pero con límites
    }

    fun loadAdventureLevel(levelId: Int) {
        val level = AdventureLevels.levels.find { it.id == levelId } ?: return
        currentLevelId = levelId
        
        // 1. Limpiar tablero y cargar el layout manual
        val newGrid = mutableMapOf<GridPosition, Bubble>()
        level.layout.forEachIndexed { row, rowText ->
            rowText.forEachIndexed { col, char ->
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
                    newGrid[GridPosition(row, col)] = Bubble(color = color)
                }
            }
        }
        
        bubblesByPosition = newGrid
        shotsRemaining = level.maxShots
        score = 0
        gameState = GameState.IDLE
        isPaused = false
        rowsDroppedCount = 0
        
        nextBubbleColor = generateProjectileColor()
        previewBubbleColor = generateProjectileColor()
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
            metrics?.let { checkGameConditions(it) }
        }
    }

    private fun saveProgress() {
        val completedLevels = prefs.getInt("adventure_progress", 0)
        if (currentLevelId > completedLevels) {
            prefs.edit().putInt("adventure_progress", currentLevelId).apply()
        }
    }
}
