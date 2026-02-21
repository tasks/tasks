package org.tasks.data

import org.jetbrains.compose.resources.DrawableResource
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.PACKAGE_DAVX5
import org.tasks.data.entity.CaldavAccount.Companion.PACKAGE_DAVX5_MANAGED
import org.tasks.data.entity.CaldavAccount.Companion.PACKAGE_DECSYNC
import org.tasks.data.entity.CaldavAccount.Companion.PACKAGE_ETESYNC
import org.tasks.data.entity.CaldavAccount.Companion.isDecSync
import org.tasks.data.entity.CaldavAccount.Companion.isDavx5
import org.tasks.data.entity.CaldavAccount.Companion.isDavx5Managed
import org.tasks.data.entity.CaldavAccount.Companion.isEteSync
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.ic_davx5_icon_blue_bg
import tasks.kmp.generated.resources.ic_davx5_icon_green_bg
import tasks.kmp.generated.resources.ic_decsync
import tasks.kmp.generated.resources.ic_etesync
import tasks.kmp.generated.resources.ic_google
import tasks.kmp.generated.resources.ic_microsoft_tasks
import tasks.kmp.generated.resources.ic_outline_cloud_off_24px
import tasks.kmp.generated.resources.ic_round_icon
import tasks.kmp.generated.resources.ic_webdav_logo

data class AccountIcon(val drawable: DrawableResource, val tinted: Boolean)

data class OpenTaskApp(val name: String, val packageName: String)

val CaldavAccount.openTaskApp: OpenTaskApp?
    get() = when {
        uuid.isDavx5() -> OpenTaskApp("DAVx\u2075", PACKAGE_DAVX5)
        uuid.isDavx5Managed() -> OpenTaskApp("DAVx\u2075", PACKAGE_DAVX5_MANAGED)
        uuid.isEteSync() -> OpenTaskApp("EteSync", PACKAGE_ETESYNC)
        uuid.isDecSync() -> OpenTaskApp("DecSync CC", PACKAGE_DECSYNC)
        else -> null
    }

val CaldavAccount.composeIcon: AccountIcon?
    get() = when {
        isTasksOrg -> AccountIcon(Res.drawable.ic_round_icon, false)
        isCaldavAccount -> AccountIcon(Res.drawable.ic_webdav_logo, true)
        isEtebaseAccount || uuid.isEteSync() -> AccountIcon(Res.drawable.ic_etesync, false)
        uuid.isDavx5() -> AccountIcon(Res.drawable.ic_davx5_icon_green_bg, false)
        uuid.isDavx5Managed() -> AccountIcon(Res.drawable.ic_davx5_icon_blue_bg, false)
        uuid.isDecSync() -> AccountIcon(Res.drawable.ic_decsync, false)
        isMicrosoft -> AccountIcon(Res.drawable.ic_microsoft_tasks, false)
        isGoogleTasks -> AccountIcon(Res.drawable.ic_google, false)
        isLocalList -> AccountIcon(Res.drawable.ic_outline_cloud_off_24px, true)
        else -> null
    }
