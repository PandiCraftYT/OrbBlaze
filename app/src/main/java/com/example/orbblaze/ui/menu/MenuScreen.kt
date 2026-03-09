package com.example.orbblaze.ui.menu

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.R
import com.example.orbblaze.ui.components.ReferenceButton
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.theme.*

val LocalFontScale = compositionLocalOf { 1f }

@Composable
fun MenuScreen(
    onModesClick: () -> Unit,
    onScoreClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onFriendsClick: () -> Unit,
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

    val configuration = LocalConfiguration.current
    val fontScale = (configuration.screenWidthDp.toFloat() / 411f).coerceIn(0.6f, 1.5f)

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val color1 = Color.White.copy(alpha = 0.15f)
            val color2 = Color.White.copy(alpha = 0.08f)
            drawCircle(color = color1, radius = 150.dp.toPx(), center = Offset(x = (backgroundOffset % (size.width + 400.dp.toPx())) - 200.dp.toPx(), y = 150.dp.toPx()))
            drawCircle(color = color2, radius = 250.dp.toPx(), center = Offset(x = ((backgroundOffset * 0.7f) % (size.width + 600.dp.toPx())) - 300.dp.toPx(), y = size.height * 0.4f))
            drawCircle(color = color1, radius = 120.dp.toPx(), center = Offset(x = size.width - ((backgroundOffset * 1.2f) % (size.width + 300.dp.toPx())), y = size.height * 0.7f))
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

                // BLOQUE DE BOTONES REESTRUCTURADO
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // JUGAR
                    ReferenceButton(
                        text = stringResource(id = R.string.menu_play),
                        backgroundColor = Color.White,
                        contentColor = SageGreen,
                        soundManager = soundManager,
                        onClick = onModesClick
                    )

                    // RÉCORDS Y LOGROS
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ReferenceButton(
                            text = stringResource(id = R.string.menu_record),
                            backgroundColor = Color.White,
                            contentColor = SageGreen,
                            soundManager = soundManager,
                            modifier = Modifier.weight(1f),
                            onClick = onScoreClick
                        )
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

                    // PERFIL Y AMIGOS (Sin iconos)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ReferenceButton(
                            text = "PERFIL",
                            backgroundColor = Color.White,
                            contentColor = SageGreen,
                            soundManager = soundManager,
                            modifier = Modifier.weight(1f),
                            onClick = onProfileClick
                        )
                        ReferenceButton(
                            text = "AMIGOS",
                            backgroundColor = Color.White,
                            contentColor = SageGreen,
                            soundManager = soundManager,
                            modifier = Modifier.weight(1f),
                            onClick = onFriendsClick
                        )
                    }

                    // AJUSTES (Blanco sólido, sin icono)
                    ReferenceButton(
                        text = "AJUSTES",
                        backgroundColor = Color.White,
                        contentColor = SageGreen,
                        soundManager = soundManager,
                        onClick = onSettingsClick
                    )
                }

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(text = "VERSION 1.0.7", color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                }
            }
        }

        if (showExitDialog) {
            OrbBlazeExitDialog(onConfirm = onExitClick, onDismiss = { showExitDialog = false })
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
