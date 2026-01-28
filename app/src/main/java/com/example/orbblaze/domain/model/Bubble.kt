package com.example.orbblaze.domain.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class BubbleColor { RED, BLUE, GREEN, YELLOW, PURPLE, CYAN }

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