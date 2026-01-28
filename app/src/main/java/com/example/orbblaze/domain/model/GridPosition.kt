package com.example.orbblaze.domain.model

data class GridPosition(
    val row: Int,
    val col: Int
) {
    // Las filas impares (1, 3, 5...) se desplazan hacia la derecha
    val isOffsetRow get() = row % 2 != 0
}