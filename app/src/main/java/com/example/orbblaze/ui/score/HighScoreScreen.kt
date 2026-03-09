package com.example.orbblaze.ui.score

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.data.LeaderboardManager
import com.example.orbblaze.data.SettingsManager
import com.example.orbblaze.domain.model.LeaderboardEntry
import com.example.orbblaze.ui.components.ReferenceButton
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.game.SoundType
import com.example.orbblaze.ui.menu.LocalFontScale
import com.example.orbblaze.ui.theme.*
import kotlinx.coroutines.flow.catch

@Composable
fun HighScoreScreen(
    soundManager: SoundManager,
    settingsManager: SettingsManager,
    leaderboardManager: LeaderboardManager,
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Clásico, 1: Contra Tiempo, 2: Aventura
    
    val highScore by settingsManager.highScoreFlow.collectAsState(initial = 0)
    val highScoreTime by settingsManager.highScoreTimeFlow.collectAsState(initial = 0)
    val adventureProgress by settingsManager.adventureProgressFlow.collectAsState(initial = 0)

    val classicLeaderboard by produceState<List<LeaderboardEntry>?>(initialValue = null, leaderboardManager) {
        leaderboardManager.getLeaderboard("CLASSIC").catch { 
            Log.e("HighScoreScreen", "Error cargando Classic: ${it.message}")
            emit(emptyList()) 
        }.collect { value = it }
    }
    
    val timeAttackLeaderboard by produceState<List<LeaderboardEntry>?>(initialValue = null, leaderboardManager) {
        leaderboardManager.getLeaderboard("TIME_ATTACK").catch { 
            Log.e("HighScoreScreen", "Error cargando Time Attack: ${it.message}")
            emit(emptyList()) 
        }.collect { value = it }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "score_animations")
    val titleFloat by infiniteTransition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "title_float"
    )

    val configuration = LocalConfiguration.current
    val fontScale = (configuration.screenWidthDp.toFloat() / 411f).coerceIn(0.6f, 1.5f)

    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalFontScale provides fontScale) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header con botón de volver estático y título con movimiento
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { 
                            soundManager.play(SoundType.POP)
                            onBackClick() 
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .shadow(4.dp, CircleShape)
                            .background(Color.White, CircleShape)
                            .size((48 * fontScale).dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = NavyDark,
                            modifier = Modifier.size((28 * fontScale).dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.graphicsLayer { translationY = titleFloat }
                    ) {
                        Text(
                            text = "RÉCORDS",
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                fontSize = (38 * fontScale).sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 2.sp,
                                shadow = Shadow(Color.Black.copy(alpha = 0.15f), Offset(0f, 8f), 12f)
                            )
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .width(80.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.5f))
                        )
                    }
                }

                Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        TabItem(text = "CLÁSICO", isSelected = selectedTab == 0, modifier = Modifier.weight(1f)) { selectedTab = 0; soundManager.play(SoundType.POP) }
                        TabItem(text = "C/TIEMPO", isSelected = selectedTab == 1, modifier = Modifier.weight(1f)) { selectedTab = 1; soundManager.play(SoundType.POP) }
                        TabItem(text = "AVENTURA", isSelected = selectedTab == 2, modifier = Modifier.weight(1f)) { selectedTab = 2; soundManager.play(SoundType.POP) }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val leaderboard = when(selectedTab) {
                        0 -> classicLeaderboard
                        1 -> timeAttackLeaderboard
                        else -> emptyList()
                    }
                    
                    val myRecord = when(selectedTab) {
                        0 -> highScore.toString()
                        1 -> highScoreTime.toString()
                        else -> "NIVEL $adventureProgress"
                    }
                    
                    val modeColor = when(selectedTab) {
                        0 -> SageGreen
                        1 -> Color(0xFF00E676)
                        else -> Color(0xFFFFD700)
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                        item {
                            Text("MI RÉCORD", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
                            MyRecordCard(value = myRecord, color = modeColor, fontScale = fontScale)
                        }

                        if (selectedTab != 2) {
                            item {
                                Spacer(Modifier.height(12.dp))
                                Text("TOP MUNDIAL", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
                            }
                            
                            if (leaderboard == null) {
                                item { 
                                    Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) { 
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp)) 
                                    } 
                                }
                            } else if (leaderboard.isEmpty()) {
                                item { 
                                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { 
                                        Text("No hay puntuaciones globales aún", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp) 
                                    } 
                                }
                            } else {
                                itemsIndexed(leaderboard) { index, entry ->
                                    LeaderboardRow(rank = index + 1, entry = entry, fontScale = fontScale)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabItem(text: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(if (isSelected) Color.White else Color.Transparent, label = "bg")
    val contentColor by animateColorAsState(if (isSelected) NavyDark else Color.White.copy(alpha = 0.6f), label = "content")
    Box(modifier = modifier.fillMaxHeight().padding(4.dp).clip(RoundedCornerShape(20.dp)).background(backgroundColor).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(text, color = contentColor, fontWeight = FontWeight.Black, fontSize = 11.sp)
    }
}

@Composable
fun MyRecordCard(value: String, color: Color, fontScale: Float) {
    Surface(color = Color.White, shape = RoundedCornerShape(28.dp), shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth().height((85 * fontScale).dp)) {
        Row(modifier = Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Star, null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("PUNTUACIÓN ACTUAL", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(value, color = NavyDark, fontSize = 28.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun LeaderboardRow(rank: Int, entry: LeaderboardEntry, fontScale: Float) {
    val rankColor = when(rank) {
        1 -> Color(0xFFFFD700) 
        2 -> Color(0xFFC0C0C0) 
        3 -> Color(0xFFCD7F32) 
        else -> Color.Gray.copy(alpha = 0.3f)
    }
    Surface(color = Color.White.copy(alpha = 0.9f), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().height((65 * fontScale).dp)) {
        Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("#$rank", color = rankColor, fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.width(45.dp))
            Text(entry.username.uppercase(), color = NavyDark, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text("${entry.score}", color = SageGreen, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
    }
}
