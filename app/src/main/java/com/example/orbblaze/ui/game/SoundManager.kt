package com.example.orbblaze.ui.game

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.example.orbblaze.R

enum class SoundType {
    SHOOT, POP, EXPLODE, SWAP, WIN, LOSE, STICK
}

class SoundManager(val context: Context) {
    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<SoundType, Int>()

    // Música de fondo
    private var mediaPlayer: MediaPlayer? = null

    private val prefs = context.getSharedPreferences("orbblaze_prefs", Context.MODE_PRIVATE)
    private var sfxVolume: Float = prefs.getFloat("sfx_volume", 1.0f)
    private var musicVolume: Float = prefs.getFloat("music_volume", 0.5f)

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

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

        initMusic()
    }

    private fun initMusic() {
        try {
            mediaPlayer = MediaPlayer.create(context, R.raw.music_background)
            mediaPlayer?.isLooping = true
            updateMusicVolume()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- FUNCIONES PARA ACTUALIZACIÓN EN TIEMPO REAL ---

    // ✅ NUEVO: Actualiza volumen de música al instante
    fun setMusicVol(vol: Float) {
        musicVolume = vol
        updateMusicVolume()
    }

    // ✅ NUEVO: Actualiza volumen de efectos al instante
    fun setSfxVol(vol: Float) {
        sfxVolume = vol
    }

    // Recarga desde preferencias (para cuando cambias de pantalla)
    fun refreshSettings() {
        sfxVolume = prefs.getFloat("sfx_volume", 1.0f)
        musicVolume = prefs.getFloat("music_volume", 0.5f)
        updateMusicVolume()
    }

    fun play(type: SoundType) {
        val soundId = soundMap[type] ?: return
        soundPool.play(soundId, sfxVolume, sfxVolume, 1, 0, 1f)
    }

    private fun updateMusicVolume() {
        mediaPlayer?.setVolume(musicVolume, musicVolume)
    }

    fun startMusic() {
        if (mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start()
        }
    }

    fun pauseMusic() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
    }

    fun release() {
        soundPool.release()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}