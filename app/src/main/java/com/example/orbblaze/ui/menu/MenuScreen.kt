package com.example.orbblaze.ui.menu

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
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

// ✅ Composición Local para escalar texto en cualquier dispositivo
val LocalFontScale = compositionLocalOf { 1f }

@Composable
fun MenuScreen(
    onPlayClick: () -> Unit,
    onModesClick: () -> Unit,
    onScoreClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onExitClick: () -> Unit,
    soundManager: SoundManager,
    onSecretClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "menu_animations")
    var showExitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        soundManager.startMusic()
    }

    BackHandler {
        showExitDialog = true
    }

    val titleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "title_scale"
    )

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
            var frameTick by remember { mutableStateOf(0L) }
            val bubbleColors = listOf(BubbleRed, BubbleBlue, BubbleGreen, BubbleYellow, BubblePurple, BubbleCyan)

            val physicsBubbles = remember(screenWidthPx, screenHeightPx) {
                List(20) {
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
                            bubble.x += bubble.vx
                            bubble.y += bubble.vy
                            bubble.vy += gravity
                            if (bubble.y > screenHeightPx + bubble.radius * 2) {
                                bubble.y = -bubble.radius
                                bubble.x = Random.nextFloat() * screenWidthPx
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
                                    onSecretClick()
                                    bubble.vy = -25f
                                    bubble.vx = (Random.nextFloat() * 12f - 6f)
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
                                colors = listOf(bubble.color.copy(alpha = 0.8f), bubble.color.copy(alpha = 0.2f)),
                                center = center,
                                radius = radius
                            ),
                            radius = radius,
                            center = center
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.4f),
                            radius = radius * 0.35f,
                            center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.2f),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 1.5f)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // ✅ TÍTULO CON COLOR BLANCO SÓLIDO
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(id = R.string.app_name).uppercase(),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                fontSize = (54 * fontScale).sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White, // Ahora es blanco sólido sin degradados
                                letterSpacing = (4 * fontScale).sp,
                                shadow = null // Sin sombras para máxima nitidez
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = titleScale
                                    scaleY = titleScale
                                }
                        )
                        Box(modifier = Modifier.width((160 * fontScale).dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy((18 * fontScale).dp)
                    ) {
                        PremiumMenuButton(
                            text = stringResource(id = R.string.menu_play),
                            icon = Icons.Default.PlayArrow,
                            color = Color(0xFF00E676),
                            onClick = onPlayClick
                        )

                        PremiumMenuButton(
                            text = stringResource(id = R.string.menu_modes),
                            color = Color(0xFF40C4FF),
                            onClick = onModesClick
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                PremiumSmallButton(
                                    text = stringResource(id = R.string.menu_record),
                                    icon = Icons.Default.Star,
                                    color = Color(0xFFFFD700),
                                    onClick = onScoreClick
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                PremiumSmallButton(
                                    text = stringResource(id = R.string.menu_achievements),
                                    color = Color(0xFFFF4081),
                                    onClick = onAchievementsClick
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.menu_version),
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = (12 * fontScale).sp,
                            fontWeight = FontWeight.Medium
                        )

                        CircleIconButton(
                            icon = Icons.Default.Settings,
                            onClick = onSettingsClick,
                            color = Color.White,
                            iconTint = Color.Gray
                        )
                    }
                }
            }
        }

        if (showExitDialog) {
            OrbBlazeExitDialog(onConfirm = onExitClick, onDismiss = { showExitDialog = false })
        }
    }
}

@Composable
fun PremiumMenuButton(
    text: String,
    icon: ImageVector? = null,
    color: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1f, label = "scale")
    val alpha by animateFloatAsState(if (isPressed) 0.7f else 1f, label = "alpha")
    val fontScale = LocalFontScale.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height((70 * fontScale).dp.coerceAtLeast(50.dp))
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .background(
                brush = Brush.horizontalGradient(listOf(color, color)),
                shape = RoundedCornerShape(24.dp)
            )
            .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
            if (icon != null) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size((28 * fontScale).dp.coerceAtLeast(20.dp)))
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = text.uppercase(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    fontSize = (20 * fontScale).sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (1.5 * fontScale).sp,
                    shadow = null
                )
            )
        }
    }
}

@Composable
fun PremiumSmallButton(
    text: String,
    icon: ImageVector? = null,
    color: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1f, label = "scale")
    val fontScale = LocalFontScale.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height((60 * fontScale).dp.coerceAtLeast(45.dp))
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(color, RoundedCornerShape(20.dp))
            .border(1.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
            if (icon != null) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size((18 * fontScale).dp.coerceAtLeast(14.dp)))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text.uppercase(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    fontSize = (13 * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    shadow = null
                )
            )
        }
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    color: Color,
    iconTint: Color = Color.White
) {
    val fontScale = LocalFontScale.current
    Box(
        modifier = Modifier
            .size((56 * fontScale).dp.coerceAtLeast(48.dp))
            .background(color, CircleShape)
            .border(1.dp, Color.Black.copy(alpha = 0.05f), CircleShape)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size((24 * fontScale).dp.coerceAtLeast(20.dp)))
    }
}

@Composable
fun OrbBlazeExitDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))) {
                Text(stringResource(id = R.string.dialog_exit_confirm), color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.dialog_exit_cancel), color = Color(0xFF1A237E), fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text(stringResource(id = R.string.dialog_exit_title), fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = {
            Text(stringResource(id = R.string.dialog_exit_desc), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White
    )
}
