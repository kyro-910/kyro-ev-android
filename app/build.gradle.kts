plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) localPropertiesFile.inputStream().use { localProperties.load(it) }

android {
    namespace = "com.kyro.ev"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kyro.ev"
        minSdk = 29
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProperties.getProperty("GEMINI_API_KEY", "")}\"")
        buildConfigField("String", "GEMINI_MODEL", "\"${localProperties.getProperty("GEMINI_MODEL", "gemini-3.8-flash")}\"")
    }

    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("EV_KEYSTORE_PATH")
            val keystorePassword = System.getenv("EV_KEYSTORE_PASSWORD")
            val keyAliasValue = System.getenv("EV_KEY_ALIAS")
            val keyPasswordValue = System.getenv("EV_KEY_PASSWORD")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                storeType = "JKS"
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
