import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.protobuf) apply false
    alias(libs.plugins.redacted) apply false
}

buildscript {
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
    gradleVersion = "8.14.2"
    distributionType = Wrapper.DistributionType.ALL
}

allprojects {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            val composeReports = project.properties["composeMetrics"] ?: project.buildDir.absolutePath
            freeCompilerArgs.addAll(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" + composeReports + "/compose-metrics",
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" + composeReports + "/compose-metrics",
            )
        }
    }
}
