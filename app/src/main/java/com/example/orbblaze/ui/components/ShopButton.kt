package com.example.orbblaze.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.orbblaze.R

@Composable
fun ShopButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Imagen principal de la tienda (se oscurece si está bloqueada)
        Image(
            painter = painterResource(id = R.drawable.shopbubble),
            contentDescription = "Tienda",
            modifier = Modifier.fillMaxSize(),
            colorFilter = if (!isEnabled) ColorFilter.tint(Color.Gray.copy(alpha = 0.4f), BlendMode.Modulate) else null
        )

        // ✅ Candado GRANDE en el centro
        if (!isEnabled) {
            Box(
                modifier = Modifier
                    .size(40.dp) // Más grande
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.5f)), // Fondo semi-transparente
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Bloqueado",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp) // Icono grande
                )
            }
        }
    }
}
