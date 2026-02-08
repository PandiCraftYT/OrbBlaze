package com.example.orbblaze.domain.model

import androidx.compose.ui.graphics.Color

enum class AdventureZone(
    val title: String,
    val bgColors: List<Color>,
    val description: String
) {
    JUNGLE("JUNGLA DE BAMBÚ", listOf(Color(0xFF2E7D32), Color(0xFF1B5E20)), "El camino serpenteante comienza aquí."),
    CAVE("CUEVA CRISTALINA", listOf(Color(0xFF455A64), Color(0xFF263238)), "La oscuridad revela tesoros brillantes."),
    CORE("NÚCLEO TERRESTRE", listOf(Color(0xFF3E2723), Color(0xFFBF360C)), "El calor es intenso. Rompe la roca."),
    SURFACE("SUPERFICIE", listOf(Color(0xFF1B5E20), Color(0xFF4FC3F7)), "Llegaste al mundo exterior."),
    SKY("ATMÓSFERA", listOf(Color(0xFF0277BD), Color(0xFFE1F5FE)), "El aire se vuelve fino."),
    SPACE("GALAXIA", listOf(Color(0xFF0D47A1), Color(0xFF000000)), "Hacia lo desconocido.")
}

sealed class LevelObjective {
    data class ClearBoard(val description: String = "Limpia todas las orbes") : LevelObjective()
    data class ReachScore(val target: Int, val description: String = "Consigue $target puntos") : LevelObjective()
    data class CollectColor(val colorChar: Char, val count: Int, val description: String = "Destruye $count orbes de color") : LevelObjective()
}

data class Level(
    val id: Int,
    val zone: AdventureZone,
    val maxShots: Int,
    val layout: List<String>,
    val objective: LevelObjective,
    val star1Threshold: Int,
    val star2Threshold: Int,
    val star3Threshold: Int,
    val rowDropInterval: Int = 0 // ✅ 0 = No caen filas. >0 = Caen cada X disparos.
)
