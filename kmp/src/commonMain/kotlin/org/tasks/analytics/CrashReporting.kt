package org.tasks.analytics

interface CrashReporting {
    fun reportException(t: Throwable)
}
