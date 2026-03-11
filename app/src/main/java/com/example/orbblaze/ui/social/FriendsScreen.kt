package com.example.orbblaze.ui.social

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.example.orbblaze.domain.model.Rank
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.game.SoundType
import com.example.orbblaze.ui.menu.LocalFontScale
import kotlinx.coroutines.launch

private val NavyDark = Color(0xFF2D324F)
private val StarGold = Color(0xFFFFD700)

@Composable
fun FriendsScreen(
    authManager: AuthManager,
    soundManager: SoundManager,
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("MIS AMIGOS", "SOLICITUDES", "BUSCAR")
    
    // Cargamos la lista de amigos una sola vez para toda la pantalla
    val friends by authManager.getFriends().collectAsState(initial = emptyList())
    val friendUids = remember(friends) { friends.map { it["uid"] as String } }

    val infiniteTransition = rememberInfiniteTransition(label = "community_animations")
    val titleFloat by infiniteTransition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "title_float"
    )

    val configuration = LocalConfiguration.current
    val fontScale = (configuration.screenWidthDp.toFloat() / 411f).coerceIn(0.6f, 1.5f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header con botón de volver estático y título con movimiento
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

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { translationY = titleFloat }
            ) {
                Text(
                    text = "COMUNIDAD",
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        fontSize = (38 * fontScale).sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 2.sp,
                        shadow = Shadow(Color.Black.copy(alpha = 0.15f), androidx.compose.ui.geometry.Offset(0f, 8f), 12f)
                    )
                )
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .width(80.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.5f))
                )
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color.White
                )
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { 
                        selectedTab = index 
                        soundManager.play(SoundType.POP)
                    },
                    text = {
                        Text(
                            title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (selectedTab == index) Color.White else Color.White.copy(alpha = 0.6f)
                        )
                    }
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shadow(12.dp, RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            color = Color.White.copy(alpha = 0.92f),
            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.5f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> MyFriendsList(authManager, friends)
                    1 -> RequestsList(authManager)
                    2 -> SearchFriendsTab(authManager, friendUids)
                }
            }
        }
    }
}

