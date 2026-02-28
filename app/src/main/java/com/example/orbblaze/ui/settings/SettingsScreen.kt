package com.example.orbblaze.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.data.AuthManager
import com.example.orbblaze.data.SettingsManager
import com.example.orbblaze.ui.components.rememberGoogleSignInHandler
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.game.SoundType
import com.example.orbblaze.ui.menu.LocalFontScale
import com.example.orbblaze.ui.menu.ReferenceButton
import com.example.orbblaze.ui.menu.SageGreen
import com.example.orbblaze.ui.menu.NavyDark
import com.example.orbblaze.ui.menu.StarGold
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    soundManager: SoundManager,
    settingsManager: SettingsManager,
    authManager: AuthManager,
    onBackClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "settings_animations")
    var showAboutDialog by remember { mutableStateOf(false) }

    val currentUser by authManager.user.collectAsState()
    val isAnonymous = currentUser?.isAnonymous ?: true

    // ✅ Usamos el manejador centralizado de login
    val handleSignIn = rememberGoogleSignInHandler(authManager, settingsManager)

    val titleFloat by infiniteTransition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "title_float"
    )

    val sfxVolume by settingsManager.sfxVolumeFlow.collectAsState(initial = 1.0f)
    val musicVolume by settingsManager.musicVolumeFlow.collectAsState(initial = 0.5f)
    val isVibrationEnabled by settingsManager.vibrationEnabledFlow.collectAsState(initial = true)
    val isMusicMuted by settingsManager.musicMutedFlow.collectAsState(initial = false)
    val isColorBlindMode by settingsManager.colorBlindModeFlow.collectAsState(initial = false)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val fontScale = (maxWidth.value / 411f).coerceIn(0.6f, 1.5f)
        CompositionLocalProvider(LocalFontScale provides fontScale) {
            Column(
                modifier = Modifier.fillMaxSize().systemBarsPadding().padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer { translationY = titleFloat }
                ) {
                    Text(text = "CONFIGURACIÓN", textAlign = TextAlign.Center, style = TextStyle(fontSize = (42 * fontScale).sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 2.sp, shadow = Shadow(Color.Black.copy(alpha = 0.15f), Offset(0f, 8f), 12f)))
                    Box(modifier = Modifier.padding(top = 4.dp).width(100.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.5f)))
                }

                Surface(shape = RoundedCornerShape(32.dp), color = Color.White, modifier = Modifier.fillMaxWidth().weight(1f, fill = false).shadow(8.dp, RoundedCornerShape(32.dp))) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SettingSliderItem("EFECTOS DE SONIDO", sfxVolume, color = SageGreen) { scope.launch { settingsManager.setSfxVolume(it) }; soundManager.setSfxVol(it) }
                        SettingSliderItem("MÚSICA", musicVolume, enabled = !isMusicMuted, color = SageGreen) { scope.launch { settingsManager.setMusicVolume(it) }; soundManager.setMusicVol(it) }
                        
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.05f), thickness = 1.dp)

                        SettingsToggleRow("SILENCIAR MÚSICA", isMusicMuted, activeColor = Color(0xFFEF4444)) { scope.launch { settingsManager.setMusicMuted(it) }; soundManager.setMusicMute(it) }
                        SettingsToggleRow("VIBRACIÓN", isVibrationEnabled, activeColor = SageGreen) { scope.launch { settingsManager.setVibrationEnabled(it) } }
                        SettingsToggleRow("DALTONISMO", isColorBlindMode, activeColor = NavyDark) { scope.launch { settingsManager.setColorBlindMode(it) } }

                        // ✅ BOTÓN SESIÓN CENTRALIZADO
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.05f), thickness = 1.dp)
                        Spacer(Modifier.height(8.dp))
                        
                        if (isAnonymous) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { handleSignIn() } // 🔥 Llama al manejador centralizado
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "INICIAR SESIÓN", 
                                    style = TextStyle(fontSize = (14 * fontScale).sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4))
                                )
                                Icon(
                                    imageVector = Icons.Default.Login,
                                    contentDescription = null,
                                    tint = Color(0xFF4285F4),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            authManager.saveProgressToCloud(settingsManager.getSyncableData())
                                            settingsManager.clearAllData()
                                            authManager.signOut(context)
                                            onBackClick()
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "CERRAR SESIÓN", 
                                    style = TextStyle(fontSize = (14 * fontScale).sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    ReferenceButton(text = "INFO", backgroundColor = NavyDark, contentColor = Color.White, icon = Icons.Default.Info, iconColor = StarGold, modifier = Modifier.weight(1f), soundManager = soundManager, onClick = { showAboutDialog = true })
                    ReferenceButton(text = "VOLVER", backgroundColor = Color.White, contentColor = Color.Gray, modifier = Modifier.weight(1f), soundManager = soundManager, onClick = onBackClick)
                }
            }
        }
        if (showAboutDialog) SettingsAboutDialog(soundManager = soundManager, onDismiss = { showAboutDialog = false })
    }
}

@Composable
fun SettingSliderItem(label: String, value: Float, enabled: Boolean = true, color: Color, onValueChange: (Float) -> Unit) {
    val fontScale = LocalFontScale.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = TextStyle(fontSize = (12 * fontScale).sp, fontWeight = FontWeight.Black, color = color.copy(alpha = 0.8f), letterSpacing = 1.sp))
        Slider(value = value, onValueChange = onValueChange, enabled = enabled, colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color, inactiveTrackColor = color.copy(alpha = 0.15f)))
    }
}

@Composable
fun SettingsToggleRow(label: String, checked: Boolean, activeColor: Color, onCheckedChange: (Boolean) -> Unit) {
    val fontScale = LocalFontScale.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = TextStyle(fontSize = (14 * fontScale).sp, fontWeight = FontWeight.Bold, color = NavyDark))
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.graphicsLayer(scaleX = 0.85f, scaleY = 0.85f), colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = activeColor, uncheckedThumbColor = Color.LightGray, uncheckedTrackColor = Color.Black.copy(alpha = 0.05f), uncheckedBorderColor = Color.Transparent))
    }
}

@Composable
fun SettingsAboutDialog(soundManager: SoundManager, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { soundManager.play(SoundType.POP); val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/carlosnvz_")); context.startActivity(intent) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)), shape = RoundedCornerShape(20.dp)) { Text("INSTAGRAM", color = Color.White, fontWeight = FontWeight.Bold) }
                TextButton(onClick = { soundManager.play(SoundType.POP); onDismiss() }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("CERRAR", color = Color.Gray, fontWeight = FontWeight.ExtraBold) }
            }
        },
        title = { Text("SOBRE ORBBLAZE", fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontSize = 22.sp) },
        text = { Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Text("Versión 1.0.7", style = TextStyle(color = Color.Gray, fontSize = 14.sp)); Spacer(Modifier.height(16.dp)); Text("Un emocionante juego de burbujas creado con amor.", textAlign = TextAlign.Center, style = TextStyle(fontSize = 15.sp)); Spacer(Modifier.height(16.dp)); Text("Creado por Carlos", style = TextStyle(color = NavyDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)); Spacer(Modifier.height(8.dp)); Text("¡Gracias por jugar!", textAlign = TextAlign.Center, fontWeight = FontWeight.Medium) } },
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White
    )
}
