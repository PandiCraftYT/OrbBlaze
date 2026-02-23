package com.example.orbblaze.ui.score

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.data.SettingsManager
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.menu.LocalFontScale
import com.example.orbblaze.ui.menu.ReferenceButton
import com.example.orbblaze.ui.theme.*

// Colores del menú
private val SageGreen = Color(0xFF8DA094)
private val NavyDark = Color(0xFF2D324F)
private val StarGold = Color(0xFFF4C491)

@Composable
fun HighScoreScreen(
    soundManager: SoundManager,
    settingsManager: SettingsManager,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val highScore by settingsManager.highScoreFlow.collectAsState(initial = 0)
    val highScoreTime by settingsManager.highScoreTimeFlow.collectAsState(initial = 0)
    
    val records = remember(highScore, highScoreTime) {
        listOf(
            Triple("MODO CLÁSICO", highScore, SageGreen),
            Triple("CONTRA TIEMPO", highScoreTime, Color(0xFF00E676)),
            Triple("MODO AVENTURA", 0, Color(0xFFFFD700)),
            Triple("MODO INVERSA", 0, Color(0xFFFF5252)),
            Triple("PUZZLE DIARIO", 0, Color(0xFFBB86FC)),
            Triple("MINIJUEGOS", 0, Color(0xFF03DAC5))
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "score_animations")
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
                // ✅ TÍTULO (Estilo Menú)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer { translationY = titleFloat }
                ) {
                    Text(
                        text = "RÉCORDS",
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

                // ✅ LISTA DE RÉCORDS
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 24.dp)
                ) {
                    items(records) { (modo, score, color) ->
                        RecordCardPremium(modo, score, color) {
                            if (score == 0) Toast.makeText(context, "¡Juega para establecer un récord!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                // ✅ BOTÓN VOLVER (Estilo Menú)
                ReferenceButton(
                    text = "VOLVER",
                    backgroundColor = Color.White,
                    contentColor = Color.Gray,
                    modifier = Modifier.width(200.dp),
                    onClick = onBackClick
                )
            }
        }
    }
}

@Composable
fun RecordCardPremium(mode: String, score: Int, color: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "scale")
    val fontScale = LocalFontScale.current
    
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = if (isPressed) 2.dp else 6.dp, // Corregido: shadowElevation nativo
        modifier = Modifier
            .fillMaxWidth()
            .height((80 * fontScale).dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mode,
                    style = TextStyle(
                        color = color, 
                        fontSize = (18 * fontScale).sp, 
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "PUNTUACIÓN MÁXIMA",
                    style = TextStyle(
                        color = Color.LightGray, 
                        fontSize = (10 * fontScale).sp, 
                        fontWeight = FontWeight.Bold, 
                        letterSpacing = 1.sp
                    )
                )
            }
            
            Text(
                text = if (score > 0) "$score" else "-",
                style = TextStyle(
                    color = NavyDark,
                    fontSize = (28 * fontScale).sp,
                    fontWeight = FontWeight.Black
                )
            )
        }
    }
}
