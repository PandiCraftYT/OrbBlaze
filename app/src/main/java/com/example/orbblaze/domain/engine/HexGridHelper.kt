package com.example.orbblaze.domain.engine

import com.example.orbblaze.domain.model.BoardMetricsPx
import com.example.orbblaze.domain.model.GridPosition
import kotlin.math.roundToInt

object HexGridHelper {

    /**
     * Obtiene los vecinos de una posición considerando el desplazamiento hexagonal (row + offset).
     * Mantiene tu lógica original de vecinos pares/impares.
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
     * CORREGIDO: Se eliminó el desplazamiento extra. Ahora respeta estrictamente el padding.
     */
    fun getBubbleCenter(pos: GridPosition, metrics: BoardMetricsPx, rowOffset: Int): Pair<Float, Float> {
        // Calculamos si esta fila lleva desplazamiento (zigzag)
        val isShiftedRow = (pos.row + rowOffset) % 2 != 0
        val rowShift = if (isShiftedRow) (metrics.bubbleDiameter / 2f) else 0f

        // X = Padding inicial + Desplazamiento por fila impar + (Columna * Ancho)
        val x = metrics.boardStartPadding + rowShift + (pos.col * metrics.horizontalSpacing)

        // Y = Padding superior + (Fila * Alto)
        val y = metrics.boardTopPadding + (pos.row * metrics.verticalSpacing)

        return x to y
    }

    /**
     * Estima la posición en la rejilla a partir de coordenadas táctiles/físicas.
     * Inversa exacta de getBubbleCenter.
     */
    fun estimateGridPosition(x: Float, y: Float, metrics: BoardMetricsPx, rowOffset: Int, columnsCount: Int): GridPosition {
        // 1. Calcular fila estimada
        val estimatedRow = ((y - metrics.boardTopPadding) / metrics.verticalSpacing).roundToInt().coerceAtLeast(0)

        // 2. Calcular el desplazamiento que tendría esa fila
        val isShiftedRow = (estimatedRow + rowOffset) % 2 != 0
        val rowShift = if (isShiftedRow) (metrics.bubbleDiameter / 2f) else 0f

        // 3. Calcular columna restando el padding y el shift antes de dividir
        val estimatedCol = ((x - (metrics.boardStartPadding + rowShift)) / metrics.horizontalSpacing).roundToInt()
            .coerceIn(0, columnsCount - 1)

        return GridPosition(estimatedRow, estimatedCol)
    }
}