package com.example.orbblaze.ui.game

import android.app.Application
import com.example.orbblaze.data.SettingsManager
import com.example.orbblaze.data.AuthManager

class ClassicViewModel(
    application: Application,
    settingsManager: SettingsManager,
    authManager: AuthManager
) : GameViewModel(application, settingsManager, authManager) {

    init {
        changeGameMode(GameMode.CLASSIC)
        loadLevel(5) 
    }

    override fun onPostSnap() {
        shotsFiredCount++
        metrics?.let { checkGameConditions(it) }
    }
}
