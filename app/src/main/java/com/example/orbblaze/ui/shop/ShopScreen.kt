package com.example.orbblaze.ui.shop

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.ui.theme.*

data class ShopItem(
    val id: String,
    val name: String,
    val description: String,
    val price: Int,
    val icon: String // Podríamos usar imágenes reales luego
)

@Composable
fun ShopScreen(onBackClick: () -> Unit) {
    var selectedCategory by remember { mutableStateOf("POWERUPS") }
    val coins by remember { mutableIntStateOf(1250) } // Ejemplo de saldo

    val powerUps = listOf(
        ShopItem("fire", "Burbuja Fuego", "Quema filas enteras", 150, "🔥"),
        ShopItem("ice", "Burbuja Hielo", "Detiene el techo", 200, "❄️"),
        ShopItem("laser", "Super Guía", "Puntería infinita", 100, "🎯")
    )

    val skins = listOf(
        ShopItem("gold_cannon", "Cañón Oro", "Aspecto legendario", 1000, "✨"),
        ShopItem("space_panda", "Panda Astro", "Traje espacial", 800, "👨‍🚀")
    )

    val displayItems = if (selectedCategory == "POWERUPS") powerUps else skins

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TIENDA", style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White))
                
                // Saldo de Monedas
                Surface(
                    color = Color.Black.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color(0xFFFFD700))
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🪙 $coins", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // CATEGORÍAS
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryTab("PODERES", selectedCategory == "POWERUPS") { selectedCategory = "POWERUPS" }
                CategoryTab("ASPECTOS", selectedCategory == "SKINS") { selectedCategory = "SKINS" }
            }

            Spacer(Modifier.height(24.dp))

            // LISTA DE PRODUCTOS
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(displayItems) { item ->
                    ShopCard(item)
                }
            }

            Spacer(Modifier.height(24.dp))

            // BOTÓN VOLVER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(50))
                    .border(2.dp, Color.White, RoundedCornerShape(50))
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Text("VOLVER AL MENÚ", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CategoryTab(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) Color(0xFF64FFDA) else Color.White.copy(alpha = 0.1f)
    val textColor = if (isSelected) Color(0xFF1A237E) else Color.White
    
    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun ShopCard(item: ShopItem) {
    Surface(
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth().aspectRatio(0.8f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(item.icon, fontSize = 40.sp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(item.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(item.description, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
            }
            Button(
                onClick = { /* Lógica de compra */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                modifier = Modifier.fillMaxWidth().height(36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("🪙 ${item.price}", color = Color(0xFF1A237E), fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
    }
}
