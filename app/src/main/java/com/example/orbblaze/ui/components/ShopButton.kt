package com.example.orbblaze.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ShopButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val goldDark = Color(0xFFB8860B)
    val goldLight = Color(0xFFFFD700)
    
    Box(
        modifier = modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF2C3E50), Color.Black)
                )
            )
            .border(2.dp, Brush.sweepGradient(listOf(goldDark, goldLight, goldDark)), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Icono de Carrito
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = "Tienda",
            tint = goldLight.copy(alpha = 0.5f), // Más tenue por estar bloqueado
            modifier = Modifier.size(24.dp)
        )
        
        // ✅ ICONO DE CANDADO PEQUEÑO
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-2).dp, y = (-2).dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.7f))
                .border(1.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}
