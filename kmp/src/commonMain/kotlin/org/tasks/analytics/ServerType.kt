package org.tasks.analytics

import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_MAILBOX_ORG
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_NEXTCLOUD
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_OPEN_XCHANGE
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_OTHER
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_OWNCLOUD
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_SABREDAV
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_SYNOLOGY_CALENDAR
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_TASKS

val CaldavAccount.serverTypeName: String
    get() = when (serverType) {
        SERVER_TASKS -> "tasks"
        SERVER_OWNCLOUD -> "owncloud"
        SERVER_SABREDAV -> "sabredav"
        SERVER_OPEN_XCHANGE -> "open_xchange"
        SERVER_NEXTCLOUD -> "nextcloud"
        SERVER_SYNOLOGY_CALENDAR -> "synology_calendar"
        SERVER_MAILBOX_ORG -> "mailbox_org"
        SERVER_OTHER -> "other"
        else -> "unknown"
    }

val CaldavAccount.etebaseServerName: String
    get() {
        val host = url.orEmpty()
            .substringAfter("://")
            .substringBefore("/")
            .substringBefore(":")
            .lowercase()
        return when {
            host.isHostOrSubdomainOf("etebase.com") ||
                host.isHostOrSubdomainOf("etesync.com") -> "etesync.com"
            host.isHostOrSubdomainOf("silentsuite.io") -> "silentsuite.io"
            else -> "self-hosted"
        }
    }

private fun String.isHostOrSubdomainOf(domain: String) = this == domain || endsWith(".$domain")
