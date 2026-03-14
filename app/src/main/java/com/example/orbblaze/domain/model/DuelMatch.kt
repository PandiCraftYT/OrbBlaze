package com.example.orbblaze.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class DuelMatch(
    val matchId: String = "",
    val opponentName: String = "",
    val opponentAvatar: String? = null,
    val result: String = "LOSS", // WIN, LOSS, DRAW
    val eloChange: Int = 0,
    val score: Int = 0,
    val opponentScore: Int = 0,
    @ServerTimestamp
    val timestamp: Date? = null
)
