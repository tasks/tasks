import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

version = libs.versions.versionName.get().let {
    if (it.count { c -> c == '.' } < 2) "$it.0" else it
}

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.gms.google-services")
    id("dev.hydraulic.conveyor") version "2.0"
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop")

    // iOS requires adding iOS targets to data and kmp modules first
    // listOf(
    //     iosX64(),
    //     iosArm64(),
    //     iosSimulatorArm64(),
    // ).forEach { iosTarget ->
    //     iosTarget.binaries.framework {
    //         baseName = "ComposeApp"
    //         isStatic = true
    //     }
    // }

    sourceSets {
        val desktopMain by getting
        val desktopTest by getting

        desktopTest.dependencies {
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.mockito.kotlin)
        }

        commonMain.dependencies {
            implementation(projects.data)
            implementation(projects.kmp)
            implementation(libs.androidx.room)
            implementation(libs.androidx.sqlite)
            implementation(libs.androidx.datastore)
            implementation(compose.components.resources)
            implementation(compose.foundation)
            implementation("androidx.compose.material3:material3:1.5.0-alpha15")
            implementation(compose.materialIconsExtended)
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.jetbrains.lifecycle.viewmodel.navigation3)
            implementation(libs.kotlinx.immutable)
            implementation(libs.kotlinx.serialization)
            implementation(libs.kermit)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.markdown.renderer.m3)
            implementation("org.jetbrains.compose.material3.adaptive:adaptive:1.1.2")
            implementation("org.jetbrains.compose.material3.adaptive:adaptive-layout:1.1.2")
            implementation("org.jetbrains.compose.material3.adaptive:adaptive-navigation:1.1.2")
        }
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.androidx.browser)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.java.keyring)
            implementation(libs.posthog)
        }
    }
}

android {
    namespace = "org.tasks"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    defaultConfig {
        applicationId = "org.tasks"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    flavorDimensions += "store"
    productFlavors {
        create("googleplay") {
            dimension = "store"
        }
        create("fdroid") {
            dimension = "store"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    dependencies {
        coreLibraryDesugaring(libs.desugar.jdk.libs)
        debugImplementation(compose.uiTooling)
        "googleplayImplementation"(platform(libs.firebase))
        "googleplayImplementation"(libs.firebase.messaging)
        "googleplayImplementation"(libs.play.services.code.scanner)
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "tasks-org"
            packageVersion = libs.versions.versionName.get().let {
                if (it.count { c -> c == '.' } < 2) "$it.0" else it
            }
        }
    }
}

// Conveyor platform-specific Compose runtime dependencies
dependencies {
    "linuxAmd64"(compose.desktop.linux_x64)
    "macAmd64"(compose.desktop.macos_x64)
    "macAarch64"(compose.desktop.macos_arm64)
    "windowsAmd64"(compose.desktop.windows_x64)
}
