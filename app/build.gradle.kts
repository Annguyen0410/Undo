import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// RevenueCat SDK keys are never committed. They are read from local.properties
// (git-ignored) or from the environment, so this repository stays safe to publish.
// See README → "RevenueCat setup" for the two keys this app expects:
//   REVENUECAT_TEST_KEY    Test Store key, used by debug builds (no store set up required)
//   REVENUECAT_GOOGLE_KEY  Google Play public SDK key, used by release builds
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun secret(name: String): String =
    (localProperties.getProperty(name) ?: System.getenv(name) ?: "")
        .trim()
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

android {
    namespace = "com.zhiend.regretnote"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.zhiend.regretnote"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Debug builds talk to RevenueCat's Test Store: real offerings, real
            // entitlements and real purchases reach the dashboard with no Google Play,
            // Galaxy Store or App Store setup. Leave REVENUECAT_TEST_KEY empty to fall
            // back to the app's offline demo paywall.
            buildConfigField("String", "REVENUECAT_API_KEY", "\"${secret("REVENUECAT_TEST_KEY")}\"")
        }
        release {
            // Real store key (`goog_…` for Google Play, `galx_…` for the Galaxy Store).
            // Never ship a Test Store key in a release build. Leave it empty until the
            // store listing exists — the paywall then stays in its locked demo state.
            buildConfigField("String", "REVENUECAT_API_KEY", "\"${secret("REVENUECAT_GOOGLE_KEY")}\"")
            optimization {
                enable = false
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.revenuecat.purchases)
    implementation(libs.revenuecat.purchases.ui)
    implementation(libs.vico.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
