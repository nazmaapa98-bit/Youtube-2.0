plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.tonmoy.ytplayer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tonmoy.ytplayer"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.0.5"
    }

    // Release signing config — reads from environment vars (CI) or uses included release.keystore
    signingConfigs {
        create("release") {
            val storeFilePath = System.getenv("STORE_FILE") ?: "release.keystore"
            val storeFile = file(storeFilePath)
            if (storeFile.exists()) {
                this.storeFile = storeFile
                storePassword = System.getenv("STORE_PASSWORD") ?: "ytplayer123"
                keyAlias = System.getenv("KEY_ALIAS") ?: "ytplayer"
                keyPassword = System.getenv("KEY_PASSWORD") ?: "ytplayer123"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true  // Enable BuildConfig for version checking
    }
}

dependencies {
    // Java 8+ / NIO Desugaring (required for NewPipeExtractor & OkHttp on older APIs)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // AndroidX Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.webkit)

    // Jetpack Compose (BOM-managed)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Media3 (ExoPlayer + MediaSession + OkHttp DataSource)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.datasource.okhttp)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // OkHttp (BOM-managed)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // NewPipe Extractor (YouTube audio stream extraction)
    implementation(libs.newpipe.extractor)
}
