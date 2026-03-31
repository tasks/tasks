@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    applyDefaultHierarchyTemplate()
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.addAll("-P", "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=org.tasks.CommonParcelize")
        }
    }
    jvm()
    sourceSets {
        val jvmCommonMain by creating {
            dependsOn(commonMain.get())
        }
        androidMain {
            dependsOn(jvmCommonMain)
            dependencies {
                implementation(libs.androidx.ui.tooling.preview.android)
                implementation(libs.bitfire.cert4android)
                implementation(libs.bitfire.ical4android.get().toString()) {
                    exclude(group = "commons-logging")
                    exclude(group = "org.json", module = "json")
                    exclude(group = "org.codehaus.groovy", module = "groovy")
                    exclude(group = "org.codehaus.groovy", module = "groovy-dateutil")
                }
                implementation(libs.persistent.cookiejar)
                implementation(libs.pebblekit)
            }
        }
        jvmMain {
            dependsOn(jvmCommonMain)
            dependencies {
                implementation(libs.xpp3)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.junit)
            }
        }
        jvmCommonMain.dependencies {
            api(libs.ical4j)
            api(libs.bitfire.dav4jvm.get().toString()) {
                exclude(group = "junit")
                exclude(group = "org.ogce", module = "xpp3")
            }
        }
        commonMain.dependencies {
            implementation(projects.data)
            api(compose.components.resources)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.runtime)
            implementation(libs.androidx.datastore)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.kermit)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.immutable)
            implementation(libs.kotlinx.serialization)
            implementation(libs.material.kolor)
        }
    }
    tasks.register("testClasses")
}

val generateJvmBuildConfig by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/jvmBuildConfig")
    val versionCode = libs.versions.versionCode.get()
    val tasks_dev_url: String? by project
    val devUrl = tasks_dev_url ?: ""
    inputs.property("versionCode", versionCode)
    inputs.property("devUrl", devUrl)
    outputs.dir(outputDir)
    doLast {
        outputDir.get().asFile.resolve("JvmBuildConfig.kt").apply {
            parentFile.mkdirs()
            writeText("""
                |package org.tasks.kmp
                |
                |object JvmBuildConfig {
                |    const val VERSION_CODE = $versionCode
                |    const val DEV_URL = "$devUrl"
                |}
            """.trimMargin())
        }
    }
}

kotlin.sourceSets.named("jvmMain") {
    kotlin.srcDir(generateJvmBuildConfig)
}

compose.resources {
    publicResClass = true
    generateResClass = always
}

android {
    namespace = "org.tasks.kmp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        buildConfigField("int", "VERSION_CODE", libs.versions.versionCode.get())
        val tasks_dev_url: String? by project
        buildConfigField("String", "DEV_URL", "\"${tasks_dev_url ?: ""}\"")
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    dependencies {
        coreLibraryDesugaring(libs.desugar.jdk.libs)
        debugImplementation(compose.uiTooling)
    }
}
