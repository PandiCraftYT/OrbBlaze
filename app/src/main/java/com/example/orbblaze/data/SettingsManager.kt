package com.example.orbblaze.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val SFX_VOLUME = floatPreferencesKey("sfx_volume")
        val MUSIC_VOLUME = floatPreferencesKey("music_volume")
        val MUSIC_MUTED = booleanPreferencesKey("music_muted")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val HIGH_SCORE = intPreferencesKey("high_score")
        val HIGH_SCORE_TIME = intPreferencesKey("high_score_time")
        val COINS = intPreferencesKey("coins")
        val ADVENTURE_PROGRESS = intPreferencesKey("adventure_progress")
        val TUTORIAL_COMPLETED = booleanPreferencesKey("tutorial_completed")
    }

    // Obtener todas las estrellas de golpe para evitar lag
    val allStarsFlow: Flow<Map<Int, Int>> = context.dataStore.data.map { prefs ->
        val starsMap = mutableMapOf<Int, Int>()
        prefs.asMap().forEach { (key, value) ->
            if (key.name.startsWith("level_stars_")) {
                val levelId = key.name.removePrefix("level_stars_").toIntOrNull()
                if (levelId != null && value is Int) {
                    starsMap[levelId] = value
                }
            }
        }
        starsMap
    }

    val tutorialCompletedFlow: Flow<Boolean> = context.dataStore.data.map { it[TUTORIAL_COMPLETED] ?: false }
    suspend fun setTutorialCompleted(completed: Boolean) {
        context.dataStore.edit { it[TUTORIAL_COMPLETED] = completed }
    }

    val sfxVolumeFlow: Flow<Float> = context.dataStore.data.map { it[SFX_VOLUME] ?: 1.0f }
    suspend fun setSfxVolume(volume: Float) {
        context.dataStore.edit { it[SFX_VOLUME] = volume }
    }

    val musicVolumeFlow: Flow<Float> = context.dataStore.data.map { it[MUSIC_VOLUME] ?: 0.5f }
    suspend fun setMusicVolume(volume: Float) {
        context.dataStore.edit { it[MUSIC_VOLUME] = volume }
    }

    val musicMutedFlow: Flow<Boolean> = context.dataStore.data.map { it[MUSIC_MUTED] ?: false }
    suspend fun setMusicMuted(muted: Boolean) {
        context.dataStore.edit { it[MUSIC_MUTED] = muted }
    }

    val vibrationEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[VIBRATION_ENABLED] ?: true }
    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[VIBRATION_ENABLED] = enabled }
    }

    val highScoreFlow: Flow<Int> = context.dataStore.data.map { it[HIGH_SCORE] ?: 0 }
    suspend fun setHighScore(score: Int) {
        context.dataStore.edit { it[HIGH_SCORE] = score }
    }

    val highScoreTimeFlow: Flow<Int> = context.dataStore.data.map { it[HIGH_SCORE_TIME] ?: 0 }
    suspend fun setHighScoreTime(score: Int) {
        context.dataStore.edit { it[HIGH_SCORE_TIME] = score }
    }

    val adventureProgressFlow: Flow<Int> = context.dataStore.data.map { it[ADVENTURE_PROGRESS] ?: 0 }
    suspend fun setAdventureProgress(level: Int) {
        context.dataStore.edit { it[ADVENTURE_PROGRESS] = level }
    }

    fun getLevelStars(levelId: Int): Flow<Int> {
        val key = intPreferencesKey("level_stars_$levelId")
        return context.dataStore.data.map { it[key] ?: 0 }
    }

    suspend fun setLevelStars(levelId: Int, stars: Int) {
        val key = intPreferencesKey("level_stars_$levelId")
        context.dataStore.edit { it[key] = stars }
    }

    val coinsFlow: Flow<Int> = context.dataStore.data.map { it[COINS] ?: 0 }
    suspend fun setCoins(coins: Int) {
        context.dataStore.edit { it[COINS] = coins }
    }

    fun isAchievementUnlocked(id: String): Flow<Boolean> {
        val key = booleanPreferencesKey("ach_$id")
        return context.dataStore.data.map { it[key] ?: false }
    }

    suspend fun unlockAchievement(id: String) {
        val key = booleanPreferencesKey("ach_$id")
        context.dataStore.edit { it[key] = true }
    }
}
