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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.R
import com.example.orbblaze.data.LeaderboardManager
import com.example.orbblaze.data.SettingsManager
import com.example.orbblaze.domain.model.LeaderboardEntry
import com.example.orbblaze.domain.model.Rank
import com.example.orbblaze.ui.game.SoundManager
import com.example.orbblaze.ui.game.SoundType
import com.example.orbblaze.ui.menu.LocalFontScale
import com.example.orbblaze.ui.theme.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull

@Composable
fun HighScoreScreen(
    soundManager: SoundManager,
    settingsManager: SettingsManager,
    leaderboardManager: LeaderboardManager,
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Clásico, 1: Contra Tiempo, 2: Aventura, 3: Duelo
    
    val highScore by settingsManager.highScoreFlow.collectAsState(initial = 0)
    val highScoreTime by settingsManager.highScoreTimeFlow.collectAsState(initial = 0)
    val adventureProgress by settingsManager.adventureProgressFlow.collectAsState(initial = 0)
    val duelElo by settingsManager.duelEloFlow.collectAsState(initial = 1000)

    val classicLeaderboard by produceState<List<LeaderboardEntry>?>(initialValue = null, leaderboardManager) {
        leaderboardManager.getLeaderboard("CLASSIC").catch { emit(emptyList()) }.collect { value = it }
    }
    
    val timeAttackLeaderboard by produceState<List<LeaderboardEntry>?>(initialValue = null, leaderboardManager) {
        leaderboardManager.getLeaderboard("TIME_ATTACK").catch { emit(emptyList()) }.collect { value = it }
    }

    val adventureLeaderboard by produceState<List<LeaderboardEntry>?>(initialValue = null, leaderboardManager) {
        leaderboardManager.getLeaderboard("ADVENTURE").catch { emit(emptyList()) }.collect { value = it }
    }

    val duelLeaderboard by produceState<List<LeaderboardEntry>?>(initialValue = null, leaderboardManager) {
        leaderboardManager.getLeaderboard("DUEL").catch { emit(emptyList()) }.collect { value = it }
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
                // Header
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = { soundManager.play(SoundType.POP); onBackClick() },
                        modifier = Modifier.align(Alignment.CenterStart).shadow(4.dp, CircleShape).background(Color.White, CircleShape).size((48 * fontScale).dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = NavyDark, modifier = Modifier.size((28 * fontScale).dp))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { translationY = titleFloat }) {
                        Text(
                            text = stringResource(id = R.string.menu_record),
                            textAlign = TextAlign.Center,
                            style = TextStyle(fontSize = (38 * fontScale).sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 2.sp, shadow = Shadow(Color.Black.copy(alpha = 0.15f), Offset(0f, 8f), 12f))
                        )
                        Box(modifier = Modifier.padding(top = 2.dp).width(80.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.5f)))
                    }
                }

                Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        TabItem(text = "CLASSIC", isSelected = selectedTab == 0, modifier = Modifier.weight(1f)) { selectedTab = 0; soundManager.play(SoundType.POP) }
                        TabItem(text = "CONTRA TIEMPO", isSelected = selectedTab == 1, modifier = Modifier.weight(1f)) { selectedTab = 1; soundManager.play(SoundType.POP) }
                        TabItem(text = "ADVENTURE", isSelected = selectedTab == 2, modifier = Modifier.weight(1f)) { selectedTab = 2; soundManager.play(SoundType.POP) }
                        TabItem(text = "DUEL 1v1", isSelected = selectedTab == 3, modifier = Modifier.weight(1f)) { selectedTab = 3; soundManager.play(SoundType.POP) }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val leaderboardRaw = when(selectedTab) {
                        0 -> classicLeaderboard
                        1 -> timeAttackLeaderboard
                        2 -> adventureLeaderboard
                        3 -> duelLeaderboard
                        else -> emptyList()
                    }
                    
                    var myRecordValue by remember { mutableStateOf("") }
                    var myUid by remember { mutableStateOf<String?>(null) }
                    
                    LaunchedEffect(selectedTab, highScore, highScoreTime, adventureProgress, duelElo) {
                        myUid = settingsManager.lastKnownUidFlow.firstOrNull()
                        myRecordValue = when(selectedTab) {
                            0 -> highScore.toString()
                            1 -> highScoreTime.toString()
                            2 -> adventureProgress.toString()
                            3 -> duelElo.toString()
                            else -> "0"
                        }
                    }

                    val leaderboard = remember(leaderboardRaw, myUid, myRecordValue, selectedTab) {
                        if (leaderboardRaw == null) return@remember null
                        val list = leaderboardRaw.toMutableList()
                        if (myUid != null && list.none { it.userId == myUid }) {
                            list.add(LeaderboardEntry(userId = myUid!!, username = "TÚ", score = myRecordValue.toIntOrNull() ?: 0))
                        }
                        list.sortedByDescending { it.score }
                    }
                    
                    val modeColor = when(selectedTab) {
                        0 -> SageGreen
                        1 -> Color(0xFF00E676)
                        2 -> Color(0xFFFFD700)
                        3 -> Color(0xFFF44336)
                        else -> Color.White
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                        item {
                            Text(if(selectedTab == 3) "MI RATING Y RANGO" else "MI RANKING Y RANGO", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
                            val displayValue = if(selectedTab == 2) "NIVEL $myRecordValue" else myRecordValue
                            MyRecordCard(value = displayValue, color = modeColor, fontScale = fontScale, isDuel = selectedTab == 3)
                        }

                        item {
                            Spacer(Modifier.height(12.dp))
                            Text("TOP MUNDIAL", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
                        }
                        
                        if (leaderboard == null) {
                            item { Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White) } }
                        } else if (leaderboard.isEmpty()) {
                            item { Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { Text("No hay puntuaciones aún", color = Color.White.copy(alpha = 0.5f)) } }
                        } else {
                            itemsIndexed(leaderboard) { index, entry ->
                                LeaderboardRow(rank = index + 1, entry = entry, fontScale = fontScale, isDuel = selectedTab == 3, isAdventure = selectedTab == 2, isMe = entry.userId == myUid)
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
        Text(text, color = contentColor, fontWeight = FontWeight.Black, fontSize = 9.sp, textAlign = TextAlign.Center, lineHeight = 10.sp)
    }
}

@Composable
fun MyRecordCard(value: String, color: Color, fontScale: Float, isDuel: Boolean = false) {
    val numericValue = value.filter { it.isDigit() }.toIntOrNull() ?: 0
    val currentRank = if (isDuel) Rank.fromElo(numericValue) else Rank.fromScore(numericValue)

    Surface(color = Color.White, shape = RoundedCornerShape(28.dp), shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth().height((100 * fontScale).dp)) {
        Row(modifier = Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(currentRank.color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Text(currentRank.medalName, fontSize = 32.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = currentRank.title, color = currentRank.color, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text(text = if(isDuel) "RATING ELO" else "PUNTUACIÓN ACTUAL", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(value, color = NavyDark, fontSize = 28.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun LeaderboardRow(rank: Int, entry: LeaderboardEntry, fontScale: Float, isDuel: Boolean = false, isAdventure: Boolean = false, isMe: Boolean = false) {
    val rankInfo = if (isDuel) Rank.fromElo(entry.score) else Rank.fromScore(entry.score)
    val positionColor = when(rank) {
        1 -> Color(0xFFFFE700)
        2 -> Color(0xFFC0C0C0) 
        3 -> Color(0xFFCD7F32) 
        else -> if (isMe) Color(0xFFFFD700) else Color.Gray.copy(alpha = 0.3f)
    }
    
    val myBackgroundColor = Color(0xFF3F51B5)

    Surface(color = if (isMe) myBackgroundColor else Color.White.copy(alpha = 0.9f), shape = RoundedCornerShape(24.dp), shadowElevation = if (isMe) 8.dp else 2.dp, modifier = Modifier.fillMaxWidth().height((70 * fontScale).dp)) {
        Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("#$rank", color = if (isMe && rank > 3) Color.White else positionColor, fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.width(45.dp))
            Text(rankInfo.medalName, fontSize = 22.sp, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(if(isMe && entry.username == "TÚ") "TÚ" else entry.username.uppercase(), color = if(isMe) Color.White else NavyDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(rankInfo.title, color = if(isMe) Color.White.copy(alpha = 0.8f) else rankInfo.color, fontWeight = FontWeight.Black, fontSize = 9.sp)
            }
            val scoreDisplay = if(isAdventure) "NIVEL ${entry.score}" else "${entry.score}"
            Text(scoreDisplay, color = if (isMe) Color.White else if(isDuel) Color(0xFFF44336) else SageGreen, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
    }
}
