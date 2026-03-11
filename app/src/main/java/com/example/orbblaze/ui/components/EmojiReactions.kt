package com.example.orbblaze.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun EmojiReactionPicker(
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val emojis = listOf("😎", "😂", "😮", "😡", "🔥", "👍")
    
    val rotation by animateFloatAsState(if (expanded) 135f else 0f, label = "icon_rotation")

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        // Selector Expandible
        AnimatedVisibility(
            visible = expanded,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.padding(bottom = 8.dp).border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(32.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    emojis.forEach { emoji ->
                        EmojiButton(emoji = emoji, onClick = { 
                            onEmojiSelected(emoji)
                            expanded = false 
                        })
                    }
                }
            }
        }

        // Botón Principal
        IconButton(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (expanded) Color.White else Color.Black.copy(alpha = 0.4f))
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.Close else Icons.Default.AddReaction,
                contentDescription = "Emojis",
                tint = if (expanded) Color.Black else Color.White,
                modifier = Modifier.size(24.dp).graphicsLayer { rotationZ = rotation }
            )
        }
    }
}

@Composable
fun EmojiButton(emoji: String, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.4f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Text(
        text = emoji,
        fontSize = 26.sp,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = null,
                indication = null
            ) {
                onClick()
                isPressed = true
            }
    )
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(200)
            isPressed = false
        }
    }
}

@Composable
fun PlayerReactionDisplay(
    emoji: String?,
    timestamp: Long,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(emoji, timestamp) {
        if (emoji != null) {
            visible = true
            delay(3000)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(spring(Spring.DampingRatioHighBouncy)) + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(
                    Brush.radialGradient(listOf(Color.White, Color.Transparent)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji ?: "", fontSize = 40.sp)
        }
    }
}
