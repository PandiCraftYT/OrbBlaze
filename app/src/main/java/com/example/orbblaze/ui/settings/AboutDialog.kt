package com.example.orbblaze.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.R
import com.example.orbblaze.ui.theme.*

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(300.dp)
                .padding(16.dp)
                .clickable(enabled = false) { },
            shape = RoundedCornerShape(28.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF5F5F5))))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ICONO CIRCULAR LIMPIO
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A237E).copy(alpha = 0.05f))
                        .border(1.dp, Color(0xFF1A237E).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                        contentDescription = "Logo",
                        modifier = Modifier.size(56.dp)
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    "ORBBLAZE",
                    style = TextStyle(
                        color = Color(0xFF1A237E), 
                        fontSize = 24.sp, 
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    "Explota gemas mágicas usando un cañón de precisión. ¡Alcanza el récord más alto!",
                    style = TextStyle(
                        color = Color(0xFF1A237E).copy(alpha = 0.6f), 
                        fontSize = 14.sp, 
                        lineHeight = 20.sp
                    ),
                    textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.height(20.dp))
                
                Text(
                    "Creado por Carlos",
                    style = TextStyle(
                        color = Color(0xFF1A237E), 
                        fontSize = 15.sp, 
                        fontWeight = FontWeight.Bold
                    )
                )
                
                Spacer(Modifier.height(28.dp))
                
                // BOTÓN INSTAGRAM (Estilo minimalista)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF1A237E).copy(alpha = 0.05f))
                        .border(1.5.dp, Color(0xFF1A237E).copy(alpha = 0.1f), RoundedCornerShape(50))
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/carlosnvz_"))
                            context.startActivity(intent)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "INSTAGRAM", 
                        color = Color(0xFF1A237E), 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                }
                
                Spacer(Modifier.height(12.dp))
                
                // BOTÓN CERRAR (Sólido como el menú)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF1A237E))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "¡ENTENDIDO!", 
                        color = Color.White, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
