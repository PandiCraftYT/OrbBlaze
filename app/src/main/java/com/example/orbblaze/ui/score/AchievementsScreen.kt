package com.example.orbblaze.ui.score

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.orbblaze.ui.game.GameViewModel
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.game.SoundType
import com.example.orbblaze.ui.menu.PhysicsBubble
import com.example.orbblaze.ui.theme.*
import kotlinx.coroutines.isActive
import kotlin.math.hypot
import kotlin.random.Random

@Composable
fun AchievementsScreen(
    viewModel: GameViewModel = viewModel(),
    soundManager: SoundManager,
    onBackClick: () -> Unit
) {
    var revealedId by remember { mutableStateOf<String?>(null) }
    val infiniteTransition = rememberInfiniteTransition(label = "ach_animations")
    
    val titleScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "title_scale"
    )

    val displayList = viewModel.achievements.filter { !it.isHidden || it.isUnlocked }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(BgTop, BgBottom)))
            .systemBarsPadding()
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()
        val density = LocalDensity.current
        var frameTick by remember { mutableStateOf(0L) }
        val bubbleColors = listOf(BubbleRed, BubbleBlue, BubbleGreen, BubbleYellow, BubblePurple, BubbleCyan)

        // Burbujas físicas de fondo para consistencia visual
        val physicsBubbles = remember(screenWidthPx, screenHeightPx) {
            List(15) {
                PhysicsBubble(
                    x = Random.nextFloat() * screenWidthPx,
                    y = Random.nextFloat() * screenHeightPx,
                    vx = Random.nextFloat() * 2f - 1f,
                    vy = Random.nextFloat() * -10f - 5f,
                    radius = with(density) { Random.nextInt(15, 30).dp.toPx() },
                    color = bubbleColors.random()
                )
            }
        }

        LaunchedEffect(screenWidthPx, screenHeightPx) {
            val gravity = 0.25f
            while (isActive) {
                withFrameNanos { frameTime ->
                    frameTick = frameTime
                    physicsBubbles.forEach { bubble ->
                        bubble.x += bubble.vx
                        bubble.y += bubble.vy
                        bubble.vy += gravity
                        if (bubble.y > screenHeightPx + bubble.radius * 2) {
                            bubble.y = -bubble.radius
                            bubble.x = Random.nextFloat() * screenWidthPx
                            bubble.vy = Random.nextFloat() * 2f
                        }
                        if (bubble.x < -bubble.radius) bubble.x = screenWidthPx + bubble.radius
                        if (bubble.x > screenWidthPx + bubble.radius) bubble.x = -bubble.radius
                    }
                }
            }
        }

        // Capa de fondo interactiva
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(screenWidthPx, screenHeightPx) {
                    detectTapGestures { offset ->
                        physicsBubbles.forEach { bubble ->
                            val dist = hypot(offset.x - bubble.x, offset.y - bubble.y)
                            if (dist <= bubble.radius * 1.5f) {
                                soundManager.play(SoundType.POP)
                                bubble.vy = -20f
                                bubble.vx = (Random.nextFloat() * 10f - 5f)
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                @Suppress("UNUSED_VARIABLE")
                val t = frameTick 
                physicsBubbles.forEach { bubble ->
                    val center = Offset(bubble.x, bubble.y)
                    val radius = bubble.radius
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(bubble.color.copy(alpha = 0.9f), bubble.color.copy(alpha = 0.4f)),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                    drawCircle(color = Color.White.copy(alpha = 0.3f), radius = radius * 0.3f, center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f))
                    drawCircle(color = Color.White.copy(alpha = 0.5f), radius = radius, center = center, style = Stroke(width = 2f))
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp), 
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LOGROS", 
                    style = TextStyle(
                        fontSize = 42.sp, 
                        fontWeight = FontWeight.Black, 
                        color = Color.White, 
                        letterSpacing = 2.sp,
                        shadow = Shadow(color = Color.Black, offset = Offset(4f, 4f), blurRadius = 8f)
                    ),
                    modifier = Modifier.graphicsLayer { scaleX = titleScale; scaleY = titleScale }
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(), 
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayList) { achievement ->
                        val isUnlocked = achievement.isUnlocked
                        val showDescription = isUnlocked || revealedId == achievement.id
                        // Tarjetas Blancas Opacas para logros desbloqueados
                        val cardColor = if (isUnlocked) Color.White else Color.Black.copy(alpha = 0.4f)
                        val borderColor = if (isUnlocked) Color(0xFFFFD700) else Color.White.copy(alpha = 0.2f)
                        val titleColor = if (isUnlocked) Color(0xFF1A237E) else Color.Gray
                        val descColor = if (isUnlocked) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.4f)
                        val iconColor = if (isUnlocked) Color(0xFFFFD700) else Color.Gray

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(cardColor)
                                .border(2.dp, borderColor, RoundedCornerShape(20.dp))
                                .clickable { if (!isUnlocked) revealedId = if (revealedId == achievement.id) null else achievement.id }
                                .padding(16.dp)
                                .animateContentSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if(isUnlocked) Color(0xFF1A237E).copy(alpha=0.1f) else Color.Black.copy(alpha=0.2f)), 
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = if (isUnlocked) Icons.Default.Star else Icons.Default.Lock, contentDescription = null, tint = iconColor, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(text = achievement.title, style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Black, color = titleColor))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (showDescription) achievement.description else "Bloqueado 🔒 (Toca para ver pista)", 
                                    style = TextStyle(fontSize = 13.sp, color = descColor, fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(50))
                        .border(2.dp, Color.White, RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "VOLVER", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White))
                }

                Spacer(modifier = Modifier.height(16.dp))
                
            }
        }
    }
}
