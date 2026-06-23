package com.example.infinite_track

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.example.infinite_track.presentation.main.ForegroundSessionLifecycleObserver
import com.example.infinite_track.utils.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class InfiniteTrackApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var foregroundSessionLifecycleObserver: ForegroundSessionLifecycleObserver

    override fun onCreate() {
        super.onCreate()

        // Initialize notification channel for Geofencing
        NotificationHelper.createNotificationChannel(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(foregroundSessionLifecycleObserver)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}