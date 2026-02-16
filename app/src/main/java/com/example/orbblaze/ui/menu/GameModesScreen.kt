package com.example.orbblaze.ui.menu

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.R
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.game.SoundType
import com.example.orbblaze.ui.theme.*
import kotlinx.coroutines.isActive
import kotlin.math.hypot
import kotlin.random.Random

@Composable
fun GameModesScreen(
    onModeSelect: (String) -> Unit,
    onBackClick: () -> Unit,
    soundManager: SoundManager
) {
    var showLockedDialog by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "modes_animations")
    
    val titleScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "title_scale"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
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
                                    soundManager.play(SoundType.POP)
                                    bubble.vy = -25f; bubble.vx = (Random.nextFloat() * 12f - 6f)
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
                    // ✅ TÍTULO ESTILO PREMIUM
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "MODOS DE JUEGO",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                fontSize = (36 * fontScale).sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = (2 * fontScale).sp,
                                shadow = null
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = titleScale
                                    scaleY = titleScale
                                }
                        )
                        Box(modifier = Modifier.width((120 * fontScale).dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
                    }

                    // ✅ LISTA DE MODOS REFINADA
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        item {
                            ModeCardPremium(
                                title = "CONTRA TIEMPO", 
                                icon = Icons.Default.Refresh, 
                                color = Color(0xFF00E676), 
                                isLocked = false,
                                onClick = { onModeSelect("time_attack") }
                            )
                        }
                        item {
                            ModeCardPremium(
                                title = "MODO AVENTURA", 
                                icon = Icons.Default.Place, 
                                color = Color(0xFFFFD700), 
                                isLocked = false,
                                onClick = { onModeSelect("adventure_map") }
                            )
                        }
                        item {
                            ModeCardPremium(title = "MODO INVERSA", icon = Icons.Default.KeyboardArrowUp, color = Color(0xFFFF5252), onClick = { showLockedDialog = true })
                        }
                        item {
                            ModeCardPremium(title = "PUZZLE DIARIO", icon = Icons.Default.DateRange, color = Color(0xFFBB86FC), onClick = { showLockedDialog = true })
                        }
                        item {
                            ModeCardPremium(title = "MINIJUEGOS", icon = Icons.Default.PlayArrow, color = Color(0xFF03DAC5), onClick = { showLockedDialog = true })
                        }
                    }

                    // ✅ BOTÓN VOLVER (Elevado para el anuncio)
                    Box(
                        modifier = Modifier
                            .padding(bottom = 55.dp) // Espacio para el banner
                            .width(200.dp)
                            .height(56.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .border(2.dp, Color.White, RoundedCornerShape(24.dp))
                            .clickable { onBackClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("VOLVER", color = Color.Gray, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = 1.sp)
                    }
                }
            }
        }

        if (showLockedDialog) {
            OrbBlazeLockedDialog(onDismiss = { showLockedDialog = false })
        }
    }
}

@Composable
fun ModeCardPremium(
    title: String,
    icon: ImageVector,
    color: Color,
    isLocked: Boolean = true,
    onClick: () -> Unit
) {
    val fontScale = LocalFontScale.current
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height((85 * fontScale).dp.coerceAtLeast(70.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(width = 2.dp, color = if(isLocked) Color.LightGray.copy(alpha = 0.3f) else color.copy(alpha = 0.5f), shape = RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size((50 * fontScale).dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isLocked) Color.LightGray.copy(alpha = 0.1f) else color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else icon, 
                    contentDescription = null, 
                    tint = if(isLocked) Color.Gray else color, 
                    modifier = Modifier.size((26 * fontScale).dp)
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title, 
                    style = TextStyle(
                        color = Color(0xFF1A237E), 
                        fontSize = (18 * fontScale).sp, 
                        fontWeight = FontWeight.Black
                    )
                )
                Text(
                    text = if (isLocked) "PRÓXIMAMENTE" else "MODO DISPONIBLE", 
                    style = TextStyle(
                        color = if (isLocked) Color.Gray.copy(alpha = 0.6f) else color, 
                        fontSize = (11 * fontScale).sp, 
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
            }
            
            if (!isLocked) {
                Icon(
                    imageVector = Icons.Default.PlayArrow, 
                    contentDescription = null, 
                    tint = color, 
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun OrbBlazeLockedDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ENTENDIDO", color = Color(0xFF1A237E), fontWeight = FontWeight.ExtraBold)
            }
        },
        title = {
            Text("MODO BLOQUEADO", fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = {
            Text("Este modo de juego estará disponible en futuras actualizaciones. ¡Sigue atento!", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White
    )
}
