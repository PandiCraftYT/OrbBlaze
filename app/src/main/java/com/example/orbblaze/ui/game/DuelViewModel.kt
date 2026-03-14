package com.example.orbblaze.ui.game

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.orbblaze.data.*
import com.example.orbblaze.domain.model.*
import com.example.orbblaze.domain.usecase.SyncUserDataUseCase
import com.example.orbblaze.domain.usecase.UnlockAchievementUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class DuelViewModel @Inject constructor(
    application: Application,
    settingsManager: SettingsManager,
    authManager: AuthManager,
    syncUserDataUseCase: SyncUserDataUseCase,
    unlockAchievementUseCase: UnlockAchievementUseCase,
    leaderboardManager: LeaderboardManager,
    private val matchHistoryManager: MatchHistoryManager
) : GameViewModel(application, settingsManager, authManager, syncUserDataUseCase, unlockAchievementUseCase, leaderboardManager) {

    private val matchmakingManager = MatchmakingManager(authManager)
    private var roomListenerJob: Job? = null
    private var balanceJob: Job? = null
    private var loadingJob: Job? = null
    private var matchmakingTimerJob: Job? = null
    private var botSimulationJob: Job? = null
    private var heartbeatJob: Job? = null
    private var connectionCheckJob: Job? = null

    private val _room = MutableStateFlow<GameRoom?>(null)
    val room = _room.asStateFlow()

    private val _opponent = MutableStateFlow<GameRoom?>(null)
    val opponent = _opponent.asStateFlow()

    var showRematchRequest by mutableStateOf(false)
    var isRematchAcceptedByOpponent by mutableStateOf(false)
    
    var isRematchRequestedByMe by mutableStateOf(false)
        private set

    var isShowingVS by mutableStateOf(false)
        private set
    var matchLoadingProgress by mutableFloatStateOf(0f)
        private set

    var lastEloChange by mutableIntStateOf(0)
        private set
    var previousElo by mutableIntStateOf(1000)
        private set
    
    var showRankAnimation by mutableStateOf(false)
    var showDuelResults by mutableStateOf(false)
    
    private var isMatchEndingHandled = false
    private var secondsSinceLastShot = 0

    // ✅ UX: Conteo regresivo
    var countdownValue by mutableIntStateOf(0)
        private set

    var incomingAttackNotice by mutableStateOf<String?>(null)
        private set
    var incomingAttackDetail by mutableStateOf<String?>(null)
        private set

    private var lastOpponentScore = 0

    fun findMatch(targetRoomId: String? = null) {
        resetMatchmaking() 
        
        roomListenerJob = viewModelScope.launch {
            val myElo = settingsManager.duelEloFlow.first()
            previousElo = myElo
            
            matchmakingManager.findOrCreateRoom(targetRoomId, myElo).collectLatest { gameRoom ->
                if (gameRoom?.status == "FINISHED" && !isRematchRequestedByMe) {
                    if (!isMatchEndingHandled) {
                        val myId = authManager.currentUser?.uid ?: ""
                        handleMatchEnd(gameRoom.winnerId == myId, gameRoom.roomId)
                    }
                    return@collectLatest
                }

                _room.value = gameRoom
                
                if (gameRoom != null) {
                    val myId = authManager.currentUser?.uid ?: ""
                    
                    if (gameRoom.status == "WAITING" && gameRoom.playerCount == 1) {
                        startMatchmakingTimer(gameRoom.roomId)
                    } else {
                        matchmakingTimerJob?.cancel()
                    }

                    if (gameRoom.players.size > 1) {
                        val opponentId = gameRoom.players.keys.firstOrNull { it != myId }
                        
                        if (opponentId != null) {
                            val newOpponentState = gameRoom.players[opponentId]
                            
                            if (newOpponentState?.score == -1 && gameState == GameState.PLAYING && !isMatchEndingHandled) {
                                handleMatchEnd(isWin = true, gameRoom.roomId)
                            }

                            isRematchAcceptedByOpponent = newOpponentState?.rematchReady == true

                            if (gameRoom.status == "PLAYING" && !isShowingVS) {
                                if (gameState == GameState.IDLE || isRematchRequestedByMe) {
                                    if (gameState != GameState.IDLE) restartGameLocal()
                                    _opponent.value = newOpponentState
                                    startVSPresentation()
                                }
                            }
                            
                            if (newOpponentState?.lastAttack != null && _opponent.value?.lastAttack != newOpponentState.lastAttack) {
                                handleIncomingAttack(newOpponentState.lastAttack)
                            }
                            
                            if (newOpponentState != null && newOpponentState.score > lastOpponentScore && gameState == GameState.PLAYING) {
                                spawnOpponentScoreEffect(newOpponentState.score - lastOpponentScore)
                                lastOpponentScore = newOpponentState.score
                            } else if (newOpponentState != null && newOpponentState.score <= 0) {
                                lastOpponentScore = 0
                            }

                            _opponent.value = newOpponentState

                            if ((newOpponentState?.bot == true || newOpponentState?.userId?.startsWith("BOT_") == true) && gameState == GameState.PLAYING) {
                                startBotSimulation(gameRoom.roomId, newOpponentState)
                            } else if (gameState != GameState.PLAYING) {
                                botSimulationJob?.cancel()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun spawnOpponentScoreEffect(diff: Int) {
        spawnFloatingText(metrics?.screenWidth?.minus(100f) ?: 0f, 150f, "+$diff", isOpponent = true)
    }

    private fun startMatchmakingTimer(roomId: String) {
        if (matchmakingTimerJob?.isActive == true) return
        matchmakingTimerJob = viewModelScope.launch {
            delay(10000) 
            if (_room.value?.status == "WAITING") {
                matchmakingManager.addBotToRoom(roomId)
            }
        }
    }

    private fun startBotSimulation(roomId: String, botState: GameRoom) {
        if (botSimulationJob?.isActive == true) return
        botSimulationJob = viewModelScope.launch {
            var botScore = botState.score
            var botDanger = botState.dangerLevel
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            var botSecondsSinceShot = 0

            while (gameState == GameState.PLAYING) {
                delay(1000)
                if (gameState != GameState.PLAYING) break
                botSecondsSinceShot++

                if (botSecondsSinceShot >= Random.nextInt(3, 8)) {
                    botSecondsSinceShot = 0
                    botScore += Random.nextInt(50, 250)
                    botDanger = (botDanger + Random.nextFloat() * 0.04f - 0.02f).coerceIn(0f, 1f)
                    
                    val combo = Random.nextInt(2, 6)
                    val attackType = if (combo >= 4) "ROW_2" else if (combo >= 2) "ROW_1" else null
                    val attack = if (attackType != null) "${attackType}_COMBO${combo}_${System.currentTimeMillis()}" else null
                    
                    val updates = mutableMapOf<String, Any>(
                        "players.${botState.userId}.score" to botScore,
                        "players.${botState.userId}.dangerLevel" to botDanger,
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                    attack?.let { updates["players.${botState.userId}.lastAttack"] = it }
                    
                    db.collection("gameRooms").document(roomId).update(updates)
                }

                if (botDanger >= 1.0f) {
                    db.collection("gameRooms").document(roomId).update("players.${botState.userId}.score", -1)
                    break
                }
            }
        }
    }

    private fun startVSPresentation() {
        loadingJob?.cancel()
        isShowingVS = true
        matchLoadingProgress = 0f
        isRematchRequestedByMe = false
        showRankAnimation = false
        showDuelResults = false
        isMatchEndingHandled = false 
        countdownValue = 0 // Reset
        
        loadingJob = viewModelScope.launch {
            previousElo = settingsManager.duelEloFlow.first()
            
            val totalDuration = 3000L 
            val interval = 50L
            val steps = totalDuration / interval
            for (i in 1..steps) {
                delay(interval)
                matchLoadingProgress = i.toFloat() / steps
            }
            isShowingVS = false
            
            // ✅ Iniciar Conteo Regresivo antes de empezar
            startCountdown()
        }
    }

    private fun startCountdown() {
        viewModelScope.launch {
            for (i in 3 downTo 1) {
                countdownValue = i
                soundEvent = SoundType.POP
                delay(1000)
            }
            countdownValue = 0 // 0 significa "¡YA!" o oculto
            startGame()
        }
    }

    fun requestRematch() {
        val roomId = _room.value?.roomId ?: return
        if (isRematchRequestedByMe) return
        
        isRematchRequestedByMe = true 
        viewModelScope.launch {
            matchmakingManager.updatePlayerRematchStatus(roomId, true)
        }
    }

    override fun startGame() {
        if (bubblesByPosition.isEmpty()) {
            loadLevel(initialRows = 6)
        }
        super.startGame()
        gameMode = GameMode.DUEL
        isRematchAcceptedByOpponent = false
        showRematchRequest = false
        isRematchRequestedByMe = false
        secondsSinceLastShot = 0
        incomingAttackNotice = null
        incomingAttackDetail = null
        lastOpponentScore = 0
        startDifficultyBalanceLoop()
        startHeartbeatLoop()
        startConnectionCheckLoop()
    }

    private fun startHeartbeatLoop() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            while (gameState == GameState.PLAYING) {
                val roomId = _room.value?.roomId
                if (roomId != null) {
                    matchmakingManager.updateHeartbeat(roomId)
                }
                delay(5000)
            }
        }
    }

    private fun startConnectionCheckLoop() {
        connectionCheckJob?.cancel()
        connectionCheckJob = viewModelScope.launch {
            while (gameState == GameState.PLAYING) {
                delay(10000)
                val opp = _opponent.value
                val roomId = _room.value?.roomId
                if (opp != null && roomId != null && !opp.bot) {
                    val lastHeartbeat = opp.lastHeartbeat
                    if (System.currentTimeMillis() - lastHeartbeat > 20000) { 
                        handleMatchEnd(isWin = true, roomId)
                        spawnFloatingText(metrics?.screenWidth?.div(2) ?: 0f, 200f, "RIVAL DESCONECTADO")
                    }
                }
            }
        }
    }

    private fun restartGameLocal() {
        loadLevel(initialRows = 6)
        gameState = GameState.IDLE
        isRematchAcceptedByOpponent = false
        showRematchRequest = false
        isRematchRequestedByMe = false
        isMatchEndingHandled = false
        showRankAnimation = false
        showDuelResults = false
        incomingAttackNotice = null
        incomingAttackDetail = null
        lastOpponentScore = 0
        countdownValue = 0
        botSimulationJob?.cancel()
        heartbeatJob?.cancel()
        connectionCheckJob?.cancel()
    }

    override fun restartGame() {
        restartGameLocal()
    }

    private fun startDifficultyBalanceLoop() {
        balanceJob?.cancel()
        balanceJob = viewModelScope.launch {
            val inactivityLimit = 15
            while (gameState == GameState.PLAYING) {
                delay(1000)
                if (isPaused) continue
                secondsSinceLastShot++
                if (secondsSinceLastShot >= inactivityLimit) {
                    addRows(1)
                    triggerShake(8f)
                    spawnFloatingText(metrics?.screenWidth?.div(2) ?: 0f, metrics?.boardTopPadding ?: 0f, "¡INACTIVO! +1 FILA")
                    secondsSinceLastShot = inactivityLimit - 5 
                }
            }
        }
    }

    override fun onShoot(spawnX: Float, spawnY: Float) {
        secondsSinceLastShot = 0
        super.onShoot(spawnX, spawnY)
    }

    fun resetMatchmaking() {
        roomListenerJob?.cancel()
        balanceJob?.cancel()
        loadingJob?.cancel()
        matchmakingTimerJob?.cancel()
        botSimulationJob?.cancel()
        heartbeatJob?.cancel()
        connectionCheckJob?.cancel()
        
        val currentRoomId = _room.value?.roomId
        
        _room.value = null
        _opponent.value = null
        isShowingVS = false
        isRematchRequestedByMe = false
        matchLoadingProgress = 0f
        showRankAnimation = false
        showDuelResults = false
        isMatchEndingHandled = false
        incomingAttackNotice = null
        incomingAttackDetail = null
        countdownValue = 0
        
        restartGameLocal() 
        
        if (currentRoomId != null) {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val myId = authManager.currentUser?.uid
            if (myId != null) {
                db.collection("gameRooms").document(currentRoomId)
                    .update("players.$myId", com.google.firebase.firestore.FieldValue.delete())
            }
        }
    }

    private fun handleIncomingAttack(attack: String?) {
        if (attack == null || gameState != GameState.PLAYING) return
        val parts = attack.split("_")
        val type = parts.firstOrNull() ?: return
        val reason = parts.find { it.startsWith("COMBO") }?.replace("COMBO", "Combo x") ?: "Gran Jugada"
        
        val rowsToAdd = when (type) {
            "ROW_1" -> 1
            "ROW_2" -> 2
            else -> 0
        }
        if (rowsToAdd > 0) {
            addRows(rowsToAdd)
            triggerShake(rowsToAdd * 5f)
            vibrationEvent = true 
            
            viewModelScope.launch {
                incomingAttackNotice = if (rowsToAdd == 1) "¡RIVAL ATACA!" else "¡ATAQUE BRUTAL!"
                incomingAttackDetail = "Causa: $reason (+${rowsToAdd} filas)"
                delay(2500)
                incomingAttackNotice = null
                incomingAttackDetail = null
            }
        }
    }

    override fun onPostSnap() {
        super.onPostSnap()
        viewModelScope.launch {
            val roomId = _room.value?.roomId ?: return@launch
            val lowestRow = bubblesByPosition.keys.maxOfOrNull { it.row } ?: 0
            val dangerLevel = (lowestRow.toFloat() / dynamicDangerRow).coerceIn(0f, 1f)

            val attackType = when {
                comboMultiplier >= 4 -> "ROW_2"
                comboMultiplier >= 2 -> "ROW_1"
                else -> null
            }
            
            val attackString = if (attackType != null) {
                "${attackType}_COMBO${comboMultiplier}_${System.currentTimeMillis()}"
            } else null
            
            if (attackType != null) {
                attacksSentInMatch += if (attackType == "ROW_2") 2 else 1
            }
            
            val myId = authManager.currentUser?.uid ?: return@launch
            val updates = mutableMapOf<String, Any>(
                "players.$myId.score" to score,
                "players.$myId.dangerLevel" to dangerLevel,
                "players.$myId.bubblesPopped" to bubblesPoppedInMatch,
                "players.$myId.maxCombo" to maxComboInMatch,
                "players.$myId.attacksSent" to attacksSentInMatch,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            
            attackString?.let { updates["players.$myId.lastAttack"] = it }
            
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("gameRooms").document(roomId).update(updates)
        }
    }

    override fun checkGameConditions(m: BoardMetricsPx) {
        val prevGameState = gameState
        super.checkGameConditions(m)
        
        if (gameState != GameState.PLAYING && prevGameState == GameState.PLAYING) {
            clearSoundEvent()
        }

        if (gameState == GameState.LOST && prevGameState == GameState.PLAYING && !isMatchEndingHandled) {
            val roomId = _room.value?.roomId
            if (roomId != null) {
                viewModelScope.launch {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("gameRooms").document(roomId).update("players.${authManager.currentUser?.uid}.score", -1)
                    handleMatchEnd(isWin = false, roomId, playSound = true)
                }
            }
        }

        if (gameState == GameState.WON && prevGameState == GameState.PLAYING && !isMatchEndingHandled) {
            val roomId = _room.value?.roomId
            if (roomId != null) {
                viewModelScope.launch {
                    handleMatchEnd(isWin = true, roomId, playSound = true)
                }
            }
        }

        if (_opponent.value?.score == -1 && gameState == GameState.PLAYING && !isMatchEndingHandled) {
            val roomId = _room.value?.roomId
            if (roomId != null) {
                handleMatchEnd(isWin = true, roomId, playSound = true)
            }
        }
    }

    private fun handleMatchEnd(isWin: Boolean, roomId: String, playSound: Boolean = true) {
        if (isMatchEndingHandled) return
        isMatchEndingHandled = true
        
        gameState = if (isWin) GameState.WON else GameState.LOST
        
        val myElo = previousElo
        val oppElo = _opponent.value?.elo ?: 1000 
        lastEloChange = calculateEloChange(isWin, myElo, oppElo)

        showRankAnimation = true
        showDuelResults = false

        if (playSound) {
            soundEvent = if (isWin) SoundType.WIN else SoundType.LOSE
        }
        
        botSimulationJob?.cancel()
        balanceJob?.cancel()
        heartbeatJob?.cancel()
        connectionCheckJob?.cancel()
        incomingAttackNotice = null
        incomingAttackDetail = null
        
        val user = authManager.currentUser
        if (user != null) {
            viewModelScope.launch {
                val winnerId = if (isWin) user.uid else _opponent.value?.userId ?: ""
                matchmakingManager.endGame(roomId, winnerId)
                
                val currentElo = settingsManager.duelEloFlow.first()
                previousElo = currentElo
                
                val newElo = (currentElo + lastEloChange).coerceAtLeast(0)
                settingsManager.setDuelElo(newElo)

                leaderboardManager.updateDuelRating(
                    userId = user.uid,
                    username = user.displayName ?: "Jugador",
                    avatarUrl = user.photoUrl?.toString(),
                    finalElo = newElo
                )

                val match = DuelMatch(
                    matchId = roomId,
                    opponentName = _opponent.value?.displayName ?: "Oponente",
                    opponentAvatar = _opponent.value?.avatarUrl,
                    result = if (isWin) "WIN" else "LOSS",
                    eloChange = lastEloChange,
                    score = score,
                    opponentScore = if (_opponent.value?.score == -1) 0 else (_opponent.value?.score ?: 0)
                )
                matchHistoryManager.saveMatch(user.uid, match)
                
                syncUserDataUseCase.uploadProgress()
            }
        }
    }

    private fun calculateEloChange(isWin: Boolean, myElo: Int, opponentElo: Int): Int {
        val kFactor = 32
        val expectedScore = 1.0 / (1.0 + Math.pow(10.0, (opponentElo - myElo).toDouble() / 400.0))
        val actualScore = if (isWin) 1.0 else 0.0
        val change = (kFactor * (actualScore - expectedScore)).toInt()
        
        return if (isWin) change.coerceAtLeast(15) else change.coerceAtMost(-10)
    }

    fun sendReaction(emoji: String) {
        val roomId = _room.value?.roomId ?: return
        viewModelScope.launch {
            matchmakingManager.sendReaction(roomId, emoji)
        }
    }

    override fun onCleared() {
        val currentRoomId = _room.value?.roomId
        val myId = authManager.currentUser?.uid
        if (currentRoomId != null && myId != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("gameRooms").document(currentRoomId)
                .update("players.$myId", com.google.firebase.firestore.FieldValue.delete())
        }
        super.onCleared()
    }
}
