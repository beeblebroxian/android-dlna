plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.cajor.dk.dlna"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.cajor.dk.dlna"
        minSdk = 15
        targetSdk = 35
        versionCode = 20
        versionName = "2.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    lint {
        disable.add("ResourceType")
        disable.add("ExpiredTargetSdkVersion")
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.mmupnp)
}