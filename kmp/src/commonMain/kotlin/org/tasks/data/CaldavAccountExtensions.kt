package org.tasks.data

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.openTaskProvider
import org.tasks.data.entity.OpenTaskProvider
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.caldav
import tasks.kmp.generated.resources.davx5
import tasks.kmp.generated.resources.decsync
import tasks.kmp.generated.resources.etesync
import tasks.kmp.generated.resources.gtasks_GPr_header
import tasks.kmp.generated.resources.ic_davx5_icon_blue_bg
import tasks.kmp.generated.resources.ic_davx5_icon_green_bg
import tasks.kmp.generated.resources.ic_decsync
import tasks.kmp.generated.resources.ic_etesync
import tasks.kmp.generated.resources.ic_google
import tasks.kmp.generated.resources.ic_ksync
import tasks.kmp.generated.resources.ic_microsoft_tasks
import tasks.kmp.generated.resources.ic_outline_cloud_off_24px
import tasks.kmp.generated.resources.ic_round_icon
import tasks.kmp.generated.resources.ic_webdav_logo
import tasks.kmp.generated.resources.ksync
import tasks.kmp.generated.resources.local_lists
import tasks.kmp.generated.resources.microsoft
import tasks.kmp.generated.resources.tasks_org_account

data class AccountIcon(val drawable: DrawableResource, val tinted: Boolean)

data class OpenTaskApp(val name: String, val packageName: String)

val OpenTaskProvider.titleRes: StringResource
    get() = when (this) {
        OpenTaskProvider.DAVX5, OpenTaskProvider.DAVX5_MANAGED -> Res.string.davx5
        OpenTaskProvider.ETESYNC -> Res.string.etesync
        OpenTaskProvider.DECSYNC -> Res.string.decsync
        OpenTaskProvider.KSYNC -> Res.string.ksync
    }

val OpenTaskProvider.iconRes: DrawableResource
    get() = when (this) {
        OpenTaskProvider.DAVX5 -> Res.drawable.ic_davx5_icon_green_bg
        OpenTaskProvider.DAVX5_MANAGED -> Res.drawable.ic_davx5_icon_blue_bg
        OpenTaskProvider.ETESYNC -> Res.drawable.ic_etesync
        OpenTaskProvider.DECSYNC -> Res.drawable.ic_decsync
        OpenTaskProvider.KSYNC -> Res.drawable.ic_ksync
    }

val CaldavAccount.openTaskApp: OpenTaskApp?
    get() = uuid.openTaskProvider()?.let {
        OpenTaskApp(it.displayName, it.packageName)
    }

val CaldavAccount.composeTitle: StringResource?
    get() = when {
        isTasksOrg -> Res.string.tasks_org_account
        isCaldavAccount -> Res.string.caldav
        isEtebaseAccount -> Res.string.etesync
        isMicrosoft -> Res.string.microsoft
        isGoogleTasks -> Res.string.gtasks_GPr_header
        isLocalList -> Res.string.local_lists
        else -> uuid.openTaskProvider()?.titleRes
    }

val CaldavAccount.composeIcon: AccountIcon?
    get() = when {
        isTasksOrg -> AccountIcon(Res.drawable.ic_round_icon, false)
        isCaldavAccount -> AccountIcon(Res.drawable.ic_webdav_logo, true)
        isEtebaseAccount -> AccountIcon(Res.drawable.ic_etesync, false)
        isMicrosoft -> AccountIcon(Res.drawable.ic_microsoft_tasks, false)
        isGoogleTasks -> AccountIcon(Res.drawable.ic_google, false)
        isLocalList -> AccountIcon(Res.drawable.ic_outline_cloud_off_24px, true)
        else -> uuid.openTaskProvider()?.let { AccountIcon(it.iconRes, false) }
    }
