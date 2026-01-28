package com.example.orbblaze.domain.engine

import com.example.orbblaze.domain.model.GridPosition
import com.example.orbblaze.domain.model.Bubble

class MatchFinder {
    fun findMatches(
        startPos: GridPosition,
        grid: Map<GridPosition, Bubble>
    ): Set<GridPosition> {
        val targetColor = grid[startPos]?.color ?: return emptySet()
        val connected = mutableSetOf<GridPosition>()
        val queue = ArrayDeque<GridPosition>().apply { add(startPos) }

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current in connected) continue

            if (grid[current]?.color == targetColor) {
                connected.add(current)
                // Obtenemos vecinos de la grilla hexagonal
                getNeighbors(current).forEach { neighbor ->
                    if (grid.containsKey(neighbor)) queue.add(neighbor)
                }
            }
        }
        return if (connected.size >= 3) connected else emptySet()
    }

    private fun getNeighbors(pos: GridPosition): List<GridPosition> {
        val r = pos.row
        val c = pos.col
        // Las reglas de vecinos cambian si la fila es par o impar (Hexagonal)
        return if (r % 2 == 0) {
            listOf(
                GridPosition(r, c-1), GridPosition(r, c+1),
                GridPosition(r-1, c-1), GridPosition(r-1, c),
                GridPosition(r+1, c-1), GridPosition(r+1, c)
            )
        } else {
            listOf(
                GridPosition(r, c-1), GridPosition(r, c+1),
                GridPosition(r-1, c), GridPosition(r-1, c+1),
                GridPosition(r+1, c), GridPosition(r+1, c+1)
            )
        }
    }
}