plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.shadowbrowser.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.shadowbrowser.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "2.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        viewBinding = true
    }
    packaging {
        resources.excludes += setOf(
            "META-INF/DEPENDENCIES",
            "META-INF/AL2.0",
            "META-INF/LGPL2.1"
        )
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    // GeckoView（Mozilla 内核，随 APK 打包，不依赖系统 WebView）
    implementation("org.mozilla.geckoview:geckoview-arm64-v8a:153.0.20260810162159")
    // 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // 订阅抓取
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // JSON
    implementation("com.google.code.gson:gson:2.11.0")
}
