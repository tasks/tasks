buildscript {
    repositories {
        mavenCentral()
        google()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:7.2.1")
        classpath("com.google.gms:google-services:4.3.10")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
        classpath("com.google.dagger:hilt-android-gradle-plugin:${Versions.hilt}")
        classpath("com.google.android.gms:oss-licenses-plugin:0.10.5")
    }
}

tasks.getByName<Wrapper>("wrapper") {
    gradleVersion = "7.0-rc-1"
    distributionType = Wrapper.DistributionType.ALL
}
