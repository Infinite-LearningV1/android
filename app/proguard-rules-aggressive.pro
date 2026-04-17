# ================================================================================================
# INFINITE TRACK - AGGRESSIVE RELEASE BUILD PROGUARD RULES
# ================================================================================================
# Purpose: Maximum code shrinking & obfuscation for smallest APK size
# Strategy: Strip all debug code, logging, and unused code aggressively
# Last Updated: 2025-10-28
# AGP Version: 8.5.2 (R8 default)
# ================================================================================================

# ================================================================================================
# AGGRESSIVE LOGGING REMOVAL
# ================================================================================================
# WHY: Remove ALL android.util.Log calls from release builds
# This strips debug, info, warn, and even error logs for maximum security & size reduction
# IMPACT: ~50KB+ reduction, prevents information leakage

-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# Alternative: Use maximum log level removal (keeps only wtf/assert)
# Uncomment if you want to keep error logs
# -maximumremovedandroidloglevel 4

# Remove println calls (debug prints)
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

# Remove Kotlin println
-assumenosideeffects class kotlin.io.ConsoleKt {
    public static *** println(...);
    public static *** print(...);
}

# ================================================================================================
# AGGRESSIVE OPTIMIZATION SETTINGS
# ================================================================================================

# Allow aggressive optimization
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively

# Repackage classes to reduce APK size
-repackageclasses ''
-flattenpackagehierarchy

# Remove attributes not needed in release
-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*,Exceptions

# ================================================================================================
# KOTLIN OPTIMIZATIONS
# ================================================================================================

-keep class kotlin.Metadata { *; }
-keep class kotlin.jvm.internal.** { *; }

# Keep coroutines (minimal)
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Remove Kotlin debugging metadata
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
}

# ================================================================================================
# GSON / JSON SERIALIZATION (CRITICAL - MUST KEEP)
# ================================================================================================
# WHY: Gson uses reflection to serialize/deserialize data classes
# Without these rules, runtime crashes will occur

-keep class com.example.infinite_track.data.soucre.network.request.** { *; }
-keep class com.example.infinite_track.data.soucre.network.response.** { *; }

-keepattributes Signature
-keep class com.google.gson.** { *; }
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Prevent stripping of generic signatures
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ================================================================================================
# RETROFIT & OKHTTP (MINIMAL RULES)
# ================================================================================================

-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep,allowobfuscation interface com.example.infinite_track.data.soucre.network.retrofit.** { *; }

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp platform detection
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ================================================================================================
# HILT / DAGGER (MINIMAL - RELY ON CONSUMER RULES)
# ================================================================================================

-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.Module class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }

-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ================================================================================================
# JETPACK COMPOSE (MINIMAL)
# ================================================================================================

-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# ================================================================================================
# ANDROIDX CAMERA X (CRITICAL FOR FACE RECOGNITION)
# ================================================================================================

-keep class androidx.camera.** { *; }
-keep interface androidx.camera.** { *; }
-keep class androidx.camera.camera2.** { *; }
-keep class androidx.camera.extensions.** { *; }

# ================================================================================================
# ML KIT & TENSORFLOW LITE (CRITICAL FOR FACE DETECTION)
# ================================================================================================

-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.vision.** { *; }
-keep class com.google.mlkit.vision.face.** { *; }

-keep class org.tensorflow.lite.** { *; }
-keep interface org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }
-keep class org.tensorflow.lite.support.** { *; }
-keep class org.tensorflow.lite.task.** { *; }

-keepclasseswithmembers class * {
    native <methods>;
}

# ================================================================================================
# ANDROIDX ROOM DATABASE
# ================================================================================================

-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Database class * { *; }
-keep interface * extends androidx.room.Dao { *; }
-keep class androidx.room.** { *; }

# ================================================================================================
# ANDROIDX WORK MANAGER
# ================================================================================================

-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class androidx.work.** { *; }
-keep class androidx.hilt.work.** { *; }
-keep class com.example.infinite_track.data.worker.LocationEventWorker { *; }

# ================================================================================================
# GOOGLE PLAY SERVICES (MINIMAL)
# ================================================================================================

-keep class com.google.android.gms.maps.** { *; }
-keep interface com.google.android.gms.maps.** { *; }
-keep class com.google.android.gms.location.** { *; }
-keep class com.google.android.libraries.places.** { *; }

# ================================================================================================
# MAPBOX (CRITICAL FOR MAPS)
# ================================================================================================

-keep class com.mapbox.** { *; }
-keep interface com.mapbox.** { *; }
-dontwarn com.mapbox.services.**

# ================================================================================================
# FIREBASE
# ================================================================================================

-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.example.infinite_track.presentation.fcm.InfiniteTrackFCMService { *; }

# ================================================================================================
# COIL IMAGE LOADING
# ================================================================================================

-keep class coil.** { *; }
-keep interface coil.** { *; }

# ================================================================================================
# LOTTIE ANIMATION
# ================================================================================================

-keep class com.airbnb.lottie.** { *; }

# ================================================================================================
# PARCELIZE
# ================================================================================================

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keep @kotlinx.parcelize.Parcelize class * { *; }

# ================================================================================================
# APPLICATION SPECIFIC (MINIMAL)
# ================================================================================================

-keep class com.example.infinite_track.InfiniteTrackApplication { *; }
-keep class com.example.infinite_track.presentation.main.MainActivity { *; }
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class com.example.infinite_track.presentation.geofencing.** { *; }

# Keep BuildConfig for runtime checks
-keep class com.example.infinite_track.BuildConfig { *; }

# ================================================================================================
# AGGRESSIVE DEAD CODE REMOVAL
# ================================================================================================

# Remove unused enum values
-optimizations !class/unboxing/enum

# Remove unused code paths
-assumenosideeffects class android.os.Build$VERSION {
    public static int SDK_INT return 26..2147483647;
}

# Remove debug-only code branches
-assumenosideeffects class com.example.infinite_track.BuildConfig {
    public static boolean DEBUG return false;
}

# ================================================================================================
# REMOVE DEBUG TOOLS & LIBRARIES
# ================================================================================================

# Remove any Timber/Debug logging libraries if present
-dontwarn timber.log.**
-assumenosideeffects class timber.log.** {
    *;
}

# Remove LeakCanary if present
-dontwarn com.squareup.leakcanary.**

# Remove Stetho if present  
-dontwarn com.facebook.stetho.**

# ================================================================================================
# SUPPRESS WARNINGS FOR OPTIONAL DEPENDENCIES
# ================================================================================================

-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn com.google.errorprone.annotations.**

# ================================================================================================
# END OF AGGRESSIVE PROGUARD RULES
# ================================================================================================

