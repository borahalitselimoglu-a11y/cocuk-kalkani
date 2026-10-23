plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.cocukkalkani.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cocukkalkani.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"
    }

    // İki sürüm:
    //  full → GitHub'dan APK. Uygulama kilidi + reklam/zararlı site engelleyen DNS kalkanı (VPN).
    //  play → Google Play. Uygulama kilidi + DNS ayarı için yazılı rehber. VPN ve internet izni yok.
    flavorDimensions += "dagitim"
    productFlavors {
        create("full") {
            dimension = "dagitim"
            applicationId = "com.cocukkalkani.app"
        }
        create("play") {
            dimension = "dagitim"
            applicationId = "com.cocukkalkani.aile"
        }
    }

    // Sabit imza anahtarı: her derleme aynı imzayla çıkar, telefonda güncelleme
    // eski sürümün üstüne kurulabilir. Play için "yükleme anahtarı" olarak da kullanılır
    // (asıl imzayı Google Play App Signing yapar).
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

    lint {
        checkReleaseBuilds = false
        abortOnError = false
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
