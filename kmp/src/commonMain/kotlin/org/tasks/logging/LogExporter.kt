package org.tasks.logging

interface LogExporter {
    suspend fun export()
}
