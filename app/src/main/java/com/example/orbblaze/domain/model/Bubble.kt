package com.example.orbblaze.domain.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class BubbleColor { RED, BLUE, GREEN, YELLOW, PURPLE, CYAN, BOMB,RAINBOW }

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

data class Projectile(
    val x: Float,
    val y: Float,
    val color: BubbleColor,
    val velocityX: Float,
    val velocityY: Float
)