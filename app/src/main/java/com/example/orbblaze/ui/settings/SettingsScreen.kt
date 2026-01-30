package com.example.orbblaze.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.theme.BgBottom
import com.example.orbblaze.ui.theme.BgTop

@Composable
fun SettingsScreen(
    soundManager: SoundManager,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("orbblaze_prefs", Context.MODE_PRIVATE) }

    // --- ESTADOS ---
    var sfxVolume by remember { mutableStateOf(prefs.getFloat("sfx_volume", 1.0f)) }
    var musicVolume by remember { mutableStateOf(prefs.getFloat("music_volume", 0.5f)) }
    var isVibrationEnabled by remember { mutableStateOf(prefs.getBoolean("vibration_enabled", true)) }
    var isMusicMuted by remember { mutableStateOf(soundManager.isMusicMuted()) }

    // --- FUNCIONES ---
    fun saveSfx(value: Float) {
        sfxVolume = value
        prefs.edit().putFloat("sfx_volume", value).apply()
        soundManager.setSfxVol(value)
    }

    fun saveMusic(value: Float) {
        musicVolume = value
        prefs.edit().putFloat("music_volume", value).apply()
        soundManager.setMusicVol(value)
    }

    fun toggleMute(muted: Boolean) {
        isMusicMuted = muted
        soundManager.setMusicMute(muted)
    }

    fun saveVibration(enabled: Boolean) {
        isVibrationEnabled = enabled
        prefs.edit().putBoolean("vibration_enabled", enabled).apply()
    }

    fun resetHighScore() {
        prefs.edit().putInt("high_score", 0).apply()
        Toast.makeText(context, "¡Récord reiniciado!", Toast.LENGTH_SHORT).show()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(BgTop, BgBottom)))
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "CONFIGURACIÓN",
                style = TextStyle(
                    fontSize = 36.sp, 
                    fontWeight = FontWeight.Black, 
                    color = Color.White, 
                    shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 4f)
                )
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 1. VOLUMEN EFECTOS
            Text(text = "EFECTOS DE SONIDO", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f)))
            Slider(
                value = sfxVolume,
                onValueChange = { saveSfx(it) },
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFFD700), 
                    activeTrackColor = Color.White, 
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                ),
                modifier = Modifier.width(280.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. VOLUMEN MÚSICA
            Text(text = "MÚSICA DE FONDO", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f)))
            Slider(
                value = musicVolume,
                onValueChange = { saveMusic(it) },
                enabled = !isMusicMuted,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF64FFDA), 
                    activeTrackColor = Color.White, 
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f), 
                    disabledThumbColor = Color.Gray
                ),
                modifier = Modifier.width(280.dp)
            )

            // 3. INTERRUPTOR SILENCIAR MÚSICA
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.width(280.dp).padding(vertical = 8.dp)
            ) {
                Text(text = "SILENCIAR MÚSICA", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White))
                Switch(
                    checked = isMusicMuted,
                    onCheckedChange = { toggleMute(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFFF4D4D), 
                        checkedTrackColor = Color.White.copy(alpha = 0.5f)
                    )
                )
            }

            // 4. VIBRACIÓN
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.width(280.dp).padding(vertical = 8.dp)
            ) {
                Text(text = "VIBRACIÓN", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White))
                Switch(
                    checked = isVibrationEnabled,
                    onCheckedChange = { saveVibration(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFFFD700), 
                        checkedTrackColor = Color.White.copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 5. BOTÓN BORRAR RÉCORD (Estilizado)
            Box(
                modifier = Modifier
                    .width(240.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFFF4D4D).copy(alpha = 0.2f))
                    .border(2.dp, Color(0xFFFF4D4D), RoundedCornerShape(50))
                    .clickable { resetHighScore() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "BORRAR RÉCORD", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF4D4D)))
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 6. BOTÓN VOLVER (Estilo secundario del menú)
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(50))
                    .border(2.dp, Color.White, RoundedCornerShape(50))
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "VOLVER", 
                    style = TextStyle(
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = Color.White
                    )
                )
            }
        }
    }
}
