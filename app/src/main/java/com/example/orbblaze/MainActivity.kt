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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
                val adsManager = remember { AdsManager(context) }
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
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = context as? Activity
    
    val classicVm: ClassicViewModel = viewModel()
    val timeAttackVm: TimeAttackViewModel = viewModel()
    val sharedViewModel: GameViewModel = viewModel()

    // Sincronizar sonidos al cambiar de pantalla
    LaunchedEffect(navController.currentBackStackEntry) {
        soundManager.refreshSettings()
    }

    NavHost(navController = navController, startDestination = "menu") {
        composable("menu") {
            MenuScreen(
                onPlayClick = { navController.navigate("game") },
                onModesClick = { navController.navigate("modes") },
                onScoreClick = { navController.navigate("score") },
                onAchievementsClick = { navController.navigate("achievements") },
                onSettingsClick = { navController.navigate("settings") },
                onExitClick = { activity?.finish() },
                soundManager = soundManager,
                onSecretClick = { sharedViewModel.unlockAchievement("secret_popper") }
            )
        }
        composable("modes") {
            GameModesScreen(
                onModeSelect = { mode -> navController.navigate(mode) },
                onBackClick = { navController.popBackStack() },
                soundManager = soundManager
            )
        }
        composable("game") {
            LevelScreen(
                viewModel = classicVm,
                soundManager = soundManager,
                onMenuClick = { navController.navigate("menu") { popUpTo("menu") { inclusive = true } } },
                onShopClick = { navController.navigate("shop") },
                onShowAd = { onReward -> 
                    activity?.let { adsManager.showRewardedAd(it, onReward) }
                }
            )
        }
        composable("time_attack") {
            LevelScreen(
                viewModel = timeAttackVm,
                soundManager = soundManager,
                onMenuClick = { navController.navigate("menu") { popUpTo("menu") { inclusive = true } } },
                onShopClick = { navController.navigate("shop") },
                onShowAd = { onReward -> 
                    activity?.let { adsManager.showRewardedAd(it, onReward) }
                }
            )
        }
        composable("shop") {
            ShopScreen(onBackClick = { navController.popBackStack() })
        }
        composable("score") {
            HighScoreScreen(soundManager = soundManager, onBackClick = { navController.popBackStack() })
        }
        composable("achievements") {
            AchievementsScreen(viewModel = sharedViewModel, soundManager = soundManager, onBackClick = { navController.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(soundManager = soundManager, onBackClick = { navController.popBackStack() })
        }
    }
}
