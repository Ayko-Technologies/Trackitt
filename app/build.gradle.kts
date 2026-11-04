import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

/*
 * Signing keys live outside the repository, in ~/.config/trackitt/signing (or the directory given by
 * -Ptrackitt.signingDir), or come from TRACKITT_* environment variables in CI:
 *  - trackitt-dev.keystore: debug builds. One shared key means any machine's build installs over
 *    another's without wiping data, and Google sign-in keeps working.
 *  - release-credentials.properties (storeFile, storePassword, keyAlias, keyPassword): release builds.
 * Without them, debug builds fall back to the machine's own debug key and release builds are unsigned.
 */
val signingDir = file(
    providers.gradleProperty("trackitt.signingDir").orNull
        ?: "${System.getProperty("user.home")}/.config/trackitt/signing"
)
val devKeystore = (System.getenv("TRACKITT_DEV_STORE_FILE")?.let(::file) ?: File(signingDir, "trackitt-dev.keystore"))
    .takeIf { it.exists() }
val releaseSigning = Properties().apply {
    File(signingDir, "release-credentials.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
    mapOf(
        "storeFile" to "TRACKITT_RELEASE_STORE_FILE",
        "storePassword" to "TRACKITT_RELEASE_STORE_PASSWORD",
        "keyAlias" to "TRACKITT_RELEASE_KEY_ALIAS",
        "keyPassword" to "TRACKITT_RELEASE_KEY_PASSWORD",
    ).forEach { (key, env) -> System.getenv(env)?.let { setProperty(key, it) } }
}
val hasReleaseSigning = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
    .all { !releaseSigning.getProperty(it).isNullOrBlank() }

android {
    namespace = "com.ayko.trackitt"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ayko.trackitt"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            if (devKeystore != null) {
                storeFile = devKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseSigning.getProperty("storeFile"))
                storePassword = releaseSigning.getProperty("storePassword")
                keyAlias = releaseSigning.getProperty("keyAlias")
                keyPassword = releaseSigning.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
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
        compose = true
    }

    sourceSets {
        // Exported Room schemas let MigrationTestHelper build every past database version.
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/INDEX.LIST",
            )
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.biometric)

    debugImplementation(libs.androidx.ui.tooling)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)


    // ML Kit GenAI (on-device Gemini Nano)
    implementation(libs.mlkit.genai.prompt)

    // Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
