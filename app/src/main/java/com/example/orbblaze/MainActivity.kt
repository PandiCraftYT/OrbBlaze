package com.example.orbblaze

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner // Añadido
import androidx.lifecycle.Lifecycle // Añadido
import androidx.lifecycle.LifecycleEventObserver // Añadido
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.orbblaze.ui.game.GameViewModel
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

                // --- INICIO DE LA CORRECCIÓN PARA EL CICLO DE VIDA ---
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_RESUME -> {
                                // Reanuda la música al volver
                                globalSoundManager.startMusic()
                            }
                            Lifecycle.Event.ON_PAUSE -> {
                                // Pausa la música y sonidos al salir o bloquear
                                globalSoundManager.pauseMusic()
                            }
                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }
                // --- FIN DE LA CORRECCIÓN ---

                AppNavigation(globalSoundManager)
            }
        }
    }
}

@Composable
fun AppNavigation(soundManager: SoundManager) {
    var currentScreen by remember { mutableStateOf("menu") }
    val sharedViewModel: GameViewModel = viewModel()

    // Control de pausa del motor del juego (piratas a descansar)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                // Aquí podrías llamar a una función de pausa en tu ViewModel
                // si el juego tiene un loop de física o tiempo.
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                soundManager = soundManager,
                onSecretClick = {
                    sharedViewModel.unlockAchievement("secret_popper")
                }
            )
        }
        "game" -> {
            LevelScreen(
                viewModel = sharedViewModel,
                soundManager = soundManager,
                onMenuClick = { currentScreen = "menu" }
            )
        }
        "score" -> {
            HighScoreScreen(onBackClick = { currentScreen = "menu" })
        }
        "achievements" -> {
            AchievementsScreen(
                viewModel = sharedViewModel,
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