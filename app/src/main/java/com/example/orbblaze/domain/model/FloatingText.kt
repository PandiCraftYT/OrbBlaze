package com.example.orbblaze.domain.model

data class FloatingText(
    val id: Long,
    val x: Float,
    val y: Float,
    val text: String,
    var life: Float,
    val isOpponent: Boolean = false
)
