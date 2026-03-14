package com.example.orbblaze.ui.settings

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
import com.example.orbblaze.data.MatchHistoryManager
import com.example.orbblaze.data.SettingsManager
import com.example.orbblaze.domain.model.DuelMatch
import com.example.orbblaze.domain.model.Rank
import com.example.orbblaze.ui.components.rememberGoogleSignInHandler
import com.example.orbblaze.ui.game.AdsManager
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.game.SoundType
import com.example.orbblaze.ui.menu.LocalFontScale
import com.example.orbblaze.ui.theme.SageGreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private val NavyDark = Color(0xFF2D324F)
private val StarGold = Color(0xFFF4C491)

@Composable
fun ProfileScreen(
    authManager: AuthManager,
    settingsManager: SettingsManager,
    adsManager: AdsManager,
    soundManager: SoundManager,
    matchHistoryManager: MatchHistoryManager,
    onBackClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val currentUser by authManager.user.collectAsState()
    val isAnonymous = currentUser?.isAnonymous ?: true

    // Estadísticas
    val allStars by settingsManager.allStarsFlow.collectAsState(initial = emptyMap())
    val totalStars = allStars.values.sum()
    val coins by settingsManager.coinsFlow.collectAsState(initial = 0)
    val adventureProgress by settingsManager.adventureProgressFlow.collectAsState(initial = 0)
    val highScore by settingsManager.highScoreFlow.collectAsState(initial = 0)
    val duelElo by settingsManager.duelEloFlow.collectAsState(initial = 1000)
    val nameChangesCount by settingsManager.nameChangesCountFlow.collectAsState(initial = 0)
    val adsWatchedForName by settingsManager.nameChangeAdsWatchedFlow.collectAsState(initial = 0)

    val currentRank = Rank.fromElo(duelElo)

    var showAvatarPicker by remember { mutableStateOf(false) }
    var showNameEditor by remember { mutableStateOf(false) }
    var showPayDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    var matchHistory by remember { mutableStateOf<List<DuelMatch>>(emptyList()) }
    var isLoadingHistory by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        if (currentUser != null && !isAnonymous) {
            authManager.saveProgressToCloud(settingsManager.getSyncableData())
            isLoadingHistory = true
            matchHistory = matchHistoryManager.getMatchHistory(currentUser!!.uid)
            isLoadingHistory = false
        }
    }

    val configuration = LocalConfiguration.current
    val fontScale = (configuration.screenWidthDp.toFloat() / 411f).coerceIn(0.6f, 1.5f)

    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 40.dp, top = 24.dp)
        ) {
            // Header
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { 
                            soundManager.play(SoundType.POP)
                            onBackClick() 
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .shadow(4.dp, CircleShape)
                            .background(Color.White, CircleShape)
                            .size((48 * fontScale).dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = NavyDark,
                            modifier = Modifier.size((28 * fontScale).dp)
                        )
                    }

                    Text(
                        text = "PERFIL",
                        style = TextStyle(
                            fontSize = (32 * fontScale).sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                    )
                }
            }

            // Info Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().shadow(15.dp, RoundedCornerShape(32.dp)),
                    shape = RoundedCornerShape(32.dp),
                    color = Color.White.copy(alpha = 0.92f),
                    border = BorderStroke(2.dp, Color.White.copy(alpha = 0.5f))
                ) {
                    if (isAnonymous) {
                        Column(Modifier.padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Lock, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("¡CONECTA TU CUENTA!", textAlign = TextAlign.Center, color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    } else {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    Box(
                                        modifier = Modifier
                                            .size(85.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .border(3.dp, currentRank.color, CircleShape)
                                            .clickable { showAvatarPicker = true }
                                    ) {
                                        AsyncImage(
                                            model = currentUser?.photoUrl?.toString(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                            error = painterResource(R.drawable.ic_launcher_background)
                                        )
                                    }
                                    Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(24.dp).background(NavyDark, CircleShape).border(2.dp, Color.White, CircleShape).padding(4.dp))
                                }

                                Spacer(Modifier.width(20.dp))

                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { 
                                        if (nameChangesCount >= 1) showPayDialog = true else { newName = currentUser?.displayName ?: ""; showNameEditor = true }
                                    }) {
                                        Text(text = currentUser?.displayName ?: "Jugador", fontWeight = FontWeight.Black, fontSize = 20.sp, color = NavyDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(Modifier.width(6.dp))
                                        Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                    
                                    Text(text = "RANGO ${currentRank.title}", color = currentRank.color, fontSize = 11.sp, fontWeight = FontWeight.Black)

                                    val playerId = authManager.getPlayerId()
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 2.dp).clickable {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Player ID", playerId)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "ID copiado al portapapeles", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text(text = "ID: $playerId", style = TextStyle(fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold))
                                        Spacer(Modifier.width(6.dp))
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar ID", tint = Color.LightGray, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                ProfileStatBlock(Icons.Default.Star, "ESTRELLAS", "$totalStars", StarGold)
                                ProfileStatBlock(Icons.Default.EmojiEvents, "RÉCORD", "$highScore", Color(0xFFFF9800))
                                ProfileStatBlock(Icons.Default.SportsKabaddi, "RATING", "$duelElo", Color(0xFFF44336))
                                ProfileStatBlock(Icons.Default.ShoppingCart, "MONEDAS", "$coins", Color(0xFF4CAF50))
                            }
                        }
                    }
                }
            }

            if (!isAnonymous) {
                // Historial de Partidas
                item {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "HISTORIAL DE DUELOS",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )

                        if (isLoadingHistory) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else if (matchHistory.isEmpty()) {
                            Text(
                                "No hay duelos registrados aún.",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                items(matchHistory) { match ->
                    MatchHistoryItem(match)
                }
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
            title = { Text("EDITAR NOMBRE", fontWeight = FontWeight.Black) },
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
fun MatchHistoryItem(match: DuelMatch) {
    val isWin = match.result == "WIN"
    val dateFormat = remember { SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()) }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, if (isWin) SageGreen.copy(alpha = 0.5f) else Color.Red.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isWin) SageGreen.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isWin) Icons.Default.EmojiEvents else Icons.Default.SentimentVeryDissatisfied,
                    contentDescription = null,
                    tint = if (isWin) SageGreen else Color.Red,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isWin) "VICTORIA" else "DERROTA",
                    color = if (isWin) SageGreen else Color.Red,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
                Text(
                    text = "vs ${match.opponentName}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = match.timestamp?.let { dateFormat.format(it) } ?: "Reciente",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (match.eloChange >= 0) "+${match.eloChange} ELO" else "${match.eloChange} ELO",
                    color = if (match.eloChange >= 0) SageGreen else Color.Red,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
                Text(
                    text = "${match.score} - ${match.opponentScore}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ProfileStatBlock(icon: ImageVector, label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = color)
        Text(label, fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Gray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = NavyDark)
    }
}

@Composable
fun AvatarPickerDialog(onDismiss: () -> Unit, onAvatarSelected: (String) -> Unit) {
    val playStyleAvatars = listOf(
        "https://api.dicebear.com/7.x/avataaars/png?seed=Felix",
        "https://api.dicebear.com/7.x/avataaars/png?seed=Aneka",
        "https://api.dicebear.com/7.x/pixel-art/png?seed=Gamer1",
        "https://api.dicebear.com/7.x/bottts/png?seed=B1",
        "https://api.dicebear.com/7.x/avataaars/png?seed=Luna",
        "https://api.dicebear.com/7.x/pixel-art/png?seed=Gamer2"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "SELECCIONA TU AVATAR",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = NavyDark
                )
                
                Spacer(Modifier.height(20.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(playStyleAvatars) { url ->
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFFF5F5F5))
                                .border(2.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                                .clickable { onAvatarSelected(url) },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                contentScale = ContentScale.Fit,
                                placeholder = painterResource(R.drawable.ic_launcher_background)
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(20.dp))
                
                TextButton(onClick = onDismiss) {
                    Text("CANCELAR", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
