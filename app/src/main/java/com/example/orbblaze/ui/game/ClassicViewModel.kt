package com.example.orbblaze.ui.game

import android.app.Application

class ClassicViewModel(application: Application) : GameViewModel(application) {

    init {
        changeGameMode(GameMode.CLASSIC)
        loadLevel(5)
    }

    override fun onPostSnap() {
        shotsFiredCount++
        
        // --- LÓGICA DE DIFICULTAD DINÁMICA ---
        // Si el jugador ya ha hecho muchos tiros (p. ej. > 25), 
        // aumentamos el margen de tiros antes de que bajen filas para que sea más fácil limpiar.
        val dynamicThreshold = if (shotsFiredCount > 25) 12 else 8
        
        if (shotsFiredCount % dynamicThreshold == 0) {
            // En lugar de bajar siempre, comprobamos si el tablero está casi lleno
            // para no castigar injustamente.
            addRows(1)
        } else {
            metrics?.let { checkGameConditions(it) }
        }
    }
}
