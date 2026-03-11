package com.example.orbblaze.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.orbblaze.R
import com.example.orbblaze.data.AuthManager
import com.example.orbblaze.ui.game.AdsManager
import com.example.orbblaze.ui.game.DuelViewModel
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.game.SoundType
import com.example.orbblaze.ui.theme.NavyDark
import com.example.orbblaze.ui.theme.SageGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MultiplayerLobby(
    onClose: () -> Unit,
    onMatchFound: (roomId: String) -> Unit,
    soundManager: SoundManager? = null,
    adsManager: AdsManager? = null,
    viewModel: DuelViewModel = hiltViewModel(),
    authManager: AuthManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isSearching by remember { mutableStateOf(false) }
    var showFriendSelector by remember { mutableStateOf(false) }
    var isWaitingForFriend by remember { mutableStateOf(false) }
    var currentTipIndex by remember { mutableIntStateOf(0) }

    val tips = listOf(
        "¡Usa las paredes para rebotes imposibles!",
        "La bola de fuego atraviesa cualquier color.",
        "Combina 3 o más para limpiar el tablero.",
        "¡Mantén las burbujas lejos de la línea roja!",
        "Los arcoíris combinan con CUALQUIER color."
    )

    val room by viewModel.room.collectAsStateWithLifecycle()
    val friends by authManager.getFriends().collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(room) {
        val currentRoom = room
        if (currentRoom != null && currentRoom.status == "PLAYING") {
            soundManager?.play(SoundType.MATCH_FOUND)
            onMatchFound(currentRoom.roomId)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "lobby")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Restart), label = "pulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Restart), label = "alpha"
    )

    LaunchedEffect(isSearching || isWaitingForFriend) {
        if (isSearching || isWaitingForFriend) {
            while (true) {
                delay(3000)
                currentTipIndex = (currentTipIndex + 1) % tips.size
            }
        }
    }

    Dialog(
        onDismissRequest = {
            viewModel.resetMatchmaking()
            soundManager?.switchToMenuMusic()
            onClose()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF1A237E), Color(0xFF0D47A1))))
        ) {
            // Botón de Cerrar
            IconButton(
                onClick = {
                    viewModel.resetMatchmaking()
                    soundManager?.switchToMenuMusic()
                    onClose()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "DUELO 1V1",
                    fontWeight = FontWeight.Black,
                    fontSize = 36.sp,
                    color = Color.White,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.height(8.dp))
                Box(Modifier.width(60.dp).height(4.dp).clip(CircleShape).background(Color(0xFF64FFDA)))

                Spacer(Modifier.height(48.dp))

                if (!isSearching && !isWaitingForFriend) {
                    Surface(
                        modifier = Modifier.size(120.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.05f),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(60.dp), tint = Color.White.copy(alpha = 0.7f))
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    Text(
                        text = "Compite contra maestros de todo el mundo en tiempo real.",
                        textAlign = TextAlign.Center,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    Spacer(Modifier.height(60.dp))
                    ReferenceButton(
                        text = "BUSCAR RIVAL",
                        backgroundColor = Color(0xFF00E676),
                        contentColor = NavyDark,
                        onClick = {
                            isSearching = true
                            soundManager?.play(SoundType.MATCHMAKING_START)
                            viewModel.findMatch()
                        }
                    )

                    Spacer(Modifier.height(16.dp))
                    ReferenceButton(
                        text = "INVITAR AMIGO",
                        backgroundColor = Color.Transparent,
                        contentColor = Color.White,
                        modifier = Modifier.border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(28.dp)),
                        onClick = { showFriendSelector = true }
                    )
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                        Box(Modifier.size(140.dp).graphicsLayer { scaleX = pulseScale; scaleY = pulseScale; alpha = pulseAlpha }.clip(CircleShape).background(Color(0xFF64FFDA).copy(alpha = 0.5f)))
                        Box(Modifier.size(140.dp).clip(CircleShape).background(Color(0xFF64FFDA)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Search, null, tint = NavyDark, modifier = Modifier.size(56.dp))
                        }
                    }

                    Spacer(Modifier.height(48.dp))
                    Text(
                        text = if (isWaitingForFriend) "ESPERANDO A TU AMIGO..." else "BUSCANDO RIVAL...",
                        fontWeight = FontWeight.Black, color = Color.White, fontSize = 20.sp, letterSpacing = 1.sp
                    )

                    Spacer(Modifier.height(24.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                            AnimatedContent(
                                targetState = tips[currentTipIndex],
                                transitionSpec = { fadeIn() + slideInVertically() togetherWith fadeOut() + slideOutVertically() },
                                label = "tip_anim"
                            ) { tip ->
                                Text(text = tip, textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                    
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(0.8f).height(8.dp).clip(CircleShape),
                        color = Color(0xFF64FFDA),
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )

                    Spacer(Modifier.height(48.dp))
                    TextButton(onClick = {
                        isSearching = false
                        isWaitingForFriend = false
                        viewModel.resetMatchmaking()
                        soundManager?.switchToMenuMusic()
                    }) {
                        Text("CANCELAR", color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        // Diálogo de amigos
        if (showFriendSelector) {
            FriendSelectorDialog(
                friends = friends,
                onDismiss = { showFriendSelector = false },
                onFriendSelected = { friendUid ->
                    showFriendSelector = false
                    isWaitingForFriend = true
                    soundManager?.play(SoundType.MATCHMAKING_START)
                    scope.launch {
                        viewModel.findMatch()
                        delay(1000)
                        val roomId = viewModel.room.value?.roomId
                        if (roomId != null) {
                            authManager.sendDuelInvitation(friendUid, roomId)
                            Toast.makeText(context, "¡Invitación enviada!", Toast.LENGTH_SHORT).show()
                        } else {
                            isWaitingForFriend = false
                            Toast.makeText(context, "Error al crear la sala", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun FriendSelectorDialog(
    friends: List<Map<String, Any>>,
    onDismiss: () -> Unit,
    onFriendSelected: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1A237E),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("INVITAR AMIGO", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)
                Spacer(Modifier.height(16.dp))
                
                if (friends.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No tienes amigos agregados", color = Color.White.copy(alpha = 0.5f), textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(friends) { friend ->
                            val name = friend["displayName"] as? String ?: "Jugador"
                            val uid = friend["uid"] as String
                            val photoUrl = friend["photoUrl"] as? String
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .clickable { onFriendSelected(uid) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                    error = painterResource(R.drawable.ic_launcher_background)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(name, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.Check, null, tint = Color(0xFF64FFDA), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("CERRAR", color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
