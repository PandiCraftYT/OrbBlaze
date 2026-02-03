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
        val CURRENT_REWARD_DAY = intPreferencesKey("current_reward_day")
        val LAST_REWARD_CLAIM = longPreferencesKey("last_reward_claim")
    }

    // SFX Volume
    val sfxVolumeFlow: Flow<Float> = context.dataStore.data.map { it[SFX_VOLUME] ?: 1.0f }
    suspend fun setSfxVolume(volume: Float) {
        context.dataStore.edit { it[SFX_VOLUME] = volume }
    }

    // Music Volume
    val musicVolumeFlow: Flow<Float> = context.dataStore.data.map { it[MUSIC_VOLUME] ?: 0.5f }
    suspend fun setMusicVolume(volume: Float) {
        context.dataStore.edit { it[MUSIC_VOLUME] = volume }
    }

    // Music Muted
    val musicMutedFlow: Flow<Boolean> = context.dataStore.data.map { it[MUSIC_MUTED] ?: false }
    suspend fun setMusicMuted(muted: Boolean) {
        context.dataStore.edit { it[MUSIC_MUTED] = muted }
    }

    // Vibration
    val vibrationEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[VIBRATION_ENABLED] ?: true }
    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[VIBRATION_ENABLED] = enabled }
    }

    // High Score
    val highScoreFlow: Flow<Int> = context.dataStore.data.map { it[HIGH_SCORE] ?: 0 }
    suspend fun setHighScore(score: Int) {
        context.dataStore.edit { it[HIGH_SCORE] = score }
    }

    // High Score Time Attack
    val highScoreTimeFlow: Flow<Int> = context.dataStore.data.map { it[HIGH_SCORE_TIME] ?: 0 }
    suspend fun setHighScoreTime(score: Int) {
        context.dataStore.edit { it[HIGH_SCORE_TIME] = score }
    }

    // Coins
    val coinsFlow: Flow<Int> = context.dataStore.data.map { it[COINS] ?: 0 }
    suspend fun setCoins(coins: Int) {
        context.dataStore.edit { it[COINS] = coins }
    }

    // Daily Reward
    val currentRewardDayFlow: Flow<Int> = context.dataStore.data.map { it[CURRENT_REWARD_DAY] ?: 1 }
    suspend fun setCurrentRewardDay(day: Int) {
        context.dataStore.edit { it[CURRENT_REWARD_DAY] = day }
    }

    val lastRewardClaimFlow: Flow<Long> = context.dataStore.data.map { it[LAST_REWARD_CLAIM] ?: 0L }
    suspend fun setLastRewardClaim(timestamp: Long) {
        context.dataStore.edit { it[LAST_REWARD_CLAIM] = timestamp }
    }

    // Achievements (dinámicos)
    fun isAchievementUnlocked(id: String): Flow<Boolean> {
        val key = booleanPreferencesKey("ach_$id")
        return context.dataStore.data.map { it[key] ?: false }
    }

    suspend fun unlockAchievement(id: String) {
        val key = booleanPreferencesKey("ach_$id")
        context.dataStore.edit { it[key] = true }
    }
}
