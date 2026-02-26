package com.example.orbblaze.ui.menu

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.R
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.game.SoundType
import com.example.orbblaze.ui.theme.*

val LocalFontScale = compositionLocalOf { 1f }

// Colores de la referencia
val SageGreen = Color(0xFF8DA094)
val NavyDark = Color(0xFFFFC107)
val StarGold = Color(0xFFFFFFFF)

@Composable
fun MenuScreen(
    onPlayClick: () -> Unit,
    onModesClick: () -> Unit,
    onScoreClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onExitClick: () -> Unit,
    soundManager: SoundManager,
    @Suppress("UNUSED_PARAMETER") onSecretClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "menu_animations")
    var showExitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { soundManager.startMusic() }
    BackHandler { showExitDialog = true }

    val titleFloat by infiniteTransition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "title_float"
    )

    val backgroundOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2000f,
        animationSpec = infiniteRepeatable(tween(40000, easing = LinearEasing), RepeatMode.Restart),
        label = "bg_scroll"
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val totalWidth = constraints.maxWidth.toFloat()
        val totalHeight = constraints.maxHeight.toFloat()
        val fontScale = (maxWidth.value / 411f).coerceIn(0.6f, 1.5f)
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val color1 = Color.White.copy(alpha = 0.15f)
            val color2 = Color.White.copy(alpha = 0.08f)
            drawCircle(color = color1, radius = 150.dp.toPx(), center = Offset(x = (backgroundOffset % (totalWidth + 400.dp.toPx())) - 200.dp.toPx(), y = 150.dp.toPx()))
            drawCircle(color = color2, radius = 250.dp.toPx(), center = Offset(x = ((backgroundOffset * 0.7f) % (totalWidth + 600.dp.toPx())) - 300.dp.toPx(), y = totalHeight * 0.4f))
            drawCircle(color = color1, radius = 120.dp.toPx(), center = Offset(x = totalWidth - ((backgroundOffset * 1.2f) % (totalWidth + 300.dp.toPx())), y = totalHeight * 0.7f))
        }

        CompositionLocalProvider(LocalFontScale provides fontScale) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 32.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // TÍTULO
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { translationY = titleFloat }) {
                    Text(
                        text = stringResource(id = R.string.app_name).uppercase(),
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            fontSize = (75 * fontScale).sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 2.sp,
                            shadow = Shadow(Color.Black.copy(alpha = 0.15f), Offset(0f, 8f), 12f)
                        )
                    )
                    Box(modifier = Modifier.padding(top = 4.dp).width(100.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.5f)))
                }

                // CONTENEDOR DE BOTONES REESTRUCTURADO
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // BOTÓN JUGAR
                    ReferenceButton(
                        text = stringResource(id = R.string.menu_play),
                        backgroundColor = Color.White,
                        contentColor = SageGreen,
                        soundManager = soundManager,
                        onClick = onModesClick
                    )

                    // FILA: RÉCORDS | LOGROS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // BOTÓN RÉCORDS
                        ReferenceButton(
                            text = stringResource(id = R.string.menu_record),
                            backgroundColor = Color.White,
                            contentColor = SageGreen,
                            soundManager = soundManager,
                            modifier = Modifier.weight(1f),
                            onClick = onScoreClick
                        )

                        // BOTÓN LOGROS (Azul con estrella dorada)
                        ReferenceButton(
                            text = "LOGROS",
                            backgroundColor = NavyDark,
                            contentColor = Color.White,
                            icon = Icons.Default.Star,
                            iconColor = StarGold,
                            soundManager = soundManager,
                            modifier = Modifier.weight(1f),
                            onClick = onAchievementsClick
                        )
                    }

                    // BOTÓN AJUSTES
                    ReferenceButton(
                        text = "AJUSTES",
                        backgroundColor = Color.White,
                        contentColor = SageGreen,
                        soundManager = soundManager,
                        onClick = onSettingsClick
                    )
                }

                // PIE DE PÁGINA (Solo versión)
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "VERSION 1.0.7",
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        if (showExitDialog) {
            OrbBlazeExitDialog(onConfirm = onExitClick, onDismiss = { showExitDialog = false })
        }
    }
}

@Composable
fun ReferenceButton(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconColor: Color = Color.Unspecified,
    showDot: Boolean = false,
    soundManager: SoundManager? = null, // Sonido opcional
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "scale")
    val fontScale = LocalFontScale.current

    Surface(
        onClick = {
            soundManager?.play(SoundType.POP) // Reproducir POP al hacer click
            onClick()
        },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(28.dp),
        color = backgroundColor,
        shadowElevation = if (isPressed) 2.dp else 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .height((64 * fontScale).dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(contentColor)
                )
                Spacer(Modifier.width(12.dp))
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            
            Text(
                text = text.uppercase(),
                style = TextStyle(
                    fontSize = (20 * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    letterSpacing = 1.sp
                )
            )
        }
    }
}

@Composable
fun OrbBlazeExitDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))) {
                Text(stringResource(id = R.string.dialog_exit_confirm), color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.dialog_exit_cancel), color = Color.Gray) }
        },
        title = { Text(stringResource(id = R.string.dialog_exit_title), fontWeight = FontWeight.Black) },
        text = { Text(stringResource(id = R.string.dialog_exit_desc)) },
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White
    )
}
