package com.example.orbblaze.domain.engine

import com.example.orbblaze.domain.model.Bubble
import com.example.orbblaze.domain.model.BubbleColor
import com.example.orbblaze.domain.model.GridPosition

class LevelEngine {
    private val _gridState = mutableMapOf<GridPosition, Bubble>()
    val gridState: Map<GridPosition, Bubble> get() = _gridState

    fun setupInitialLevel(rows: Int = 6, cols: Int = 10) {
        _gridState.clear()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val color = generateBaseColor()
                _gridState[GridPosition(r, c)] = Bubble(color = color)
            }
        }
    }

    fun generateBaseColor() = BubbleColor.entries.filter { 
        it != BubbleColor.BOMB && it != BubbleColor.RAINBOW
    }.random()

    /**
     * Algoritmo BFS para encontrar burbujas que ya no están conectadas al techo.
     * Devuelve una lista de posiciones que deben caer.
     */
    fun findFloatingBubbles(grid: Map<GridPosition, Bubble>, rowsDropped: Int): List<GridPosition> {
        if (grid.isEmpty()) return emptyList()
        
        val visited = mutableSetOf<GridPosition>()
        val queue = ArrayDeque<GridPosition>()
        
        // El techo son las burbujas en la fila 0 (o menor si hay scroll negativo)
        val ceilingBubbles = grid.keys.filter { it.row <= 0 }
        queue.addAll(ceilingBubbles)
        visited.addAll(ceilingBubbles)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            HexGridHelper.getNeighbors(current, rowsDropped).forEach { neighbor ->
                if (grid.containsKey(neighbor) && neighbor !in visited) {
                    visited.add(neighbor)
                    queue.add(neighbor)
                }
            }
        }

        // Cualquier burbuja en el grid que NO fue visitada desde el techo, está flotando
        return grid.keys.filter { it !in visited }
    }

    /**
     * Genera un color para el proyectil basado solo en los colores que existen en el tablero
     */
    fun getSmartProjectileColor(grid: Map<GridPosition, Bubble>): BubbleColor {
        val rand = Math.random()
        if (rand < 0.02) return BubbleColor.RAINBOW
        if (rand < 0.05) return BubbleColor.BOMB

        val currentColors = grid.values.map { it.color }.distinct()
            .filter { it != BubbleColor.RAINBOW && it != BubbleColor.BOMB }

        return if (currentColors.isNotEmpty()) {
            currentColors.random()
        } else {
            generateBaseColor()
        }
    }
}
