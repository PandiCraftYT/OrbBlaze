package com.example.orbblaze.ui.game

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.orbblaze.data.AuthManager
import com.example.orbblaze.data.LeaderboardManager
import com.example.orbblaze.data.SettingsManager
import com.example.orbblaze.domain.model.Bubble
import com.example.orbblaze.domain.model.GridPosition
import com.example.orbblaze.domain.model.GameConstants
import com.example.orbblaze.domain.usecase.SyncUserDataUseCase
import com.example.orbblaze.domain.usecase.UnlockAchievementUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimeAttackViewModel @Inject constructor(
    application: Application,
    settingsManager: SettingsManager,
    authManager: AuthManager,
    syncUserDataUseCase: SyncUserDataUseCase,
    unlockAchievementUseCase: UnlockAchievementUseCase,
    leaderboardManager: LeaderboardManager
) : GameViewModel(application, settingsManager, authManager, syncUserDataUseCase, unlockAchievementUseCase, leaderboardManager) {

    init {
        changeGameMode(GameMode.TIME_ATTACK)
        loadLevel(3)
    }

    override fun loadLevel(initialRows: Int) {
        super.loadLevel(initialRows)
        timeLeft = GameConstants.TIME_ATTACK_INITIAL 
    }

    override fun startGame() {
        super.startGame()
        timeLeft = GameConstants.TIME_ATTACK_INITIAL
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
        addRandomDifficultyRows(GameConstants.TIME_ATTACK_PENALTY_ROWS)
        timeLeft = GameConstants.TIME_ATTACK_INITIAL
        soundEvent = SoundType.STICK
    }

    private fun addRandomDifficultyRows(count: Int) {
        rowsDroppedCount += count
        val newGrid = mutableMapOf<GridPosition, Bubble>()
        
        bubblesByPosition.forEach { (pos, bubble) -> 
            newGrid[GridPosition(pos.row + count, pos.col)] = bubble 
        }
        
        for (r in 0 until count) {
            for (c in 0 until columnsCount) {
                newGrid[GridPosition(r, c)] = Bubble(color = engine.generateBaseColor())
            }
        }
        
        bubblesByPosition = newGrid
        removeFloatingBubbles(newGrid.toMutableMap())
        metrics?.let { checkGameConditions(it) }
    }

    override fun onPostSnap() {
        metrics?.let { checkGameConditions(it) }
    }
}
