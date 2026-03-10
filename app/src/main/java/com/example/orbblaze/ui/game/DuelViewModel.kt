package com.example.orbblaze.ui.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    private val _room = MutableStateFlow<GameRoom?>(null)
    val room = _room.asStateFlow()

    private val _opponent = MutableStateFlow<PlayerState?>(null)
    val opponent = _opponent.asStateFlow()

    var showRematchRequest by mutableStateOf(false)
    var isRematchAcceptedByOpponent by mutableStateOf(false)

    var isShowingVS by mutableStateOf(false)
        private set
    var matchLoadingProgress by mutableFloatStateOf(0f)
        private set

    fun findMatch(targetRoomId: String? = null) {
        resetMatchmaking() 
        roomListenerJob = viewModelScope.launch {
            matchmakingManager.findOrCreateRoom(targetRoomId).collectLatest { gameRoom ->
                _room.value = gameRoom
                
                if (gameRoom != null && gameRoom.players.size > 1) {
                    val myId = authManager.currentUser?.uid ?: ""
                    val opponentId = gameRoom.players.keys.firstOrNull { it != myId }
                    
                    if (opponentId != null) {
                        val newOpponentState = gameRoom.players[opponentId]
                        
                        // ✅ COMPROBACIÓN DE VICTORIA INSTANTÁNEA (Si el oponente ya perdió)
                        if (newOpponentState?.score == -1 && gameState == GameState.PLAYING) {
                            handleMatchEnd(isWin = true, gameRoom.roomId)
                        }

                        // ✅ Detectar si el oponente aceptó revancha
                        if (gameRoom.status == "REMATCH_REQUESTED" || gameRoom.status == "PLAYING") {
                             if (newOpponentState?.rematchReady == true || gameRoom.status == "PLAYING") {
                                 isRematchAcceptedByOpponent = true
                             }
                        }

                        // ✅ Iniciar el juego (o revancha)
                        if (gameRoom.status == "PLAYING" && !isShowingVS) {
                            if (gameState == GameState.IDLE || gameState == GameState.WON || gameState == GameState.LOST) {
                                // Si venimos de una partida finalizada, reseteamos el tablero localmente
                                if (gameState != GameState.IDLE) {
                                    restartGame()
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
                    }
                }
            }
        }
    }

    private fun startVSPresentation() {
        loadingJob?.cancel()
        isShowingVS = true
        matchLoadingProgress = 0f
        
        loadingJob = viewModelScope.launch {
            val totalDuration = 4000L 
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
        startDifficultyBalanceLoop()
    }

    override fun restartGame() {
        loadLevel(initialRows = 6)
        gameState = GameState.IDLE
        isRematchAcceptedByOpponent = false
        showRematchRequest = false
    }

    private fun startDifficultyBalanceLoop() {
        balanceJob?.cancel()
        balanceJob = viewModelScope.launch {
            var secondsPassed = 0
            var interval = 30 // Segundos iniciales
            while (gameState == GameState.PLAYING) {
                delay(1000)
                secondsPassed++
                
                if (secondsPassed >= interval) {
                    addRows(1)
                    triggerShake(5f)
                    spawnFloatingText(metrics?.screenWidth?.div(2) ?: 0f, metrics?.boardTopPadding ?: 0f, "¡MUERTE SÚBITA!")
                    secondsPassed = 0
                    
                    // Acelerar el proceso gradualmente hasta un mínimo de 10 segundos
                    if (interval > 10) {
                        interval -= 2
                    }
                }
            }
        }
    }

    fun resetMatchmaking() {
        roomListenerJob?.cancel()
        balanceJob?.cancel()
        loadingJob?.cancel()
        val currentRoomId = _room.value?.roomId
        _room.value = null
        _opponent.value = null
        isShowingVS = false
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
            
            // Calcular nivel de peligro basado en la fila más baja ocupada
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
            
            // Actualizar estado completo en Firebase
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
            
            matchmakingManager.updatePlayerState(roomId, score, attack) // Mantenemos por compatibilidad

            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("gameRooms").document(roomId).update(updates)
        }
    }

    override fun checkGameConditions(m: BoardMetricsPx) {
        super.checkGameConditions(m)
        if (gameState == GameState.LOST) {
            val roomId = _room.value?.roomId
            if (roomId != null) {
                viewModelScope.launch {
                    matchmakingManager.updatePlayerState(roomId, -1, null)
                    handleMatchEnd(isWin = false, roomId)
                }
            }
        }
        // ✅ Doble verificación de victoria en el loop de físicas
        if (_opponent.value?.score == -1 && gameState == GameState.PLAYING) {
            val roomId = _room.value?.roomId
            if (roomId != null) {
                handleMatchEnd(isWin = true, roomId)
            }
        }
    }

    private fun handleMatchEnd(isWin: Boolean, roomId: String) {
        if (gameState == GameState.WON || gameState == GameState.LOST) return
        
        gameState = if (isWin) GameState.WON else GameState.LOST
        soundEvent = if (isWin) SoundType.WIN else SoundType.LOSE
        
        val user = authManager.currentUser
        if (user != null) {
            viewModelScope.launch {
                if (isWin) {
                    matchmakingManager.endGame(roomId, user.uid)
                }
                leaderboardManager.updateDuelRating(
                    userId = user.uid,
                    username = user.displayName ?: "Jugador",
                    avatarUrl = user.photoUrl?.toString(),
                    isWin = isWin
                )
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
