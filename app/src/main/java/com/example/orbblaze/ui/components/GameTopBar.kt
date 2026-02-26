package com.example.orbblaze.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ScoreYellow = Color(0xFFFFEB3B)

@Composable
fun GameTopBar(
    score: Int,
    bestScore: Int,
    coins: Int,
    timeLeft: Int? = null,
    shotsLeft: Int? = null,
    onSettingsClick: () -> Unit,
    onSoundClick: () -> Unit = {}, // Parámetro añadido
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(60.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp) // Espacio de división
    ) {
        // PANEL PRINCIPAL DE ESTADÍSTICAS (Glassmorphism Pill)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(30.dp)
                )
                .border(
                    BorderStroke(1.5.dp, Color.White.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(30.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // IZQUIERDA: MONEDAS
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFFFFD54F), Color(0xFFF57F17))
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("C", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "$coins",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            shadow = Shadow(color = Color.Black.copy(alpha = 0.3f), blurRadius = 4f)
                        )
                    )
                }

                // CENTRO: SCORE / TIEMPO / TIROS
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1.2f)
                ) {
                    val label = when {
                        shotsLeft != null -> "TIROS"
                        timeLeft != null -> "TIEMPO"
                        else -> "SCORE"
                    }
                    val value = when {
                        shotsLeft != null -> "$shotsLeft"
                        timeLeft != null -> "$timeLeft"
                        else -> "$score"
                    }

                    Text(
                        text = label,
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = value,
                        style = TextStyle(
                            color = ScoreYellow,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            shadow = Shadow(color = Color.Black.copy(alpha = 0.4f), blurRadius = 4f)
                        )
                    )
                }

                // DERECHA: MAX
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f)
                ) {
                    val labelSide = if (shotsLeft != null) "SCORE" else "MAX"
                    val valueSide = if (shotsLeft != null) "$score" else "$bestScore"

                    Text(
                        text = labelSide,
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                    Text(
                        text = valueSide,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            shadow = Shadow(color = Color.Black.copy(alpha = 0.3f), blurRadius = 4f)
                        )
                    )
                }
            }
        }

        // BOTÓN DE CONFIGURACIÓN SEPARADO (Burbuja circular)
        Box(
            modifier = Modifier
                .size(60.dp) // Mismo alto que la barra para consistencia
                .background(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = CircleShape
                )
                .border(
                    BorderStroke(1.5.dp, Color.White.copy(alpha = 0.6f)),
                    shape = CircleShape
                )
                .clickable { 
                    onSoundClick() // Reproducir sonido
                    onSettingsClick() 
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Ajustes",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}