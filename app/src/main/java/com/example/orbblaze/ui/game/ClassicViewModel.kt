package com.example.orbblaze.ui.game

import android.app.Application
import com.example.orbblaze.data.AuthManager
import com.example.orbblaze.data.LeaderboardManager
import com.example.orbblaze.data.SettingsManager
import com.example.orbblaze.domain.usecase.SyncUserDataUseCase
import com.example.orbblaze.domain.usecase.UnlockAchievementUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ClassicViewModel @Inject constructor(
    application: Application,
    settingsManager: SettingsManager,
    authManager: AuthManager,
    syncUserDataUseCase: SyncUserDataUseCase,
    unlockAchievementUseCase: UnlockAchievementUseCase,
    leaderboardManager: LeaderboardManager
) : GameViewModel(application, settingsManager, authManager, syncUserDataUseCase, unlockAchievementUseCase, leaderboardManager) {

    init {
        changeGameMode(GameMode.CLASSIC)
        loadLevel(5) 
    }

    override fun onPostSnap() {
        shotsFiredCount++
        metrics?.let { checkGameConditions(it) }
    }
}
