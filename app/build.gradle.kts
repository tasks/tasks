@file:Suppress("UnstableApiUsage")

import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    kotlin("android")
    id("dagger.hilt.android.plugin")
    id("com.google.android.gms.oss-licenses-plugin")
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose.compiler)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

composeCompiler {
    enableStrongSkippingMode = true
}

android {
    bundle {
        language {
            enableSplit = false
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        compose = true
        buildConfig = true
    }

    lint {
        lintConfig = file("lint.xml")
        textOutput = File("stdout")
        textReport = true
    }

    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        testApplicationId = "org.tasks.test"
        applicationId = "org.tasks"
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        testInstrumentationRunner = "org.tasks.TestRunner"
    }

    signingConfigs {
        create("release") {
            val tasksKeyAlias: String? by project
            val tasksStoreFile: String? by project
            val tasksStorePassword: String? by project
            val tasksKeyPassword: String? by project

            keyAlias = tasksKeyAlias
            storeFile = file(tasksStoreFile ?: "none")
            storePassword = tasksStorePassword
            keyPassword = tasksKeyPassword
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    flavorDimensions += listOf("store")

    @Suppress("LocalVariableName")
    buildTypes {
        debug {
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
            val tasks_mapbox_key_debug: String? by project
            val tasks_google_key_debug: String? by project
            val tasks_caldav_url: String? by project
            resValue("string", "mapbox_key", tasks_mapbox_key_debug ?: "")
            resValue("string", "google_key", tasks_google_key_debug ?: "")
            resValue("string", "tasks_caldav_url", tasks_caldav_url ?: "https://caldav.tasks.org")
            resValue("string", "tasks_nominatim_url", tasks_caldav_url ?: "https://nominatim.tasks.org")
            resValue("string", "tasks_places_url", tasks_caldav_url ?: "https://places.tasks.org")
            enableUnitTestCoverage = project.hasProperty("coverage")
        }
        release {
            val tasks_mapbox_key: String? by project
            val tasks_google_key: String? by project
            resValue("string", "mapbox_key", tasks_mapbox_key ?: "")
            resValue("string", "google_key", tasks_google_key ?: "")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    productFlavors {
        create("generic") {
            dimension = "store"
        }
        create("googleplay") {
            isDefault = true
            dimension = "store"
        }
    }
    packaging {
        resources {
            excludes += setOf("META-INF/*.kotlin_module", "META-INF/INDEX.LIST")
        }
    }

    testOptions {
        managedDevices {
            localDevices {
                create("pixel2api30") {
                    device = "Pixel 2"
                    apiLevel = 30
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }

    namespace = "org.tasks"
}

configurations.all {
    exclude(group = "org.apache.httpcomponents")
    exclude(group = "org.checkerframework")
    exclude(group = "com.google.code.findbugs")
    exclude(group = "com.google.errorprone")
    exclude(group = "com.google.j2objc")
    exclude(group = "com.google.http-client", module = "google-http-client-apache-v2")
    exclude(group = "com.google.http-client", module = "google-http-client-jackson2")
}

val genericImplementation by configurations
val googleplayImplementation by configurations

dependencies {
    implementation(projects.data)
    implementation(projects.kmp)
    implementation(projects.icons)
    implementation(libs.androidx.navigation)
    implementation(libs.androidx.adaptive.navigation.android)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation.layout)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.bitfire.dav4jvm) {
        exclude(group = "junit")
        exclude(group = "org.ogce", module = "xpp3")
    }
    implementation(libs.bitfire.ical4android) {
        exclude(group = "commons-logging")
        exclude(group = "org.json", module = "json")
        exclude(group = "org.codehaus.groovy", module = "groovy")
        exclude(group = "org.codehaus.groovy", module = "groovy-dateutil")
    }
    implementation(libs.bitfire.cert4android)
    implementation(libs.dmfs.opentasks.provider) {
        exclude("com.github.tasks.opentasks", "opentasks-contract")
    }
    implementation(libs.dmfs.rfc5545.datetime)
    implementation(libs.dmfs.recur)
    implementation(libs.dmfs.jems)

    implementation(libs.dagger.hilt)
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.hilt.navigation)
    implementation(libs.androidx.hilt.work)

    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.fragment.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.room)
    implementation(libs.androidx.sqlite)
    implementation(libs.androidx.appcompat)
    implementation(libs.iconics)
    implementation(libs.markwon)
    implementation(libs.markwon.editor)
    implementation(libs.markwon.linkify)
    implementation(libs.markwon.strikethrough)
    implementation(libs.markwon.tables)
    implementation(libs.markwon.tasklist)

    debugImplementation(libs.leakcanary)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation(libs.kotlin.reflect)

    implementation(libs.kotlin.jdk8)
    implementation(libs.kotlinx.immutable)
    implementation(libs.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.persistent.cookiejar)
    implementation(libs.material)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.preference)
    implementation(libs.timber)
    implementation(libs.dashclock.api)
    implementation(libs.locale) {
        isTransitive = false
    }
    implementation(libs.jchronic) {
        isTransitive = false
    }
    implementation(libs.shortcut.badger)
    implementation(libs.google.api.tasks)
    implementation(libs.google.api.drive)
    implementation(libs.google.oauth2)
    implementation(libs.androidx.work)
    implementation(libs.etebase)
    implementation(libs.colorpicker)
    implementation(libs.appauth)
    implementation(libs.osmdroid)
    implementation(libs.androidx.recyclerview)

    implementation(platform(libs.androidx.compose))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.coil.svg)
    implementation(libs.coil.gif)

    implementation(libs.ktor)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.content.negotiation)
    implementation(libs.ktor.serialization)

    implementation(libs.accompanist.permissions)

    googleplayImplementation(platform(libs.firebase))
    googleplayImplementation(libs.firebase.crashlytics)
    googleplayImplementation(libs.firebase.analytics) {
        exclude("com.google.android.gms", "play-services-ads-identifier")
    }
    googleplayImplementation(libs.firebase.config.ktx)
    googleplayImplementation(libs.play.services.location)
    googleplayImplementation(libs.play.services.maps)
    googleplayImplementation(libs.play.billing.ktx)
    googleplayImplementation(libs.play.review)
    googleplayImplementation(libs.play.services.oss.licenses)
    googleplayImplementation(libs.horologist.datalayer.phone)
    googleplayImplementation(libs.horologist.datalayer.grpc)
    googleplayImplementation(libs.horologist.datalayer.core)
    googleplayImplementation(libs.play.services.wearable)
    googleplayImplementation(libs.microsoft.authentication) {
        exclude("com.microsoft.device.display", "display-mask")
    }
    googleplayImplementation(projects.wearDatalayer)

    androidTestImplementation(libs.dagger.hilt.testing)
    kspAndroidTest(libs.dagger.hilt.compiler)
    kspAndroidTest(libs.androidx.hilt.compiler)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.make.it.easy)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.okhttp.mockwebserver)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.make.it.easy)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.mockito.core)
    testImplementation(libs.xpp3)
}
