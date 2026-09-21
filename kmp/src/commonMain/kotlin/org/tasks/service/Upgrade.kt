package org.tasks.service

fun interface Upgrade {
    suspend fun run()
}
