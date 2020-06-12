plugins {
    id("com.android.application")
    id("checkstyle")
    id("com.google.firebase.crashlytics")
    kotlin("android")
    kotlin("kapt")
    id("com.cookpad.android.plugin.license-tools") version "1.2.2"
    id("com.github.ben-manes.versions") version "0.28.0"
    id("com.vanniktech.android.junit.jacoco") version "0.16.0"
}

repositories {
    jcenter()
    google()
    maven(url = "https://jitpack.io")
}

android {
    val commonTest = "src/commonTest/java"
    sourceSets["test"].java.srcDir(commonTest)
    sourceSets["androidTest"].java.srcDirs("src/androidTest/java", commonTest)

    bundle {
        language {
            enableSplit = false
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    lintOptions {
        disable("InvalidPeriodicWorkRequestInterval")
        lintConfig = file("lint.xml")
        textOutput("stdout")
        textReport = true
    }

    compileSdkVersion(Versions.targetSdk)

    defaultConfig {
        testApplicationId = "org.tasks.test"
        applicationId = "org.tasks"
        versionCode = 90700
        versionName = "9.7"
        targetSdkVersion(Versions.targetSdk)
        minSdkVersion(Versions.minSdk)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
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
        coreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    @Suppress("LocalVariableName")
    buildTypes {
        getByName("debug") {
            val tasks_mapbox_key_debug: String? by project
            val tasks_google_key_debug: String? by project

            applicationIdSuffix = ".debug"
            resValue("string", "mapbox_key", tasks_mapbox_key_debug ?: "")
            resValue("string", "google_key", tasks_google_key_debug ?: "")
            isTestCoverageEnabled = project.hasProperty("coverage")
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

    flavorDimensions("store")

    productFlavors {
        create("generic") {
            setDimension("store")
        }
        create("googleplay") {
            setDimension("store")
        }
    }

    packagingOptions {
        exclude("META-INF/*.kotlin_module")
    }
}

configure<CheckstyleExtension> {
    configFile = project.file("google_checks.xml")
    toolVersion = "8.16"
}

configurations.all {
    exclude(group = "com.google.guava", module = "guava-jdk5")
    exclude(group = "org.apache.httpcomponents", module = "httpclient")
    exclude(group = "com.google.http-client", module = "google-http-client-apache")
    exclude(group = "org.checkerframework")
    exclude(group = "com.google.code.findbugs")
    exclude(group = "com.google.errorprone")
    exclude(group = "com.google.j2objc")
}

val googleplayImplementation by configurations

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.0.5")
    implementation("com.gitlab.bitfireAT:dav4jvm:2.1")
    implementation("com.gitlab.bitfireAT:ical4android:a675c7194b")
    implementation("com.gitlab.bitfireAT:cert4android:1488e39a66")

    kapt("com.google.dagger:dagger-compiler:${Versions.dagger}")
    implementation("com.google.dagger:dagger:${Versions.dagger}")

    implementation("androidx.room:room-rxjava2:${Versions.room}")
    kapt("androidx.room:room-compiler:${Versions.room}")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("androidx.paging:paging-runtime:2.1.2")

    kapt("com.jakewharton:butterknife-compiler:${Versions.butterknife}")
    implementation("com.jakewharton:butterknife:${Versions.butterknife}")

    debugImplementation("com.facebook.flipper:flipper:${Versions.flipper}")
    debugImplementation("com.facebook.flipper:flipper-network-plugin:${Versions.flipper}")
    debugImplementation("com.facebook.soloader:soloader:0.9.0")

    debugImplementation("com.squareup.leakcanary:leakcanary-android:${Versions.leakcanary}")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.2")
    implementation("com.squareup.okhttp3:okhttp:${Versions.okhttp}")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("com.google.android.material:material:1.1.0")
    implementation("androidx.annotation:annotation:1.1.0")
    implementation("androidx.constraintlayout:constraintlayout:2.0.0-beta4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.0.0")
    implementation("androidx.preference:preference:1.1.1")
    implementation("com.jakewharton.timber:timber:4.7.1")
    implementation("com.google.android.apps.dashclock:dashclock-api:2.0.0")
    implementation("com.twofortyfouram:android-plugin-api-for-locale:1.0.2") {
        isTransitive = false
    }
    implementation("com.rubiconproject.oss:jchronic:0.2.6") {
        isTransitive = false
    }
    implementation("org.scala-saddle:google-rfc-2445:20110304") {
        isTransitive = false
    }
    implementation("com.wdullaer:materialdatetimepicker:4.2.3")
    implementation("me.leolin:ShortcutBadger:1.1.22@aar")
    implementation("com.google.apis:google-api-services-tasks:v1-rev20200129-1.30.9")
    implementation("com.google.apis:google-api-services-drive:v3-rev20200413-1.30.9")
    implementation("com.google.auth:google-auth-library-oauth2-http:0.20.0")
    implementation("androidx.work:work-runtime:${Versions.work}")
    implementation("com.mapbox.mapboxsdk:mapbox-android-sdk:9.2.1")
    implementation("com.mapbox.mapboxsdk:mapbox-sdk-services:5.2.1")
    implementation("com.etesync:journalmanager:1.1.0")
    implementation("com.github.QuadFlask:colorpicker:0.0.15")

    googleplayImplementation("com.crashlytics.sdk.android:crashlytics:${Versions.crashlytics}")
    googleplayImplementation("com.google.firebase:firebase-analytics:${Versions.analytics}")
    googleplayImplementation("com.google.firebase:firebase-config-ktx:${Versions.remote_config}")
    googleplayImplementation("com.google.android.gms:play-services-location:17.0.0")
    googleplayImplementation("com.google.android.gms:play-services-maps:17.0.0")
    googleplayImplementation("com.google.android.libraries.places:places:2.2.0")
    googleplayImplementation("com.android.billingclient:billing:1.2.2")

    kaptAndroidTest("com.google.dagger:dagger-compiler:${Versions.dagger}")
    kaptAndroidTest("com.jakewharton:butterknife-compiler:${Versions.butterknife}")
    androidTestImplementation("org.mockito:mockito-android:${Versions.mockito}")
    androidTestImplementation("com.natpryce:make-it-easy:${Versions.make_it_easy}")
    androidTestImplementation("androidx.test:runner:${Versions.androidx_test}")
    androidTestImplementation("androidx.test:rules:${Versions.androidx_test}")
    androidTestImplementation("androidx.test.ext:junit:1.1.1")
    androidTestImplementation("androidx.annotation:annotation:1.1.0")

    testImplementation("junit:junit:4.13")
    testImplementation("com.natpryce:make-it-easy:${Versions.make_it_easy}")
    testImplementation("androidx.test:core:${Versions.androidx_test}")
    testImplementation("org.mockito:mockito-core:${Versions.mockito}")
}

apply(mapOf("plugin" to "com.google.gms.google-services"))
