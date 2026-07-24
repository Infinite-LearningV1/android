# ================================================================================================
# INFINITE TRACK - RELEASE BUILD PROGUARD RULES
# ================================================================================================
# Purpose: Production-ready R8/ProGuard configuration for offline APK distribution
# Last Updated: 2025-10-28
# AGP Version: 8.5.2 (R8 is default)
# ================================================================================================

# ================================================================================================
# GENERAL OPTIMIZATION & OBFUSCATION SETTINGS
# ================================================================================================

# Keep source file names and line numbers for better crash reports
# Comment out for maximum obfuscation (but harder to debug production crashes)
-keepattributes SourceFile,LineNumberTable

# Keep generic signatures for Kotlin reflection and Java generics
-keepattributes Signature

# Preserve all annotations (required for Retrofit, Hilt, Room, etc.)
-keepattributes *Annotation*

# Keep exceptions for proper error handling
-keepattributes Exceptions

# ================================================================================================
# KOTLIN-SPECIFIC RULES
# ================================================================================================

# Keep Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }

# Keep Kotlin intrinsics (required for Kotlin stdlib)
-keep class kotlin.jvm.internal.** { *; }

# Preserve Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ================================================================================================
# GSON / JSON SERIALIZATION RULES
# ================================================================================================
# WHY: Gson uses reflection to access fields in data classes
# All request/response DTOs must preserve field names and structure

# Keep all data classes in network package (request/response DTOs)
-keep class com.example.infinite_track.data.soucre.network.request.** { *; }
-keep class com.example.infinite_track.data.soucre.network.response.** { *; }

# Keep Gson annotations
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.annotations.** { *; }

# Gson specific classes
-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }

# Keep generic signature of Gson classes (for parameterized types)
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from stripping @SerializedName fields
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# ================================================================================================
# RETROFIT & OKHTTP RULES
# ================================================================================================
# WHY: Retrofit uses reflection and code generation for API interfaces

# Retrofit does reflection on generic parameters and annotations
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Keep API service interfaces
-keep,allowobfuscation interface com.example.infinite_track.data.soucre.network.retrofit.** { *; }

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# OkHttp logging interceptor
-keep class okhttp3.logging.HttpLoggingInterceptor { *; }

# ================================================================================================
# HILT / DAGGER DEPENDENCY INJECTION RULES
# ================================================================================================
# WHY: Hilt uses annotation processing and reflection for DI

# Keep Hilt-generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep all Hilt modules and injected constructors
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.Module class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }

# Keep ViewModels (Hilt injects these)
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ================================================================================================
# JETPACK COMPOSE RULES
# ================================================================================================
# WHY: Compose uses reflection for @Composable functions

# Keep all Composable functions
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Compose runtime
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }

# ================================================================================================
# ANDROIDX CAMERA X RULES
# ================================================================================================
# WHY: CameraX uses reflection for camera implementations and extensions

# Keep CameraX core classes
-keep class androidx.camera.** { *; }
-keep interface androidx.camera.** { *; }

# Keep Camera2 interop classes
-keep class androidx.camera.camera2.** { *; }

# Prevent CameraX extensions from being removed
-keep class androidx.camera.extensions.** { *; }

# ================================================================================================
# ML KIT FACE DETECTION RULES
# ================================================================================================
# WHY: ML Kit uses native libraries and reflection

# Keep ML Kit classes
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.vision.** { *; }

# Keep face detection models
-keep class com.google.mlkit.vision.face.** { *; }

# ================================================================================================
# TENSORFLOW LITE RULES
# ================================================================================================
# WHY: TensorFlow Lite uses JNI and native code

# Keep TensorFlow Lite classes
-keep class org.tensorflow.lite.** { *; }
-keep interface org.tensorflow.lite.** { *; }

# Keep TensorFlow Lite GPU delegate
-keep class org.tensorflow.lite.gpu.** { *; }

# Keep model classes and metadata
-keep class org.tensorflow.lite.support.** { *; }
-keep class org.tensorflow.lite.task.** { *; }

# Prevent stripping of native methods
-keepclasseswithmembers class * {
    native <methods>;
}

# ================================================================================================
# ANDROIDX ROOM DATABASE RULES
# ================================================================================================
# WHY: Room uses annotation processing and reflection

# Keep Room database classes
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Database class * { *; }

# Keep DAO interfaces
-keep interface * extends androidx.room.Dao { *; }

# Keep Room annotations
-keepattributes *Annotation*
-keep class androidx.room.** { *; }

# ================================================================================================
# ANDROIDX WORK MANAGER RULES
# ================================================================================================
# WHY: WorkManager uses reflection to instantiate workers

# Keep all Worker classes
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }

# Keep WorkManager
-keep class androidx.work.** { *; }

# Keep Hilt Worker Factory
-keep class androidx.hilt.work.** { *; }

# Specifically keep our LocationEventWorker
-keep class com.example.infinite_track.data.worker.LocationEventWorker { *; }

# ================================================================================================
# GOOGLE PLAY SERVICES RULES
# ================================================================================================
# WHY: Play Services use reflection and native code

# Google Maps
-keep class com.google.android.gms.maps.** { *; }
-keep interface com.google.android.gms.maps.** { *; }

# Location Services
-keep class com.google.android.gms.location.** { *; }

# Places API
-keep class com.google.android.libraries.places.** { *; }

# ================================================================================================
# COIL IMAGE LOADING RULES
# ================================================================================================
# WHY: Coil uses reflection for image loading

-keep class coil.** { *; }
-keep interface coil.** { *; }

# ================================================================================================
# LOTTIE ANIMATION RULES
# ================================================================================================
# WHY: Lottie parses JSON animations

-keep class com.airbnb.lottie.** { *; }

# ================================================================================================
# GOOGLE PLAY SERVICES RULES
# ================================================================================================
# Keep Maps and other retained Play Services integrations stable under shrinking.
-keep class com.google.android.gms.** { *; }

# ================================================================================================
# PARCELIZE RULES
# ================================================================================================
# WHY: Kotlin Parcelize uses code generation

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Parcelize annotations
-keep @kotlinx.parcelize.Parcelize class * { *; }

# ================================================================================================
# APPLICATION-SPECIFIC RULES
# ================================================================================================

# Keep Application class
-keep class com.example.infinite_track.InfiniteTrackApplication { *; }

# Keep MainActivity
-keep class com.example.infinite_track.presentation.main.MainActivity { *; }

# Keep all BroadcastReceivers (required for Geofencing)
-keep class * extends android.content.BroadcastReceiver { *; }

# Keep Geofencing receivers
-keep class com.example.infinite_track.presentation.geofencing.** { *; }

# ================================================================================================
# DOMAIN MODELS (if accessed by reflection)
# ================================================================================================

# Keep domain models if they're used with Gson or reflection
# -keep class com.example.infinite_track.domain.model.** { *; }

# ================================================================================================
# MISCELLANEOUS RULES
# ================================================================================================

# Remove logging in release builds (optional - for extra security)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep BuildConfig (to check DEBUG flag at runtime)
-keep class com.example.infinite_track.BuildConfig { *; }

# Suppress warnings about missing classes (if they're optional dependencies)
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ================================================================================================
# END OF PROGUARD RULES
# ================================================================================================
