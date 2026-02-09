package com.example.orbblaze.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        val accentColor = Color(0xFF64FFDA)
        
        Surface(
            modifier = Modifier
                .width(340.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(32.dp),
            color = Color(0xFF0F1444),
            border = BorderStroke(1.5.dp, Brush.sweepGradient(listOf(accentColor, Color.Transparent, accentColor))),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(vertical = 40.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indicador de Nivel
                Surface(
                    color = accentColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "NIVEL $levelId",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        style = TextStyle(
                            color = accentColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                    )
                }

                Spacer(Modifier.height(28.dp))

                // Icono según objetivo
                val icon = when(objective) {
                    is LevelObjective.ReachScore -> Icons.Default.Star
                    is LevelObjective.ClearBoard -> Icons.Default.Refresh
                    is LevelObjective.CollectColor -> Icons.Default.CheckCircle
                }

                Box(contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(80.dp).background(accentColor.copy(alpha = 0.1f), CircleShape))
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "OBJETIVO",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                )

                Spacer(Modifier.height(12.dp))

                val objectiveDescription = when(objective) {
                    is LevelObjective.ClearBoard -> objective.description
                    is LevelObjective.ReachScore -> objective.description
                    is LevelObjective.CollectColor -> objective.description
                }

                Text(
                    text = objectiveDescription,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp
                )

                Spacer(Modifier.height(40.dp))

                Button(
                    onClick = onStartClick,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Text(
                        "¡ENTENDIDO!",
                        color = Color(0xFF0F1444),
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
