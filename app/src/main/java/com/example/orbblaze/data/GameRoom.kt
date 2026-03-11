package com.example.orbblaze.data

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Clase unificada para representar tanto el estado de una sala de juego como el estado de un jugador.
 * Corregido: Nombres de variables simplificados para evitar conflictos de mapeo con Firebase.
 */
data class GameRoom(
    // --- Campos de Estado de Jugador ---
    val userId: String = "",
    val displayName: String = "Jugador",
    val avatarUrl: String? = null,
    val score: Int = 0,
    val dangerLevel: Float = 0f,
    
    @get:PropertyName("ready")
    @set:PropertyName("ready")
    var ready: Boolean = false,
    
    val lastAttack: String? = null,
    
    @get:PropertyName("rematchReady")
    @set:PropertyName("rematchReady")
    var rematchReady: Boolean = false,
    
    val currentReaction: String? = null,
    val reactionTimestamp: Long = 0L,
    
    @get:PropertyName("bot")
    @set:PropertyName("bot")
    var bot: Boolean = false,
    
    // Estadísticas detalladas
    val bubblesPopped: Int = 0,
    val maxCombo: Int = 0,
    val attacksSent: Int = 0,

    // --- Campos de Estado de Sala ---
    val roomId: String = "",
    val players: Map<String, GameRoom> = emptyMap(),
    val playerCount: Int = 0, 
    val status: String = "WAITING", // WAITING, PLAYING, FINISHED, REMATCH_REQUESTED
    val winnerId: String? = null,
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null
)
