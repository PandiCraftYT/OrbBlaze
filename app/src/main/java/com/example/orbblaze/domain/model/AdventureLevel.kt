package com.example.orbblaze.domain.model

import androidx.compose.ui.graphics.Color

// 1. TUS ZONAS (Intactas, con colores y descripción)
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

// 2. OBJETIVOS DE MISIÓN (Lo nuevo para el estilo Candy Crush)
sealed class LevelObjective {
    data class ClearBoard(val description: String = "Limpia todas las orbes") : LevelObjective()
    data class ReachScore(val target: Int, val description: String = "Consigue $target puntos") : LevelObjective()
    data class CollectColor(val colorChar: Char, val count: Int, val description: String = "Destruye $count orbes de color") : LevelObjective()
}

// 3. TU DATA CLASS LEVEL (Adaptada con objetivos y estrellas)
data class Level(
    val id: Int,
    val zone: AdventureZone,
    val maxShots: Int,         // Mantenemos tu nombre de variable
    val layout: List<String>,  // Tu mapa visual
    val objective: LevelObjective, // NUEVO: La misión
    val star1Threshold: Int,   // Puntos para 1 estrella
    val star2Threshold: Int,   // Puntos para 2 estrellas
    val star3Threshold: Int    // Puntos para 3 estrellas
)