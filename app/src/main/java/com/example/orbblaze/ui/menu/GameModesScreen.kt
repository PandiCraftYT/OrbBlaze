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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "title_scale"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
            .systemBarsPadding()
    ) {
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
                        bubble.x += bubble.vx; bubble.y += bubble.vy; bubble.vy += gravity
                        if (bubble.y > screenHeightPx + bubble.radius * 2) {
                            bubble.y = -bubble.radius; bubble.x = Random.nextFloat() * screenWidthPx
                            bubble.vy = Random.nextFloat() * 2f
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
                            if (dist <= bubble.radius * 1.5f) {
                                soundManager.play(SoundType.POP)
                                bubble.vy = -20f; bubble.vx = (Random.nextFloat() * 10f - 5f)
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
                    drawCircle(brush = Brush.radialGradient(listOf(bubble.color.copy(alpha = 0.9f), bubble.color.copy(alpha = 0.4f)), center = center, radius = radius), radius = radius, center = center)
                    drawCircle(color = Color.White.copy(alpha = 0.3f), radius = radius * 0.3f, center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f))
                    drawCircle(color = Color.White.copy(alpha = 0.5f), radius = radius, center = center, style = Stroke(width = 2f))
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "MODOS DE JUEGO",
                    style = TextStyle(
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 2.sp,
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), offset = Offset(4f, 4f), blurRadius = 10f)
                    ),
                    modifier = Modifier.graphicsLayer { scaleX = titleScale; scaleY = titleScale }
                )

                Spacer(modifier = Modifier.height(32.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    item {
                        ModeCardPremium(
                            title = "CONTRA TIEMPO", 
                            icon = Icons.Default.Refresh, 
                            color = Color(0xFF64FFDA), 
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
                        ModeCardPremium(title = "MODO INVERSA", icon = Icons.Default.KeyboardArrowUp, color = Color(0xFFFF4D4D), onClick = { showLockedDialog = true })
                    }
                    item {
                        ModeCardPremium(title = "PUZZLE DIARIO", icon = Icons.Default.DateRange, color = Color(0xFFBB86FC), onClick = { showLockedDialog = true })
                    }
                    item {
                        ModeCardPremium(title = "MINIJUEGOS", icon = Icons.Default.PlayArrow, color = Color(0xFF03DAC5), onClick = { showLockedDialog = true })
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
                    Text("VOLVER AL MENÚ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(text = "v1.0 - OrbBlaze", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
            }
        }

        if (showLockedDialog) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable { showLockedDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.width(300.dp).padding(16.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF1A237E),
                    border = BorderStroke(2.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Lock, null, tint = Color(0xFFFFD700), modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("MODO BLOQUEADO", style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black))
                        Spacer(Modifier.height(8.dp))
                        Text("Este modo de juego próximamente estará disponible.", style = TextStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { showLockedDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64FFDA)), shape = RoundedCornerShape(50)) {
                            Text("OK", color = Color(0xFF1A237E), fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(85.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(width = 2.dp, color = color.copy(alpha = 0.6f), shape = RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = if (isLocked) Icons.Default.Lock else icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = TextStyle(color = Color(0xFF1A237E), fontSize = 18.sp, fontWeight = FontWeight.Black))
                Text(text = if (isLocked) "Próximamente..." else "¡Jugar ahora!", style = TextStyle(color = if (isLocked) Color.Black.copy(alpha = 0.4f) else Color(0xFF64FFDA), fontSize = 12.sp, fontWeight = FontWeight.Bold))
            }
            if (!isLocked) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF64FFDA), modifier = Modifier.size(24.dp))
            }
        }
    }
}
