package com.example.orbblaze

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.orbblaze.ui.game.GameViewModel // Importante
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.menu.MenuScreen
import com.example.orbblaze.ui.game.LevelScreen
import com.example.orbblaze.ui.settings.SettingsScreen
import com.example.orbblaze.ui.score.HighScoreScreen
import com.example.orbblaze.ui.score.AchievementsScreen
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

    // ✅ CREAMOS EL VIEWMODEL AQUÍ PARA COMPARTIRLO ENTRE PANTALLAS
    val sharedViewModel: GameViewModel = viewModel()

    LaunchedEffect(currentScreen) {
        soundManager.refreshSettings()
    }

    when (currentScreen) {
        "menu" -> {
            MenuScreen(
                onPlayClick = { currentScreen = "game" },
                onScoreClick = { currentScreen = "score" },
                onAchievementsClick = { currentScreen = "achievements" },
                onSettingsClick = { currentScreen = "settings" },
                onExitClick = { android.os.Process.killProcess(android.os.Process.myPid()) },
                soundManager = soundManager, // ✅ Sonido POP
                onSecretClick = {
                    // ✅ AQUÍ SE DESBLOQUEA EL LOGRO SECRETO
                    sharedViewModel.unlockAchievement("secret_popper")
                }
            )
        }
        "game" -> {
            LevelScreen(
                viewModel = sharedViewModel, // ✅ Usamos el compartido
                soundManager = soundManager,
                onMenuClick = { currentScreen = "menu" }
            )
        }
        "score" -> {
            HighScoreScreen(onBackClick = { currentScreen = "menu" })
        }
        "achievements" -> {
            AchievementsScreen(
                viewModel = sharedViewModel, // ✅ Usamos el compartido
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