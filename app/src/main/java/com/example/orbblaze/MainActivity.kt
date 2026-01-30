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
import com.example.orbblaze.ui.menu.GameModesScreen
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
                onModesClick = { currentScreen = "modes" },
                onScoreClick = { currentScreen = "score" },
                onAchievementsClick = { currentScreen = "achievements" },
                onSettingsClick = { currentScreen = "settings" },
                onExitClick = { android.os.Process.killProcess(android.os.Process.myPid()) },
                soundManager = soundManager,
                onSecretClick = { sharedViewModel.unlockAchievement("secret_popper") }
            )
        }
        "modes" -> {
            GameModesScreen(
                onModeSelect = { mode -> currentScreen = mode },
                onBackClick = { currentScreen = "menu" },
                soundManager = soundManager
            )
        }
        "game" -> {
            LevelScreen(
                viewModel = sharedViewModel,
                soundManager = soundManager,
                onMenuClick = { currentScreen = "menu" },
                onShopClick = { currentScreen = "shop" }
            )
        }
        "shop" -> {
            ShopScreen(onBackClick = { currentScreen = "game" })
        }
        "score" -> {
            HighScoreScreen(
                soundManager = soundManager, // ✅ Se añadió el parámetro faltante
                onBackClick = { currentScreen = "menu" }
            )
        }
        "achievements" -> {
            AchievementsScreen(
                viewModel = sharedViewModel,
                soundManager = soundManager, // ✅ Se añadió el parámetro faltante
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
