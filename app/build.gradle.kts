import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
}

// --- Version derived from git --------------------------------------------
// versionCode = total commit count on HEAD (monotonic across the tree).
// versionName = `git describe`. OTA gates on versionCode strictly greater
// than the installed one. Mirrors DeviceMonitor's scheme exactly.

fun gitOutput(vararg cmd: String, fallback: String): String {
    return try {
        val p = ProcessBuilder(*cmd)
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
        val out = p.inputStream.bufferedReader().readText().trim()
        p.waitFor()
        if (out.isEmpty()) fallback else out
    } catch (e: Exception) { fallback }
}

val gitVersionCode: Int = gitOutput("git", "rev-list", "--count", "HEAD", fallback = "1").toIntOrNull() ?: 1
val gitVersionName: String = gitOutput("git", "describe", "--tags", "--always", "--dirty", fallback = "0.1")

// --- Signing -------------------------------------------------------------
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) load(FileInputStream(keystorePropsFile))
}
val hasKeystore = keystorePropsFile.exists()

android {
    namespace = "com.mirar.carmenu"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mirar.carmenu"
        minSdk = 24             // androidx.car.app requires API 24+
        // Play requires targetSdk ≥ 35 for new apps & updates from Aug 2025
        // (current 2026 floor). Targeting 36 since the local SDK has it.
        targetSdk = 36
        versionCode = gitVersionCode
        versionName = gitVersionName

        val buildTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        buildConfigField("String", "BUILD_TIME", "\"$buildTime\"")
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        if (hasKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
            getByName("debug") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    testOptions {
        unitTests {
            // org.json on the Android JAR is stubbed in unit-test classpath;
            // the testImplementation org.json dep below provides the real impl.
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")
    // Android Auto / Automotive app library (template host, projected mode)
    implementation("androidx.car.app:app:1.4.0")
    // androidx.car.app.notification (CarAppExtender) — not strictly needed
    // for the minimal app but cheap to include for future use.
    // implementation("androidx.car.app:app-notification:1.4.0")

    // Local unit tests: ./gradlew test
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
