import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val releaseVersionCode = providers.environmentVariable("VERSION_CODE")
    .orElse("1")
    .map(String::toInt)
val releaseVersionName = providers.environmentVariable("VERSION_NAME")
    .orElse("1.0")

val releaseKeystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
val releaseKeystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
val releaseSigningConfigured = listOf(
    releaseKeystorePath,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

val releaseBundleRequested = gradle.startParameter.taskNames.any { taskName ->
    taskName.substringAfterLast(':').equals("bundleRelease", ignoreCase = true)
}

if (releaseBundleRequested && !releaseSigningConfigured) {
    throw GradleException(
        "bundleRelease requires ANDROID_KEYSTORE_PATH, ANDROID_KEYSTORE_PASSWORD, " +
            "ANDROID_KEY_ALIAS, and ANDROID_KEY_PASSWORD",
    )
}

android {
    namespace = "org.mecresearch.fontbook"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.mecresearch.fontbook"
        minSdk = 26
        targetSdk = 36
        versionCode = releaseVersionCode.get()
        versionName = releaseVersionName.get()
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(requireNotNull(releaseKeystorePath))
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.fontbox)
    testImplementation(libs.junit)
    debugImplementation(libs.androidx.ui.tooling)
}
