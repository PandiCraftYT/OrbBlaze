package com.example.orbblaze.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(320.dp)
                .padding(16.dp)
                .clickable(enabled = false) { },
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1A237E),
            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    "ORBBLAZE",
                    style = TextStyle(color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    "Un emocionante juego de puzles y habilidad donde debes explotar gemas mágicas usando un cañón de precisión. ¡Supera tus límites y alcanza el récord más alto!",
                    style = TextStyle(color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp),
                    textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.height(20.dp))
                
                Text(
                    "Creado por Carlos",
                    style = TextStyle(color = Color(0xFF64FFDA), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                )
                
                Spacer(Modifier.height(24.dp))
                
                // Botón de Instagram
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, Color.White, RoundedCornerShape(50))
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/carlosnvz_"))
                            context.startActivity(intent)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("VER INSTAGRAM", color = Color.White, fontWeight = FontWeight.Bold)
                }
                
                Spacer(Modifier.height(16.dp))
                
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64FFDA)),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CERRAR", color = Color(0xFF1A237E), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
