package com.example.orbblaze.ui.settings

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
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
import com.example.orbblaze.ui.components.GlobalBackground
import com.example.orbblaze.ui.game.AdsManager
import kotlinx.coroutines.launch

private val NavyDark = Color(0xFF2D324F)
private val StarGold = Color(0xFFF4C491)

@Composable
fun ProfileScreen(
    authManager: AuthManager,
    settingsManager: SettingsManager,
    adsManager: AdsManager,
    onBackClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val currentUser by authManager.user.collectAsState()
    val isAnonymous = currentUser?.isAnonymous ?: true
    val sessionError by authManager.sessionError.collectAsState()

    // Estadísticas
    val allStars by settingsManager.allStarsFlow.collectAsState(initial = emptyMap())
    val totalStars = allStars.values.sum()
    val coins by settingsManager.coinsFlow.collectAsState(initial = 0)
    val adventureProgress by settingsManager.adventureProgressFlow.collectAsState(initial = 0)
    val nameChangesCount by settingsManager.nameChangesCountFlow.collectAsState(initial = 0)
    val adsWatchedForName by settingsManager.nameChangeAdsWatchedFlow.collectAsState(initial = 0)

    var showAvatarPicker by remember { mutableStateOf(false) }
    var showNameEditor by remember { mutableStateOf(false) }
    var showPayDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    val webClientId = "16414219373-43f70abac7dp5v3tbvvq6lndspdcsh0i.apps.googleusercontent.com"

    // Sincronizar al entrar
    LaunchedEffect(currentUser) {
        if (currentUser != null && !isAnonymous) {
            authManager.saveProgressToCloud(settingsManager.getSyncableData())
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    scope.launch {
                        val currentLocalProgress = settingsManager.getSyncableData()
                        val linkResult = authManager.linkWithGoogle(idToken)
                        if (linkResult.isSuccess) {
                            authManager.saveProgressToCloud(currentLocalProgress)
                            val cloudData = authManager.loadProgressFromCloud()
                            cloudData?.let { settingsManager.updateFromSyncableData(it) }
                        } else {
                            val errorMsg = linkResult.exceptionOrNull()?.message?.lowercase() ?: ""
                            if (errorMsg.contains("associated") || errorMsg.contains("already-in-use") || errorMsg.contains("collision")) {
                                authManager.deleteCurrentUser()
                                val user = authManager.signInWithGoogle(idToken)
                                if (user != null) {
                                    val cloudData = authManager.loadProgressFromCloud()
                                    if (cloudData != null && cloudData.isNotEmpty()) {
                                        settingsManager.updateFromSyncableData(cloudData)
                                    } else {
                                        authManager.saveProgressToCloud(currentLocalProgress)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error en Google Sign In", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "MI PERFIL",
                style = TextStyle(
                    fontSize = 32.sp, 
                    fontWeight = FontWeight.Black, 
                    color = Color.White,
                    shadow = Shadow(Color.Black.copy(alpha = 0.3f), Offset(0f, 4f), 12f)
                )
            )
            
            Spacer(Modifier.height(40.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().shadow(15.dp, RoundedCornerShape(32.dp)),
                shape = RoundedCornerShape(32.dp),
                color = Color.White.copy(alpha = 0.92f),
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.5f))
            ) {
                if (isAnonymous) {
                    Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Lock, null, tint = Color(0xFFEF4444), modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("CUENTA NO VINCULADA", color = Color(0xFFEF4444), fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text("Protege tu progreso sincronizando con Google", color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = {
                                val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(webClientId).requestEmail().build()
                                val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                                googleSignInLauncher.launch(client.signInIntent)
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                        ) {
                            Text("CONECTAR AHORA", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // FOTO
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(85.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(3.dp, Color(0xFF4CAF50), CircleShape)
                                    .clickable { showAvatarPicker = true }
                            ) {
                                AsyncImage(
                                    model = currentUser?.photoUrl?.toString(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    error = painterResource(R.drawable.ic_launcher_background),
                                    placeholder = painterResource(R.drawable.ic_launcher_background)
                                )
                            }
                            Box(
                                modifier = Modifier.size(28.dp).background(NavyDark, CircleShape).border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }

                        Spacer(Modifier.width(20.dp))

                        // INFO
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { 
                                if (nameChangesCount >= 1) showPayDialog = true else { newName = currentUser?.displayName ?: ""; showNameEditor = true }
                            }) {
                                Text(
                                    text = currentUser?.displayName ?: "Jugador", 
                                    fontWeight = FontWeight.Black, 
                                    fontSize = 20.sp,
                                    color = NavyDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                            
                            // ID CON OPCIÓN DE COPIAR
                            val playerId = authManager.getPlayerId()
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Player ID", playerId)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "ID copiado al portapapeles", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Text(
                                    text = "ID: $playerId",
                                    style = TextStyle(fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar ID", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                            }
                            
                            Spacer(Modifier.height(8.dp))
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ProfileStatItem(Icons.Default.Star, "$totalStars", StarGold)
                                ProfileStatItem(Icons.Default.Place, "Lvl $adventureProgress", Color(0xFF2196F3))
                                ProfileStatItem(Icons.Default.ShoppingCart, "$coins", Color(0xFF4CAF50))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(32.dp),
                color = Color.White.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Lock, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "PRÓXIMAMENTE:\nLOGROS GLOBALES", 
                            textAlign = TextAlign.Center, 
                            color = Color.White.copy(alpha = 0.8f), 
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            
            Button(
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth().height(60.dp).shadow(10.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = NavyDark)
            ) {
                Text("VOLVER AL MENÚ", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
        }
    }

    if (showAvatarPicker) {
        AvatarPickerDialog(onDismiss = { showAvatarPicker = false }, onAvatarSelected = { url ->
            scope.launch { authManager.updateProfile(null, url); showAvatarPicker = false }
        })
    }

    if (showPayDialog) {
        AlertDialog(
            onDismissRequest = { showPayDialog = false },
            title = { Text("CAMBIO DE NOMBRE", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ya has cambiado tu nombre una vez gratis. Para hacerlo de nuevo:")
                    Button(onClick = {
                        if (coins >= 5000) {
                            scope.launch {
                                settingsManager.setCoins(coins - 5000)
                                showPayDialog = false
                                showNameEditor = true
                            }
                        } else { Toast.makeText(context, "Monedas insuficientes (5000)", Toast.LENGTH_SHORT).show() }
                    }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                        Text("PAGAR 5000 MONEDAS")
                    }
                    Button(onClick = {
                        adsManager.showRewardedAd(context as Activity) {
                            scope.launch {
                                val newCount = adsWatchedForName + 1
                                settingsManager.setNameChangeAdsWatched(newCount)
                                if (newCount >= 5) {
                                    settingsManager.setNameChangeAdsWatched(0)
                                    showPayDialog = false; showNameEditor = true
                                }
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("VER ANUNCIO ($adsWatchedForName/5)")
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showPayDialog = false }) { Text("CANCELAR") } }
        )
    }

    if (showNameEditor) {
        AlertDialog(
            onDismissRequest = { showNameEditor = false },
            title = { Text("EDITAR NOMBRE") },
            text = { TextField(value = newName, onValueChange = { if (it.length <= 15) newName = it }, placeholder = { Text("Nuevo nombre...") }) },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        if (newName.isNotBlank()) {
                            authManager.updateProfile(newName, null)
                            settingsManager.setNameChangesCount(nameChangesCount + 1)
                        }
                        showNameEditor = false
                    }
                }) { Text("GUARDAR") }
            },
            dismissButton = { TextButton(onClick = { showNameEditor = false }) { Text("CANCELAR") } }
        )
    }
}

@Composable
fun ProfileStatItem(icon: ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = color)
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D324F))
    }
}

@Composable
fun AvatarPickerDialog(onDismiss: () -> Unit, onAvatarSelected: (String) -> Unit) {
    val avatars = listOf(
        "https://api.dicebear.com/7.x/avataaars/png?seed=Felix",
        "https://api.dicebear.com/7.x/avataaars/png?seed=Aneka",
        "https://api.dicebear.com/7.x/pixel-art/png?seed=Gamer1",
        "https://api.dicebear.com/7.x/bottts/png?seed=B1",
        "https://api.dicebear.com/7.x/avataaars/png?seed=Luna",
        "https://api.dicebear.com/7.x/pixel-art/png?seed=Gamer2"
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(32.dp), color = Color.White) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ELIGE TU AVATAR", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF2D324F))
                Spacer(Modifier.height(20.dp))
                LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.height(300.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(avatars) { url: String ->
                        Box(modifier = Modifier.size(100.dp).clip(CircleShape).border(1.dp, Color.LightGray.copy(alpha = 0.5f), CircleShape).clickable { onAvatarSelected(url) }) {
                            AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                TextButton(onClick = onDismiss) { Text("CERRAR", color = Color.Gray, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
