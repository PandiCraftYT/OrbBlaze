package com.example.orbblaze.domain.usecase

import com.example.orbblaze.data.AuthManager
import com.example.orbblaze.data.SettingsManager
import com.example.orbblaze.data.LeaderboardManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncUserDataUseCase @Inject constructor(
    private val authManager: AuthManager,
    private val settingsManager: SettingsManager,
    private val leaderboardManager: LeaderboardManager
) {
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    /**
     * Sincroniza los datos locales con la nube si el usuario no es anónimo.
     */
    suspend fun uploadProgress() {
        val user = authManager.currentUser
        if (user != null) {
            _isSyncing.value = true
            try {
                // Sincronizar datos generales (monedas, progreso aventura, etc)
                if (!user.isAnonymous) {
                    val localData = settingsManager.getSyncableData()
                    authManager.saveProgressToCloud(localData)
                }

                val username = user.displayName ?: "Jugador"
                val avatarUrl = user.photoUrl?.toString()

                // Sincronizar ELO de Duelo
                val currentElo = settingsManager.duelEloFlow.first()
                leaderboardManager.updateDuelRating(
                    userId = user.uid,
                    username = username,
                    avatarUrl = avatarUrl,
                    finalElo = currentElo
                )

                // Sincronizar Progreso de Aventura
                val adventureLevel = settingsManager.adventureProgressFlow.first()
                leaderboardManager.updateScore(
                    userId = user.uid,
                    username = username,
                    score = adventureLevel,
                    avatarUrl = avatarUrl,
                    mode = "ADVENTURE"
                )

                // Sincronizar Récord de Contra Tiempo
                val timeAttackScore = settingsManager.highScoreTimeFlow.first()
                if (timeAttackScore > 0) {
                    leaderboardManager.updateScore(
                        userId = user.uid,
                        username = username,
                        score = timeAttackScore,
                        avatarUrl = avatarUrl,
                        mode = "TIME_ATTACK"
                    )
                }
                
                // Sincronizar Récord Clásico
                val classicScore = settingsManager.highScoreFlow.first()
                if (classicScore > 0) {
                    leaderboardManager.updateScore(
                        userId = user.uid,
                        username = username,
                        score = classicScore,
                        avatarUrl = avatarUrl,
                        mode = "CLASSIC"
                    )
                }

            } finally {
                _isSyncing.value = false
            }
        }
    }

    /**
     * Descarga el progreso de la nube y actualiza el almacenamiento local.
     */
    suspend fun downloadProgress() {
        val user = authManager.currentUser
        if (user != null && !user.isAnonymous) {
            _isSyncing.value = true
            try {
                val cloudData = authManager.loadProgressFromCloud()
                if (cloudData != null && cloudData.isNotEmpty()) {
                    settingsManager.updateFromSyncableData(cloudData)
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /**
     * Lógica de inicialización al arrancar la app.
     */
    suspend fun processInitialSync() {
        val coins = settingsManager.coinsFlow.first()
        val duelElo = settingsManager.duelEloFlow.first()
        val adventureLevel = settingsManager.adventureProgressFlow.first()
        
        if (coins >= 10 || authManager.currentUser != null || duelElo != 1000 || adventureLevel > 0) {
            _isSyncing.value = true
            try {
                authManager.signInAnonymously()
                authManager.refreshUser()
                
                val currentUid = authManager.currentUser?.uid
                val lastKnownUid = settingsManager.lastKnownUidFlow.first()
                
                if (lastKnownUid != null && currentUid != lastKnownUid) {
                    settingsManager.clearAllData()
                }
                settingsManager.setLastKnownUid(currentUid)
                
                val user = authManager.currentUser
                if (user != null) {
                    if (!user.isAnonymous) {
                        val cloudData = authManager.loadProgressFromCloud()
                        if (cloudData != null && cloudData.isNotEmpty()) {
                            settingsManager.updateFromSyncableData(cloudData)
                        }
                    }
                    
                    // Sincronizar todos los rankings al inicio
                    uploadProgress()
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
