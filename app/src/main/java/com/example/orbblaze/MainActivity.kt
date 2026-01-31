package com.example.orbblaze

import android.app.Activity
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
import com.example.orbblaze.ui.game.*
import com.example.orbblaze.ui.menu.MenuScreen
import com.example.orbblaze.ui.menu.GameModesScreen
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
                val adsManager = remember { AdsManager(context) } // ✅ AdsManager instanciado
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

                AppNavigation(globalSoundManager, adsManager)
            }
        }
    }
}

@Composable
fun AppNavigation(soundManager: SoundManager, adsManager: AdsManager) {
    var currentScreen by remember { mutableStateOf("menu") }
    val context = LocalContext.current
    val activity = context as? Activity
    
    val classicVm: ClassicViewModel = viewModel()
    val timeAttackVm: TimeAttackViewModel = viewModel()
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
                viewModel = classicVm,
                soundManager = soundManager,
                onMenuClick = { currentScreen = "menu" },
                onShopClick = { currentScreen = "shop" },
                onShowAd = { onReward -> 
                    activity?.let { adsManager.showRewardedAd(it, onReward) }
                }
            )
        }
        "time_attack" -> {
            LevelScreen(
                viewModel = timeAttackVm,
                soundManager = soundManager,
                onMenuClick = { currentScreen = "menu" },
                onShopClick = { currentScreen = "shop" },
                onShowAd = { onReward -> 
                    activity?.let { adsManager.showRewardedAd(it, onReward) }
                }
            )
        }
        "shop" -> {
            ShopScreen(onBackClick = { currentScreen = "game" })
        }
        "score" -> {
            HighScoreScreen(soundManager = soundManager, onBackClick = { currentScreen = "menu" })
        }
        "achievements" -> {
            AchievementsScreen(viewModel = sharedViewModel, soundManager = soundManager, onBackClick = { currentScreen = "menu" })
        }
        "settings" -> {
            SettingsScreen(soundManager = soundManager, onBackClick = { currentScreen = "menu" })
        }
    }
}
