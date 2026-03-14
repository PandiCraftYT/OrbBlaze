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
     * Genera un color para el proyectil basado en los colores del tablero con pesos inteligentes.
     * allowSpecials permite o bloquea la generación de Bombas y Arcoíris.
     * comboMultiplier influye en la probabilidad de obtener burbujas especiales.
     */
    fun getSmartProjectileColor(
        grid: Map<GridPosition, Bubble>, 
        allowSpecials: Boolean = true,
        comboMultiplier: Int = 1
    ): BubbleColor {
        // 1. Probabilidad dinámica de Especiales basada en el Combo actual
        if (allowSpecials) {
            val rand = Math.random()
            // Aumentamos la probabilidad base (2% y 5%) según el combo
            val bonusChance = (comboMultiplier - 1) * 0.015 
            if (rand < (0.02 + bonusChance)) return BubbleColor.RAINBOW
            if (rand < (0.05 + bonusChance)) return BubbleColor.BOMB
        }

        // 2. Conteo de colores presentes para equilibrar el juego
        val colorCounts = grid.values
            .filter { it.color != BubbleColor.RAINBOW && it.color != BubbleColor.BOMB }
            .groupingBy { it.color }
            .eachCount()

        if (colorCounts.isEmpty()) return generateBaseColor()

        // 3. Sistema de Pesos: Favorecemos ligeramente los colores con menos presencia
        // para ayudar al jugador a limpiar el tablero ("Limpieza Inteligente").
        val totalBubbles = colorCounts.values.sum().toFloat()
        val weights = colorCounts.mapValues { (_, count) ->
            // Peso inverso: a menos burbujas de ese color, más probabilidad de que salga
            (1f - (count.toFloat() / totalBubbles)).coerceAtLeast(0.2f)
        }

        val totalWeight = weights.values.sum()
        var randomThreshold = Math.random() * totalWeight
        
        for ((color, weight) in weights) {
            randomThreshold -= weight
            if (randomThreshold <= 0) return color
        }

        return colorCounts.keys.random()
    }
}
