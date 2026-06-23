package com.example.infinite_track.di

import android.content.Context
import android.util.Log
import com.example.infinite_track.data.face.FaceProcessor
import com.example.infinite_track.data.soucre.local.preferences.AttendancePreference
import com.example.infinite_track.data.soucre.local.preferences.LocalizationPreference
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.soucre.local.preferences.dataLanguage
import com.example.infinite_track.data.soucre.local.preferences.dataUserStore
import com.example.infinite_track.data.soucre.local.room.AppDatabase
import com.example.infinite_track.data.soucre.local.room.UserDao
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    @ApplicationCoroutineScope
    fun provideApplicationCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }

    // Core database providers
    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideUserDao(appDatabase: AppDatabase): UserDao {
        return appDatabase.userDao()
    }

    // Core preference providers
    @Provides
    fun provideUserPreference(@ApplicationContext context: Context): UserPreference {
        try {
            return UserPreference(context.dataUserStore)
        } catch (e: Exception) {
            Log.e("AppModule", "Error Providing UserPreference: ${e.message}", e)
            throw e
        }
    }

    @Provides
    fun providerMultiLanguage(@ApplicationContext context: Context): LocalizationPreference {
        try {
            return LocalizationPreference(context.dataLanguage)
        } catch (e: Exception) {
            Log.e("AppModule", "Error Providing HistoryRepository: ${e.message}", e)
            throw e
        }
    }

    @Provides
    fun provideAttendancePreference(@ApplicationContext context: Context): AttendancePreference {
        try {
            return AttendancePreference(context)
        } catch (e: Exception) {
            Log.e("AppModule", "Error Providing AttendancePreference: ${e.message}", e)
            throw e
        }
    }

    // Location services provider
    @Provides
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    // Face processor provider
    @Singleton
    @Provides
    fun provideFaceProcessor(@ApplicationContext context: Context): FaceProcessor {
        return FaceProcessor(context)
    }
}
