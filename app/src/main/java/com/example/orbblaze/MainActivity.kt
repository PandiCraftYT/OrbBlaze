package com.example.orbblaze

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.orbblaze.ui.game.GameViewModel
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.menu.MenuScreen
import com.example.orbblaze.ui.game.LevelScreen
import com.example.orbblaze.ui.settings.SettingsScreen
import com.example.orbblaze.ui.score.HighScoreScreen
import com.example.orbblaze.ui.score.AchievementsScreen
import com.example.orbblaze.ui.shop.ShopScreen
import com.example.orbblaze.ui.theme.OrbBlazeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OrbBlazeTheme {
                val context = LocalContext.current
                val globalSoundManager = remember { SoundManager(context) }
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_RESUME -> globalSoundManager.startMusic()
                            Lifecycle.Event.ON_PAUSE -> globalSoundManager.pauseMusic()
                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                AppNavigation(globalSoundManager)
            }
        }
    }
}

@Composable
fun AppNavigation(soundManager: SoundManager) {
    var currentScreen by remember { mutableStateOf("menu") }
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
                soundManager = soundManager,
                onSecretClick = { sharedViewModel.unlockAchievement("secret_popper") }
            )
        }
        "game" -> {
            LevelScreen(
                viewModel = sharedViewModel,
                soundManager = soundManager,
                onMenuClick = { currentScreen = "menu" },
                onShopClick = { currentScreen = "shop" } // ✅ Redirección a tienda
            )
        }
        "shop" -> {
            ShopScreen(onBackClick = { currentScreen = "game" }) // ✅ Volver al juego
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
