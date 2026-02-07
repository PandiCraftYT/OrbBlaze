package com.example.orbblaze.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.domain.model.LevelObjective

@Composable
fun AdventureStartDialog(
    levelId: Int,
    objective: LevelObjective,
    onStartClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(320.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1A237E),
            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "NIVEL $levelId",
                    style = TextStyle(
                        color = Color(0xFF64FFDA),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    text = "OBJETIVO",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                )
                
                Spacer(Modifier.height(16.dp))
                
                val objectiveText = when(objective) {
                    is LevelObjective.ClearBoard -> objective.description
                    is LevelObjective.ReachScore -> objective.description
                    is LevelObjective.CollectColor -> objective.description
                }
                
                Text(
                    text = objectiveText,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                )
                
                Spacer(Modifier.height(32.dp))
                
                Button(
                    onClick = onStartClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64FFDA)),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(
                        "¡ENTENDIDO!",
                        color = Color(0xFF1A237E),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
