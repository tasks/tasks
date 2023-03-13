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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
    flavorDimensions += listOf("store")

    @Suppress("LocalVariableName")
    buildTypes {
        getByName("debug") {
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
        getByName("release") {
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
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.2")
    implementation("com.github.bitfireAT:dav4jvm:2.2") {
        exclude(group = "junit")
    }
    implementation("com.github.tasks:ical4android:27dc5bf") {
        exclude(group = "commons-logging")
        exclude(group = "org.json", module = "json")
        exclude(group = "org.codehaus.groovy", module = "groovy")
        exclude(group = "org.codehaus.groovy", module = "groovy-dateutil")
    }
    implementation("com.github.bitfireAT:cert4android:7814052")
    implementation("com.github.tasks.opentasks:opentasks-provider:562fec5") {
        exclude("com.github.tasks.opentasks", "opentasks-contract")
    }
    implementation("org.dmfs:rfc5545-datetime:0.2.4")
    implementation("org.dmfs:lib-recur:0.11.4")
    implementation("org.dmfs:jems:1.33")

    implementation(libs.dagger.hilt)
    kapt(libs.dagger.hilt.compiler)
    kapt(libs.androidx.hilt.compiler)
    implementation(libs.androidx.hilt.work)

    implementation("androidx.fragment:fragment-ktx:1.5.5")
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.room)
    kapt(libs.androidx.room.compiler)
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.paging:paging-runtime:3.1.1")
    implementation(libs.bundles.markwon)

    debugImplementation(libs.bundles.flipper)
    debugImplementation(libs.leakcanary)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation(libs.kotlin.reflect)

    implementation(libs.kotlin.jdk8)
    implementation(libs.okhttp)
    implementation("com.github.franmontiel:PersistentCookieJar:v1.0.1")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.preference:preference:1.2.0")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("com.google.android.apps.dashclock:dashclock-api:2.0.0")
    implementation("com.github.twofortyfouram:android-plugin-api-for-locale:1.0.2") {
        isTransitive = false
    }
    implementation("com.rubiconproject.oss:jchronic:0.2.6") {
        isTransitive = false
    }
    implementation("me.leolin:ShortcutBadger:1.1.22@aar")
    implementation("com.google.apis:google-api-services-tasks:v1-rev20210709-1.32.1")
    implementation("com.google.apis:google-api-services-drive:v3-rev20210725-1.32.1")
    implementation("com.google.auth:google-auth-library-oauth2-http:0.26.0")
    implementation(libs.androidx.work)
    implementation("com.etebase:client:2.3.2")
    implementation("com.github.QuadFlask:colorpicker:0.0.15")
    implementation("net.openid:appauth:0.11.1")
    implementation("org.osmdroid:osmdroid-android:6.1.11@aar")
    implementation(libs.bundles.retrofit)
    implementation("androidx.recyclerview:recyclerview:1.3.0-rc01")

    implementation(platform(libs.androidx.compose))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation(libs.compose.theme.adapter)
    implementation("androidx.activity:activity-compose:1.6.1")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.5.1")
    implementation("androidx.compose.ui:ui-viewbinding")
    implementation(libs.bundles.coil)
    releaseCompileOnly("androidx.compose.ui:ui-tooling")

    implementation(libs.accompanist.flowlayout)
    implementation(libs.accompanist.permissions)

    googleplayImplementation(libs.firebase.crashlytics)
    googleplayImplementation(libs.firebase.analytics) {
        exclude("com.google.android.gms", "play-services-ads-identifier")
    }
    googleplayImplementation(libs.firebase.config)
    googleplayImplementation("com.google.android.gms:play-services-location:19.0.1")
    googleplayImplementation("com.google.android.gms:play-services-maps:18.1.0")
    googleplayImplementation("com.android.billingclient:billing-ktx:4.0.0")
    googleplayImplementation("com.google.android.play:core:1.10.3")
    googleplayImplementation("com.google.android.play:core-ktx:1.8.1")
    googleplayImplementation("com.google.android.gms:play-services-oss-licenses:17.0.0")

    androidTestImplementation(libs.dagger.hilt.testing)
    kaptAndroidTest(libs.dagger.hilt.compiler)
    kaptAndroidTest(libs.androidx.hilt.compiler)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.make.it.easy)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation("androidx.test.ext:junit:1.1.4")
    androidTestImplementation(libs.okhttp.mockwebserver)

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    testImplementation(libs.make.it.easy)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.mockito.core)
    testImplementation("org.ogce:xpp3:1.1.6")
}
