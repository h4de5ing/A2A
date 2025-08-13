plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.android.h4de5ing.cclient"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.android.h4de5ing.cclient"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    signingConfigs {
        create("release") {
            keyAlias = "android"
            keyPassword = "android"
            storeFile = file("D:\\Android12SignerGUI\\SignFiles\\NewPublic\\platform.jks")
            storePassword = "android"
        }
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_22
        targetCompatibility = JavaVersion.VERSION_22
    }
}

dependencies {
    compileOnly(files("libs/framework.jar"))
}