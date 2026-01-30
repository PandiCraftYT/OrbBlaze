package com.example.orbblaze.domain.engine

import com.example.orbblaze.domain.model.GridPosition
import com.example.orbblaze.domain.model.Bubble

class MatchFinder {
    fun findMatches(
        startPos: GridPosition,
        grid: Map<GridPosition, Bubble>,
        rowOffset: Int = 0 // ✅ Ahora recibe el desplazamiento global
    ): Set<GridPosition> {
        val targetColor = grid[startPos]?.color ?: return emptySet()
        val connected = mutableSetOf<GridPosition>()
        val queue = ArrayDeque<GridPosition>().apply { add(startPos) }

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current in connected) continue

            if (grid[current]?.color == targetColor) {
                connected.add(current)
                getNeighbors(current, rowOffset).forEach { neighbor ->
                    if (grid.containsKey(neighbor)) queue.add(neighbor)
                }
            }
        }
        return if (connected.size >= 3) connected else emptySet()
    }

    fun getNeighbors(pos: GridPosition, rowOffset: Int): List<GridPosition> {
        val r = pos.row
        val c = pos.col
        // ✅ La lógica hexagonal debe considerar el desplazamiento total (fila + offset)
        return if ((r + rowOffset) % 2 == 0) {
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
