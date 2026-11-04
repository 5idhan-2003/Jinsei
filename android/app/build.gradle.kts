plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.thelifecalendar.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.thelifecalendar.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 8
        versionName = "1.0.7"
        setProperty("archivesBaseName", "Jinsei")
    }

    signingConfigs {
        create("shared") {
            storeFile = file("keystore.jks")
            storePassword = "jinseiapppassword"
            keyAlias = "jinsei"
            keyPassword = "jinseiapppassword"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("shared")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("shared")
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
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
}

