package org.tasks.caldav

interface CaldavDiscoveryClient : AutoCloseable {
    suspend fun homeSet(): String
}
