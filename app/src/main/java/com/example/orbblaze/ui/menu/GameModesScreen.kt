package com.example.orbblaze.ui.menu

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.data.AuthManager
import com.example.orbblaze.ui.components.MultiplayerLobby
import com.example.orbblaze.ui.game.DuelViewModel
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.game.SoundType
import com.example.orbblaze.ui.theme.*

data class GameModeItem(
    val id: String,
    val title: String,
    val description: String,
    val brush: Brush,
    val icon: ImageVector,
    val isLocked: Boolean = false,
    val isPrincipal: Boolean = false,
    val isNew: Boolean = false,
    val isWide: Boolean = false
)

@Composable
fun GameModesScreen(
    onModeSelect: (String) -> Unit,
    onBackClick: () -> Unit,
    soundManager: SoundManager,
    duelViewModel: DuelViewModel,
    authManager: AuthManager // ✅ Recibido para pasar al Lobby
) {
    var showLockedDialog by remember { mutableStateOf(false) }
    var showLobby by remember { mutableStateOf(false) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "modes_animations")
    val titleFloat by infiniteTransition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "title_float"
    )

    val configuration = LocalConfiguration.current
    val fontScale = (configuration.screenWidthDp.toFloat() / 411f).coerceIn(0.6f, 1.5f)

    val modes = listOf(
        GameModeItem("duel", "DUELO 1V1", "¡Compite en vivo contra el mundo!", Brush.linearGradient(colors = listOf(Color(0xFFFF5722), Color(0xFFFFAB40))), Icons.Default.Public, isNew = true, isWide = true),
        GameModeItem("game", "CLÁSICO", "¡Sobrevive!", Brush.linearGradient(colors = listOf(SageGreen, Color(0xFFB2DFDB))), Icons.Default.PlayArrow, isPrincipal = true),
        GameModeItem("time_attack", "TIEMPO", "¡Sé rápido!", Brush.linearGradient(colors = listOf(Color(0xFF00E676), Color(0xFF69F0AE))), Icons.Default.Timer),
        GameModeItem("adventure_map", "MODO AVENTURA", "¡Explora mundos y supera retos!", Brush.linearGradient(colors = listOf(Color(0xFFFFD700), Color(0xFFFFF176))), Icons.Default.Map, isWide = true),
        GameModeItem("minigames", "MINIJUEGOS", "Diversión", Brush.linearGradient(colors = listOf(Color(0xFF03DAC5), Color(0xFF80CBC4))), Icons.Default.Gamepad, isLocked = true),
        GameModeItem("reverse", "INVERSA", "Reto extra", Brush.linearGradient(colors = listOf(Color(0xFFFF5252), Color(0xFFFF8A80))), Icons.Default.SwapVert, isLocked = true),
        GameModeItem("daily", "DIARIO", "Nuevo puzzle", Brush.linearGradient(colors = listOf(Color(0xFFBB86FC), Color(0xFFE1BEE7))), Icons.Default.Event, isLocked = true, isWide = true)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalFontScale provides fontScale) {
            Column(
                modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = { soundManager.play(SoundType.POP); onBackClick() },
                        modifier = Modifier.align(Alignment.CenterStart).shadow(4.dp, CircleShape).background(Color.White, CircleShape).size((44 * fontScale).dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = NavyDark, modifier = Modifier.size((24 * fontScale).dp))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { translationY = titleFloat }) {
                        Text("ELIGE TU MODO", style = TextStyle(fontSize = (32 * fontScale).sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 2.sp, shadow = Shadow(Color.Black.copy(alpha = 0.2f), Offset(0f, 6f), 10f)))
                        Box(Modifier.padding(top = 2.dp).width(60.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.5f)))
                    }
                    
                    // Pulido Firebase: Icono de sincronización
                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.align(Alignment.CenterEnd).size(20.dp).padding(end = 8.dp))
                }

                // Grid de Modos
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    contentPadding = PaddingValues(bottom = 40.dp, top = 16.dp)
                ) {
                    items(modes, span = { mode -> GridItemSpan(if (mode.isWide) 2 else 1) }) { mode ->
                        ModeFlexibleCard(
                            mode = mode,
                            soundManager = soundManager,
                            onClick = {
                                if (mode.id == "duel") showLobby = true
                                else if (mode.isLocked) showLockedDialog = true
                                else onModeSelect(mode.id)
                            }
                        )
                    }
                }
            }
        }

        if (showLobby) { 
            MultiplayerLobby(
                onClose = { showLobby = false }, 
                onMatchFound = { roomId -> 
                    showLobby = false
                    onModeSelect("duel") 
                }, 
                soundManager = soundManager,
                viewModel = duelViewModel,
                authManager = authManager // ✅ Ahora sí se pasa correctamente
            ) 
        }
        if (showLockedDialog) { OrbBlazeLockedDialog(soundManager = soundManager, onDismiss = { showLockedDialog = false }) }
    }
}

@Composable
fun ModeFlexibleCard(
    mode: GameModeItem,
    soundManager: SoundManager,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1f, label = "scale")
    val fontScale = LocalFontScale.current

    val infiniteTransition = rememberInfiniteTransition(label = "flex_card")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "glow"
    )

    // EL TRUCO: Box con padding externo para que la sombra no se corte
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp) // Espacio vital para la sombra
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(if (mode.isWide) Modifier.height((115 * fontScale).dp) else Modifier.aspectRatio(1f))
            .shadow(
                elevation = if (isPressed) 6.dp else 12.dp,
                shape = RoundedCornerShape(32.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = Color.Black.copy(alpha = 0.4f)
            )
            .clip(RoundedCornerShape(32.dp))
            .background(mode.brush)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { if (!mode.isLocked) { soundManager.play(SoundType.POP); onClick() } else onClick() }
            )
    ) {
        // Icono de fondo decorativo
        Icon(
            imageVector = mode.icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.15f),
            modifier = Modifier
                .size(if (mode.isWide) 110.dp else 90.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 20.dp, y = if (mode.isWide) 0.dp else 20.dp)
        )

        Row(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (mode.isNew || mode.isPrincipal) {
                    Surface(
                        color = Color.White.copy(alpha = 0.3f),
                        shape = CircleShape,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = if (mode.isNew) "NEW" else "TOP",
                            color = Color.White,
                            fontSize = (9 * fontScale).sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = mode.title,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = (if (mode.isWide) 22 else 18).sp * fontScale,
                        fontWeight = FontWeight.Black,
                        shadow = Shadow(Color.Black.copy(alpha = 0.2f), Offset(0f, 4f), 8f)
                    )
                )
                Text(
                    text = if (mode.isLocked) "PRÓXIMAMENTE" else mode.description,
                    style = TextStyle(
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = (if (mode.isWide) 12 else 11).sp * fontScale,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(start = 8.dp)) {
                if (mode.isNew || mode.isPrincipal) {
                    Box(
                        Modifier.size(38.dp)
                        .graphicsLayer { alpha = if (mode.isLocked) 0f else glowAlpha; scaleX = 1.3f; scaleY = 1.3f }
                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                    )
                }
                Icon(
                    imageVector = if (mode.isLocked) Icons.Default.Lock else if (mode.isNew) Icons.Default.FlashOn else mode.icon, 
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun OrbBlazeLockedDialog(soundManager: SoundManager, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { soundManager.play(SoundType.POP); onDismiss() }) {
                Text("ENTENDIDO", color = Color(0xFF1A237E), fontWeight = FontWeight.ExtraBold)
            }
        },
        title = { Text("MODO BLOQUEADO", fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = { Text("Este modo de juego estará disponible en futuras actualizaciones.", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White
    )
}
