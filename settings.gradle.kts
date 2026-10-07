rootProject.name = "Tasks"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.hq.hydraulic.software")
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

val version = java.util.Properties().apply {
    providers.fileContents(layout.rootDirectory.file("version.properties")).asText.get().reader().use(::load)
}
val appVersionCode = version.getProperty("VERSION_CODE").toInt()
val appVersionName: String = version.getProperty("VERSION_NAME")
require(appVersionCode % 2 == 0) { "VERSION_CODE must be even, wear uses VERSION_CODE + 1" }
gradle.beforeProject {
    extra["appVersionCode"] = appVersionCode
    extra["appVersionName"] = appVersionName
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
            content {
                includeModule("com.github.franmontiel", "PersistentCookieJar")
                includeModule("com.github.jheld", "colorpicker")
                includeModule("com.github.tasks", "ical4android")
                includeModule("com.github.tasks.opentasks", "opentasks-provider")
            }
        }
    }
}

include("app")
include("data")
include(":kmp")
include(":wear")
include(":wear-datalayer")
include(":cert4android")
include(":composeApp")
include(":baselineprofile")
