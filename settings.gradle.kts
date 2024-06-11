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
}

include("app")
include("data")
include(":kmp")
