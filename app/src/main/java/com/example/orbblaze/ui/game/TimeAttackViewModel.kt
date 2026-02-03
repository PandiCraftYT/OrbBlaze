package com.example.orbblaze.ui.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TimeAttackViewModel(application: Application) : GameViewModel(application) {

    private var currentPhase by mutableIntStateOf(1) // 1: 60s, 2: 40s, 3: 20s

    init {
        changeGameMode(GameMode.TIME_ATTACK)
        loadLevel(3)
    }

    override fun startGame() {
        super.startGame()
        currentPhase = 1
        timeLeft = 60
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
        when (currentPhase) {
            1 -> {
                addRows(3)
                currentPhase = 2
                timeLeft = 40
            }
            2 -> {
                addRows(3)
                currentPhase = 3
                timeLeft = 20
            }
            3 -> {
                gameState = GameState.LOST
                soundEvent = SoundType.LOSE
            }
        }
    }

    override fun onPostSnap() {
        metrics?.let { checkGameConditions(it) }
    }
}
