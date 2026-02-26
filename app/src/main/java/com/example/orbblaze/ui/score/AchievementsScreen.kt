package com.example.orbblaze.ui.score

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.orbblaze.ui.game.GameViewModel
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.game.SoundType
import com.example.orbblaze.ui.menu.LocalFontScale
import com.example.orbblaze.ui.menu.ReferenceButton
import com.example.orbblaze.ui.theme.*

private val SageGreen = Color(0xFF8DA094)
private val NavyDark = Color(0xFF2D324F)
private val StarGold = Color(0xFFF4C491)

@Composable
fun AchievementsScreen(
    viewModel: GameViewModel = viewModel(),
    soundManager: SoundManager,
    onBackClick: () -> Unit
) {
    var revealedId by remember { mutableStateOf<String?>(null) }
    val infiniteTransition = rememberInfiniteTransition(label = "ach_animations")
    
    val titleFloat by infiniteTransition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "title_float"
    )

    val displayList = viewModel.achievements.filter { !it.isHidden || it.isUnlocked }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val fontScale = (maxWidth.value / 411f).coerceIn(0.6f, 1.5f)
        
        CompositionLocalProvider(LocalFontScale provides fontScale) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer { translationY = titleFloat }
                ) {
                    Text(
                        text = "LOGROS",
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            fontSize = (42 * fontScale).sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 2.sp,
                            shadow = Shadow(Color.Black.copy(alpha = 0.15f), Offset(0f, 8f), 12f)
                        )
                    )
                    Box(modifier = Modifier.padding(top = 4.dp).width(100.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.5f)))
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 24.dp)
                ) {
                    items(displayList) { achievement ->
                        AchievementCardPremium(
                            achievement = achievement,
                            isRevealed = revealedId == achievement.id,
                            soundManager = soundManager,
                            onRevealToggle = { revealedId = if (revealedId == achievement.id) null else achievement.id }
                        )
                    }
                }

                ReferenceButton(
                    text = "VOLVER",
                    backgroundColor = Color.White,
                    contentColor = Color.Gray,
                    modifier = Modifier.width(200.dp),
                    soundManager = soundManager,
                    onClick = onBackClick
                )
            }
        }
    }
}

@Composable
fun AchievementCardPremium(
    achievement: com.example.orbblaze.domain.model.Achievement,
    isRevealed: Boolean,
    soundManager: SoundManager,
    onRevealToggle: () -> Unit
) {
    val isUnlocked = achievement.isUnlocked
    val fontScale = LocalFontScale.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "scale")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { 
                scaleX = scale
                scaleY = scale
            }
            .animateContentSize()
    ) {
        Surface(
            onClick = { 
                soundManager.play(SoundType.POP)
                if (!isUnlocked) onRevealToggle() 
            },
            interactionSource = interactionSource,
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = if (isPressed) 2.dp else 6.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size((56 * fontScale).dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isUnlocked) StarGold.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isUnlocked) Icons.Default.Star else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isUnlocked) StarGold else Color.LightGray,
                        modifier = Modifier.size((28 * fontScale).dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = achievement.title.uppercase(),
                        style = TextStyle(
                            fontSize = (16 * fontScale).sp,
                            fontWeight = FontWeight.Black,
                            color = if (isUnlocked) NavyDark else Color.Gray,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = if (isUnlocked || isRevealed) achievement.description else "TOCA PARA PISTA",
                        style = TextStyle(
                            fontSize = (12 * fontScale).sp,
                            color = if (isUnlocked) Color.Gray else Color.LightGray,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                
                if (isUnlocked) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = StarGold,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
