package com.example.orbblaze

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.menu.MenuScreen
import com.example.orbblaze.ui.game.LevelScreen
import com.example.orbblaze.ui.settings.SettingsScreen
import com.example.orbblaze.ui.score.HighScoreScreen
import com.example.orbblaze.ui.theme.OrbBlazeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OrbBlazeTheme {
                val context = LocalContext.current
                val globalSoundManager = remember { SoundManager(context) }

                LaunchedEffect(Unit) {
                    globalSoundManager.startMusic()
                }

                AppNavigation(globalSoundManager)
            }
        }
    }
}

@Composable
fun AppNavigation(soundManager: SoundManager) {
    var currentScreen by remember { mutableStateOf("menu") }

    LaunchedEffect(currentScreen) {
        soundManager.refreshSettings()
    }

    when (currentScreen) {
        "menu" -> {
            MenuScreen(
                onPlayClick = { currentScreen = "game" },
                onScoreClick = { currentScreen = "score" },
                onSettingsClick = { currentScreen = "settings" },
                onExitClick = { android.os.Process.killProcess(android.os.Process.myPid()) }
            )
        }
        "game" -> {
            LevelScreen(
                soundManager = soundManager, // ✅ AHORA PASAMOS EL MANAGER
                onMenuClick = { currentScreen = "menu" }
            )
        }
        "score" -> {
            HighScoreScreen(
                onBackClick = { currentScreen = "menu" }
            )
        }
        "settings" -> {
            SettingsScreen(
                soundManager = soundManager,
                onBackClick = { currentScreen = "menu" }
            )
        }
    }
}