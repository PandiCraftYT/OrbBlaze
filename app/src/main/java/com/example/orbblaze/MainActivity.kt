package com.example.orbblaze

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.orbblaze.data.AuthManager
import com.example.orbblaze.data.LeaderboardManager
import com.example.orbblaze.data.SettingsManager
import com.example.orbblaze.domain.usecase.SyncUserDataUseCase
import com.example.orbblaze.ui.game.*
import com.example.orbblaze.ui.menu.MenuScreen
import com.example.orbblaze.ui.menu.GameModesScreen
import com.example.orbblaze.ui.menu.SplashScreen
import com.example.orbblaze.ui.settings.SettingsScreen
import com.example.orbblaze.ui.settings.ProfileScreen
import com.example.orbblaze.ui.social.FriendsScreen
import com.example.orbblaze.ui.score.HighScoreScreen
import com.example.orbblaze.ui.score.AchievementsScreen
import com.example.orbblaze.ui.shop.ShopScreen
import com.example.orbblaze.ui.theme.OrbBlazeTheme
import com.example.orbblaze.ui.components.GlobalBackground
import com.example.orbblaze.ui.components.SyncIndicator
import com.example.orbblaze.ui.components.AchievementNotification
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsManager: SettingsManager
    @Inject lateinit var globalSoundManager: SoundManager
    @Inject lateinit var adsManager: AdsManager
    @Inject lateinit var authManager: AuthManager
    @Inject lateinit var syncUserDataUseCase: SyncUserDataUseCase
    @Inject lateinit var leaderboardManager: LeaderboardManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        setContent {
            OrbBlazeTheme {
                val lifecycleOwner = LocalLifecycleOwner.current
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                
                val isLevel = currentRoute in listOf("game", "time_attack", "adventure_game", "game_adventure")
                val showBanner = currentRoute != "splash" && currentRoute != null

                LaunchedEffect(Unit) {
                    syncUserDataUseCase.processInitialSync()
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
                    val sharedViewModel: GameViewModel = hiltViewModel()

                    Column(modifier = Modifier.fillMaxSize()) {
                        
                        Box(modifier = Modifier.weight(1f)) {
                            AppNavigation(
                                navController = navController,
                                soundManager = globalSoundManager, 
                                adsManager = adsManager, 
                                settingsManager = settingsManager, 
                                authManager = authManager, 
                                leaderboardManager = leaderboardManager,
                                sharedViewModel = sharedViewModel
                            )

                            // Feedback overlays
                            AchievementNotification(sharedViewModel.activeAchievement)
                            SyncIndicator(syncUserDataUseCase)

                            if (showBanner && isLevel) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                                    AdBannerCapsule(adsManager)
                                }
                            }
                        }

                        if (showBanner && !isLevel) {
                            AdBannerCapsule(adsManager)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdBannerCapsule(adsManager: AdsManager) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
            contentAlignment = Alignment.Center
        ) {
            adsManager.BannerAd()
        }
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    soundManager: SoundManager,
    adsManager: AdsManager,
    settingsManager: SettingsManager,
    authManager: AuthManager,
    leaderboardManager: LeaderboardManager,
    sharedViewModel: GameViewModel
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val classicVm: ClassicViewModel = hiltViewModel()
    val timeAttackVm: TimeAttackViewModel = hiltViewModel()
    val adventureVm: AdventureViewModel = hiltViewModel()

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
                onModesClick = { navController.navigate("modes") },
                onScoreClick = { navController.navigate("score") },
                onAchievementsClick = { navController.navigate("achievements") },
                onSettingsClick = { navController.navigate("settings") },
                onProfileClick = { navController.navigate("profile") },
                onFriendsClick = { navController.navigate("friends") },
                onExitClick = { activity?.finish() },
                soundManager = soundManager,
                onSecretClick = { sharedViewModel.unlockAchievement("secret_popper") }
            )
        }
        composable("profile") {
            ProfileScreen(
                authManager = authManager,
                settingsManager = settingsManager,
                adsManager = adsManager,
                soundManager = soundManager,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("friends") {
            FriendsScreen(
                authManager = authManager,
                soundManager = soundManager,
                onBackClick = { navController.popBackStack() }
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
                onShowAd = { onReward ->
                    activity?.let { adsManager.showRewardedAd(it, onReward) }
                }
            )
        }
        composable("game_adventure") {
             LevelScreen(
                viewModel = adventureVm,
                soundManager = soundManager,
                onMenuClick = { navController.popBackStack() },
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
                leaderboardManager = leaderboardManager,
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
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
