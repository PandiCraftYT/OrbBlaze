package com.example.orbblaze.ui.settings

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.R
import com.example.orbblaze.data.SettingsManager
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.game.SoundType
import com.example.orbblaze.ui.menu.LocalFontScale
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
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "title_scale"
    )

    val sfxVolume by settingsManager.sfxVolumeFlow.collectAsState(initial = 1.0f)
    val musicVolume by settingsManager.musicVolumeFlow.collectAsState(initial = 0.5f)
    val isVibrationEnabled by settingsManager.vibrationEnabledFlow.collectAsState(initial = true)
    val isMusicMuted by settingsManager.musicMutedFlow.collectAsState(initial = false)
    val isColorBlindMode by settingsManager.colorBlindModeFlow.collectAsState(initial = false)

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
                                bubble.y = -bubble.radius; bubble.x = Random.nextFloat() * screenWidthPx; bubble.vy = Random.nextFloat() * 1.5f
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
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "CONFIGURACIÓN",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                fontSize = (36 * fontScale).sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = (1.5 * fontScale).sp,
                                shadow = null
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = titleScale
                                    scaleY = titleScale
                                }
                        )
                        Box(modifier = Modifier.width((100 * fontScale).dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
                    }

                    // ✅ PANEL DE OPCIONES (Sólido, ya no es transparente)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color(0xFFE3F2FD)) // Azul muy claro y sólido
                            .border(2.dp, Color.White, RoundedCornerShape(32.dp))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingSliderItem("EFECTOS DE SONIDO", sfxVolume) {
                            scope.launch { settingsManager.setSfxVolume(it) }
                            soundManager.setSfxVol(it)
                        }

                        SettingSliderItem("MÚSICA", musicVolume, enabled = !isMusicMuted) {
                            scope.launch { settingsManager.setMusicVolume(it) }
                            soundManager.setMusicVol(it)
                        }

                        HorizontalDivider(color = Color.Black.copy(alpha = 0.1f), thickness = 1.dp)

                        SettingsToggleRow("SILENCIAR MÚSICA", isMusicMuted) { 
                            scope.launch { settingsManager.setMusicMuted(it) }
                            soundManager.setMusicMute(it) 
                        }
                        SettingsToggleRow("VIBRACIÓN", isVibrationEnabled) { 
                            scope.launch { settingsManager.setVibrationEnabled(it) }
                        }
                        SettingsToggleRow("DALTONISMO", isColorBlindMode) { 
                            scope.launch { settingsManager.setColorBlindMode(it) }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 50.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircleSettingsButton(
                            icon = Icons.Default.Info,
                            onClick = { showAboutDialog = true },
                            color = Color.White // Botón sólido
                        )

                        Spacer(modifier = Modifier.width(40.dp))

                        CircleSettingsButton(
                            icon = Icons.Default.Home,
                            onClick = onBackClick,
                            color = Color(0xFF00E676)
                        )
                    }
                }
            }
        }

        if (showAboutDialog) {
            SettingsAboutDialog(onDismiss = { showAboutDialog = false })
        }
    }
}

@Composable
fun SettingSliderItem(label: String, value: Float, enabled: Boolean = true, onValueChange: (Float) -> Unit) {
    val fontScale = LocalFontScale.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = TextStyle(fontSize = (11 * fontScale).sp, fontWeight = FontWeight.Black, color = Color(0xFF1A237E).copy(alpha = 0.7f), letterSpacing = 1.sp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF1A237E),
                activeTrackColor = Color(0xFF1A237E),
                inactiveTrackColor = Color(0xFF1A237E).copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
fun SettingsToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val fontScale = LocalFontScale.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = TextStyle(fontSize = (13 * fontScale).sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A237E)))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.graphicsLayer(scaleX = 0.85f, scaleY = 0.85f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF00E676),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.Black.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
fun CircleSettingsButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    color: Color
) {
    val fontScale = LocalFontScale.current
    Box(
        modifier = Modifier
            .size((60 * fontScale).dp)
            .background(color, CircleShape)
            .border(2.dp, Color.White, CircleShape)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = if(color == Color.White) Color.Gray else Color.White, modifier = Modifier.size((28 * fontScale).dp))
    }
}

@Composable
fun SettingsAboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // BOTÓN INSTAGRAM
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/carlosnvz_"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("INSTAGRAM", color = Color.White, fontWeight = FontWeight.Bold)
                }

                // BOTÓN CERRAR
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("CERRAR", color = Color(0xFF1A237E), fontWeight = FontWeight.ExtraBold)
                }
            }
        },
        title = {
            Text(
                "SOBRE ORBBLAZE", 
                fontWeight = FontWeight.Black, 
                textAlign = TextAlign.Center, 
                modifier = Modifier.fillMaxWidth(),
                fontSize = 22.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Versión 1.0.0",
                    style = TextStyle(color = Color.Gray, fontSize = 14.sp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Un emocionante juego de burbujas creado con amor.",
                    textAlign = TextAlign.Center,
                    style = TextStyle(fontSize = 15.sp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Creado por Carlos",
                    style = TextStyle(
                        color = Color(0xFF1A237E),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "¡Gracias por jugar!",
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White
    )
}
