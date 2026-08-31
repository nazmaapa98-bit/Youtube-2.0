package com.tonmoy.ytplayer.di

import android.content.Context
import com.tonmoy.ytplayer.webview.AdBlockEngine
import com.tonmoy.ytplayer.webview.WebViewState
import com.tonmoy.ytplayer.playback.AudioExtractor
import com.tonmoy.ytplayer.playback.WakeLockManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt dependency injection module providing application-wide singletons.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }

        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideAdBlockEngine(
        @ApplicationContext context: Context
    ): AdBlockEngine {
        return AdBlockEngine(context)
    }

    @Provides
    @Singleton
    fun provideAudioExtractor(
        okHttpClient: OkHttpClient
    ): AudioExtractor {
        return AudioExtractor(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideWakeLockManager(
        @ApplicationContext context: Context
    ): WakeLockManager {
        return WakeLockManager(context)
    }

    @Provides
    @Singleton
    fun provideWebViewState(): WebViewState {
        return WebViewState()
    }
}
