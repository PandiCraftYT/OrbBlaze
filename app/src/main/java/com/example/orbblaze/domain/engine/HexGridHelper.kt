package com.example.orbblaze.domain.engine

import com.example.orbblaze.domain.model.BoardMetricsPx
import com.example.orbblaze.domain.model.GridPosition
import kotlin.math.roundToInt

object HexGridHelper {

    /**
     * Obtiene los vecinos de una posición considerando el desplazamiento hexagonal (row + offset).
     */
    fun getNeighbors(pos: GridPosition, rowOffset: Int): List<GridPosition> {
        val r = pos.row
        val c = pos.col
        // Si la fila absoluta (r + rowOffset) es par o impar cambia el patrón de vecinos
        return if ((r + rowOffset) % 2 == 0) {
            listOf(
                GridPosition(r, c - 1), GridPosition(r, c + 1),
                GridPosition(r - 1, c - 1), GridPosition(r - 1, c),
                GridPosition(r + 1, c - 1), GridPosition(r + 1, c)
            )
        } else {
            listOf(
                GridPosition(r, c - 1), GridPosition(r, c + 1),
                GridPosition(r - 1, c), GridPosition(r - 1, c + 1),
                GridPosition(r + 1, c), GridPosition(r + 1, c + 1)
            )
        }
    }

    /**
     * Calcula el centro (X, Y) de una burbuja en píxeles.
     */
    fun getBubbleCenter(pos: GridPosition, metrics: BoardMetricsPx, rowOffset: Int): Pair<Float, Float> {
        val xOffset = if ((pos.row + rowOffset) % 2 != 0) (metrics.bubbleDiameter / 2f) else 0f
        val x = metrics.boardStartPadding + xOffset + (pos.col * metrics.horizontalSpacing)
        val y = metrics.boardTopPadding + (pos.row * metrics.verticalSpacing)
        return x to y
    }

    /**
     * Estima la posición en la rejilla a partir de coordenadas táctiles/físicas.
     */
    fun estimateGridPosition(x: Float, y: Float, metrics: BoardMetricsPx, rowOffset: Int): GridPosition {
        val estimatedRow = ((y - metrics.boardTopPadding) / metrics.verticalSpacing).roundToInt().coerceAtLeast(0)
        val xOffsetEst = if ((estimatedRow + rowOffset) % 2 != 0) (metrics.bubbleDiameter / 2f) else 0f
        val estimatedCol = ((x - (metrics.boardStartPadding + xOffsetEst)) / metrics.horizontalSpacing).roundToInt().coerceAtLeast(0)
        return GridPosition(estimatedRow, estimatedCol)
    }
}
