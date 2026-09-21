package org.tasks.caldav.metadata

import org.tasks.data.entity.CaldavAccount

interface TagMetadataActivation {
    suspend fun probeViability(url: String, username: String, password: String): Boolean
    suspend fun enablePrimary(account: CaldavAccount, skipProbe: Boolean = false): Boolean
    suspend fun disable()
}
