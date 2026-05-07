package org.tasks.extensions

import java.lang.management.ManagementFactory
import java.nio.file.Path
import kotlin.system.exitProcess

actual fun restartApplication() {
    val javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toString()
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
