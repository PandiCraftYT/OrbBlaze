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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class DuelViewModel @Inject constructor(
    application: Application,
    settingsManager: SettingsManager,
    authManager: AuthManager,
    syncUserDataUseCase: SyncUserDataUseCase,
    unlockAchievementUseCase: UnlockAchievementUseCase,
    leaderboardManager: LeaderboardManager
) : GameViewModel(application, settingsManager, authManager, syncUserDataUseCase, unlockAchievementUseCase, leaderboardManager) {

    private val matchmakingManager = MatchmakingManager(authManager)
    private var roomListenerJob: Job? = null
    private var balanceJob: Job? = null
    private var loadingJob: Job? = null
    private var matchmakingTimerJob: Job? = null
    private var botSimulationJob: Job? = null

    private val _room = MutableStateFlow<GameRoom?>(null)
    val room = _room.asStateFlow()

    private val _opponent = MutableStateFlow<GameRoom?>(null)
    val opponent = _opponent.asStateFlow()

    var showRematchRequest by mutableStateOf(false)
    var isRematchAcceptedByOpponent by mutableStateOf(false)
    
    // Flag para evitar reinicios automáticos indeseados
    private var isRematchRequestedByMe = false

    var isShowingVS by mutableStateOf(false)
        private set
    var matchLoadingProgress by mutableFloatStateOf(0f)
        private set

    // --- Animación de Rango ---
    var lastEloChange by mutableIntStateOf(0)
        private set
    var previousElo by mutableIntStateOf(1000)
        private set
    var showRankAnimation by mutableStateOf(false)
    
    // Flag para asegurar que la lógica de fin de partida se ejecute solo una vez
    private var isMatchEndingHandled = false

    fun findMatch(targetRoomId: String? = null) {
        resetMatchmaking() 
        roomListenerJob = viewModelScope.launch {
            // Sincronizar el ELO real antes de empezar
            previousElo = settingsManager.duelEloFlow.first()
            
            matchmakingManager.findOrCreateRoom(targetRoomId).collectLatest { gameRoom ->
                _room.value = gameRoom
                
                if (gameRoom != null) {
                    val myId = authManager.currentUser?.uid ?: ""
                    
                    // Lógica de Bot de Emergencia
                    if (gameRoom.status == "WAITING" && gameRoom.playerCount == 1) {
                        startMatchmakingTimer(gameRoom.roomId)
                    } else {
                        matchmakingTimerJob?.cancel()
                    }

                    if (gameRoom.players.size > 1) {
                        val opponentId = gameRoom.players.keys.firstOrNull { it != myId }
                        
                        if (opponentId != null) {
                            val newOpponentState = gameRoom.players[opponentId]
                            
                            // ✅ COMPROBACIÓN DE VICTORIA INSTANTÁNEA
                            if (newOpponentState?.score == -1 && (gameState == GameState.PLAYING || gameState == GameState.IDLE)) {
                                handleMatchEnd(isWin = true, gameRoom.roomId)
                            }

                            // ✅ Detectar si el oponente aceptó revancha
                            if (gameRoom.status == "REMATCH_REQUESTED" || gameRoom.status == "PLAYING") {
                                 if (newOpponentState?.rematchReady == true || (gameRoom.status == "PLAYING" && gameState != GameState.PLAYING)) {
                                     isRematchAcceptedByOpponent = true
                                 }
                            } else {
                                isRematchAcceptedByOpponent = false
                            }

                            // ✅ Iniciar el juego
                            if (gameRoom.status == "PLAYING" && !isShowingVS) {
                                val isFirstMatch = gameState == GameState.IDLE
                                val isAuthorizedRematch = (gameState == GameState.WON || gameState == GameState.LOST) && isRematchRequestedByMe
                                
                                if (isFirstMatch || isAuthorizedRematch) {
                                    if (!isFirstMatch) {
                                        restartGameLocal()
                                    }
                                    _opponent.value = newOpponentState
                                    startVSPresentation()
                                }
                            }
                            
                            if (_opponent.value?.lastAttack != newOpponentState?.lastAttack) {
                                _opponent.value = newOpponentState
                                handleIncomingAttack(newOpponentState?.lastAttack)
                            } else {
                                _opponent.value = newOpponentState
                            }

                            // Lógica de simulación de bot
                            if (newOpponentState?.isBot == true && gameState == GameState.PLAYING) {
                                startBotSimulation(gameRoom.roomId, newOpponentState)
                            } else if (newOpponentState?.isBot == false || gameState != GameState.PLAYING) {
                                botSimulationJob?.cancel()
                            }
                        }
                    }
                }
            }
        }
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
            
            while (gameState == GameState.PLAYING) {
                delay(Random.nextLong(2500, 6000))
                if (gameState != GameState.PLAYING) break
                
                // Simular progreso del bot
                botScore += Random.nextInt(50, 250)
                botDanger = (botDanger + Random.nextFloat() * 0.06f).coerceIn(0f, 1f)
                
                val attack = if (Random.nextFloat() < 0.15f) "ROW_1" else if (Random.nextFloat() < 0.04f) "ROW_2" else null
                
                val updates = mutableMapOf<String, Any>(
                    "players.${botState.userId}.score" to botScore,
                    "players.${botState.userId}.dangerLevel" to botDanger,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                attack?.let { updates["players.${botState.userId}.lastAttack"] = it }
                
                if (Random.nextFloat() < 0.08f) {
                    val emojis = listOf("😎", "🔥", "😲", "😜", "🤖", "💪")
                    updates["players.${botState.userId}.currentReaction"] = emojis.random()
                    updates["players.${botState.userId}.reactionTimestamp"] = System.currentTimeMillis()
                }

                db.collection("gameRooms").document(roomId).update(updates)

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
        isMatchEndingHandled = false 
        
        loadingJob = viewModelScope.launch {
            val totalDuration = 3500L 
            val interval = 50L
            val steps = totalDuration / interval
            
            for (i in 1..steps) {
                delay(interval)
                matchLoadingProgress = i.toFloat() / steps
            }
            
            isShowingVS = false
            startGame()
        }
    }

    fun requestRematch() {
        val roomId = _room.value?.roomId ?: return
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
        startDifficultyBalanceLoop()
    }

    private fun restartGameLocal() {
        loadLevel(initialRows = 6)
        gameState = GameState.IDLE
        isRematchAcceptedByOpponent = false
        showRematchRequest = false
        botSimulationJob?.cancel()
    }

    override fun restartGame() {
        restartGameLocal()
    }

    private fun startDifficultyBalanceLoop() {
        balanceJob?.cancel()
        balanceJob = viewModelScope.launch {
            var secondsPassed = 0
            var interval = 30
            while (gameState == GameState.PLAYING) {
                delay(1000)
                secondsPassed++
                
                if (secondsPassed >= interval) {
                    addRows(1)
                    triggerShake(5f)
                    spawnFloatingText(metrics?.screenWidth?.div(2) ?: 0f, metrics?.boardTopPadding ?: 0f, "¡MUERTE SÚBITA!")
                    secondsPassed = 0
                    if (interval > 10) interval -= 2
                }
            }
        }
    }

    fun resetMatchmaking() {
        roomListenerJob?.cancel()
        balanceJob?.cancel()
        loadingJob?.cancel()
        matchmakingTimerJob?.cancel()
        botSimulationJob?.cancel()
        val currentRoomId = _room.value?.roomId
        _room.value = null
        _opponent.value = null
        isShowingVS = false
        isRematchRequestedByMe = false
        matchLoadingProgress = 0f
        if (currentRoomId != null) {
            viewModelScope.launch {
                matchmakingManager.leaveRoom(currentRoomId)
            }
        }
    }

    private fun handleIncomingAttack(attack: String?) {
        if (attack == null || gameState != GameState.PLAYING) return
        val rowsToAdd = when (attack) {
            "ROW_1" -> 1
            "ROW_2" -> 2
            else -> 0
        }
        if (rowsToAdd > 0) {
            addRows(rowsToAdd)
            triggerShake(rowsToAdd * 3f)
        }
    }

    override fun onPostSnap() {
        super.onPostSnap()
        viewModelScope.launch {
            val roomId = _room.value?.roomId ?: return@launch
            val lowestRow = bubblesByPosition.keys.maxOfOrNull { it.row } ?: 0
            val dangerLevel = (lowestRow.toFloat() / dynamicDangerRow).coerceIn(0f, 1f)

            val attack = when {
                comboMultiplier >= 4 -> {
                    attacksSentInMatch += 2
                    "ROW_2"
                }
                comboMultiplier >= 2 -> {
                    attacksSentInMatch += 1
                    "ROW_1"
                }
                else -> null
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
            
            attack?.let { updates["players.$myId.lastAttack"] = it }
            
            matchmakingManager.updatePlayerState(roomId, score, attack)

            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("gameRooms").document(roomId).update(updates)
        }
    }

    override fun checkGameConditions(m: BoardMetricsPx) {
        val prevGameState = gameState
        super.checkGameConditions(m)
        
        // Supress super's sound to handle it once in handleMatchEnd
        if (gameState != GameState.PLAYING && prevGameState == GameState.PLAYING) {
            clearSoundEvent()
        }

        if (gameState == GameState.LOST && prevGameState == GameState.PLAYING && !isMatchEndingHandled) {
            val roomId = _room.value?.roomId
            if (roomId != null) {
                viewModelScope.launch {
                    matchmakingManager.updatePlayerState(roomId, -1, null)
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
        if (playSound) {
            soundEvent = if (isWin) SoundType.WIN else SoundType.LOSE
        }
        
        botSimulationJob?.cancel()
        
        val user = authManager.currentUser
        if (user != null) {
            viewModelScope.launch {
                val winnerId = if (isWin) user.uid else _opponent.value?.userId ?: ""
                matchmakingManager.endGame(roomId, winnerId)
                
                // ✅ GUARDAR VALORES PARA ANIMACIÓN
                val currentElo = settingsManager.duelEloFlow.first()
                val pointsChange = if (isWin) 25 else -20
                val newElo = (currentElo + pointsChange).coerceAtLeast(0)
                
                previousElo = currentElo
                lastEloChange = pointsChange
                
                // Primero persistir localmente
                settingsManager.setDuelElo(newElo)

                // ✅ ACTUALIZAR RANKING GLOBAL (FIRESTORE)
                leaderboardManager.updateDuelRating(
                    userId = user.uid,
                    username = user.displayName ?: "Jugador",
                    avatarUrl = user.photoUrl?.toString(),
                    isWin = isWin
                )
                
                // ✅ SINCRONIZAR TODO EL PROGRESO A LA NUBE
                syncUserDataUseCase.uploadProgress()

                // ✅ MOSTRAR ANIMACIÓN INMEDIATAMENTE
                showRankAnimation = true
            }
        }
    }

    fun sendReaction(emoji: String) {
        val roomId = _room.value?.roomId ?: return
        viewModelScope.launch {
            matchmakingManager.sendReaction(roomId, emoji)
        }
    }

    override fun onCleared() {
        super.onCleared()
        resetMatchmaking()
    }
}
