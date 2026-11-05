plugins {
    id("com.android.application")
}

android {
    namespace = "com.fastshoppr.spike"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fastshoppr.spike"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "0.1-phase0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.mlkit:digital-ink-recognition:19.0.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:genai-prompt:1.0.0-beta5")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jsoup:jsoup:1.18.3")
}
