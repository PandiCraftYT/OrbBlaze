package com.example.orbblaze.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Colores del menú
private val SageGreen = Color(0xFF8DA094)
private val NavyDark = Color(0xFF2D324F)
private val StarGold = Color(0xFFF4C491)

@Composable
fun GameTopBar(
    score: Int,
    bestScore: Int,
    coins: Int,
    timeLeft: Int? = null,
    shotsLeft: Int? = null,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .fillMaxWidth()
            .height(64.dp),
        // Forma de píldora elegante con bordes redondeados
        shape = RoundedCornerShape(32.dp),
        color = Color.White.copy(alpha = 0.92f),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // IZQUIERDA: MONEDAS
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(StarGold, Color(0xFFB8860B)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("C", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "$coins",
                    style = TextStyle(
                        color = NavyDark,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                )
            }

            // CENTRO: TIROS O TIEMPO O SCORE
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val label = when {
                    shotsLeft != null -> "TIROS"
                    timeLeft != null -> "TIEMPO"
                    else -> "SCORE"
                }
                val value = when {
                    shotsLeft != null -> "$shotsLeft"
                    timeLeft != null -> "$timeLeft s"
                    else -> "$score"
                }
                val valueColor = when {
                    shotsLeft != null && shotsLeft <= 3 -> Color(0xFFEF4444)
                    timeLeft != null && timeLeft < 10 -> Color(0xFFEF4444)
                    else -> SageGreen
                }

                Text(
                    text = label,
                    style = TextStyle(
                        color = Color.Gray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = value,
                    style = TextStyle(
                        color = valueColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                )
            }

            // DERECHA: SCORE (AVENTURA) O MAX (OTROS) + BOTÓN PAUSA
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    val isAdventure = (shotsLeft != null)
                    val label = if (isAdventure) "SCORE" else "MAX"
                    val value = if (isAdventure) "$score" else "$bestScore"

                    Text(
                        text = label,
                        style = TextStyle(
                            color = Color.Gray,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                    Text(
                        text = value,
                        style = TextStyle(
                            color = NavyDark.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(NavyDark.copy(alpha = 0.05f))
                        .clickable { onSettingsClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = NavyDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
