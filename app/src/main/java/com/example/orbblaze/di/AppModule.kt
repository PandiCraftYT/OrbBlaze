package com.example.orbblaze.di

import android.content.Context
import com.example.orbblaze.data.AuthManager
import com.example.orbblaze.data.SettingsManager
import com.example.orbblaze.ui.game.AdsManager
import com.example.orbblaze.ui.game.SoundManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager(context)
    }

    @Provides
    @Singleton
    fun provideAuthManager(): AuthManager {
        return AuthManager()
    }

    @Provides
    @Singleton
    fun provideSoundManager(
        @ApplicationContext context: Context,
        settingsManager: SettingsManager
    ): SoundManager {
        return SoundManager(context, settingsManager)
    }

    @Provides
    @Singleton
    fun provideAdsManager(@ApplicationContext context: Context): AdsManager {
        return AdsManager(context)
    }
}
