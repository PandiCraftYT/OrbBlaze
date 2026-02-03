package com.example.orbblaze.ui.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.data.SettingsManager
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.game.SoundType
import com.example.orbblaze.ui.menu.PhysicsBubble
import com.example.orbblaze.ui.theme.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.random.Random

@Composable
fun SettingsScreen(
    soundManager: SoundManager,
    settingsManager: SettingsManager,
    onBackClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val infiniteTransition = rememberInfiniteTransition(label = "settings_animations")
    var showAboutDialog by remember { mutableStateOf(false) }

    val titleScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "title_scale"
    )

    // Observamos los flujos de DataStore de forma reactiva
    val sfxVolume by settingsManager.sfxVolumeFlow.collectAsState(initial = 1.0f)
    val musicVolume by settingsManager.musicVolumeFlow.collectAsState(initial = 0.5f)
    val isVibrationEnabled by settingsManager.vibrationEnabledFlow.collectAsState(initial = true)
    val isMusicMuted by settingsManager.musicMutedFlow.collectAsState(initial = false)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(BgTop, BgBottom)))
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
                            bubble.y = -bubble.radius; bubble.x = Random.nextFloat() * screenWidthPx; bubble.vy = Random.nextFloat() * 2f
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
                                soundManager.play(SoundType.POP); bubble.vy = -20f; bubble.vx = (Random.nextFloat() * 10f - 5f)
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
                    text = "CONFIGURACIÓN",
                    style = TextStyle(fontSize = 42.sp, fontWeight = FontWeight.Black, color = Color.White, shadow = Shadow(color = Color.Black, offset = Offset(4f, 4f), blurRadius = 10f)),
                    modifier = Modifier.graphicsLayer { scaleX = titleScale; scaleY = titleScale }
                )

                Spacer(modifier = Modifier.height(40.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(32.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "EFECTOS DE SONIDO", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFD700), letterSpacing = 1.sp))
                    Slider(
                        value = sfxVolume, 
                        onValueChange = { 
                            scope.launch { settingsManager.setSfxVolume(it) }
                            soundManager.setSfxVol(it) 
                        }, 
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color.White)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "MÚSICA DE FONDO", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF64FFDA), letterSpacing = 1.sp))
                    Slider(
                        value = musicVolume, 
                        onValueChange = { 
                            scope.launch { settingsManager.setMusicVolume(it) }
                            soundManager.setMusicVol(it) 
                        }, 
                        enabled = !isMusicMuted, 
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF64FFDA), activeTrackColor = Color.White)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsRow("SILENCIAR MÚSICA", isMusicMuted) { 
                        scope.launch { settingsManager.setMusicMuted(it) }
                        soundManager.setMusicMute(it) 
                    }
                    SettingsRow("VIBRACIÓN", isVibrationEnabled) { 
                        scope.launch { settingsManager.setVibrationEnabled(it) }
                    }
                }

                Spacer(modifier = Modifier.height(50.dp))

                // ✅ FILA DE BOTONES CIRCULARES CENTRADA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // BOTÓN INFORMACIÓN
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .border(2.dp, Color.White, CircleShape)
                            .clickable { showAboutDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    Spacer(modifier = Modifier.width(40.dp))

                    // BOTÓN VOLVER (CIRCULAR CON ICONO HOME)
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF64FFDA).copy(alpha = 0.2f))
                            .border(2.dp, Color.White, CircleShape)
                            .clickable { onBackClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showAboutDialog) {
            AboutDialog(onDismiss = { showAboutDialog = false })
        }
    }
}

@Composable
fun SettingsRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700))
        )
    }
}
