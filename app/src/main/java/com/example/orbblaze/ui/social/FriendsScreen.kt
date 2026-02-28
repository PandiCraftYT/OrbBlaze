package com.example.orbblaze.ui.social

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.launch

private val NavyDark = Color(0xFF2D324F)
private val StarGold = Color(0xFFFFD700)

@Composable
fun FriendsScreen(
    authManager: AuthManager,
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("MIS AMIGOS", "SOLICITUDES", "BUSCAR")
    
    // Cargamos la lista de amigos una sola vez para toda la pantalla
    val friends by authManager.getFriends().collectAsState(initial = emptyList())
    val friendUids = remember(friends) { friends.map { it["uid"] as String } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "COMUNIDAD",
            style = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.3f),
                    offset = androidx.compose.ui.geometry.Offset(0f, 4f),
                    blurRadius = 12f
                )
            )
        )

        Spacer(Modifier.height(24.dp))

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
                    onClick = { selectedTab = index },
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

        Spacer(Modifier.height(16.dp))

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

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onBackClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(10.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = NavyDark
            )
        ) {
            Text("VOLVER AL MENÚ", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color.White, modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp).clip(CircleShape).border(3.dp, if(isFavorite && isFriend) StarGold else Color.LightGray, CircleShape),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_launcher_background)
                )
                Spacer(Modifier.height(16.dp))
                Text(name, fontWeight = FontWeight.Black, fontSize = 22.sp, color = NavyDark)
                Text("ID: ORB-${(data["uid"] as String).takeLast(6).uppercase()}", fontSize = 12.sp, color = Color.Gray)
                
                Spacer(Modifier.height(20.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatBox(Icons.Default.Star, "$stars", Color(0xFFFFD700))
                    StatBox(Icons.Default.Place, "Lvl $level", Color(0xFF2196F3))
                    StatBox(Icons.Default.ShoppingCart, "$coins", Color(0xFF4CAF50))
                }

                if (!isMe) {
                    Spacer(Modifier.height(32.dp))
                    if (isFriend) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = onToggleFavorite, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if(isFavorite) Color.Gray else StarGold)) {
                                Icon(if(isFavorite) Icons.Default.StarOutline else Icons.Default.Star, null, tint = Color.White)
                                Text(if(isFavorite) "QUITAR" else "FAV", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp))
                            }
                            Button(onClick = onRemove, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2))) {
                                Icon(Icons.Default.Delete, null, tint = Color.Red)
                                Text("BORRAR", color = Color.Red, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    } else if (isRequest) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = onRejectRequest,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2))
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.Red)
                                Text("RECHAZAR", color = Color.Red, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp))
                            }
                            Button(
                                onClick = onAcceptRequest,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F5E9))
                            ) {
                                Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50))
                                Text("ACEPTAR", color = Color(0xFF4CAF50), fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    } else {
                        Button(
                            onClick = onAddFriend,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Icon(Icons.Default.PersonAdd, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("AGREGAR $name", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 12.dp)) {
                    Text("CERRAR", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
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
