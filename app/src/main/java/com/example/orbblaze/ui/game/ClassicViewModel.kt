package com.example.orbblaze.ui.game

import android.app.Application

class ClassicViewModel(application: Application) : GameViewModel(application) {

    init {
        changeGameMode(GameMode.CLASSIC)
        loadLevel(5) // Iniciamos con 6 filas de burbujas
    }

    override fun onPostSnap() {
        shotsFiredCount++
        
        // --- MODO LIBRE ---
        // Eliminamos la lógica de addRows.
        // Ahora el jugador puede disparar infinitamente sin que bajen nuevas filas.
        metrics?.let { checkGameConditions(it) }
    }
}
