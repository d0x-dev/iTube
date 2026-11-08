import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.github.itube"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.github.itube"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
        release {
            // Judge speed and smoothness on this build type, never on debug.
            isMinifyEnabled = true
            isShrinkResources = true
            // Sideloaded personal build: the debug key is fine until there's a reason
            // to distribute it.
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // NewPipeExtractor uses java.nio / java.time APIs that need desugaring below API 33.
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }

    packaging {
        // Compressed in the APK (unpacked on install): the translator's library shrinks from 16 MB to under 7.
        jniLibs { useLegacyPackaging = true }
        resources {
            excludes += listOf("META-INF/AL2.0", "META-INF/LGPL2.1", "META-INF/*.kotlin_module", "META-INF/DEPENDENCIES")
        }
    }
}

// Keep the standard assemble tasks, and also emit consistently named APKs for sharing.
afterEvaluate {
    val copyDebugApk = tasks.register("copyDebugApkAsITube") {
        dependsOn("packageDebug")
        doLast {
            val dir = layout.buildDirectory.dir("outputs/apk/debug").get().asFile
            if (dir.exists()) {
                dir.listFiles { _, name -> name.endsWith(".apk") && !name.startsWith("iTube") }?.forEach { apk ->
                    val debugName = apk.name.replace("app-", "iTube-")
                    val cleanName = debugName.replace("-debug", "")
                    apk.copyTo(File(dir, cleanName), overwrite = true)
                    apk.copyTo(File(dir, debugName), overwrite = true)
                    if (apk.name.contains("universal") || apk.name == "app-debug.apk") {
                        apk.copyTo(File(dir, "iTube.apk"), overwrite = true)
                        apk.copyTo(File(dir, "iTube-debug.apk"), overwrite = true)
                    }
                }
            }
        }
    }
    tasks.named("assembleDebug") {
        finalizedBy(copyDebugApk)
    }

    val copyReleaseApk = tasks.register("copyReleaseApkAsITube") {
        dependsOn("packageRelease")
        doLast {
            val dir = layout.buildDirectory.dir("outputs/apk/release").get().asFile
            val targetDir = project.file("release")
            targetDir.mkdirs()
            if (dir.exists()) {
                dir.listFiles { _, name -> name.endsWith(".apk") && !name.startsWith("iTube") }?.forEach { apk ->
                    val releaseName = apk.name.replace("app-", "iTube-")
                    val cleanName = releaseName.replace("-release", "")
                    apk.copyTo(File(dir, cleanName), overwrite = true)
                    apk.copyTo(File(dir, releaseName), overwrite = true)
                    apk.copyTo(File(targetDir, cleanName), overwrite = true)
                    apk.copyTo(File(targetDir, releaseName), overwrite = true)
                    apk.copyTo(File(targetDir, apk.name), overwrite = true)
                    if (apk.name.contains("universal") || apk.name == "app-release.apk") {
                        apk.copyTo(File(dir, "iTube.apk"), overwrite = true)
                        apk.copyTo(File(dir, "iTube-release.apk"), overwrite = true)
                        apk.copyTo(File(targetDir, "iTube.apk"), overwrite = true)
                        apk.copyTo(File(targetDir, "iTube-release.apk"), overwrite = true)
                        apk.copyTo(File(targetDir, "app-release.apk"), overwrite = true)
                    }
                }
            }
        }
    }
    tasks.named("assembleRelease") {
        finalizedBy(copyReleaseApk)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    val media3 = "1.10.1"

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")

    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.palette:palette:1.0.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3:1.5.0-alpha18")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-exoplayer-dash:$media3")
    implementation("androidx.media3:media3-exoplayer-hls:$media3")
    implementation("androidx.media3:media3-datasource-okhttp:$media3")
    implementation("androidx.media3:media3-session:$media3")
    implementation("androidx.media3:media3-ui-compose:$media3")

    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.26.5")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.google.guava:guava:33.3.1-android")
    // Captions translated on the phone (YouTube refuses its own auto-translate): a model per language, downloaded on first use.
    implementation("com.google.mlkit:translate:17.0.3")
    // The bell's new-video check runs every 15 minutes with no window open (NewVideos).
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Parser tests run on the JVM against real InnerTube responses (src/test/resources).
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
