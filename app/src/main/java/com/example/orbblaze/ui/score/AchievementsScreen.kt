package com.example.orbblaze.ui.score

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.orbblaze.ui.game.GameViewModel
import com.example.orbblaze.ui.theme.BgBottom
import com.example.orbblaze.ui.theme.BgTop

@Composable
fun AchievementsScreen(
    viewModel: GameViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    var revealedId by remember { mutableStateOf<String?>(null) }

    val displayList = viewModel.achievements.filter {
        !it.isHidden || it.isUnlocked
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(BgTop, BgBottom)))
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp), 
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "LOGROS", 
                style = TextStyle(
                    fontSize = 36.sp, 
                    fontWeight = FontWeight.Black, 
                    color = Color.White, 
                    shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 4f)
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(), 
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayList) { achievement ->
                    val isUnlocked = achievement.isUnlocked
                    val showDescription = isUnlocked || revealedId == achievement.id
                    val cardColor = if (isUnlocked) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.3f)
                    val borderColor = if (isUnlocked) Color(0xFFFFD700) else Color.Gray
                    val titleColor = if (isUnlocked) Color.White else Color.Gray
                    val icon = if (isUnlocked) Icons.Default.Star else Icons.Default.Lock
                    val iconColor = if (isUnlocked) Color(0xFFFFD700) else Color.Gray.copy(alpha = 0.5f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardColor)
                            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
                            .clickable { 
                                if (!isUnlocked) revealedId = if (revealedId == achievement.id) null else achievement.id 
                            }
                            .padding(16.dp)
                            .animateContentSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.3f)), 
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = achievement.title, style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = titleColor))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (showDescription) achievement.description else "Bloqueado 🔒 (Toca para ver pista)", 
                                style = TextStyle(fontSize = 14.sp, color = if (showDescription) Color.White.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Botón VOLVER estilo secundario (Transparente con borde blanco)
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(50))
                    .border(2.dp, Color.White, RoundedCornerShape(50))
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "VOLVER", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White))
            }
        }
    }
}
