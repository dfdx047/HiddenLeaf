plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.dfdx047.hiddenleaf"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dfdx047.hiddenleaf"
        minSdk = 29
        targetSdk = 35
        // CI passes the tag (android-v0.1.0 -> 0.1.0) and the run number; a local build is named after the line below
        val vName = System.getenv("HL_VERSION_NAME") ?: "0.1.0"
        val vParts = vName.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        versionCode = System.getenv("HL_VERSION_CODE")?.toInt()
            ?: maxOf(1, vParts.getOrElse(0) { 0 } * 10000 + vParts.getOrElse(1) { 0 } * 100 + vParts.getOrElse(2) { 0 })
        versionName = vName

        // The recompiled engine is arm64 only.
        ndk { abiFilters += listOf("arm64-v8a") }
    }

    // Release signing: a keystore given by the environment (CI secrets), else the debug key.
    val ks = System.getenv("HL_KEYSTORE")
    signingConfigs {
        // Test builds (debug, and the nightly): one fixed key kept in the repository, so a newer build installs over
        // the old one and keeps the game's data. Not for publishing.
        getByName("debug") {
            storeFile = file("hiddenleaf-test.jks")
            storePassword = "hiddenleaf"
            keyAlias = "hiddenleaf-test"
            keyPassword = "hiddenleaf"
        }
        if (ks != null && file(ks).exists()) {
            create("release") {
                storeFile = file(ks)
                storePassword = System.getenv("HL_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("HL_KEY_ALIAS")
                keyPassword = System.getenv("HL_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
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
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        // the engine is installed as files, with its symbols (crash reports name the functions)
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols += "**/libun5.so"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)
}
