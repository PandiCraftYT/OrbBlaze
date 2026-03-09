package com.example.orbblaze.domain.model

import com.google.firebase.Timestamp

data class LeaderboardEntry(
    val userId: String = "",
    val username: String = "Anónimo",
    val score: Int = 0,
    val avatarUrl: String? = null,
    val timestamp: Timestamp? = null
)
