package org.tasks.opentasks

interface OpenTasksSyncer {
    suspend fun sync(hasPro: Boolean)
}
