package com.example.orbblaze.ui.menu

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.R
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.game.SoundType
import com.example.orbblaze.ui.theme.*

@Composable
fun GameModesScreen(
    onModeSelect: (String) -> Unit,
    onBackClick: () -> Unit,
    soundManager: SoundManager
) {
    var showLockedDialog by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "modes_animations")
    
    val titleFloat by infiniteTransition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "title_float"
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val fontScale = (maxWidth.value / 411f).coerceIn(0.6f, 1.5f)
        
        CompositionLocalProvider(LocalFontScale provides fontScale) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer { translationY = titleFloat }
                ) {
                    Text(
                        text = "ELIGE TU MODO",
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            fontSize = (42 * fontScale).sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 2.sp,
                            shadow = Shadow(Color.Black.copy(alpha = 0.15f), Offset(0f, 8f), 12f)
                        )
                    )
                    Box(modifier = Modifier.padding(top = 4.dp).width(100.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.5f)))
                }

                // ✅ LISTA DE MODOS CON DESCRIPCIONES
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 24.dp)
                ) {
                    item {
                        ModeCardPremium(
                            title = "MODO CLÁSICO", 
                            description = "¡Sobrevive todo lo que puedas!",
                            color = SageGreen, 
                            isLocked = false,
                            isPrincipal = true,
                            soundManager = soundManager,
                            onClick = { onModeSelect("game") }
                        )
                    }
                    
                    item {
                        ModeCardPremium(
                            title = "CONTRA TIEMPO", 
                            description = "¡Sé rápido antes de que acabe el tiempo!",
                            color = Color(0xFF00E676), 
                            isLocked = false,
                            soundManager = soundManager,
                            onClick = { onModeSelect("time_attack") }
                        )
                    }
                    
                    item {
                        ModeCardPremium(
                            title = "MODO AVENTURA", 
                            description = "¡Supera niveles y descubre mundos!",
                            color = Color(0xFFFFD700), 
                            isLocked = false,
                            soundManager = soundManager,
                            onClick = { onModeSelect("adventure_map") }
                        )
                    }
                    
                    item {
                        ModeCardPremium(
                            title = "MODO INVERSA", 
                            description = "Nuevos desafíos próximamente.",
                            color = Color(0xFFFF5252), 
                            soundManager = soundManager,
                            onClick = { showLockedDialog = true }
                        )
                    }
                    
                    item {
                        ModeCardPremium(
                            title = "PUZZLE DIARIO", 
                            description = "Un rompecabezas nuevo cada día.",
                            color = Color(0xFFBB86FC), 
                            soundManager = soundManager,
                            onClick = { showLockedDialog = true }
                        )
                    }
                    item {
                        ModeCardPremium(
                            title = "MINIJUEGOS", 
                            description = "Diversión rápida y variada.",
                            color = Color(0xFF03DAC5), 
                            soundManager = soundManager,
                            onClick = { showLockedDialog = true }
                        )
                    }
                }

                ReferenceButton(
                    text = "VOLVER",
                    backgroundColor = Color.White,
                    contentColor = Color.Gray,
                    modifier = Modifier.width(200.dp),
                    soundManager = soundManager,
                    onClick = onBackClick
                )
            }
        }

        if (showLockedDialog) {
            OrbBlazeLockedDialog(soundManager = soundManager, onDismiss = { showLockedDialog = false })
        }
    }
}

@Composable
fun ModeCardPremium(
    title: String,
    description: String, // Parámetro añadido
    color: Color,
    isLocked: Boolean = true,
    isPrincipal: Boolean = false,
    soundManager: SoundManager,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "scale")
    val fontScale = LocalFontScale.current
    
    Surface(
        onClick = {
            soundManager.play(SoundType.POP)
            onClick()
        },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isPrincipal) (100 * fontScale).dp else (85 * fontScale).dp) // Un poco más alto para la descripción
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (isPressed) 2.dp else 6.dp, RoundedCornerShape(28.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title, 
                    style = TextStyle(
                        color = if(isLocked) Color.Gray else color, 
                        fontSize = ((if (isPrincipal) 18 else 16) * fontScale).sp, 
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = description, 
                    style = TextStyle(
                        color = if(isLocked) Color.LightGray else Color.Gray, 
                        fontSize = (11 * fontScale).sp, 
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isLocked) "PRÓXIMAMENTE" else if (isPrincipal) "MODO RECOMENDADO" else "DISPONIBLE", 
                    style = TextStyle(
                        color = if(isLocked) Color.LightGray else color.copy(alpha = 0.6f), 
                        fontSize = (9 * fontScale).sp, 
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                )
            }
            
            if (isLocked) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color.LightGray)
            } else if (isPrincipal) {
                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            } else {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = color.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun ReferenceButton(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconColor: Color = Color.Unspecified,
    soundManager: SoundManager? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "scale")
    val fontScale = LocalFontScale.current

    Surface(
        onClick = {
            soundManager?.play(SoundType.POP)
            onClick()
        },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(28.dp),
        color = backgroundColor,
        modifier = modifier
            .fillMaxWidth()
            .height((64 * fontScale).dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (isPressed) 2.dp else 4.dp, RoundedCornerShape(28.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            
            Text(
                text = text.uppercase(),
                style = TextStyle(
                    fontSize = (16 * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    letterSpacing = 1.sp
                )
            )
        }
    }
}

@Composable
fun OrbBlazeLockedDialog(soundManager: SoundManager, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                soundManager.play(SoundType.POP)
                onDismiss()
            }) {
                Text("ENTENDIDO", color = Color(0xFF1A237E), fontWeight = FontWeight.ExtraBold)
            }
        },
        title = {
            Text("MODO BLOQUEADO", fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = {
            Text("Este modo de juego estará disponible en futuras actualizaciones.", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White
    )
}
