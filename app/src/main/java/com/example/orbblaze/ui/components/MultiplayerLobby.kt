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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.orbblaze.R
import com.example.orbblaze.data.AuthManager
import com.example.orbblaze.ui.game.DuelViewModel
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.theme.NavyDark
import com.example.orbblaze.ui.theme.SageGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MultiplayerLobby(
    onClose: () -> Unit,
    onMatchFound: (roomId: String) -> Unit,
    soundManager: SoundManager? = null,
    viewModel: DuelViewModel = hiltViewModel(),
    authManager: AuthManager // ✅ Cambiado: Se recibe como parámetro para evitar error de hiltViewModel
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.width(340.dp).padding(16.dp),
            shape = RoundedCornerShape(32.dp),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("DUELO 1V1", fontWeight = FontWeight.Black, fontSize = 24.sp, color = NavyDark)
                    IconButton(onClick = {
                        viewModel.resetMatchmaking() 
                        onClose()
                    }) { 
                        Icon(Icons.Default.Close, contentDescription = "Cerrar") 
                    }
                }

                Spacer(Modifier.height(32.dp))

                if (!isSearching && !isWaitingForFriend) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(80.dp), tint = SageGreen.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))
                    Text("Compite contra maestros de todo el mundo en tiempo real.", textAlign = TextAlign.Center, color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    
                    Spacer(Modifier.height(32.dp))
                    
                    ReferenceButton(
                        text = "BUSCAR RIVAL",
                        backgroundColor = SageGreen,
                        contentColor = Color.White,
                        onClick = { 
                            isSearching = true 
                            viewModel.findMatch()
                        }
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    
                    ReferenceButton(
                        text = "INVITAR AMIGO",
                        backgroundColor = Color.White,
                        contentColor = NavyDark,
                        modifier = Modifier.border(2.dp, NavyDark.copy(alpha = 0.1f), RoundedCornerShape(28.dp)),
                        onClick = { 
                            showFriendSelector = true
                        }
                    )
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                        Box(Modifier.size(110.dp).graphicsLayer { scaleX = pulseScale; scaleY = pulseScale; alpha = pulseAlpha }.clip(CircleShape).background(SageGreen))
                        Box(Modifier.size(110.dp).clip(CircleShape).background(SageGreen), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Search, null, tint = Color.White, modifier = Modifier.size(44.dp))
                        }
                    }
                    
                    Spacer(Modifier.height(32.dp))
                    
                    Text(
                        text = if (isWaitingForFriend) "ESPERANDO A TU AMIGO..." else "BUSCANDO RIVAL...",
                        fontWeight = FontWeight.Black, 
                        color = NavyDark, 
                        letterSpacing = 1.sp
                    )
                    
                    Spacer(Modifier.height(16.dp))

                    AnimatedContent(
                        targetState = tips[currentTipIndex],
                        transitionSpec = { fadeIn() + slideInVertically() togetherWith fadeOut() + slideOutVertically() },
                        label = "tip_anim"
                    ) { tip ->
                        Text(
                            text = tip,
                            textAlign = TextAlign.Center,
                            color = Color.Gray,
                            fontSize = 13.sp,
                            modifier = Modifier.height(40.dp).padding(horizontal = 8.dp)
                        )
                    }

                    LinearProgressIndicator(
                        modifier = Modifier.padding(top = 16.dp).fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = SageGreen,
                        trackColor = SageGreen.copy(alpha = 0.1f)
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { 
                        isSearching = false
                        isWaitingForFriend = false
                        viewModel.resetMatchmaking()
                    }) {
                        Text("CANCELAR", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showFriendSelector) {
        FriendSelectorDialog(
            friends = friends,
            onDismiss = { showFriendSelector = false },
            onFriendSelected = { friendUid ->
                showFriendSelector = false
                isWaitingForFriend = true
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
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("INVITAR AMIGO", fontWeight = FontWeight.Black, fontSize = 20.sp, color = NavyDark)
                Spacer(Modifier.height(16.dp))
                
                if (friends.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No tienes amigos agregados", color = Color.Gray, textAlign = TextAlign.Center)
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
                                    .background(Color.Gray.copy(alpha = 0.05f))
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
                                Text(name, fontWeight = FontWeight.Bold, color = NavyDark, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.Check, null, tint = SageGreen, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("CERRAR", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
