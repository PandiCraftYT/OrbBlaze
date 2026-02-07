package com.example.orbblaze.domain.engine

import com.example.orbblaze.domain.model.GridPosition
import com.example.orbblaze.domain.model.Bubble
import com.example.orbblaze.domain.model.BubbleColor

class MatchFinder {
    /**
     * Encuentra grupos de burbujas del mismo color conectadas DIRECTAMENTE.
     * Solo agrupa burbujas si hay un camino ininterrumpido de ese color.
     */
    fun findMatches(
        startPos: GridPosition,
        grid: Map<GridPosition, Bubble>,
        rowOffset: Int = 0
    ): Set<GridPosition> {
        val startBubble = grid[startPos] ?: return emptySet()
        val targetColor = startBubble.color
        
        // No buscamos coincidencias para tipos especiales de forma directa aquí
        if (targetColor == BubbleColor.BOMB || targetColor == BubbleColor.RAINBOW) return emptySet()

        val connected = mutableSetOf<GridPosition>()
        val queue = ArrayDeque<GridPosition>().apply { add(startPos) }

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            
            if (current in connected) continue
            connected.add(current)

            // Exploramos solo vecinos que tengan EXACTAMENTE el mismo color
            HexGridHelper.getNeighbors(current, rowOffset).forEach { neighbor ->
                val neighborBubble = grid[neighbor]
                if (neighborBubble != null && 
                    neighborBubble.color == targetColor && 
                    neighbor !in connected) {
                    queue.add(neighbor)
                }
            }
        }
        
        // Solo devolvemos el grupo si se cumple la regla de "3 o más"
        return if (connected.size >= 3) connected else emptySet()
    }
}
