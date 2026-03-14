package com.example.orbblaze

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
import com.example.orbblaze.data.MatchHistoryManager
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
import com.example.orbblaze.ui.theme.NavyDark
import com.example.orbblaze.ui.theme.SageGreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsManager: SettingsManager
    @Inject lateinit var globalSoundManager: SoundManager
    @Inject lateinit var adsManager: AdsManager
    @Inject lateinit var authManager: AuthManager
    @Inject lateinit var syncUserDataUseCase: SyncUserDataUseCase
    @Inject lateinit var leaderboardManager: LeaderboardManager
    @Inject lateinit var matchHistoryManager: MatchHistoryManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        setContent {
            OrbBlazeTheme {
                val lifecycleOwner = LocalLifecycleOwner.current
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                
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
                                matchHistoryManager = matchHistoryManager,
                                sharedViewModel = sharedViewModel
                            )

                            AchievementNotification(sharedViewModel.activeAchievement)
                            SyncIndicator(syncUserDataUseCase)
                        }
                    }
                }
            }
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
    matchHistoryManager: MatchHistoryManager,
    sharedViewModel: GameViewModel
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val classicVm: ClassicViewModel = hiltViewModel()
    val timeAttackVm: TimeAttackViewModel = hiltViewModel()
    val adventureVm: AdventureViewModel = hiltViewModel()
    val duelVm: DuelViewModel = hiltViewModel() 

    val duelInvites by authManager.getDuelInvitations().collectAsState(initial = emptyList())
    
    if (duelInvites.isNotEmpty()) {
        val invite = duelInvites.first()
        DuelInviteDialog(
            fromName = invite["fromName"] as? String ?: "Jugador",
            onAccept = {
                scope.launch {
                    val roomId = invite["roomId"] as? String
                    val inviteId = invite["id"] as? String ?: ""
                    authManager.deleteDuelInvitation(inviteId)
                    if (roomId != null) {
                        duelVm.findMatch(roomId)
                        navController.navigate("duel")
                    }
                }
            },
            onReject = {
                scope.launch {
                    val inviteId = invite["id"] as? String ?: ""
                    authManager.deleteDuelInvitation(inviteId)
                }
            }
        )
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
                matchHistoryManager = matchHistoryManager,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("friends") {
            FriendsScreen(
                authManager = authManager,
                soundManager = soundManager,
                matchHistoryManager = matchHistoryManager,
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
                soundManager = soundManager,
                duelViewModel = duelVm,
                authManager = authManager
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
        composable("duel") { 
            LevelScreen(
                viewModel = duelVm,
                soundManager = soundManager,
                onMenuClick = { 
                    duelVm.resetMatchmaking()
                    navController.navigate("menu") { popUpTo("menu") { inclusive = true } } 
                },
                onShowAd = { }
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
                matchHistoryManager = matchHistoryManager,
                authManager = authManager,
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

@Composable
fun DuelInviteDialog(fromName: String, onAccept: () -> Unit, onReject: () -> Unit) {
    Dialog(onDismissRequest = onReject) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color.White, modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔥 ¡DESAFÍO ENTRANTE!", fontWeight = FontWeight.Black, fontSize = 20.sp, color = NavyDark)
                Spacer(Modifier.height(12.dp))
                Text("¡$fromName te ha invitado a un duelo 1v1!", textAlign = TextAlign.Center, color = Color.Gray)
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onReject, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2))) {
                        Text("IGNORAR", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = onAccept, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = SageGreen)) {
                        Text("¡ACEPTAR!", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
