package com.example.orbblaze.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.orbblaze.ui.menu.MenuButton
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
    // ✅ NUEVO: Estado del Mute
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
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "CONFIGURACIÓN",
                style = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White, shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 4f))
            )

            Spacer(modifier = Modifier.height(30.dp))

            // 1. VOLUMEN EFECTOS
            Text(text = "EFECTOS DE SONIDO", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f)))
            Slider(
                value = sfxVolume,
                onValueChange = { saveSfx(it) },
                colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.3f)),
                modifier = Modifier.width(280.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. VOLUMEN MÚSICA
            Text(text = "MÚSICA DE FONDO", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f)))
            Slider(
                value = musicVolume,
                onValueChange = { saveMusic(it) },
                enabled = !isMusicMuted, // Se deshabilita si está muteado
                colors = SliderDefaults.colors(thumbColor = Color(0xFF64FFDA), activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.3f), disabledThumbColor = Color.Gray),
                modifier = Modifier.width(280.dp)
            )

            // ✅ 3. INTERRUPTOR SILENCIAR MÚSICA
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.width(280.dp)
            ) {
                Text(text = "SILENCIAR MÚSICA", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White))
                Switch(
                    checked = isMusicMuted,
                    onCheckedChange = { toggleMute(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF4D4D), checkedTrackColor = Color.White.copy(alpha = 0.5f))
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. VIBRACIÓN
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.width(280.dp)
            ) {
                Text(text = "VIBRACIÓN", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White))
                Switch(
                    checked = isVibrationEnabled,
                    onCheckedChange = { saveVibration(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color.White.copy(alpha = 0.5f))
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 5. BORRAR RÉCORD
            Button(
                onClick = { resetHighScore() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4D).copy(alpha = 0.8f)),
                shape = RoundedCornerShape(50),
                modifier = Modifier.width(200.dp).height(45.dp)
            ) {
                Text(text = "BORRAR RÉCORD", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(50.dp))

            MenuButton(text = "VOLVER", onClick = onBackClick, isSecondary = true)
        }
    }
}