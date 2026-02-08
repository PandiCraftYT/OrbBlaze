package com.example.orbblaze.ui.game

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.orbblaze.domain.model.Bubble
import com.example.orbblaze.domain.model.GridPosition
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class TimeAttackViewModel(application: Application) : GameViewModel(application) {

    init {
        changeGameMode(GameMode.TIME_ATTACK)
        loadLevel(3)
    }

    override fun loadLevel(initialRows: Int) {
        super.loadLevel(initialRows)
        timeLeft = 90 // Aseguramos que empiece en 90
    }

    override fun startGame() {
        super.startGame()
        timeLeft = 90
        startTimer()
    }

    override fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (gameState == GameState.PLAYING) {
                delay(1000)
                if (!isPaused) {
                    timeLeft--
                    if (timeLeft <= 0) {
                        handleTimeOut()
                    }
                }
            }
        }
    }

    private fun handleTimeOut() {
        // ✅ 1. CAEN 3 FILAS
        addRandomDifficultyRows(3)
        
        // ✅ 2. REINICIAR TIEMPO A 90s DE LEY
        timeLeft = 90
        
        // Efecto de sonido para avisar que cayeron filas
        soundEvent = SoundType.STICK
    }

    /**
     * Añade filas con dificultad aleatoria (colores mezclados sin patrón fijo)
     */
    private fun addRandomDifficultyRows(count: Int) {
        rowsDroppedCount += count
        val newGrid = mutableMapOf<GridPosition, Bubble>()
        
        // Desplazar las burbujas actuales hacia abajo
        bubblesByPosition.forEach { (pos, bubble) -> 
            newGrid[GridPosition(pos.row + count, pos.col)] = bubble 
        }
        
        // Añadir nuevas filas arriba con colores aleatorios puros para aumentar dificultad
        for (r in 0 until count) {
            for (c in 0 until columnsCount) {
                newGrid[GridPosition(r, c)] = Bubble(color = generateBoardBubbleColor())
            }
        }
        
        bubblesByPosition = newGrid
        removeFloatingBubbles(newGrid)
        metrics?.let { checkGameConditions(it) }
    }

    override fun onPostSnap() {
        metrics?.let { checkGameConditions(it) }
    }
}
