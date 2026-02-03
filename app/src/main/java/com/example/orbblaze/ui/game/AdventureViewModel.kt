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
        changeGameMode(GameMode.CLASSIC)
    }

    fun loadAdventureLevel(levelId: Int) {
        val level = AdventureLevels.levels.find { it.id == levelId } ?: return
        currentLevelId = levelId
        
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
        
        // Inicializar con colores inteligentes
        nextBubbleColor = generateSmartColor()
        previewBubbleColor = generateSmartColor()
    }

    /**
     * ✅ Generador Inteligente:
     * Solo devuelve colores que existen en el tablero para que el nivel
     * sea un reto de habilidad y no de suerte.
     */
    private fun generateSmartColor(): BubbleColor {
        val availableColors = bubblesByPosition.values
            .map { it.color }
            .filter { it != BubbleColor.BOMB && it != BubbleColor.RAINBOW }
            .distinct()
        
        return if (availableColors.isNotEmpty()) {
            availableColors.random()
        } else {
            // Si el tablero está casi vacío, colores básicos aleatorios
            BubbleColor.entries.filter { it != BubbleColor.BOMB && it != BubbleColor.RAINBOW }.random()
        }
    }

    override fun onShoot(spawnX: Float, spawnY: Float) {
        // Antes de disparar, preparamos el siguiente color inteligentemente
        super.onShoot(spawnX, spawnY)
        // Sobrescribimos el preview que genera la clase base
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
            // Aseguramos que el siguiente proyectil siempre sea útil
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
