package org.tasks.service

interface TaskCleanup {
    suspend fun cleanup(tasks: List<Long>) {}
    suspend fun onMarkedDeleted() {}
}
