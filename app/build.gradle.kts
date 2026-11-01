import java.io.File
import java.util.Base64
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

/**
 * La clé de signature doit être la MÊME à chaque build, sinon une installation
 * par-dessus l'app existante échoue et la mise à jour intégrée ne fonctionne plus.
 * CI : variables d'environnement. Local : fichiers de secrets.
 */
fun secretFromFiles(key: String): String? {
    val candidates = listOf(
        File("/root/.secrets/keystores-android/mauxdetete-keystore.env"),
        File("/root/.secrets/keystores-android/passwords.env"),
    )
    for (file in candidates) {
        if (!file.exists()) continue
        val props = Properties()
        file.inputStream().use { props.load(it) }
        val value = props.getProperty(key) ?: props.getProperty(key.replaceFirst("MAUXDETETE_", ""))
        if (!value.isNullOrBlank()) return value
    }
    return null
}

val storePassword: String? = System.getenv("MAUXDETETE_KEYSTORE_PASSWORD")
    ?: secretFromFiles("MAUXDETETE_KEYSTORE_PASSWORD")
val keyAliasValue: String = System.getenv("MAUXDETETE_KEY_ALIAS") ?: "mauxdetete"
val keyPasswordValue: String? = System.getenv("MAUXDETETE_KEY_PASSWORD") ?: storePassword

val keystoreFile: File? = run {
    val local = File("/root/.secrets/keystores-android/mauxdetete-release.keystore")
    val b64 = System.getenv("MAUXDETETE_KEYSTORE_B64")
    when {
        !b64.isNullOrBlank() -> {
            val target = File(System.getenv("RUNNER_TEMP") ?: System.getProperty("java.io.tmpdir"), "mauxdetete-release.keystore")
            target.writeBytes(Base64.getDecoder().decode(b64))
            target
        }
        local.exists() -> local
        else -> null
    }
}

android {
    namespace = "com.trucdecomptable.mauxdetete"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.trucdecomptable.mauxdetete"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        resourceConfigurations += listOf("fr")
    }

    signingConfigs {
        if (keystoreFile != null && storePassword != null) {
            create("release") {
                storeFile = keystoreFile
                storePassword = this@Build_gradle.storePassword
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Stockage local
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Verrouillage biométrique
    implementation("androidx.biometric:biometric:1.1.0")

    // Téléchargement de l'APK pour la mise à jour intégrée
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20231013")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
