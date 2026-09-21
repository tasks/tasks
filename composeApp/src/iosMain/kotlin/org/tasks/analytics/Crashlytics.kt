package org.tasks.analytics

internal var crashlyticsInstalled = false
    private set

fun installCrashlytics() {
    crashlyticsInstalled = true
}
