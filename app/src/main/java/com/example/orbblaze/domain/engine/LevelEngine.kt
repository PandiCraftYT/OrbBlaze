package com.example.orbblaze.domain.engine

import com.example.orbblaze.domain.model.Bubble
import com.example.orbblaze.domain.model.BubbleColor
import com.example.orbblaze.domain.model.GridPosition

class LevelEngine {
    // Usamos un mapa para que sea fácil buscar burbujas por su posición
    private val _gridState = mutableMapOf<GridPosition, Bubble>()
    val gridState: Map<GridPosition, Bubble> get() = _gridState

    fun setupInitialLevel(rows: Int, cols: Int) {
        _gridState.clear()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                // Llenamos solo las primeras 6 filas para dejar espacio abajo
                if (r < 6) {
                    _gridState[GridPosition(r, c)] = Bubble(
                        color = BubbleColor.values().random()
                    )
                }
            }
        }
    }
}