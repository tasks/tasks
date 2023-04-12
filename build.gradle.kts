buildscript {
    repositories {
        mavenCentral()
        google()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:7.4.2")
        classpath("com.google.gms:google-services:4.3.15")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.4")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
        classpath("com.google.dagger:hilt-android-gradle-plugin:${libs.versions.dagger.hilt.get()}")
        classpath("com.google.android.gms:oss-licenses-plugin:0.10.6")
    }
}

tasks.getByName<Wrapper>("wrapper") {
    gradleVersion = "8.1"
    distributionType = Wrapper.DistributionType.ALL
}
