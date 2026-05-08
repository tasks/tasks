package org.tasks.extensions

import java.lang.management.ManagementFactory
import java.nio.file.Path
import kotlin.system.exitProcess

actual fun restartApplication() {
    val javaHome = Path.of(System.getProperty("java.home"))

    // Detect macOS .app bundle and relaunch via `open -b` with the bundle
    // identifier to avoid App Translocation issues with direct java exec
    val appBundle = generateSequence(javaHome) { it.parent }
        .firstOrNull { it.toString().endsWith(".app") }
    if (appBundle != null) {
        val bundleId = ProcessBuilder(
            "defaults", "read", "$appBundle/Contents/Info", "CFBundleIdentifier"
        ).start().inputReader().readText().trim()
        if (bundleId.isNotEmpty()) {
            ProcessBuilder("open", "-n", "-b", bundleId)
                .inheritIO()
                .start()
            exitProcess(0)
        }
    }

    val javaBin = Path.of(javaHome.toString(), "bin", "java").toString()
    val classpath = System.getProperty("java.class.path") ?: run {
        exitProcess(0)
    }
    val mainClass = System.getProperty("sun.java.command")
        ?.split(" ")
        ?.firstOrNull()
        ?: run { exitProcess(0) }
    val jvmArgs = ManagementFactory.getRuntimeMXBean().inputArguments

    ProcessBuilder(buildList {
        add(javaBin)
        addAll(jvmArgs)
        add("-cp")
        add(classpath)
        add(mainClass)
    })
        .inheritIO()
        .start()

    exitProcess(0)
}
