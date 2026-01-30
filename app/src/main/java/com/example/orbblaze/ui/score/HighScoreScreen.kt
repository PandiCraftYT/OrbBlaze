package com.example.orbblaze.ui.score

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.ui.theme.BgBottom
import com.example.orbblaze.ui.theme.BgTop

@Composable
fun HighScoreScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("orbblaze_prefs", Context.MODE_PRIVATE) }
    val highScore = prefs.getInt("high_score", 0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(BgTop, BgBottom)))
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "MEJOR PUNTUACIÓN",
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 4f)
                )
            )

            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text = "$highScore",
                style = TextStyle(
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD700), // Dorado
                    shadow = Shadow(color = Color.Black, offset = Offset(4f, 4f), blurRadius = 10f)
                )
            )

            Text(
                text = "PUNTOS",
                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
            )

            Spacer(modifier = Modifier.height(80.dp))

            // Botón VOLVER adaptado (Copia del estilo de MenuScreen)
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(50))
                    .border(2.dp, Color.White, RoundedCornerShape(50))
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "VOLVER", 
                    style = TextStyle(
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = Color.White
                    )
                )
            }
        }
    }
}
