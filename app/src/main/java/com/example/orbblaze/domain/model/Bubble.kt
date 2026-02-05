package com.example.orbblaze.domain.model

import java.util.UUID

enum class BubbleColor { RED, BLUE, GREEN, YELLOW, PURPLE, CYAN, BOMB, RAINBOW }

enum class BubbleState {
    STATIONARY,
    ACTIVE,
    POPPING,
    FALLING
}

data class Bubble(
    val id: String = UUID.randomUUID().toString(),
    val color: BubbleColor,
    val state: BubbleState = BubbleState.STATIONARY
)

// Centralizamos todos los modelos de juego aquí
data class Projectile(
    val x: Float,
    val y: Float,
    val color: BubbleColor,
    val velocityX: Float,
    val velocityY: Float,
    val isFireball: Boolean = false
)

data class GameParticle(
    val id: Long,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: BubbleColor,
    val size: Float,
    val life: Float
)

data class FloatingText(
    val id: Long,
    val x: Float,
    val y: Float,
    val text: String,
    val life: Float
)

data class BoardMetricsPx(
    val bubbleDiameter: Float,
    val horizontalSpacing: Float,
    val verticalSpacing: Float,
    val boardTopPadding: Float,
    val boardStartPadding: Float,
    val ceilingY: Float,
    val screenWidth: Float = 1080f // ✅ Añadido para rebotes precisos
)
