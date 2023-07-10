buildscript {
    repositories {
        mavenCentral()
        google()
    }

    dependencies {
        classpath(libs.gradle)
        classpath(libs.google.services)
        classpath(libs.firebase.crashlytics.gradle)
        classpath(libs.kotlin.gradle)
        classpath(libs.dagger.hilt.gradle)
        classpath(libs.oss.licenses.plugin)
    }
}

tasks.getByName<Wrapper>("wrapper") {
    gradleVersion = "8.2.1"
    distributionType = Wrapper.DistributionType.ALL
}
