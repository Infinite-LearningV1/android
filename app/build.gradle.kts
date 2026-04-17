import java.util.Properties

// Kode untuk membaca local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
val mapboxAccessToken: String = localProperties.getProperty("MAPBOX_ACCESS_TOKEN") ?: ""

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.infinite_track"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.infinite_track"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Add Mapbox access token to manifest dan BuildConfig
        manifestPlaceholders["MAPBOX_PUBLIC_TOKEN"] = mapboxAccessToken
        buildConfigField("String", "MAPBOX_PUBLIC_TOKEN", "\"$mapboxAccessToken\"")
        
        // Optimize native libraries - only include necessary ABIs
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
    }

    buildTypes {
        release {
            // ============================================================================
            // AGGRESSIVE RELEASE BUILD CONFIGURATION
            // ============================================================================
            
            // CRITICAL: Enable R8 code shrinking, obfuscation, and optimization
            isMinifyEnabled = true
            
            // CRITICAL: Enable resource shrinking (removes ALL unused resources)
            isShrinkResources = true
            
            // CRITICAL: Explicitly disable debugging for production security
            isDebuggable = false
            
            // Enable aggressive ProGuard optimization
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "proguard-rules-aggressive.pro"  // ← ADDED: Aggressive rules
            )
            
            // Enable R8 full mode for maximum optimization
            // This enables more aggressive optimizations
            // Uncomment if you want even more aggressive optimization:
            // android.enableR8.fullMode=true (in gradle.properties)
            
            // Optional: Enable code signing (configure keystore separately)
            // signingConfig = signingConfigs.getByName("release")
            
            // Generate mapping file for stack trace deobfuscation
            // Mapping file: app/build/outputs/mapping/release/mapping.txt
            
            // Optimize native libraries (strip debug symbols)
            ndk {
                debugSymbolLevel = "NONE"  // Remove all debug symbols from .so files
            }
        }
        
        debug {
            // Debug builds - NO optimization for faster build times
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            
            // NOTE: applicationIdSuffix removed to match google-services.json configuration
            // If you need separate debug/release apps, add a new client in Firebase Console
            // with package name "com.example.infinite_track.debug"
            // applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            
            // Enable strict mode in debug (optional - for detecting performance issues)
            // buildConfigField("boolean", "ENABLE_STRICT_MODE", "true")
        }
    }
    
    // Optional: Configure signing configs for release
    // Uncomment and configure with your keystore
    /*
    signingConfigs {
        create("release") {
            storeFile = file("path/to/your/keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }
    */
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
        mlModelBinding = true// Enable BuildConfig generation
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"

            // Fix for duplicate libc++_shared.so files
            pickFirst("**/libc++_shared.so")
        }
    }
}

dependencies {
    // ============================================================================
    // CORE DEPENDENCIES (ALL BUILD TYPES)
    // ============================================================================

    implementation(libs.lottie)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.kotlin.stdlib)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.foundation.android)
    implementation(libs.androidx.runtime.livedata)
    
    // DataStore
    implementation(libs.androidx.datastore.preferences.core.jvm)
    implementation(libs.androidx.datastore.preferences)
    
    // Firebase
    implementation(libs.firebase.messaging)

    // WorkManager untuk background tasks
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    
    // TensorFlow Lite
    implementation(libs.tensorflow.lite.gpu)

    // ============================================================================
    // TEST DEPENDENCIES
    // ============================================================================
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    
    // ============================================================================
    // DEBUG-ONLY DEPENDENCIES (NOT INCLUDED IN RELEASE)
    // ============================================================================
    // WHY: These are only needed during development and increase APK size
    
    debugImplementation(libs.androidx.ui.tooling)        // Compose preview tools
    debugImplementation(libs.androidx.ui.test.manifest)   // Test manifest

    //DataStore
    implementation(libs.androidx.datastore.preferences)

    //Navigation
    implementation(libs.coil.compose.v230)
    implementation(libs.androidx.navigation.compose)

    // ============================================================================
    // NETWORKING
    // ============================================================================
    
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)  // Moved from below

    //Camera X
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.camera2)

    // ML Kit Face Detection
    implementation(libs.face.detection)

    //coil
    implementation(libs.coil.kt.coil.compose)

    //Icon
    implementation(libs.androidx.material.icons.extended)

    //Activity
    implementation(libs.androidx.activity.compose)

    // Gambar Bitmap
    implementation(libs.ui)
    implementation(libs.androidx.exifinterface)

    // ============================================================================
    // DEPENDENCY INJECTION (HILT)
    // ============================================================================
    
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    kapt(libs.androidx.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // ============================================================================
    // UI COMPONENTS
    // ============================================================================
    
    implementation(libs.library)  // SweetAlert dialog


    // Jetpack Compose core libraries
    implementation(libs.androidx.ui.v150)
    implementation(libs.material3)
    implementation(libs.ui.tooling.preview)

    // Google Maps Composec
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.places)
    implementation(libs.play.services.location)

    // Accompanist Permissions (untuk handling permissions di Compose)
    implementation(libs.accompanist.permissions)

    // Mapbox
    implementation(libs.mapboxMapsSdk)
    implementation(libs.mapboxMapsComposeExtension) // Added Mapbox Compose Extension

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation("androidx.room:room-ktx:2.6.1") // Using hardcoded version for Room KTX
    kapt(libs.room.compiler)

    //Tensorflow Lite
    implementation(libs.tensorflow.lite.task.vision)
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.metadata)
    implementation(libs.tensorflow.lite.support)
}