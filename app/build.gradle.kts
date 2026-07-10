plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.d4nzxml.kythera"
    compileSdk = 35

    // 🔥 JURUS AUTO-JADI: Gembok dibikin & dipasang otomatis di server GitHub!
    signingConfigs {
        create("release") {
            val keystoreFile = file("kythera-auto.jks")
            if (!keystoreFile.exists()) {
                exec {
                    commandLine(
                        "keytool", "-genkey", "-v",
                        "-keystore", keystoreFile.absolutePath,
                        "-alias", "kythera",
                        "-keyalg", "RSA", "-keysize", "2048",
                        "-validity", "10000",
                        "-storepass", "jonggol123",
                        "-keypass", "jonggol123",
                        "-dname", "CN=JonggolGameCenter, O=Jonggol, C=ID"
                    )
                }
            }
            storeFile = keystoreFile
            storePassword = "jonggol123"
            keyAlias = "kythera"
            keyPassword = "jonggol123"
        }
    }

    defaultConfig {
        applicationId = "com.d4nzxml.kythera"
        minSdk = 24
        targetSdk = 28
        versionCode = 1
        versionName = "1.0.0"

        ndk {
            abiFilters.add("arm64-v8a")
        }
        
        // Pasang gembok otomatis ke defaultConfig
        signingConfig = signingConfigs.getByName("release")
    }

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
            // Pasang gembok otomatis ke tipe release
            signingConfig = signingConfigs.getByName("release")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Bagian dependencies ke bawah jangan dirubah, biarkan seperti bawaan repo lu
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    
    // Kalau ada library ffmpeg atau ncnn lu di bawah, biarkan saja tetap ada di sini
}
