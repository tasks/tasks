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
    }
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
                includeModule("com.github.bitfireAT", "cert4android")
                includeModule("com.github.bitfireAT", "dav4jvm")
                includeModule("com.github.franmontiel", "PersistentCookieJar")
                includeModule("com.github.QuadFlask", "colorpicker")
                includeModule("com.github.tasks", "ical4android")
                includeModule("com.github.tasks.opentasks", "opentasks-provider")
            }
        }
    }
}

include("app")
include("data")
include(":kmp")
include(":icons")
include(":wear")
include(":wear-datalayer")
