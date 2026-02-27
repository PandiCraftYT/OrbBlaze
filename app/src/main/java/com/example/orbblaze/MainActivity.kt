package com.example.orbblaze

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.orbblaze.data.AuthManager
import com.example.orbblaze.data.SettingsManager
import com.example.orbblaze.ui.game.*
import com.example.orbblaze.ui.menu.MenuScreen
import com.example.orbblaze.ui.menu.GameModesScreen
import com.example.orbblaze.ui.menu.SplashScreen
import com.example.orbblaze.ui.settings.SettingsScreen
import com.example.orbblaze.ui.score.HighScoreScreen
import com.example.orbblaze.ui.score.AchievementsScreen
import com.example.orbblaze.ui.shop.ShopScreen
import com.example.orbblaze.ui.theme.OrbBlazeTheme
import com.example.orbblaze.ui.components.GlobalBackground
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        setContent {
            OrbBlazeTheme {
                val context = LocalContext.current
                val application = context.applicationContext as android.app.Application

                val settingsManager = remember { SettingsManager(context) }
                val globalSoundManager = remember { SoundManager(context, settingsManager) }
                val adsManager = remember { AdsManager(context) }
                val authManager = remember { AuthManager() }
                val factory = remember { OrbBlazeViewModelFactory(settingsManager, authManager, application) }
                val lifecycleOwner = LocalLifecycleOwner.current

                // ✅ Sincronización inteligente al arrancar
                LaunchedEffect(Unit) {
                    val coins = settingsManager.coinsFlow.first()
                    
                    if (coins >= 10 || authManager.currentUser != null) {
                        val user = authManager.signInAnonymously()
                        authManager.refreshUser()
                        
                        val currentUid = authManager.currentUser?.uid
                        val lastKnownUid = settingsManager.lastKnownUidFlow.first()

                        if (lastKnownUid != null && currentUid != lastKnownUid) {
                            settingsManager.clearAllData()
                        }
                        
                        settingsManager.setLastKnownUid(currentUid)

                        if (user != null && !user.isAnonymous) {
                            val cloudData = authManager.loadProgressFromCloud()
                            cloudData?.let { settingsManager.updateFromSyncableData(it) }
                        }
                    }
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_RESUME -> {
                                globalSoundManager.refreshSettings()
                                globalSoundManager.startMusic()
                            }
                            Lifecycle.Event.ON_PAUSE -> {
                                globalSoundManager.pauseMusic()
                            }
                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                        globalSoundManager.release()
                    }
                }

                GlobalBackground {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AppNavigation(factory, globalSoundManager, adsManager, settingsManager, authManager)

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .fillMaxWidth()
                        ) {
                            adsManager.BannerAd()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    factory: OrbBlazeViewModelFactory,
    soundManager: SoundManager,
    adsManager: AdsManager,
    settingsManager: SettingsManager,
    authManager: AuthManager
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = context as? Activity

    val classicVm: ClassicViewModel = viewModel(factory = factory)
    val timeAttackVm: TimeAttackViewModel = viewModel(factory = factory)
    val adventureVm: AdventureViewModel = viewModel(factory = factory)
    val sharedViewModel: GameViewModel = viewModel(factory = factory)

    LaunchedEffect(navController.currentBackStackEntry) {
        soundManager.refreshSettings()
    }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onAnimationFinished = {
                navController.navigate("menu") {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }
        composable("menu") {
            MenuScreen(
                onPlayClick = {
                    classicVm.loadLevel()
                    navController.navigate("game")
                },
                onModesClick = { navController.navigate("modes") },
                onScoreClick = { navController.navigate("score") },
                onAchievementsClick = { navController.navigate("achievements") },
                onSettingsClick = { navController.navigate("settings") },
                onExitClick = { activity?.finish() },
                soundManager = soundManager,
                onSecretClick = { sharedViewModel.unlockAchievement("secret_popper") }
            )
        }
        composable("adventure_map") {
            AdventureMapScreen(
                onLevelSelect = { levelId ->
                    adventureVm.loadAdventureLevel(levelId)
                    navController.navigate("adventure_game")
                },
                onBackClick = { navController.popBackStack() },
                settingsManager = settingsManager,
                soundManager = soundManager
            )
        }
        composable("adventure_game") {
            LevelScreen(
                viewModel = adventureVm,
                soundManager = soundManager,
                onMenuClick = {
                    navController.popBackStack()
                },
                onShopClick = { navController.navigate("shop") },
                onShowAd = { onReward ->
                    activity?.let { adsManager.showRewardedAd(it, onReward) }
                }
            )
        }
        composable("modes") {
            GameModesScreen(
                onModeSelect = { mode ->
                    when(mode) {
                        "game" -> classicVm.loadLevel()
                        "time_attack" -> timeAttackVm.loadLevel()
                    }
                    navController.navigate(mode)
                },
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
            HighScoreScreen(
                soundManager = soundManager,
                settingsManager = settingsManager,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("achievements") {
            AchievementsScreen(
                viewModel = sharedViewModel,
                soundManager = soundManager,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                soundManager = soundManager,
                settingsManager = settingsManager,
                authManager = authManager,
                adsManager = adsManager,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
