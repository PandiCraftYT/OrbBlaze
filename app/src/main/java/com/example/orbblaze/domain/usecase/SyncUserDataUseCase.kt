package com.example.orbblaze.domain.usecase

import com.example.orbblaze.data.AuthManager
import com.example.orbblaze.data.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncUserDataUseCase @Inject constructor(
    private val authManager: AuthManager,
    private val settingsManager: SettingsManager
) {
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    /**
     * Sincroniza los datos locales con la nube si el usuario no es anónimo.
     */
    suspend fun uploadProgress() {
        val user = authManager.currentUser
        if (user != null && !user.isAnonymous) {
            _isSyncing.value = true
            try {
                val localData = settingsManager.getSyncableData()
                authManager.saveProgressToCloud(localData)
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
        if (coins >= 10 || authManager.currentUser != null) {
            _isSyncing.value = true
            try {
                authManager.signInAnonymously()
                authManager.refreshUser()
                
                val currentUid = authManager.currentUser?.uid
                val lastKnownUid = settingsManager.lastKnownUidFlow.first()
                
                // Si el UID cambió (cambio de cuenta), limpiamos datos locales
                if (lastKnownUid != null && currentUid != lastKnownUid) {
                    settingsManager.clearAllData()
                }
                settingsManager.setLastKnownUid(currentUid)
                
                // Si no es anónimo, bajamos sus datos
                val user = authManager.currentUser
                if (user != null && !user.isAnonymous) {
                    val cloudData = authManager.loadProgressFromCloud()
                    if (cloudData != null && cloudData.isNotEmpty()) {
                        settingsManager.updateFromSyncableData(cloudData)
                    }
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
