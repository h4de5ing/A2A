plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.galexander.sshd"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.galexander.sshd.v2"
        minSdk = 30//24 30
        targetSdk = 35
        versionCode = 34
        versionName = "3.4-h4de5ing"
        ndk.abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        buildConfigField("int", "NR_OF_LOG_LINES", "50")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
    sourceSets.getByName("main").jniLibs {
        srcDir("src/main/jniLibs")
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.fragment:fragment-ktx:1.8.6")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.transition:transition:1.5.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.github.mwiede:jsch:0.2.18")
}