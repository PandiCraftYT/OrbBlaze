package com.example.orbblaze.ui.game

import android.app.Application

class ClassicViewModel(application: Application) : GameViewModel(application) {

    init {
        // ✅ Renombrado para evitar choque de firmas con el setter automático
        changeGameMode(GameMode.CLASSIC)
        loadLevel(6) 
    }

    override fun onPostSnap() {
        shotsFiredCount++
        if (shotsFiredCount >= dropThreshold) {
            shotsFiredCount = 0
            addRows(1)
        } else {
            metrics?.let { checkGameConditions(it) }
        }
    }
}
