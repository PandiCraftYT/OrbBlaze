package com.example.orbblaze.domain.engine

import com.example.orbblaze.domain.model.GridPosition
import com.example.orbblaze.domain.model.Bubble

class MatchFinder {
    fun findMatches(
        startPos: GridPosition,
        grid: Map<GridPosition, Bubble>,
        rowOffset: Int = 0
    ): Set<GridPosition> {
        val targetColor = grid[startPos]?.color ?: return emptySet()
        val connected = mutableSetOf<GridPosition>()
        val queue = ArrayDeque<GridPosition>().apply { add(startPos) }

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current in connected) continue

            if (grid[current]?.color == targetColor) {
                connected.add(current)
                // Usamos el helper centralizado
                HexGridHelper.getNeighbors(current, rowOffset).forEach { neighbor ->
                    if (grid.containsKey(neighbor)) queue.add(neighbor)
                }
            }
        }
        return if (connected.size >= 3) connected else emptySet()
    }
}
