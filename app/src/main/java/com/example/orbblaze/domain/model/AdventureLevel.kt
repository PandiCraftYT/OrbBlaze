package com.example.orbblaze.domain.model

import androidx.compose.ui.graphics.Color

enum class AdventureZone(
    val title: String,
    val bgColors: List<Color>,
    val description: String
) {
    CORE("NÚCLEO TERRESTRE", listOf(Color(0xFF3E2723), Color(0xFFBF360C)), "El calor es intenso. Rompe la roca."),
    SURFACE("SUPERFICIE", listOf(Color(0xFF1B5E20), Color(0xFF4FC3F7)), "Llegaste al mundo exterior."),
    SKY("ATMÓSFERA", listOf(Color(0xFF0277BD), Color(0xFFE1F5FE)), "El aire se vuelve fino."),
    SPACE("GALAXIA", listOf(Color(0xFF0D47A1), Color(0xFF000000)), "Hacia lo desconocido.")
}

data class AdventureLevel(
    val id: Int,
    val zone: AdventureZone,
    val layout: List<String>, // 'R'=Red, 'B'=Blue, 'G'=Green, 'Y'=Yellow, 'P'=Purple, 'C'=Cyan, '.'=Empty
    val maxShots: Int,
    val targetScore: Int,
    val isUnlocked: Boolean = false,
    val stars: Int = 0
)
