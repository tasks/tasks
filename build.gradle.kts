buildscript {
    repositories {
        jcenter()
        google()
        maven("https://maven.fabric.io/public")
    }

    dependencies {
        classpath("com.android.tools.build:gradle:3.5.0")
        classpath("com.google.gms:google-services:4.3.0")
        // https://docs.fabric.io/android/changelog.html#fabric-gradle-plugin
        classpath("io.fabric.tools:gradle:1.29.0")
        classpath("com.github.ben-manes:gradle-versions-plugin:0.21.0")
        classpath("com.cookpad.android.licensetools:license-tools-plugin:1.7.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
    }
}

plugins {
    id("com.github.ben-manes.versions") version "0.21.0"
}

tasks.getByName<Wrapper>("wrapper") {
    gradleVersion = "5.4.1"
    distributionType = Wrapper.DistributionType.ALL
}
