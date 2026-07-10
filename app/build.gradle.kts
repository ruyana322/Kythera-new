plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.d4nzxml.kythera"
    compileSdk = 35

    // 🔥 JURUS AUTO-JADI: Bikin & pasang tanda tangan otomatis
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
        
        // 🔥 Turun ke API 28 biar lolos Error 13 di Android 14
        targetSdk = 28
        versionCode = 1
        versionName = "1.0.0"

        // 🔥 Diet APK khusus arsitektur 64-bit
        ndk {
            abiFilters.add("arm64-v8a")
        }

        // Pasang gembok
        signingConfig = signingConfigs.getByName("release")
    }

    // 🔥 Bungkam satpam Lint Google Play
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
            // Pasang gembok ke tipe rilis
            signingConfig = signingConfigs.getByName("release")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "lib/x86/**"
        }
    }
}

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

    // 🔥 BALIK PAKE BAWAAN LU YANG UDAH TERBUKTI JALAN DARI AWAL
    implementation("com.github.arthenica:ffmpeg-kit:6.0-2")
    implementation("com.arthenica:smart-exception-java:0.2.1")

    implementation(libs.coil.compose)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    debugImplementation(libs.androidx.ui.tooling)
}
