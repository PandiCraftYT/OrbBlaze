package com.example.orbblaze.domain.engine

import com.example.orbblaze.domain.model.BoardMetricsPx
import com.example.orbblaze.domain.model.GridPosition
import kotlin.math.roundToInt

object HexGridHelper {

    // Pre-calculamos los offsets de los vecinos para evitar crear listas en cada frame
    private val evenRowNeighbors = listOf(
        -1 to -1, -1 to 0,
        0 to -1, 0 to 1,
        1 to -1, 1 to 0
    )
    private val oddRowNeighbors = listOf(
        -1 to 0, -1 to 1,
        0 to -1, 0 to 1,
        1 to 0, 1 to 1
    )

    fun getNeighbors(pos: GridPosition, rowOffset: Int): List<GridPosition> {
        val offsets = if ((pos.row + rowOffset) % 2 == 0) evenRowNeighbors else oddRowNeighbors
        return offsets.map { GridPosition(pos.row + it.first, pos.col + it.second) }
    }

    fun getBubbleCenter(pos: GridPosition, metrics: BoardMetricsPx, rowOffset: Int): Pair<Float, Float> {
        val isShiftedRow = (pos.row + rowOffset) % 2 != 0
        val rowShift = if (isShiftedRow) (metrics.bubbleDiameter / 2f) else 0f
        val x = metrics.boardStartPadding + rowShift + (pos.col * metrics.horizontalSpacing)
        val y = metrics.boardTopPadding + (pos.row * metrics.verticalSpacing)
        return x to y
    }

    fun estimateGridPosition(x: Float, y: Float, metrics: BoardMetricsPx, rowOffset: Int, columnsCount: Int): GridPosition {
        val estimatedRow = ((y - metrics.boardTopPadding) / metrics.verticalSpacing).roundToInt().coerceAtLeast(0)
        val isShiftedRow = (estimatedRow + rowOffset) % 2 != 0
        val rowShift = if (isShiftedRow) (metrics.bubbleDiameter / 2f) else 0f
        val estimatedCol = ((x - (metrics.boardStartPadding + rowShift)) / metrics.horizontalSpacing).roundToInt()
            .coerceIn(0, columnsCount - 1)
        return GridPosition(estimatedRow, estimatedCol)
    }
}