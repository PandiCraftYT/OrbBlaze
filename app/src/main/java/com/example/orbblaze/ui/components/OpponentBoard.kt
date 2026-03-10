package com.example.orbblaze.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.orbblaze.data.PlayerState
import com.example.orbblaze.ui.theme.NavyDark

@Composable
fun DuelTopBar(
    modifier: Modifier = Modifier,
    myScore: Int,
    myDanger: Float,
    myAvatar: String?,
    opponent: PlayerState?,
    onSettingsClick: () -> Unit
) {
    if (opponent == null) return

    val animatedMyScore by animateIntAsState(targetValue = myScore, label = "my_score")
    val animatedOpponentScore by animateIntAsState(targetValue = if (opponent.score == -1) 0 else opponent.score, label = "opp_score")
    
    val animatedMyDanger by animateFloatAsState(targetValue = myDanger, label = "my_danger")
    val animatedOpponentDanger by animateFloatAsState(targetValue = opponent.dangerLevel, label = "opp_danger")

    // Pulsación si alguien está en peligro
    val infiniteTransition = rememberInfiniteTransition(label = "danger_pulse")
    val dangerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "alpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // --- JUGADOR (IZQUIERDA) ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    modifier = Modifier.size(45.dp),
                    shape = CircleShape,
                    border = BorderStroke(2.dp, if (myDanger > 0.8f) Color.Red.copy(alpha = dangerAlpha) else Color(0xFF2196F3)),
                    color = Color.DarkGray
                ) {
                    AsyncImage(
                        model = myAvatar,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        error = androidx.compose.ui.res.painterResource(com.example.orbblaze.R.drawable.ic_launcher_background)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "TÚ",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$animatedMyScore",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        style = TextStyle(shadow = Shadow(Color.Black, blurRadius = 4f))
                    )
                }
            }

            // --- VS / AJUSTES (CENTRO) ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable { onSettingsClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Settings, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                }
                Text(
                    "VS",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    style = TextStyle(shadow = Shadow(Color.Red, blurRadius = 8f))
                )
            }

            // --- RIVAL (DERECHA) ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        opponent.displayName.uppercase(),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        textAlign = TextAlign.End
                    )
                    Text(
                        "$animatedOpponentScore",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        style = TextStyle(shadow = Shadow(Color.Black, blurRadius = 4f))
                    )
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    modifier = Modifier.size(45.dp),
                    shape = CircleShape,
                    border = BorderStroke(2.dp, if (opponent.dangerLevel > 0.8f) Color.Red.copy(alpha = dangerAlpha) else Color(0xFFF44336)),
                    color = Color.DarkGray
                ) {
                    AsyncImage(
                        model = opponent.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        error = androidx.compose.ui.res.painterResource(com.example.orbblaze.R.drawable.ic_launcher_background)
                    )
                }
            }
        }

        // --- BARRAS DE PROGRESO (FULL WIDTH SPLIT) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Color.Black.copy(alpha = 0.3f))
        ) {
            // Mi barra (crece hacia la derecha)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(end = 1.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedMyDanger)
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, if (myDanger > 0.8f) Color.Red else Color(0xFF2196F3))
                            )
                        )
                )
            }
            
            // Barra rival (crece hacia la izquierda)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 1.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedOpponentDanger)
                        .fillMaxHeight()
                        .align(Alignment.CenterStart)
                        .background(
                            Brush.horizontalGradient(
                                listOf(if (opponent.dangerLevel > 0.8f) Color.Red else Color(0xFFF44336), Color.Transparent)
                            )
                        )
                )
            }
        }
    }
}
