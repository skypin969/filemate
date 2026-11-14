plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.filemate.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.skypin.filemate"
        // Akses semua berkas (MANAGE_EXTERNAL_STORAGE) tersedia mulai Android 11
        minSdk = 30
        targetSdk = 35
        versionCode = 969
        versionName = "9.6.9"
    }

    // Tanda tangan rilis dipasang hanya jika FILEMATE_KEYSTORE_PATH terisi (lewat workflow Release APK).
    // Kuncinya tidak pernah disimpan di repo: isinya datang dari GitHub Secrets.
    signingConfigs {
        create("release") {
            System.getenv("FILEMATE_KEYSTORE_PATH")?.let { path ->
                storeFile = file(path)
                storePassword = System.getenv("FILEMATE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("FILEMATE_KEY_ALIAS")
                keyPassword = System.getenv("FILEMATE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (System.getenv("FILEMATE_KEYSTORE_PATH") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Library penanda tangan APK dari Android (sama seperti yang dipakai Android Studio)
    implementation("com.android.tools.build:apksig:8.7.3")
    // Pembaca berkas DEX, untuk alat Analisis kelas
    implementation("com.android.tools.smali:smali-dexlib2:3.0.3")
    // D8, pengubah JAR menjadi DEX, untuk alat Jar2Dex
    implementation("com.android.tools:r8:8.7.18")
}
