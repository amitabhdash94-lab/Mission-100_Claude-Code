import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// ---------------------------------------------------------------------------
// Signing resolution order:
//   1. Environment variables (used by GitHub Actions)
//   2. keystore.properties in the repo root (optional local override)
//   3. Debug signing fallback, so the project always configures
// ---------------------------------------------------------------------------

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}

fun pick(envName: String, propName: String): String? {
    val fromEnv = System.getenv(envName)
    if (!fromEnv.isNullOrBlank()) return fromEnv
    val fromProps = keystoreProperties.getProperty(propName)
    if (!fromProps.isNullOrBlank()) return fromProps
    return null
}

val releaseStorePath: String? = pick("KEYSTORE_PATH", "storeFile")
val releaseStorePassword: String? = pick("KEYSTORE_PASSWORD", "storePassword")
val releaseKeyAlias: String? = pick("KEY_ALIAS", "keyAlias")
val releaseKeyPassword: String? = pick("KEY_PASSWORD", "keyPassword")

val releaseKeystoreFile: File? = releaseStorePath
    ?.let { rootProject.file(it) }
    ?.takeIf { it.exists() }

val hasReleaseSigning: Boolean = releaseKeystoreFile != null &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

if (!hasReleaseSigning) {
    logger.warn(
        "NEUROFIT: no release signing material found, falling back to DEBUG signing. " +
            "A debug signed APK will NOT install as an upgrade over a release signed one. " +
            "Provide KEYSTORE_PATH, KEYSTORE_PASSWORD, KEY_ALIAS and KEY_PASSWORD, " +
            "or a keystore.properties file. See README section 1."
    )
}

// versionCode is driven by the CI run number so every build installs as an upgrade.
val ciBuildNumber: Int = System.getenv("BUILD_NUMBER")?.trim()?.toIntOrNull() ?: 1

android {
    namespace = "com.neurofit.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.neurofit.app"
        minSdk = 31
        targetSdk = 36
        versionCode = ciBuildNumber
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Galaxy S23 Ultra only. Keep the APK lean.
        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = false
            }
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        getByName("release") {
            // Phase 1 decision: R8 stays OFF until the pipeline is proven green.
            // The keep rules in proguard-rules.pro are already written and correct,
            // so flipping these two flags to true is a one line change later.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    // Single universal APK. No ABI splits, no density splits, no bundle.
    splits {
        abi { isEnable = false }
        density { isEnable = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
            excludes.add("/META-INF/DEPENDENCIES")
            excludes.add("/META-INF/LICENSE*")
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.core)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.animation.core)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

// Used by CI to name the release asset. Prints only the version name.
tasks.register("printVersionName") {
    val resolved = android.defaultConfig.versionName ?: "0.0.0"
    doLast { println(resolved) }
}
