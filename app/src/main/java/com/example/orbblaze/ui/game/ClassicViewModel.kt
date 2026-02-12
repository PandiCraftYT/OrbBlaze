package com.example.orbblaze.ui.game

import android.app.Application
import com.example.orbblaze.data.SettingsManager

class ClassicViewModel(
    application: Application,
    settingsManager: SettingsManager
) : GameViewModel(application, settingsManager) {

    init {
        changeGameMode(GameMode.CLASSIC)
        loadLevel(5) 
    }

    override fun onPostSnap() {
        shotsFiredCount++
        metrics?.let { checkGameConditions(it) }
    }
}
