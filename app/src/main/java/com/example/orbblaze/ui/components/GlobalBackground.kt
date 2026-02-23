package com.example.orbblaze.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.orbblaze.ui.theme.BgBottom
import com.example.orbblaze.ui.theme.BgTop

@Composable
fun GlobalBackground(content: @Composable () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "global_bg_anim")
    
    val backgroundOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bg_scroll"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalWidth = size.width
            val totalHeight = size.height
            val color1 = Color.White.copy(alpha = 0.15f)
            val color2 = Color.White.copy(alpha = 0.08f)
            
            drawCircle(
                color = color1, 
                radius = 150.dp.toPx(), 
                center = Offset(
                    x = (backgroundOffset % (totalWidth + 400.dp.toPx())) - 200.dp.toPx(), 
                    y = 150.dp.toPx()
                )
            )
            drawCircle(
                color = color2, 
                radius = 250.dp.toPx(), 
                center = Offset(
                    x = ((backgroundOffset * 0.7f) % (totalWidth + 600.dp.toPx())) - 300.dp.toPx(), 
                    y = totalHeight * 0.4f
                )
            )
            drawCircle(
                color = color1, 
                radius = 120.dp.toPx(), 
                center = Offset(
                    x = totalWidth - ((backgroundOffset * 1.2f) % (totalWidth + 300.dp.toPx())), 
                    y = totalHeight * 0.7f
                )
            )
        }
        content()
    }
}
