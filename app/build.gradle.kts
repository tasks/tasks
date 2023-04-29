@file:Suppress("UnstableApiUsage")

import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension

plugins {
    id("com.android.application")
    id("checkstyle")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    kotlin("android")
    kotlin("kapt")
    id("dagger.hilt.android.plugin")
    id("com.google.android.gms.oss-licenses-plugin")
    id("kotlin-parcelize")
}

repositories {
    mavenCentral()
    google()
    maven {
        url = uri("https://jitpack.io")
        content {
            includeGroup("com.github.tasks")
            includeModule("com.github.bitfireAT", "cert4android")
            includeModule("com.github.bitfireAT", "dav4jvm")
            includeModule("com.github.tasks.opentasks", "opentasks-provider")
            includeModule("com.github.QuadFlask", "colorpicker")
            includeModule("com.github.twofortyfouram", "android-plugin-api-for-locale")
            includeModule("com.github.franmontiel", "PersistentCookieJar")
        }
    }
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

    compileSdk = 33

    defaultConfig {
        testApplicationId = "org.tasks.test"
        applicationId = "org.tasks"
        versionCode = 130200
        versionName = "13.2"
        targetSdk = 33
        minSdk = 24
        testInstrumentationRunner = "org.tasks.TestRunner"

        kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
                arg("room.incremental", "true")
            }
        }
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

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    kotlinOptions {
        jvmTarget = "17"
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
            dimension = "store"
        }
    }
    packagingOptions {
        resources {
            excludes += setOf("META-INF/*.kotlin_module")
        }
    }

    namespace = "org.tasks"
}

configure<CheckstyleExtension> {
    configFile = project.file("google_checks.xml")
    toolVersion = "8.16"
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
    kapt(libs.dagger.hilt.compiler)
    kapt(libs.androidx.hilt.compiler)
    implementation(libs.androidx.hilt.work)

    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.room)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.appcompat)
    implementation(libs.bundles.markwon)

    debugImplementation(libs.bundles.flipper)
    debugImplementation(libs.leakcanary)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation(libs.kotlin.reflect)

    implementation(libs.kotlin.jdk8)
    implementation(libs.okhttp)
    implementation(libs.persistent.cookiejar)
    implementation(libs.gson)
    implementation(libs.material)
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
    implementation(libs.bundles.retrofit)
    implementation(libs.androidx.recyclerview)

    implementation(platform(libs.androidx.compose))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation(libs.androidx.activity.compose)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("androidx.compose.ui:ui-viewbinding")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation(libs.bundles.coil)

    implementation(libs.accompanist.flowlayout)
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.themeadapter)

    googleplayImplementation(platform(libs.firebase))
    googleplayImplementation("com.google.firebase:firebase-crashlytics")
    googleplayImplementation("com.google.firebase:firebase-analytics") {
        exclude("com.google.android.gms", "play-services-ads-identifier")
    }
    googleplayImplementation("com.google.firebase:firebase-config-ktx")
    googleplayImplementation(libs.play.services.location)
    googleplayImplementation(libs.play.services.maps)
    googleplayImplementation(libs.play.billing.ktx)
    googleplayImplementation(libs.play.core)
    googleplayImplementation(libs.play.core.ktx)
    googleplayImplementation(libs.play.services.oss.licenses)

    androidTestImplementation(libs.dagger.hilt.testing)
    kaptAndroidTest(libs.dagger.hilt.compiler)
    kaptAndroidTest(libs.androidx.hilt.compiler)
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
