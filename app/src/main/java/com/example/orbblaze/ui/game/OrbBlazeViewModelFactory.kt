package com.example.orbblaze.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.orbblaze.data.SettingsManager
import com.example.orbblaze.data.AuthManager

class OrbBlazeViewModelFactory(
    private val settingsManager: SettingsManager,
    private val authManager: AuthManager,
    private val application: android.app.Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ClassicViewModel::class.java) -> {
                ClassicViewModel(application, settingsManager, authManager) as T
            }
            modelClass.isAssignableFrom(TimeAttackViewModel::class.java) -> {
                TimeAttackViewModel(application, settingsManager, authManager) as T
            }
            modelClass.isAssignableFrom(AdventureViewModel::class.java) -> {
                AdventureViewModel(application, settingsManager, authManager) as T
            }
            modelClass.isAssignableFrom(GameViewModel::class.java) -> {
                GameViewModel(application, settingsManager, authManager) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
