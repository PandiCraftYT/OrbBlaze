package com.example.orbblaze.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.R
import com.example.orbblaze.data.AuthManager
import com.example.orbblaze.data.SettingsManager
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.game.SoundType
import com.example.orbblaze.ui.menu.LocalFontScale
import com.example.orbblaze.ui.menu.ReferenceButton
import com.example.orbblaze.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

private val SageGreen = Color(0xFF8DA094)
private val NavyDark = Color(0xFF2D324F)
private val StarGold = Color(0xFFF4C491)

@Composable
fun SettingsScreen(
    soundManager: SoundManager,
    settingsManager: SettingsManager,
    authManager: AuthManager,
    onBackClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val currentUser by authManager.user.collectAsState()
    val isAnonymous = currentUser?.isAnonymous ?: true
    val userEmail = currentUser?.email
    val sessionError by authManager.sessionError.collectAsState()

    var showConflictDialog by remember { mutableStateOf(false) }
    var cloudDataToCompare by remember { mutableStateOf<Map<String, Any>?>(null) }
    var localDataToCompare by remember { mutableStateOf<Map<String, Any>?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "settings_animations")
    var showAboutDialog by remember { mutableStateOf(false) }

    val webClientId = "16414219373-43f70abac7dp5v3tbvvq6lndspdcsh0i.apps.googleusercontent.com"
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

    LaunchedEffect(Unit) {
        authManager.refreshUser()
    }

    if (sessionError != null) {
        AlertDialog(
            onDismissRequest = { authManager.clearSessionError() },
            icon = { Icon(Icons.Default.Warning, null, tint = Color(0xFFEF4444)) },
            title = { Text("SESIÓN EXPIRADA", fontWeight = FontWeight.Black) },
            text = { Text(sessionError ?: "") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            settingsManager.clearAllData()
                            authManager.clearSessionError()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("ENTENDIDO", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White
        )
    }

    if (showConflictDialog && cloudDataToCompare != null && localDataToCompare != null) {
        ConflictDialog(
            localData = localDataToCompare!!,
            cloudData = cloudDataToCompare!!,
            onUseLocal = {
                scope.launch {
                    authManager.saveProgressToCloud(localDataToCompare!!)
                    showConflictDialog = false
                    Toast.makeText(context, "Progreso local guardado en la nube", Toast.LENGTH_SHORT).show()
                }
            },
            onUseCloud = {
                scope.launch {
                    settingsManager.updateFromSyncableData(cloudDataToCompare!!)
                    showConflictDialog = false
                    Toast.makeText(context, "Progreso de la nube recuperado", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    scope.launch {
                        // 1. Guardamos progreso local actual
                        val currentLocalProgress = settingsManager.getSyncableData()
                        val localCoins = (currentLocalProgress["coins"] as? Number)?.toInt() ?: 0
                        val localScore = (currentLocalProgress["high_score"] as? Number)?.toInt() ?: 0

                        val linkResult = authManager.linkWithGoogle(idToken)
                        if (linkResult.isSuccess) {
                            authManager.saveProgressToCloud(currentLocalProgress)
                            val cloudData = authManager.loadProgressFromCloud()
                            cloudData?.let { settingsManager.updateFromSyncableData(it) }
                            Toast.makeText(context, "¡Sesión iniciada!", Toast.LENGTH_LONG).show()
                        } else {
                            val errorMsg = linkResult.exceptionOrNull()?.message?.lowercase() ?: ""
                            if (errorMsg.contains("associated") || errorMsg.contains("already-in-use") || errorMsg.contains("collision")) {
                                // PASO CLAVE: Borramos el anónimo para que no quede en la consola
                                authManager.deleteCurrentUser()
                                
                                // Iniciamos sesión con Google
                                val user = authManager.signInWithGoogle(idToken)
                                if (user != null) {
                                    val cloudData = authManager.loadProgressFromCloud()
                                    
                                    // Si no hay progreso local real, cargamos nube directo
                                    if (localCoins == 0 && localScore == 0) {
                                        cloudData?.let { settingsManager.updateFromSyncableData(it) }
                                        Toast.makeText(context, "Progreso recuperado de la nube", Toast.LENGTH_SHORT).show()
                                    } else if (cloudData != null && cloudData.isNotEmpty()) {
                                        localDataToCompare = currentLocalProgress
                                        cloudDataToCompare = cloudData
                                        showConflictDialog = true
                                    } else {
                                        authManager.saveProgressToCloud(currentLocalProgress)
                                        Toast.makeText(context, "Cuenta vinculada", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Error: ${linkResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error en Google Sign In", Toast.LENGTH_SHORT).show()
            }
        }
    }

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

                Surface(shape = RoundedCornerShape(28.dp), color = Color.White.copy(alpha = 0.95f), modifier = Modifier.fillMaxWidth(), shadowElevation = 4.dp) {
                    Column(modifier = Modifier.padding(vertical = 24.dp, horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isAnonymous) {
                            Text("SIN VINCULAR", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.2.sp)
                            Spacer(Modifier.height(16.dp))
                            Surface(
                                onClick = {
                                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(webClientId).requestEmail().build()
                                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                },
                                shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFDADCE0)), modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                                    Icon(Icons.Default.AccountCircle, null, tint = Color(0xFF4285F4), modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text("INICIAR SESIÓN CON GOOGLE", style = TextStyle(color = Color(0xFF3C4043), fontSize = 14.sp, fontWeight = FontWeight.Medium))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Guarda tu progreso en la nube", color = Color.Gray.copy(alpha = 0.7f), fontSize = 10.sp, textAlign = TextAlign.Center)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("CONECTADO", color = Color(0xFF4CAF50), fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
                            }
                            userEmail?.let { Text(it, color = NavyDark.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp)) }
                            Spacer(Modifier.height(16.dp))
                            TextButton(onClick = { 
                                scope.launch { 
                                    val currentData = settingsManager.getSyncableData()
                                    authManager.saveProgressToCloud(currentData)
                                    // 🔥 ORDEN LIMPIO: Borramos local -> Cerramos Firebase
                                    settingsManager.clearAllData()
                                    authManager.signOut(context)
                                    Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show() 
                                } 
                            }) {
                                Text("CERRAR SESIÓN", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                Surface(shape = RoundedCornerShape(32.dp), color = Color.White, modifier = Modifier.fillMaxWidth().weight(1f, fill = false).shadow(8.dp, RoundedCornerShape(32.dp))) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SettingSliderItem("EFECTOS DE SONIDO", sfxVolume, color = SageGreen) { scope.launch { settingsManager.setSfxVolume(it) }; soundManager.setSfxVol(it) }
                        SettingSliderItem("MÚSICA", musicVolume, enabled = !isMusicMuted, color = SageGreen) { scope.launch { settingsManager.setMusicVolume(it) }; soundManager.setMusicVol(it) }
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.05f), thickness = 1.dp)
                        SettingsToggleRow("SILENCIAR MÚSICA", isMusicMuted, activeColor = Color(0xFFEF4444)) { scope.launch { settingsManager.setMusicMuted(it) }; soundManager.setMusicMute(it) }
                        SettingsToggleRow("VIBRACIÓN", isVibrationEnabled, activeColor = SageGreen) { scope.launch { settingsManager.setVibrationEnabled(it) } }
                        SettingsToggleRow("DALTONISMO", isColorBlindMode, activeColor = NavyDark) { scope.launch { settingsManager.setColorBlindMode(it) } }
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
fun ConflictDialog(
    localData: Map<String, Any>,
    cloudData: Map<String, Any>,
    onUseLocal: () -> Unit,
    onUseCloud: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("CONFLICTO DE PROGRESO", fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Hemos encontrado una partida guardada en la nube. ¿Cuál quieres conservar?", fontSize = 14.sp, textAlign = TextAlign.Center)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ConflictCard("MÓVIL (LOCAL)", localData, Modifier.weight(1f), Color(0xFF4285F4))
                    ConflictCard("NUBE (GOOGLE)", cloudData, Modifier.weight(1f), Color(0xFF4CAF50))
                }
            }
        },
        confirmButton = {
            Button(onClick = onUseCloud, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                Text("USAR PROGRESO DE LA NUBE", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onUseLocal, modifier = Modifier.fillMaxWidth()) {
                Text("USAR PROGRESO ACTUAL (LOCAL)", color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White
    )
}

@Composable
fun ConflictCard(title: String, data: Map<String, Any>, modifier: Modifier, accentColor: Color) {
    val coins = (data["coins"] as? Number)?.toInt() ?: 0
    val score = (data["high_score"] as? Number)?.toInt() ?: 0
    val level = (data["adventure_progress"] as? Number)?.toInt() ?: 0

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = accentColor.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.Black, color = accentColor)
            Spacer(Modifier.height(8.dp))
            StatRow(Icons.Default.Star, "$score")
            StatRow(Icons.Default.Place, "Lvl $level")
            StatRow(Icons.Default.ShoppingCart, "$coins")
        }
    }
}

@Composable
fun StatRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icon, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyDark)
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