@Composable
fun SearchFriendsTab(authManager: AuthManager, friendUids: List<String>) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentUser by authManager.user.collectAsState()
    val isAnonymous = currentUser?.isAnonymous ?: true

    var searchQuery by remember { mutableStateOf("") }
    var searchResult by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var requestStatus by remember { mutableStateOf<Boolean?>(null) }
    
    var selectedUserForProfile by remember { mutableStateOf<Map<String, Any>?>(null) }

    if (isAnonymous) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    "¡CONECTA TU CUENTA!\n\nDebes iniciar sesión con Google para buscar y agregar amigos.",
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    } else {
        Column(Modifier.padding(20.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it.trim().uppercase()
                    if (searchResult != null) { searchResult = null; requestStatus = null }
                },
                placeholder = { Text("Introduce el ID (Ej: ORB-MTX4F2)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = {
                        val cleanQuery = searchQuery.trim().uppercase()
                        if (cleanQuery.isNotBlank()) {
                            scope.launch {
                                isSearching = true
                                searchResult = authManager.findUserByPlayerId(cleanQuery)
                                isSearching = false
                                requestStatus = null
                                if (searchResult == null) {
                                    Toast.makeText(context, "ID no encontrado", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Default.Search, null, tint = NavyDark)
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            if (isSearching) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NavyDark)
                }
            } else {
                searchResult?.let { user ->
                    val userUid = user["uid"] as String
                    val isMe = userUid == currentUser?.uid
                    val isFriend = friendUids.contains(userUid)

                    UserCard(
                        name = user["displayName"] as? String ?: "Jugador",
                        photoUrl = user["photoUrl"] as? String,
                        status = requestStatus,
                        isMe = isMe,
                        isFriend = isFriend,
                        onClick = { selectedUserForProfile = user },
                        onAddClick = {
                            scope.launch {
                                val success = authManager.sendFriendRequest(userUid)
                                requestStatus = success
                                if (!success) {
                                    Toast.makeText(context, "No se pudo enviar la solicitud", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (selectedUserForProfile != null) {
        val userUid = selectedUserForProfile!!["uid"] as String
        val isMe = userUid == currentUser?.uid
        val isFriend = friendUids.contains(userUid)

        FriendProfileDialog(
            data = selectedUserForProfile!!,
            onDismiss = { selectedUserForProfile = null },
            isMe = isMe,
            isFriend = isFriend,
            onToggleFavorite = { },
            onRemove = { },
            onAddFriend = {
                scope.launch {
                    val success = authManager.sendFriendRequest(userUid)
                    requestStatus = success
                    selectedUserForProfile = null
                    if (success) Toast.makeText(context, "Solicitud enviada", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun RequestsList(authManager: AuthManager) {
    val requests by authManager.getFriendRequests().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedRequestProfile by remember { mutableStateOf<Map<String, Any>?>(null) }

    if (requests.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Email, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                Spacer(Modifier.height(16.dp))
                Text("No tienes solicitudes pendientes", color = Color.Gray, textAlign = TextAlign.Center)
            }
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(requests) { req ->
                RequestCard(
                    name = req["fromName"] as? String ?: "Jugador",
                    photoUrl = req["fromPhoto"] as? String,
                    onClick = {
                        scope.launch {
                            val profile = authManager.getUserProfile(req["fromUid"] as String)
                            if (profile != null) {
                                selectedRequestProfile = profile
                            }
                        }
                    },
                    onAccept = {
                        scope.launch {
                            val success = authManager.acceptFriendRequest(req["fromUid"] as String)
                            if (success) Toast.makeText(context, "¡Amigo agregado!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onReject = {
                        scope.launch {
                            val success = authManager.rejectFriendRequest(req["fromUid"] as String)
                            if (success) Toast.makeText(context, "Solicitud rechazada", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    if (selectedRequestProfile != null) {
        FriendProfileDialog(
            data = selectedRequestProfile!!,
            onDismiss = { selectedRequestProfile = null },
            isMe = false,
            isFriend = false,
            isRequest = true,
            onToggleFavorite = { },
            onRemove = { },
            onAddFriend = { },
            onAcceptRequest = {
                scope.launch {
                    val success = authManager.acceptFriendRequest(selectedRequestProfile!!["uid"] as String)
                    if (success) Toast.makeText(context, "¡Amigo agregado!", Toast.LENGTH_SHORT).show()
                    selectedRequestProfile = null
                }
            },
            onRejectRequest = {
                scope.launch {
                    val success = authManager.rejectFriendRequest(selectedRequestProfile!!["uid"] as String)
                    if (success) Toast.makeText(context, "Solicitud rechazada", Toast.LENGTH_SHORT).show()
                    selectedRequestProfile = null
                }
            }
        )
    }
}

@Composable
fun MyFriendsList(authManager: AuthManager, friends: List<Map<String, Any>>) {
    var selectedFriend by remember { mutableStateOf<Map<String, Any>?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val sortedFriends = friends.sortedByDescending { it["isFavorite"] as? Boolean ?: false }

    if (friends.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                Spacer(Modifier.height(16.dp))
                Text("Aún no tienes amigos agregados", color = Color.Gray, textAlign = TextAlign.Center)
            }
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(sortedFriends) { friend ->
                FriendCard(
                    name = friend["displayName"] as? String ?: "Jugador",
                    photoUrl = friend["photoUrl"] as? String,
                    isFavorite = friend["isFavorite"] as? Boolean ?: false,
                    onClick = { selectedFriend = friend }
                )
            }
        }
    }

    if (selectedFriend != null) {
        FriendProfileDialog(
            data = selectedFriend!!,
            onDismiss = { selectedFriend = null },
            isMe = false,
            isFriend = true,
            onToggleFavorite = {
                scope.launch {
                    val isFav = selectedFriend!!["isFavorite"] as? Boolean ?: false
                    authManager.toggleFavoriteFriend(selectedFriend!!["uid"] as String, !isFav)
                    selectedFriend = null
                }
            },
            onRemove = {
                scope.launch {
                    val success = authManager.removeFriend(selectedFriend!!["uid"] as String)
                    if (success) {
                        selectedFriend = null
                        Toast.makeText(context, "Amigo eliminado", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onAddFriend = { }
        )
    }
}

@Composable
fun FriendProfileDialog(
    data: Map<String, Any>, 
    onDismiss: () -> Unit, 
    isMe: Boolean,
    isFriend: Boolean,
    isRequest: Boolean = false,
    onToggleFavorite: () -> Unit, 
    onRemove: () -> Unit,
    onAddFriend: () -> Unit,
    onAcceptRequest: () -> Unit = {},
    onRejectRequest: () -> Unit = {}
) {
    val name = data["displayName"] as? String ?: "Jugador"
    val photoUrl = data["photoUrl"] as? String
    val isFavorite = data["isFavorite"] as? Boolean ?: false
    val stars = (data["level_stars_all_total"] as? Number)?.toInt() ?: 0 
    val level = (data["adventure_progress"] as? Number)?.toInt() ?: 0
    val coins = (data["coins"] as? Number)?.toInt() ?: 0
    val duelElo = (data["duel_elo"] as? Number)?.toInt() ?: 1000
    val rank = Rank.fromElo(duelElo)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(32.dp), 
            color = Color(0xFFF8F9FF), // Un blanco azulado muy limpio
            modifier = Modifier.padding(16.dp).shadow(24.dp, RoundedCornerShape(32.dp))
        ) {
            Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                // AVATAR CON ANILLO DE GRADIENTE DEL RANGO
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .background(
                                Brush.sweepGradient(listOf(rank.color.copy(alpha = 0.6f), rank.color, rank.color.copy(alpha = 0.6f))),
                                CircleShape
                            )
                    )
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(102.dp)
                            .clip(CircleShape)
                            .border(4.dp, Color.White, CircleShape),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_launcher_background)
                    )
                    
                    // Medalla de rango pequeña en la esquina
                    Box(
                        modifier = Modifier.size(32.dp).align(Alignment.BottomEnd).offset(x = (-4).dp, y = (-4).dp)
                            .background(Color.White, CircleShape).border(1.dp, rank.color, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(rank.medalName, fontSize = 16.sp)
                    }
                }
                
                Spacer(Modifier.height(20.dp))
                
                Text(
                    text = name, 
                    fontWeight = FontWeight.Black, 
                    fontSize = 24.sp, 
                    color = NavyDark,
                    textAlign = TextAlign.Center
                )
                
                // RANGO TEXTO
                Text(
                    text = "RANGO ${rank.title}",
                    color = rank.color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                
                Surface(
                    color = Color.Black.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = "ID: ORB-${(data["uid"] as String).takeLast(6).uppercase()}", 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                
                Spacer(Modifier.height(28.dp))
                
                // STATS CARDS
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(Modifier.weight(1f), Icons.Default.Star, "$stars", "ESTRELLAS", Color(0xFFFFD700))
                    StatCard(Modifier.weight(1f), Icons.Default.Explore, "Lvl $level", "AVANCE", Color(0xFF2196F3))
                    StatCard(Modifier.weight(1f), Icons.Default.MonetizationOn, "$coins", "ORBES", Color(0xFF4CAF50))
                }

                Spacer(Modifier.height(32.dp))

                if (!isMe) {
                    if (isFriend) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(
                                onClick = onToggleFavorite, 
                                modifier = Modifier.weight(1.2f).height(54.dp), 
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if(isFavorite) Color(0xFF9E9E9E) else Color(0xFFFFC107)),
                                elevation = ButtonDefaults.buttonElevation(4.dp)
                            ) {
                                Icon(if(isFavorite) Icons.Default.StarOutline else Icons.Default.Star, null, tint = Color.White)
                                Spacer(Modifier.width(6.dp))
                                Text(if(isFavorite) "QUITAR" else "FAVORITO", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            
                            Surface(
                                onClick = onRemove,
                                modifier = Modifier.size(54.dp),
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFFFFEBEE),
                                border = BorderStroke(1.dp, Color(0xFFFFCDD2))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.DeleteForever, null, tint = Color(0xFFEF5350))
                                }
                            }
                        }
                    } else if (isRequest) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = onRejectRequest,
                                modifier = Modifier.weight(1f).height(54.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE))
                            ) {
                                Text("RECHAZAR", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onAcceptRequest,
                                modifier = Modifier.weight(1f).height(54.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F5E9))
                            ) {
                                Text("ACEPTAR", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Button(
                            onClick = onAddFriend,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            elevation = ButtonDefaults.buttonElevation(6.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, null, tint = Color.White)
                            Spacer(Modifier.width(12.dp))
                            Text("ENVIAR SOLICITUD", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    }
                }
                
                TextButton(
                    onClick = onDismiss, 
                    modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
                ) {
                    Text("VOLVER", color = Color.Gray, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier, icon: ImageVector, value: String, label: String, color: Color) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Black, color = NavyDark, fontSize = 15.sp)
            Text(label, fontWeight = FontWeight.Bold, color = Color.LightGray, fontSize = 8.sp)
        }
    }
}

@Composable
fun StatBox(icon: ImageVector, text: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Text(text, fontWeight = FontWeight.Bold, color = NavyDark, fontSize = 14.sp)
    }
}

@Composable
fun FriendCard(name: String, photoUrl: String?, isFavorite: Boolean, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(20.dp), color = Color.White, border = BorderStroke(1.dp, if(isFavorite) StarGold.copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.3f)), shadowElevation = 2.dp) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = photoUrl, contentDescription = null, modifier = Modifier.size(45.dp).clip(CircleShape).background(Color.LightGray.copy(alpha = 0.2f)), contentScale = ContentScale.Crop, error = painterResource(R.drawable.ic_launcher_background))
            Spacer(Modifier.width(12.dp))
            Text(name, fontWeight = FontWeight.Bold, color = NavyDark, modifier = Modifier.weight(1f))
            if (isFavorite) Icon(Icons.Default.Star, null, tint = StarGold, modifier = Modifier.size(20.dp))
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
        }
    }
}

@Composable
fun RequestCard(name: String, photoUrl: String?, onClick: () -> Unit, onAccept: () -> Unit, onReject: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(20.dp), color = Color.White, border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)), shadowElevation = 2.dp) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = photoUrl, contentDescription = null, modifier = Modifier.size(45.dp).clip(CircleShape).background(Color.LightGray.copy(alpha = 0.2f)), contentScale = ContentScale.Crop, error = painterResource(R.drawable.ic_launcher_background))
            Spacer(Modifier.width(12.dp))
            Text(name, fontWeight = FontWeight.Bold, color = NavyDark, modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onReject, modifier = Modifier.background(Color(0xFFFEE2E2), CircleShape).size(36.dp)) { Icon(Icons.Default.Close, null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp)) }
                IconButton(onClick = onAccept, modifier = Modifier.background(Color(0xFFE8F5E9), CircleShape).size(36.dp)) { Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp)) }
            }
        }
    }
}

@Composable
fun UserCard(
    name: String, 
    photoUrl: String?, 
    status: Boolean?, 
    isMe: Boolean, 
    isFriend: Boolean,
    onClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)),
        shadowElevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = photoUrl, contentDescription = null, modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.LightGray.copy(alpha = 0.2f)), contentScale = ContentScale.Crop, error = painterResource(R.drawable.ic_launcher_background))
            Spacer(Modifier.width(16.dp))
            Text(text = name, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyDark), modifier = Modifier.weight(1f))
            
            if (!isMe) {
                if (!isFriend) {
                    IconButton(
                        onClick = { if (status == null) onAddClick() },
                        modifier = Modifier.size(40.dp).background(when(status) { true -> Color(0xFFE8F5E9); false -> Color(0xFFFEE2E2); else -> Color(0xFFE3F2FD) }, CircleShape)
                    ) {
                        Icon(imageVector = when(status) { true -> Icons.Default.Check; false -> Icons.Default.Close; else -> Icons.Default.Add }, null, tint = when(status) { true -> Color(0xFF4CAF50); false -> Color(0xFFEF4444); else -> Color(0xFF2196F3) }, modifier = Modifier.size(20.dp))
                    }
                } else {
                    Icon(Icons.Default.People, null, tint = Color(0xFF4CAF50), modifier = Modifier.padding(end = 8.dp))
                }
            } else {
                Text("TÚ", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
            }
        }
    }
}
