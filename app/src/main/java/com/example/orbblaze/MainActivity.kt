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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OrbBlazeTheme {
                val context = LocalContext.current

                // Inicializar managers
                val settingsManager = remember { SettingsManager(context) }
                val globalSoundManager = remember { SoundManager(context, settingsManager) }
                val adsManager = remember { AdsManager(context) }

                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_RESUME -> globalSoundManager.startMusic()
                            Lifecycle.Event.ON_PAUSE -> globalSoundManager.pauseMusic()
                            Lifecycle.Event.ON_DESTROY -> globalSoundManager.release()
                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                // ✅ DISEÑO ULTRA-MINIMALISTA Y SEAMLESS
                // Usamos un Box para que el contenido del juego ocupe TODA la pantalla (incluso detrás del banner)
                Box(modifier = Modifier.fillMaxSize()) {

                    // 1. Capa de Navegación (Fondo y Juego)
                    AppNavigation(globalSoundManager, adsManager, settingsManager)

                    // 2. Capa del Banner (Flotando sin barras de fondo)
                    // Al no tener Spacer ni Column, el fondo que ves a los lados del banner
                    // será el mismo fondo azul de tu menú o el de tus niveles.
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .safeDrawingPadding() // Evita que el banner toque los botones del sistema
                            .padding(bottom = 4.dp)
                    ) {
                        adsManager.BannerAd()
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    soundManager: SoundManager,
    adsManager: AdsManager,
    settingsManager: SettingsManager
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = context as? Activity

    val classicVm: ClassicViewModel = viewModel()
    val timeAttackVm: TimeAttackViewModel = viewModel()
    val adventureVm: AdventureViewModel = viewModel()
    val sharedViewModel: GameViewModel = viewModel()

    LaunchedEffect(settingsManager) {
        classicVm.initManager(settingsManager)
        timeAttackVm.initManager(settingsManager)
        adventureVm.initManager(settingsManager)
        sharedViewModel.initManager(settingsManager)
    }

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
                settingsManager = settingsManager
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
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
