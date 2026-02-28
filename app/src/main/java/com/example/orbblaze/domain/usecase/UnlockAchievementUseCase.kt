package com.example.orbblaze.domain.usecase

import com.example.orbblaze.data.SettingsManager
import com.example.orbblaze.domain.model.Achievement
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnlockAchievementUseCase @Inject constructor(
    private val settingsManager: SettingsManager,
    private val syncUserDataUseCase: SyncUserDataUseCase
) {
    /**
     * Intenta desbloquear un logro. Si se desbloquea, otorga monedas y sincroniza con la nube.
     * Retorna el objeto Achievement si se desbloqueó por primera vez, null en caso contrario.
     */
    suspend fun execute(id: String, availableAchievements: List<Achievement>): Achievement? {
        val isAlreadyUnlocked = settingsManager.isAchievementUnlocked(id).first()
        if (isAlreadyUnlocked) return null

        val achievement = availableAchievements.find { it.id == id } ?: return null

        // Persistencia local
        settingsManager.unlockAchievement(id)
        
        // Recompensa
        val currentCoins = settingsManager.coinsFlow.first()
        settingsManager.setCoins(currentCoins + 50)

        // Sincronización con la nube (delegada al otro Use Case)
        syncUserDataUseCase.uploadProgress()

        return achievement
    }
}
