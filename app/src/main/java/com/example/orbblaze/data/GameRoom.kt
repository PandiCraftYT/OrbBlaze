package com.example.orbblaze.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class PlayerState(
    val userId: String = "",
    val displayName: String = "Jugador",
    val avatarUrl: String? = null,
    val score: Int = 0,
    val dangerLevel: Float = 0f, // 0.0 a 1.0 (cuánto ha llenado el tablero)
    val isReady: Boolean = false,
    val lastAttack: String? = null,
    val rematchReady: Boolean = false,
    val currentReaction: String? = null,
    val reactionTimestamp: Long = 0L,
    
    // Estadísticas detalladas
    val bubblesPopped: Int = 0,
    val maxCombo: Int = 0,
    val attacksSent: Int = 0
)

data class GameRoom(
    val roomId: String = "",
    val players: Map<String, PlayerState> = emptyMap(),
    val playerCount: Int = 0, 
    val status: String = "WAITING", // WAITING, PLAYING, FINISHED, REMATCH_REQUESTED
    val winnerId: String? = null,
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null
)
