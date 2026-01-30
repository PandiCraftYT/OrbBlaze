package com.example.orbblaze.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.game.SoundType
import com.example.orbblaze.ui.menu.PhysicsBubble
import com.example.orbblaze.ui.theme.*
import kotlinx.coroutines.isActive
import kotlin.math.hypot
import kotlin.random.Random

@Composable
fun SettingsScreen(
    soundManager: SoundManager,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("orbblaze_prefs", Context.MODE_PRIVATE) }
    val infiniteTransition = rememberInfiniteTransition(label = "settings_animations")

    // Animación de escala para el título
    val titleScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "title_scale"
    )

    // --- ESTADOS DE CONFIGURACIÓN ---
    var sfxVolume by remember { mutableStateOf(prefs.getFloat("sfx_volume", 1.0f)) }
    var musicVolume by remember { mutableStateOf(prefs.getFloat("music_volume", 0.5f)) }
    var isVibrationEnabled by remember { mutableStateOf(prefs.getBoolean("vibration_enabled", true)) }
    var isMusicMuted by remember { mutableStateOf(soundManager.isMusicMuted()) }

    // --- LÓGICA DE FONDO (IDÉNTICA A MENUSCREEN) ---
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

        // Capa de Interacción y Dibujo
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

            // CONTENIDO DE CONFIGURACIÓN
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "CONFIGURACIÓN",
                    style = TextStyle(
                        fontSize = 42.sp, 
                        fontWeight = FontWeight.Black, 
                        color = Color.White, 
                        shadow = Shadow(color = Color.Black, offset = Offset(4f, 4f), blurRadius = 10f)
                    ),
                    modifier = Modifier.graphicsLayer { scaleX = titleScale; scaleY = titleScale }
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Panel de controles Glassmorphism
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(32.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. VOLUMEN EFECTOS
                    Text(text = "EFECTOS DE SONIDO", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFD700), letterSpacing = 1.sp))
                    Slider(
                        value = sfxVolume,
                        onValueChange = { sfxVolume = it; prefs.edit().putFloat("sfx_volume", it).apply(); soundManager.setSfxVol(it) },
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color.White)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. VOLUMEN MÚSICA
                    Text(text = "MÚSICA DE FONDO", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF64FFDA), letterSpacing = 1.sp))
                    Slider(
                        value = musicVolume,
                        onValueChange = { musicVolume = it; prefs.edit().putFloat("music_volume", it).apply(); soundManager.setMusicVol(it) },
                        enabled = !isMusicMuted,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF64FFDA), activeTrackColor = Color.White)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. INTERRUPTORES
                    SettingsRow("SILENCIAR MÚSICA", isMusicMuted) { isMusicMuted = it; soundManager.setMusicMute(it) }
                    SettingsRow("VIBRACIÓN", isVibrationEnabled) { isVibrationEnabled = it; prefs.edit().putBoolean("vibration_enabled", it).apply() }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // BOTONES DE ACCIÓN
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(50.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFFF4D4D).copy(alpha = 0.2f))
                        .border(2.dp, Color(0xFFFF4D4D), RoundedCornerShape(50))
                        .clickable { prefs.edit().putInt("high_score", 0).apply(); Toast.makeText(context, "Récord borrado", Toast.LENGTH_SHORT).show() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "BORRAR RÉCORD", color = Color(0xFFFF4D4D), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(50))
                        .border(2.dp, Color.White, RoundedCornerShape(50))
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("VOLVER AL MENÚ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
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
