package com.example.orbblaze.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

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
        val COLOR_BLIND_MODE = booleanPreferencesKey("color_blind_mode")
        val LAST_KNOWN_UID = stringPreferencesKey("last_known_uid")
        val NAME_CHANGES_COUNT = intPreferencesKey("name_changes_count")
        val NAME_CHANGE_ADS_WATCHED = intPreferencesKey("name_change_ads_watched") // 🔥 Persistencia de Ads
    }

    val lastKnownUidFlow: Flow<String?> = context.dataStore.data.map { it[LAST_KNOWN_UID] }
    suspend fun setLastKnownUid(uid: String?) {
        context.dataStore.edit { prefs ->
            if (uid == null) prefs.remove(LAST_KNOWN_UID) else prefs[LAST_KNOWN_UID] = uid
        }
    }

    val nameChangesCountFlow: Flow<Int> = context.dataStore.data.map { it[NAME_CHANGES_COUNT] ?: 0 }
    suspend fun setNameChangesCount(count: Int) {
        context.dataStore.edit { it[NAME_CHANGES_COUNT] = count }
    }

    val nameChangeAdsWatchedFlow: Flow<Int> = context.dataStore.data.map { it[NAME_CHANGE_ADS_WATCHED] ?: 0 }
    suspend fun setNameChangeAdsWatched(count: Int) {
        context.dataStore.edit { it[NAME_CHANGE_ADS_WATCHED] = count }
    }

    // --- FLUJOS PROTEGIDOS ---

    val allStarsFlow: Flow<Map<Int, Int>> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val starsMap = mutableMapOf<Int, Int>()
            prefs.asMap().forEach { (key, value) ->
                if (key.name.startsWith("level_stars_")) {
                    val levelId = key.name.removePrefix("level_stars_").toIntOrNull()
                    if (levelId != null) {
                        val stars = (value as? Number)?.toInt() ?: 0
                        starsMap[levelId] = stars
                    }
                }
            }
            starsMap
        }

    val tutorialCompletedFlow: Flow<Boolean> = context.dataStore.data.map { it[TUTORIAL_COMPLETED] ?: false }
    suspend fun setTutorialCompleted(completed: Boolean) { context.dataStore.edit { it[TUTORIAL_COMPLETED] = completed } }

    val sfxVolumeFlow: Flow<Float> = context.dataStore.data.map { (it[SFX_VOLUME] as? Number)?.toFloat() ?: 1.0f }
    suspend fun setSfxVolume(volume: Float) { context.dataStore.edit { it[SFX_VOLUME] = volume } }

    val musicVolumeFlow: Flow<Float> = context.dataStore.data.map { (it[MUSIC_VOLUME] as? Number)?.toFloat() ?: 0.5f }
    suspend fun setMusicVolume(volume: Float) { context.dataStore.edit { it[MUSIC_VOLUME] = volume } }

    val musicMutedFlow: Flow<Boolean> = context.dataStore.data.map { it[MUSIC_MUTED] ?: false }
    suspend fun setMusicMuted(muted: Boolean) { context.dataStore.edit { it[MUSIC_MUTED] = muted } }

    val vibrationEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[VIBRATION_ENABLED] ?: true }
    suspend fun setVibrationEnabled(enabled: Boolean) { context.dataStore.edit { it[VIBRATION_ENABLED] = enabled } }

    val colorBlindModeFlow: Flow<Boolean> = context.dataStore.data.map { it[COLOR_BLIND_MODE] ?: false }
    suspend fun setColorBlindMode(enabled: Boolean) { context.dataStore.edit { it[COLOR_BLIND_MODE] = enabled } }

    val highScoreFlow: Flow<Int> = context.dataStore.data.map { (it[HIGH_SCORE] as? Number)?.toInt() ?: 0 }
    suspend fun setHighScore(score: Int) { context.dataStore.edit { it[HIGH_SCORE] = score } }

    val highScoreTimeFlow: Flow<Int> = context.dataStore.data.map { (it[HIGH_SCORE_TIME] as? Number)?.toInt() ?: 0 }
    suspend fun setHighScoreTime(score: Int) { context.dataStore.edit { it[HIGH_SCORE_TIME] = score } }

    val adventureProgressFlow: Flow<Int> = context.dataStore.data.map { (it[ADVENTURE_PROGRESS] as? Number)?.toInt() ?: 0 }
    suspend fun setAdventureProgress(level: Int) { context.dataStore.edit { it[ADVENTURE_PROGRESS] = level } }

    fun getLevelStars(levelId: Int): Flow<Int> {
        val key = intPreferencesKey("level_stars_$levelId")
        return context.dataStore.data.map { (it[key] as? Number)?.toInt() ?: 0 }
    }

    suspend fun setLevelStars(levelId: Int, stars: Int) {
        val key = intPreferencesKey("level_stars_$levelId")
        context.dataStore.edit { it[key] = stars }
    }

    val coinsFlow: Flow<Int> = context.dataStore.data.map { (it[COINS] as? Number)?.toInt() ?: 0 }
    suspend fun setCoins(coins: Int) { context.dataStore.edit { it[COINS] = coins } }

    fun isAchievementUnlocked(id: String): Flow<Boolean> {
        val key = booleanPreferencesKey("ach_$id")
        return context.dataStore.data.map { it[key] ?: false }
    }

    suspend fun unlockAchievement(id: String) {
        val key = booleanPreferencesKey("ach_$id")
        context.dataStore.edit { it[key] = true }
    }

    suspend fun clearAllData() {
        context.dataStore.edit { it.clear() }
    }

    // --- SINCRONIZACIÓN ---

    suspend fun getSyncableData(): Map<String, Any> {
        return try {
            val prefs = context.dataStore.data.first()
            prefs.asMap().mapKeys { it.key.name }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun updateFromSyncableData(data: Map<String, Any>) {
        try {
            context.dataStore.edit { prefs ->
                data.forEach { (keyName, value) ->
                    try {
                        when {
                            value is Boolean -> prefs[booleanPreferencesKey(keyName)] = value
                            value is Number -> {
                                if (keyName.contains("volume")) prefs[floatPreferencesKey(keyName)] = value.toFloat()
                                else prefs[intPreferencesKey(keyName)] = value.toInt()
                            }
                            value is String -> prefs[stringPreferencesKey(keyName)] = value
                        }
                    } catch (e: Exception) {
                        Log.e("SettingsManager", "Error en clave $keyName: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SettingsManager", "Error general: ${e.message}")
        }
    }
}
