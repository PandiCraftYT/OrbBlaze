package com.example.orbblaze.ui.game

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.SoundPool
import android.os.Build
import com.example.orbblaze.R
import com.example.orbblaze.data.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SoundManager(val context: Context, private val settingsManager: SettingsManager) {
    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<SoundType, Int>()
    private var mediaPlayer: MediaPlayer? = null
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var sfxVolume: Float = 1.0f
    private var musicVolume: Float = 0.5f
    private var isMusicMuted: Boolean = false

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
        try {
            soundMap[SoundType.SHOOT] = soundPool.load(context, R.raw.sfx_shoot, 1)
            soundMap[SoundType.POP] = soundPool.load(context, R.raw.sfx_pop, 1)
            soundMap[SoundType.EXPLODE] = soundPool.load(context, R.raw.sfx_explode, 1)
            soundMap[SoundType.SWAP] = soundPool.load(context, R.raw.sfx_swap, 1)
            soundMap[SoundType.WIN] = soundPool.load(context, R.raw.sfx_win, 1)
            soundMap[SoundType.LOSE] = soundPool.load(context, R.raw.sfx_lose, 1)
            soundMap[SoundType.STICK] = soundPool.load(context, R.raw.sfx_stick, 1)
        } catch (e: Exception) {
            e.printStackTrace()
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
            e.printStackTrace()
        }
    }

    fun setMusicSpeed(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let {
                    // Permitimos cambiar la velocidad incluso si está pausado
                    it.playbackParams = it.playbackParams.setSpeed(speed)
                }
            } catch (e: Exception) {
                // Si falla por estado (ej. no inicializado), intentamos con un objeto nuevo
                try {
                    mediaPlayer?.playbackParams = PlaybackParams().apply { this.speed = speed }
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
        }
    }

    // Estos métodos ahora llaman a settingsManager (asíncrono)
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
        // Con DataStore y flows, el refresh es automático, pero reseteamos velocidad
        setMusicSpeed(1.0f)
    }

    fun play(type: SoundType) {
        val soundId = soundMap[type] ?: return
        soundPool.play(soundId, sfxVolume, sfxVolume, 1, 0, 1f)
    }

    private fun updateMusicVolume() {
        val finalVolume = if (isMusicMuted) 0f else musicVolume
        try {
            mediaPlayer?.setVolume(finalVolume, finalVolume)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startMusic() {
        try {
            if (mediaPlayer == null) initMusic()
            if (mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pauseMusic() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
            soundPool.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
