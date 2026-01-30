package com.example.orbblaze.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = "Tienda",
            tint = goldLight,
            modifier = Modifier.size(26.dp)
        )
    }
}
