plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.cocukkalkani.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cocukkalkani.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // Sabit imza anahtarı: her derleme aynı imzayla çıkar, telefonda güncelleme
    // eski sürümün üstüne kurulabilir (kaldırıp yeniden yüklemek gerekmez).
    signingConfigs {
        create("sabit") {
            storeFile = file("cocuk-kalkani.keystore")
            storePassword = "cocukkalkani"
            keyAlias = "cocukkalkani"
            keyPassword = "cocukkalkani"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("sabit")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("sabit")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

// Harici kütüphane yok: yalnızca Android çatısı + Kotlin standart kütüphanesi.
