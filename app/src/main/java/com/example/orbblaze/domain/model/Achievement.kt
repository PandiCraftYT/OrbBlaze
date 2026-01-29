package com.example.orbblaze.domain.model

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    var isUnlocked: Boolean = false,
    val isHidden: Boolean = false // ✅ NUEVO: Para logros secretos
)