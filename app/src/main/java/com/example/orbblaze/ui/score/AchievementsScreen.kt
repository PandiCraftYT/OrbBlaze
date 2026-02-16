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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.orbblaze.ui.game.GameViewModel
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.game.SoundType
import com.example.orbblaze.ui.menu.LocalFontScale
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
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "title_scale"
    )

    val displayList = viewModel.achievements.filter { !it.isHidden || it.isUnlocked }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(BgTop, BgBottom)))
    ) {
        val fontScale = (maxWidth.value / 411f).coerceIn(0.6f, 1.5f)
        
        CompositionLocalProvider(LocalFontScale provides fontScale) {
            val screenWidthPx = constraints.maxWidth.toFloat()
            val screenHeightPx = constraints.maxHeight.toFloat()
            val density = LocalDensity.current
            var frameTick by remember { mutableLongStateOf(0L) }
            val bubbleColors = listOf(BubbleRed, BubbleBlue, BubbleGreen, BubbleYellow, BubblePurple, BubbleCyan)

            val physicsBubbles = remember(screenWidthPx, screenHeightPx) {
                List(15) {
                    PhysicsBubble(
                        x = Random.nextFloat() * screenWidthPx,
                        y = Random.nextFloat() * screenHeightPx,
                        vx = Random.nextFloat() * 1.5f - 0.75f,
                        vy = Random.nextFloat() * -8f - 2f,
                        radius = with(density) { Random.nextInt(10, 40).dp.toPx() },
                        color = bubbleColors.random()
                    )
                }
            }

            LaunchedEffect(screenWidthPx, screenHeightPx) {
                val gravity = 0.15f
                while (isActive) {
                    withFrameNanos { frameTime ->
                        frameTick = frameTime
                        physicsBubbles.forEach { bubble ->
                            bubble.x += bubble.vx; bubble.y += bubble.vy; bubble.vy += gravity
                            if (bubble.y > screenHeightPx + bubble.radius * 2) {
                                bubble.y = -bubble.radius; bubble.x = Random.nextFloat() * screenWidthPx
                                bubble.vy = Random.nextFloat() * 1.5f
                            }
                            if (bubble.x < -bubble.radius) bubble.x = screenWidthPx + bubble.radius
                            if (bubble.x > screenWidthPx + bubble.radius) bubble.x = -bubble.radius
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(screenWidthPx, screenHeightPx) {
                        detectTapGestures { offset ->
                            physicsBubbles.forEach { bubble ->
                                val dist = hypot(offset.x - bubble.x, offset.y - bubble.y)
                                if (dist <= bubble.radius * 1.8f) {
                                    soundManager.play(SoundType.POP); bubble.vy = -25f; bubble.vx = (Random.nextFloat() * 12f - 6f)
                                }
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    @Suppress("UNUSED_VARIABLE")
                    val t = frameTick 
                    physicsBubbles.forEach { bubble ->
                        val center = Offset(bubble.x, bubble.y); val radius = bubble.radius
                        drawCircle(brush = Brush.radialGradient(listOf(bubble.color.copy(alpha = 0.8f), bubble.color.copy(alpha = 0.2f)), center = center, radius = radius), radius = radius, center = center)
                        drawCircle(color = Color.White.copy(alpha = 0.4f), radius = radius * 0.35f, center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f))
                        drawCircle(color = Color.White.copy(alpha = 0.2f), radius = radius, center = center, style = Stroke(width = 1.5f))
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // ✅ TÍTULO PREMIUM
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "LOGROS",
                            maxLines = 1,
                            style = TextStyle(
                                fontSize = (42 * fontScale).sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = (2 * fontScale).sp
                            ),
                            modifier = Modifier.graphicsLayer { scaleX = titleScale; scaleY = titleScale }
                        )
                        Box(modifier = Modifier.width((80 * fontScale).dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
                    }

                    // ✅ LISTA DE LOGROS
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(displayList) { achievement ->
                            AchievementCardPremium(
                                achievement = achievement,
                                isRevealed = revealedId == achievement.id,
                                onRevealToggle = { revealedId = if (revealedId == achievement.id) null else achievement.id }
                            )
                        }
                    }

                    // ✅ BOTÓN VOLVER (Elevado para anuncio)
                    Box(
                        modifier = Modifier
                            .padding(bottom = 55.dp)
                            .width(200.dp)
                            .height(56.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .clickable { onBackClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("CERRAR", color = Color.Gray, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementCardPremium(
    achievement: com.example.orbblaze.domain.model.Achievement,
    isRevealed: Boolean,
    onRevealToggle: () -> Unit
) {
    val isUnlocked = achievement.isUnlocked
    val fontScale = LocalFontScale.current
    val cardColor = if (isUnlocked) Color.White else Color.White.copy(alpha = 0.6f)
    val iconColor = if (isUnlocked) Color(0xFFFFD700) else Color.Gray

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(RoundedCornerShape(20.dp))
            .background(cardColor)
            .border(2.dp, if(isUnlocked) Color(0xFFFFD700).copy(alpha=0.4f) else Color.White.copy(alpha=0.3f), RoundedCornerShape(20.dp))
            .clickable { if (!isUnlocked) onRevealToggle() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size((50 * fontScale).dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if(isUnlocked) Color(0xFFFFD700).copy(alpha=0.1f) else Color.Black.copy(alpha=0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isUnlocked) Icons.Default.Star else Icons.Default.Lock,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size((28 * fontScale).dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = achievement.title.uppercase(),
                    style = TextStyle(
                        fontSize = (16 * fontScale).sp,
                        fontWeight = FontWeight.Black,
                        color = if(isUnlocked) Color(0xFF1A237E) else Color.DarkGray
                    )
                )
                Text(
                    text = if (isUnlocked || isRevealed) achievement.description else "BLOQUEADO (TOCA PARA PISTA)",
                    style = TextStyle(
                        fontSize = (12 * fontScale).sp,
                        color = if(isUnlocked) Color.Black.copy(alpha = 0.6f) else Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}
