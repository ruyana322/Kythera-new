plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.d4nzxml.kythera"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.d4nzxml.kythera"
        minSdk = 24
        targetSdk = 28
        versionCode = 1
        versionName = "1.0.0"

        // Jurus diet APK khusus Poco X6 Pro
        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    // Jurus bungkam satpam Lint
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
    
    buildFeatures {
        compose = true
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
} // <--- NAH, TUTUPNYA YANG BENER DI SINI KANG!

// (Biarkan kodingan dependencies { ... } dan ke bawahnya tetap utuh, jangan dihapus)


dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.appcompat)

        // Hapus yang ini: implementation("com.arthenica:ffmpeg-kit-full-gpl:6.0-2")
    // Ganti jadi ini lagi:
    implementation(files("libs/ffmpeg-kit-full-gpl-6.0-2.LTS.aar"))


    implementation(libs.coil.compose)
    debugImplementation(libs.androidx.ui.tooling)
}
