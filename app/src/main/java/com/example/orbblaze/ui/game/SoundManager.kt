package com.example.orbblaze.ui.game

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.SoundPool
import android.os.Build
import android.util.Log
import com.example.orbblaze.R
import com.example.orbblaze.data.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SoundManager(val context: Context, private val settingsManager: SettingsManager) {
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<SoundType, Int>()
    private var mediaPlayer: MediaPlayer? = null
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var sfxVolume: Float = 1.0f
    private var musicVolume: Float = 0.5f
    private var isMusicMuted: Boolean = false
    
    // ✅ NUEVO: Control de intención para evitar música superpuesta o indebida
    private var shouldPlayMusic: Boolean = true

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        loadSounds()
        initMusic()
        observeSettings()
    }

    private fun loadSounds() {
        val pool = soundPool ?: return
        try {
            soundMap[SoundType.SHOOT] = pool.load(context, R.raw.sfx_shoot, 1)
            soundMap[SoundType.POP] = pool.load(context, R.raw.sfx_pop, 1)
            soundMap[SoundType.EXPLODE] = pool.load(context, R.raw.sfx_explode, 1)
            soundMap[SoundType.SWAP] = pool.load(context, R.raw.sfx_swap, 1)
            soundMap[SoundType.WIN] = pool.load(context, R.raw.sfx_win, 1)
            soundMap[SoundType.LOSE] = pool.load(context, R.raw.sfx_lose, 1)
            soundMap[SoundType.STICK] = pool.load(context, R.raw.sfx_stick, 1)
        } catch (e: Exception) {
            Log.e("SoundManager", "Error loading sounds", e)
        }
    }

    private fun observeSettings() {
        scope.launch {
            settingsManager.sfxVolumeFlow.collectLatest { sfxVolume = it }
        }
        scope.launch {
            settingsManager.musicVolumeFlow.collectLatest { 
                musicVolume = it
                updateMusicVolume()
            }
        }
        scope.launch {
            settingsManager.musicMutedFlow.collectLatest { 
                isMusicMuted = it
                updateMusicVolume()
            }
        }
    }

    private fun initMusic() {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(context, R.raw.music_background)
                mediaPlayer?.isLooping = true
            }
            updateMusicVolume()
        } catch (e: Exception) {
            Log.e("SoundManager", "Error initializing music", e)
        }
    }

    fun setMusicSpeed(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let {
                    it.playbackParams = it.playbackParams.setSpeed(speed.coerceIn(0.5f, 2.0f))
                }
            } catch (e: Exception) {
                Log.e("SoundManager", "Error setting music speed", e)
            }
        }
    }

    fun setMusicVol(vol: Float) {
        scope.launch { settingsManager.setMusicVolume(vol) }
    }

    fun setSfxVol(vol: Float) {
        scope.launch { settingsManager.setSfxVolume(vol) }
    }

    fun setMusicMute(muted: Boolean) {
        scope.launch { settingsManager.setMusicMuted(muted) }
    }

    fun isMusicMuted(): Boolean = isMusicMuted

    fun refreshSettings() {
        setMusicSpeed(1.0f)
    }

    fun play(type: SoundType) {
        // ✅ Si ganamos o perdemos, la intención de la música de fondo cambia a FALSE
        if (type == SoundType.WIN || type == SoundType.LOSE) {
            shouldPlayMusic = false
            pauseMusic()
        }
        
        val pool = soundPool ?: return
        val soundId = soundMap[type] ?: return
        pool.play(soundId, sfxVolume, sfxVolume, 1, 0, 1f)
    }

    private fun updateMusicVolume() {
        val finalVolume = if (isMusicMuted) 0f else musicVolume
        try {
            mediaPlayer?.setVolume(finalVolume, finalVolume)
        } catch (e: Exception) {
            Log.e("SoundManager", "Error updating music volume", e)
        }
    }

    fun startMusic() {
        // ✅ Solo iniciamos si el contexto actual lo permite (shouldPlayMusic)
        if (!shouldPlayMusic) return
        
        try {
            if (mediaPlayer == null) initMusic()
            if (mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error starting music", e)
        }
    }
    
    // ✅ Nuevo: Fuerza el inicio de la música (para cuando volvemos a jugar)
    fun forceStartMusic() {
        shouldPlayMusic = true
        startMusic()
    }

    fun pauseMusic() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error pausing music", e)
        }
    }

    // ✅ Nuevo: Pausa la música y marca que no debería sonar hasta nueva orden
    fun stopMusicIntentional() {
        shouldPlayMusic = false
        pauseMusic()
    }

    fun release() {
        try {
            scope.cancel()
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            mediaPlayer = null
            soundPool?.release()
            soundPool = null
            soundMap.clear()
        } catch (e: Exception) {
            Log.e("SoundManager", "Error releasing SoundManager", e)
        }
    }
}
