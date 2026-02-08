package com.example.orbblaze.domain.engine

import com.example.orbblaze.domain.model.Bubble
import com.example.orbblaze.domain.model.BubbleColor
import com.example.orbblaze.domain.model.GridPosition
import kotlin.random.Random

class LevelEngine {
    private val _gridState = mutableMapOf<GridPosition, Bubble>()
    val gridState: Map<GridPosition, Bubble> get() = _gridState

    fun setupInitialLevel(rows: Int = 6, cols: Int = 10) {
        _gridState.clear()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                // Evitamos filas vacías al inicio si es necesario, 
                // pero por ahora llenamos con colores aleatorios básicos
                val color = BubbleColor.entries.filter { 
                    it != BubbleColor.BOMB && it != BubbleColor.RAINBOW 
                }.random()
                _gridState[GridPosition(r, c)] = Bubble(color = color)
            }
        }
    }
}
