package com.example.orbblaze.ui.components

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

@Composable
fun GameTopBar(
    score: Int,
    bestScore: Int,
    coins: Int,
    timeLeft: Int? = null, // ✅ NUEVO: Opcional
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 16.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(32.dp))
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // MONEDAS
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFFFFD700), Color(0xFFB8860B)))), contentAlignment = Alignment.Center) {
                Text("C", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(8.dp))
            Text(text = "$coins", style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold))
        }

        // CENTRO: MOSTRAR TIEMPO O SCORE
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (timeLeft != null) {
                Text("TIEMPO", style = TextStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold))
                Text(text = "$timeLeft s", style = TextStyle(color = if (timeLeft < 10) Color.Red else Color(0xFF64FFDA), fontSize = 24.sp, fontWeight = FontWeight.Black, shadow = Shadow(color = Color.Black, blurRadius = 4f)))
            } else {
                Text("SCORE", style = TextStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold))
                Text(text = "$score", style = TextStyle(color = Color(0xFFFFD700), fontSize = 22.sp, fontWeight = FontWeight.Black, shadow = Shadow(color = Color.Black, blurRadius = 4f)))
            }
        }

        // DERECHA: BEST + TUERCA
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End) {
                Text("MAX", style = TextStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold))
                Text(text = "$bestScore", style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold))
            }
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape).clickable { onSettingsClick() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}
