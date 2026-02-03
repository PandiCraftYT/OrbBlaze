package com.example.orbblaze.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.orbblaze.R

@Composable
fun ShopButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(64.dp) // Un poco más grande para que destaque
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // ✅ TU NUEVO ICONO: ShopBubble.png (Pura imagen)
        Image(
            painter = painterResource(id = R.drawable.shopbubble),
            contentDescription = "Tienda",
            modifier = Modifier.fillMaxSize()
        )
    }
}
