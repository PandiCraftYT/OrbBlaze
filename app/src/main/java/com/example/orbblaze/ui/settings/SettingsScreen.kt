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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.orbblaze.R
import com.example.orbblaze.data.AuthManager
import com.example.orbblaze.data.SettingsManager
import com.example.orbblaze.ui.game.AdsManager
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.game.SoundType
import com.example.orbblaze.ui.menu.LocalFontScale
import com.example.orbblaze.ui.menu.ReferenceButton
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
    adsManager: AdsManager,
    onBackClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val currentUser by authManager.user.collectAsState()
    val isAnonymous = currentUser?.isAnonymous ?: true
    val sessionError by authManager.sessionError.collectAsState()

    // Estadísticas
    val highScore by settingsManager.highScoreFlow.collectAsState(initial = 0)
    val coins by settingsManager.coinsFlow.collectAsState(initial = 0)
    val adventureProgress by settingsManager.adventureProgressFlow.collectAsState(initial = 0)
    val nameChangesCount by settingsManager.nameChangesCountFlow.collectAsState(initial = 0)
    val adsWatchedForName by settingsManager.nameChangeAdsWatchedFlow.collectAsState(initial = 0)

    var showConflictDialog by remember { mutableStateOf(false) }
    var cloudDataToCompare by remember { mutableStateOf<Map<String, Any>?>(null) }
    var localDataToCompare by remember { mutableStateOf<Map<String, Any>?>(null) }

    var showAvatarPicker by remember { mutableStateOf(false) }
    var showNameEditor by remember { mutableStateOf(false) }
    var showPayDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

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
                            authManager.signInAnonymously()
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
                    authManager.deleteCloudProgress()
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

    if (showAvatarPicker) {
        AvatarPickerDialog(
            onDismiss = { showAvatarPicker = false },
            onAvatarSelected = { url ->
                scope.launch {
                    val success = authManager.updateProfile(null, url)
                    if (success) Toast.makeText(context, "Avatar actualizado", Toast.LENGTH_SHORT).show()
                    showAvatarPicker = false
                }
            }
        )
    }

    if (showPayDialog) {
        AlertDialog(
            onDismissRequest = { showPayDialog = false },
            title = { Text("CAMBIO DE NOMBRE", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ya has cambiado tu nombre anteriormente. Para cambiarlo de nuevo debes:")
                    
                    Button(
                        onClick = {
                            if (coins >= 5000) {
                                scope.launch {
                                    settingsManager.setCoins(coins - 5000)
                                    showPayDialog = false
                                    showNameEditor = true
                                }
                            } else {
                                Toast.makeText(context, "No tienes suficientes monedas (5000)", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.ShoppingCart, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("PAGAR 5000 MONEDAS", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            adsManager.showRewardedAd(context as Activity) {
                                scope.launch {
                                    val newCount = adsWatchedForName + 1
                                    settingsManager.setNameChangeAdsWatched(newCount)
                                    if (newCount >= 5) {
                                        settingsManager.setNameChangeAdsWatched(0)
                                        showPayDialog = false
                                        showNameEditor = true
                                    } else {
                                        Toast.makeText(context, "Llevas $newCount de 5 anuncios", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("VER ANUNCIO ($adsWatchedForName/5)", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPayDialog = false }) { Text("CANCELAR") }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White
        )
    }

    if (showNameEditor) {
        AlertDialog(
            onDismissRequest = { showNameEditor = false },
            title = { Text("EDITAR NOMBRE", fontWeight = FontWeight.Black) },
            text = {
                TextField(
                    value = newName,
                    onValueChange = { if (it.length <= 15) newName = it },
                    placeholder = { Text("Tu nuevo apodo...") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        if (newName.isNotBlank()) {
                            val success = authManager.updateProfile(newName, null)
                            if (success) {
                                settingsManager.setNameChangesCount(nameChangesCount + 1)
                                Toast.makeText(context, "Nombre actualizado", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showNameEditor = false
                    }
                }) {
                    Text("GUARDAR", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameEditor = false }) { Text("CANCELAR") }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White
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
                                authManager.deleteCurrentUser()
                                val user = authManager.signInWithGoogle(idToken)
                                if (user != null) {
                                    val cloudData = authManager.loadProgressFromCloud()
                                    if (localCoins == 0 && localScore == 0) {
                                        cloudData?.let { settingsManager.updateFromSyncableData(it) }
                                        Toast.makeText(context, "Progreso recuperado", Toast.LENGTH_SHORT).show()
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
                // Título
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer { translationY = titleFloat }
                ) {
                    Text(text = "CONFIGURACIÓN", textAlign = TextAlign.Center, style = TextStyle(fontSize = (42 * fontScale).sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 2.sp, shadow = Shadow(Color.Black.copy(alpha = 0.15f), Offset(0f, 8f), 12f)))
                    Box(modifier = Modifier.padding(top = 4.dp).width(100.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.5f)))
                }

                // Sección de Perfil Gamer Horizontal
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 4.dp
                ) {
                    if (isAnonymous) {
                        Column(
                            modifier = Modifier.padding(vertical = 24.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
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
                            Text("Guarda tu progreso en la nube", color = Color.Gray.copy(alpha = 0.7f), fontSize = 10.sp)
                        }
                    } else {
                        // DISEÑO HORIZONTAL
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Foto con Lápiz
                            Box(
                                modifier = Modifier.size(64.dp),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .border(2.dp, Color(0xFF4CAF50), CircleShape)
                                        .clickable { showAvatarPicker = true }
                                ) {
                                    AsyncImage(
                                        model = currentUser?.photoUrl?.toString(),
                                        contentDescription = "Foto",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        error = painterResource(R.drawable.ic_launcher_background),
                                        placeholder = painterResource(R.drawable.ic_launcher_background)
                                    )
                                }
                                Box(
                                    modifier = Modifier.size(20.dp).background(NavyDark, CircleShape).border(1.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Nombre y Stats
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { 
                                        if (nameChangesCount >= 1) {
                                            showPayDialog = true 
                                        } else {
                                            newName = currentUser?.displayName ?: ""
                                            showNameEditor = true 
                                        }
                                    }
                                ) {
                                    Text(
                                        text = currentUser?.displayName ?: "Jugador",
                                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Black, color = NavyDark),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                }
                                
                                // Estadísticas Rápidas
                                Row(
                                    modifier = Modifier.padding(top = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatIconText(Icons.Default.Star, "$highScore", StarGold)
                                    StatIconText(Icons.Default.Place, "Lvl $adventureProgress", Color(0xFF4285F4))
                                    StatIconText(Icons.Default.ShoppingCart, "$coins", Color(0xFF4CAF50))
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(10.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("CONECTADO", color = Color(0xFF4CAF50), fontSize = 8.sp, fontWeight = FontWeight.Black)
                                }
                            }

                            // Botón Salir Rojo
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        val currentData = settingsManager.getSyncableData()
                                        authManager.saveProgressToCloud(currentData)
                                        settingsManager.clearAllData()
                                        authManager.signOut(context)
                                        Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(40.dp).background(Color(0xFFFEE2E2), CircleShape)
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
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
fun StatIconText(icon: ImageVector, text: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(10.dp), tint = tint)
        Spacer(Modifier.width(2.dp))
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NavyDark.copy(alpha = 0.8f))
    }
}

@Composable
fun AvatarPickerDialog(onDismiss: () -> Unit, onAvatarSelected: (String) -> Unit) {
    val avatars = listOf(
        "https://api.dicebear.com/7.x/avataaars/png?seed=Felix",
        "https://api.dicebear.com/7.x/avataaars/png?seed=Aneka",
        "https://api.dicebear.com/7.x/avataaars/png?seed=Harley",
        "https://api.dicebear.com/7.x/avataaars/png?seed=Jack",
        "https://api.dicebear.com/7.x/avataaars/png?seed=Luna",
        "https://api.dicebear.com/7.x/avataaars/png?seed=Milo",
        "https://api.dicebear.com/7.x/pixel-art/png?seed=Gamer1",
        "https://api.dicebear.com/7.x/pixel-art/png?seed=Gamer2",
        "https://api.dicebear.com/7.x/pixel-art/png?seed=Gamer3",
        "https://api.dicebear.com/7.x/pixel-art/png?seed=Gamer4",
        "https://api.dicebear.com/7.x/bottts/png?seed=B1",
        "https://api.dicebear.com/7.x/bottts/png?seed=B2"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color.White, modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ELIGE TU AVATAR", fontWeight = FontWeight.Black, fontSize = 18.sp, color = NavyDark)
                Spacer(Modifier.height(16.dp))
                LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.height(300.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(avatars) { url ->
                        Box(
                            modifier = Modifier.size(80.dp).clip(CircleShape).border(1.dp, Color.LightGray, CircleShape).clickable { onAvatarSelected(url) }
                        ) {
                            AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss) { Text("CERRAR", color = Color.Gray, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun ConflictDialog(localData: Map<String, Any>, cloudData: Map<String, Any>, onUseLocal: () -> Unit, onUseCloud: () -> Unit) {
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
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = accentColor.copy(alpha = 0.05f), border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))) {
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
