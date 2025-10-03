plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
    // id("com.google.gms.google-services")
}

android {
    namespace = "ec.net.csed.tv"
    compileSdk = 35

    defaultConfig {
        applicationId = "ec.net.csed.tv"
        minSdk = 21
        targetSdk = 35
        versionCode = 3
        versionName = "2.2"
        vectorDrawables {
            useSupportLibrary = true
        }

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("androidx.navigation:navigation-compose:2.7.5")
    implementation("androidx.compose.material3:material3:1.2.1")
    // Para llamadas HTTP
    implementation("io.ktor:ktor-client-core:2.3.4")
    implementation("io.ktor:ktor-client-okhttp:2.3.4")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.4")
    // ver los canales en grilla
    implementation("io.coil-kt:coil-compose:2.4.0")
    // Para el reproductor de video
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.2.0")
    implementation("androidx.media3:media3-common:1.2.0")
    implementation("androidx.media3:media3-session:1.2.0")

    // Para reproducir audio/video usando VLC
    // implementation("org.videolan.android:libvlc-all:3.5.1")
    // implementation("org.videolan.android:vlc-android-sdk:3.5.1")
    implementation("org.videolan.android:libvlc-all:3.6.1")
    //implementation("org.videolan.android:libvlc-all:3.5.2-eap1")
    implementation("androidx.compose.runtime:runtime-saveable:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("androidx.compose.ui:ui:1.5.1")
    implementation("androidx.compose.foundation:foundation:1.5.1")

    //implementation("androidx.tv:tv-foundation:1.0.0-alpha07")
    //implementation("androidx.tv:tv-foundation:1.0.0-alpha10")
    //implementation("androidx.tv:tv-material3:1.0.0-alpha10")
    implementation("androidx.tv:tv-foundation:1.0.0-alpha10")
    implementation("androidx.tv:tv-material:1.0.0-alpha10")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.mediarouter:mediarouter:1.6.0")
    // implementation("com.google.android.gms:play-services-cast-framework:21.3.0")
    implementation("com.google.android.gms:play-services-cast-framework:21.4.0")
// Google Cast SDK (para botones, sesiones, media control, notificaciones, etc.)
    implementation("androidx.appcompat:appcompat:1.6.1") // Necesaria para Cast context
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    implementation("androidx.compose.material:material-icons-extended-android:1.6.5")
}

apply(plugin = "com.google.gms.google-services")