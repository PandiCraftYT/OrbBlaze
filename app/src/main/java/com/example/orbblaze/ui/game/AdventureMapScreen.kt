package com.example.orbblaze.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.domain.model.AdventureLevels
import com.example.orbblaze.domain.model.AdventureZone

@Composable
fun AdventureMapScreen(
    onLevelSelect: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    val mapGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF000000), // Espacio (Arriba)
            Color(0xFF0D47A1), // Atmósfera
            Color(0xFF4FC3F7), // Cielo
            Color(0xFF1B5E20), // Superficie
            Color(0xFFBF360C), // Núcleo (Abajo)
        )
    )

    Box(modifier = Modifier.fillMaxSize().background(mapGradient)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            reverseLayout = true,
            contentPadding = PaddingValues(top = 100.dp, bottom = 100.dp, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    "EL VIAJE COMIENZA AQUÍ",
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 40.dp)
                )
            }

            items(AdventureLevels.levels) { level ->
                LevelNode(level.id, level.zone) { onLevelSelect(level.id) }
                Spacer(Modifier.height(40.dp))
            }

            item {
                Spacer(Modifier.height(40.dp))
                Text(
                    "PRÓXIMAMENTE:\nOTRAS GALAXIAS",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(top = 40.dp)
                )
            }
        }
        
        IconButton(onClick = onBackClick, modifier = Modifier.align(Alignment.TopStart).padding(16.dp).statusBarsPadding()) {
            Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
        }
    }
}

@Composable
fun LevelNode(id: Int, zone: AdventureZone, onClick: () -> Unit) {
    val nodeColor = when(zone) {
        AdventureZone.CORE -> Color(0xFFD32F2F)
        AdventureZone.SURFACE -> Color(0xFF388E3C)
        AdventureZone.SKY -> Color(0xFF1976D2)
        AdventureZone.SPACE -> Color(0xFF7B1FA2)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(70.dp).clickable { onClick() },
            shape = CircleShape,
            color = nodeColor,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("$id", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
            }
        }
        Text(zone.title, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
