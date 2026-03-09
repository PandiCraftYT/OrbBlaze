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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.domain.model.LevelObjective
import com.example.orbblaze.ui.theme.*

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
        Surface(
            modifier = Modifier
                .width(320.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(40.dp),
            color = Color.White,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indicador de Nivel
                Surface(
                    color = SageGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, SageGreen.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "NIVEL $levelId",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        style = TextStyle(
                            color = SageGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Icono según objetivo
                val icon = when(objective) {
                    is LevelObjective.ReachScore -> Icons.Default.Star
                    is LevelObjective.ClearBoard -> Icons.Default.Refresh
                    is LevelObjective.CollectColor -> Icons.Default.CheckCircle
                }

                Box(contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(70.dp).background(NavyDark.copy(alpha = 0.05f), CircleShape))
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = NavyDark.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "OBJETIVO",
                    style = TextStyle(
                        color = NavyDark,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                )
                Box(modifier = Modifier.padding(top = 4.dp).width(60.dp).height(4.dp).clip(CircleShape).background(NavyDark.copy(alpha = 0.1f)))

                Spacer(Modifier.height(16.dp))

                val objectiveDescription = when(objective) {
                    is LevelObjective.ClearBoard -> objective.description
                    is LevelObjective.ReachScore -> objective.description
                    is LevelObjective.CollectColor -> objective.description
                }

                Text(
                    text = objectiveDescription,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 22.sp
                )

                Spacer(Modifier.height(32.dp))

                ReferenceButton(
                    text = "¡ENTENDIDO!",
                    backgroundColor = SageGreen,
                    contentColor = Color.White,
                    onClick = onStartClick
                )
            }
        }
    }
}
