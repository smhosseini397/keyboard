plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.farshekashan.keyfarsi"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.farshekashan.keyfarsi"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

dependencies {
    // ✅ برای WindowInsetsCompat و ViewCompat (رفع مشکل زیر اکشن‌بار)
    implementation("androidx.core:core-ktx:1.13.1")

    // ✅ برای مدیریت بهتر Activity و edge-to-edge
    implementation("androidx.activity:activity-ktx:1.9.3")

    // ✅ برای ایموجی‌ها روی دستگاه‌های قدیمی (minSdk 23)
    implementation("androidx.emoji2:emoji2:1.5.0")
    implementation("androidx.emoji2:emoji2-views-helper:1.5.0")
}
