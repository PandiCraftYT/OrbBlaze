package com.example.orbblaze.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.theme.NavyDark
import com.example.orbblaze.ui.theme.SageGreen
import kotlinx.coroutines.delay

@Composable
fun MultiplayerLobby(
    onClose: () -> Unit,
    onMatchFound: () -> Unit,
    soundManager: SoundManager? = null
) {
    var isSearching by remember { mutableStateOf(false) }
    var searchProgress by remember { mutableFloatStateOf(0f) }
    var currentTipIndex by remember { mutableIntStateOf(0) }
    
    val tips = listOf(
        "¡Usa las paredes para rebotes imposibles!",
        "La bola de fuego atraviesa cualquier color.",
        "Combina 3 o más para limpiar el tablero.",
        "¡Mantén las burbujas lejos de la línea roja!",
        "Los arcoíris combinan con CUALQUIER color."
    )

    val infiniteTransition = rememberInfiniteTransition(label = "lobby")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Restart), label = "pulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Restart), label = "alpha"
    )

    LaunchedEffect(isSearching) {
        if (isSearching) {
            searchProgress = 0f
            while (searchProgress < 1f) {
                delay(100)
                searchProgress += 0.01f
                if ((searchProgress * 100).toInt() % 20 == 0) {
                    currentTipIndex = (currentTipIndex + 1) % tips.size
                }
            }
            onMatchFound()
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
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) }
                }

                Spacer(Modifier.height(32.dp))

                if (!isSearching) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(80.dp), tint = SageGreen.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))
                    Text("Compite contra maestros de todo el mundo en tiempo real.", textAlign = TextAlign.Center, color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    
                    Spacer(Modifier.height(32.dp))
                    
                    ReferenceButton(
                        text = "BUSCAR RIVAL",
                        backgroundColor = SageGreen,
                        contentColor = Color.White,
                        onClick = { isSearching = true }
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    
                    ReferenceButton(
                        text = "INVITAR AMIGO",
                        backgroundColor = Color.White,
                        contentColor = NavyDark,
                        modifier = Modifier.border(2.dp, NavyDark.copy(alpha = 0.1f), RoundedCornerShape(28.dp)),
                        onClick = { /* Próximamente Firebase */ }
                    )
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                        Box(Modifier.size(110.dp).graphicsLayer { scaleX = pulseScale; scaleY = pulseScale; alpha = pulseAlpha }.clip(CircleShape).background(SageGreen))
                        Box(Modifier.size(110.dp).clip(CircleShape).background(SageGreen), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Search, null, tint = Color.White, modifier = Modifier.size(44.dp))
                        }
                    }
                    
                    Spacer(Modifier.height(32.dp))
                    
                    Text("BUSCANDO RIVAL...", fontWeight = FontWeight.Black, color = NavyDark, letterSpacing = 1.sp)
                    
                    Spacer(Modifier.height(16.dp))

                    // Tip Dinámico con Animación de entrada/salida
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
                        progress = { searchProgress },
                        modifier = Modifier.padding(top = 16.dp).fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = SageGreen,
                        trackColor = SageGreen.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}
