package com.example.orbblaze.domain.model

import androidx.compose.ui.graphics.Color

enum class Rank(
    val title: String,
    val minScore: Int,
    val color: Color,
    val medalName: String
) {
    BRONZE("BRONCE", 0, Color(0xFFCD7F32), "🥉"),
    SILVER("PLATA", 10000, Color(0xFFC0C0C0), "🥈"),
    GOLD("ORO", 50000, Color(0xFFFFD700), "🥇"),
    PLATINUM("PLATINO", 100000, Color(0xFFE5E4E2), "💎"),
    DIAMOND("DIAMANTE", 250000, Color(0xFFB9F2FF), "✨"),
    MASTER("MAESTRO", 500000, Color(0xFFFF4500), "👑");

    companion object {
        fun fromScore(score: Int): Rank {
            return entries.findLast { score >= it.minScore } ?: BRONZE
        }

        fun fromElo(elo: Int): Rank {
            return when {
                elo >= 2400 -> MASTER
                elo >= 2100 -> DIAMOND
                elo >= 1800 -> PLATINUM
                elo >= 1500 -> GOLD
                elo >= 1200 -> SILVER
                else -> BRONZE
            }
        }
    }
}
