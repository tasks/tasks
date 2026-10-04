import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.gms.google-services")
    id("dev.hydraulic.conveyor") version "2.0"
}

val appVersionCode: Int by extra
val appVersionName: String by extra
version = appVersionName.let { if (it.count { c -> c == '.' } < 2) "$it.0" else it }

kotlin {
    applyDefaultHierarchyTemplate()

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(libs.versions.jdk.get()))
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        val desktopMain by getting
        val desktopTest by getting
        val jvmCommonMain by creating {
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(jvmCommonMain)
        desktopMain.dependsOn(jvmCommonMain)

        desktopTest.dependencies {
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.mockito.kotlin)
            implementation(libs.okhttp.mockwebserver)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            implementation(compose.desktop.currentOs)
            implementation(project(path = ":kmp", configuration = "jvmTestOutput"))
        }

        commonMain.dependencies {
            implementation(projects.data)
            implementation(projects.kmp)
            implementation(libs.androidx.room3)
            implementation(libs.androidx.sqlite)
            implementation(libs.androidx.datastore)
            implementation(compose.components.resources)
            implementation(compose.foundation)
            implementation(libs.jetbrains.material3)
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
            implementation(libs.reorderable)
            implementation(libs.jetbrains.adaptive)
            implementation(libs.jetbrains.adaptive.layout)
            implementation(libs.jetbrains.adaptive.navigation)
            implementation(libs.jetbrains.adaptive.navigation3)
        }
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.androidx.browser)
        }
        iosMain.dependencies {
            implementation(libs.crashkios.crashlytics)
            implementation(libs.kermit.crashlytics)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.mcp.kotlin.sdk)
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.cio)
            implementation(libs.ktor.server.sse)
            implementation(libs.java.keyring)
            implementation(libs.posthog)
            implementation(libs.nucleus.notification.linux)
            implementation(libs.nucleus.notification.macos)
            implementation(libs.nucleus.notification.windows)
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
        versionCode = appVersionCode
        versionName = appVersionName
    }
    packaging {
        resources {
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/INDEX.LIST", "META-INF/DEPENDENCIES")
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
            packageVersion = version.toString()

            modules(
                "java.compiler",
                "java.management",
                "java.naming",
                "java.security.jgss",
                "java.sql",
                "jdk.accessibility",
                "jdk.httpserver",
                "jdk.localedata",
                "jdk.net",
                "jdk.security.auth",
                "jdk.unsupported",
                "jdk.zipfs",
            )
        }
    }
}

val sqliteNatives = providers.gradleProperty("sqliteNatives")
    .map { layout.projectDirectory.dir(it) }

kotlin.sourceSets.named("desktopMain") {
    sqliteNatives.orNull?.let { resources.srcDir(it) }
}

if (providers.gradleProperty("release").isPresent) {
    val libs = sqliteNatives.map { dir ->
        listOf("windows_arm64/sqliteJni.dll", "osx_x64/libsqliteJni.dylib")
            .map { dir.file("natives/$it").asFile }
    }
    tasks.named("desktopProcessResources") {
        doFirst {
            check(libs.orNull?.all { it.isFile } == true) {
                "Release builds need -PsqliteNatives=<dir from build-sqlite-natives.sh>"
            }
        }
    }
}

// Conveyor platform-specific Compose runtime dependencies
dependencies {
    "linuxAmd64"(compose.desktop.linux_x64)
    "linuxAarch64"(compose.desktop.linux_arm64)
    "macAmd64"(compose.desktop.macos_x64)
    "macAarch64"(compose.desktop.macos_arm64)
    "windowsAmd64"(compose.desktop.windows_x64)
    "windowsAarch64"(compose.desktop.windows_arm64)
}
