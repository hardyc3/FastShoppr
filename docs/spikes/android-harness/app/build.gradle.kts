plugins {
    id("com.android.application")
}

android {
    namespace = "com.fastshoppr.harness"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fastshoppr.harness"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1-phase0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation("androidx.activity:activity:1.12.4")
    implementation("androidx.core:core:1.17.0")
    implementation("com.google.mlkit:digital-ink-recognition:19.0.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:genai-prompt:1.0.0-beta5")
}
