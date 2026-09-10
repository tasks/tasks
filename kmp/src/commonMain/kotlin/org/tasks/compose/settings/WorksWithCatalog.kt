package org.tasks.compose.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Api
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.tasks.AppStore
import org.tasks.PlatformConfiguration
import org.tasks.TasksUrls
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.acalendar
import tasks.kmp.generated.resources.android
import tasks.kmp.generated.resources.apple_reminders
import tasks.kmp.generated.resources.caldav_server_nextcloud
import tasks.kmp.generated.resources.davx5
import tasks.kmp.generated.resources.davx5_selection_description
import tasks.kmp.generated.resources.decsync
import tasks.kmp.generated.resources.decsync_selection_description
import tasks.kmp.generated.resources.desktop
import tasks.kmp.generated.resources.etesync
import tasks.kmp.generated.resources.etesync_selection_description
import tasks.kmp.generated.resources.google_tasks_selection_description
import tasks.kmp.generated.resources.gtasks_GPr_header
import tasks.kmp.generated.resources.ios
import tasks.kmp.generated.resources.ic_davx5_icon_green_bg
import tasks.kmp.generated.resources.ic_decsync
import tasks.kmp.generated.resources.ic_etesync
import tasks.kmp.generated.resources.ic_ksync
import tasks.kmp.generated.resources.ic_google
import tasks.kmp.generated.resources.ic_microsoft_tasks
import tasks.kmp.generated.resources.ic_round_icon
import tasks.kmp.generated.resources.kvaesitso
import tasks.kmp.generated.resources.ksync
import tasks.kmp.generated.resources.mcp_server
import tasks.kmp.generated.resources.microsoft
import tasks.kmp.generated.resources.microsoft_selection_description
import tasks.kmp.generated.resources.microsoft_selection_description_googleplay
import tasks.kmp.generated.resources.pebble
import tasks.kmp.generated.resources.radicale
import tasks.kmp.generated.resources.tasker
import tasks.kmp.generated.resources.tasks_org_account
import tasks.kmp.generated.resources.thunderbird
import tasks.kmp.generated.resources.todoagenda
import tasks.kmp.generated.resources.wear_os
import tasks.kmp.generated.resources.works_with_acalendar_description
import tasks.kmp.generated.resources.works_with_android
import tasks.kmp.generated.resources.works_with_android_description
import tasks.kmp.generated.resources.works_with_apple_reminders_description
import tasks.kmp.generated.resources.works_with_automation
import tasks.kmp.generated.resources.works_with_calendars
import tasks.kmp.generated.resources.works_with_content_provider
import tasks.kmp.generated.resources.works_with_content_provider_description
import tasks.kmp.generated.resources.works_with_coming_soon
import tasks.kmp.generated.resources.works_with_cloud_description
import tasks.kmp.generated.resources.works_with_desktop
import tasks.kmp.generated.resources.works_with_desktop_apps
import tasks.kmp.generated.resources.works_with_desktop_description
import tasks.kmp.generated.resources.works_with_developers
import tasks.kmp.generated.resources.works_with_ios
import tasks.kmp.generated.resources.works_with_kvaesitso_description
import tasks.kmp.generated.resources.works_with_mobile
import tasks.kmp.generated.resources.works_with_ksync_description
import tasks.kmp.generated.resources.works_with_launchers
import tasks.kmp.generated.resources.works_with_mcp_description
import tasks.kmp.generated.resources.works_with_nextcloud_description
import tasks.kmp.generated.resources.works_with_pebble_description
import tasks.kmp.generated.resources.works_with_radicale_description
import tasks.kmp.generated.resources.works_with_tag_official
import tasks.kmp.generated.resources.works_with_tag_sync
import tasks.kmp.generated.resources.works_with_servers
import tasks.kmp.generated.resources.works_with_services
import tasks.kmp.generated.resources.works_with_sync
import tasks.kmp.generated.resources.works_with_tasker_description
import tasks.kmp.generated.resources.works_with_todoagenda_description
import tasks.kmp.generated.resources.works_with_wear_requires_googleplay
import tasks.kmp.generated.resources.works_with_wearables
import tasks.kmp.generated.resources.works_with_thunderbird_description
import tasks.kmp.generated.resources.works_with_wear_description

data class WorksWithLinks(
    val web: String,
    val play: String? = null,
    val fdroid: String? = null,
) {
    fun url(store: AppStore): String = when (store) {
        AppStore.GOOGLE_PLAY -> play?.let { "market://details?id=$it" }
        AppStore.FDROID -> fdroid?.let { "https://f-droid.org/packages/$it/" }
        AppStore.NONE -> null
    } ?: web
}

enum class WorksWithTag(val label: StringResource) {
    OFFICIAL(Res.string.works_with_tag_official),
    ANDROID(Res.string.android),
    IOS(Res.string.ios),
    DESKTOP(Res.string.desktop),
    SYNC(Res.string.works_with_tag_sync),
}

data class WorksWithEntry(
    val title: StringResource,
    val description: StringResource,
    val libreDescription: StringResource? = null,
    val links: WorksWithLinks? = null,
    val icon: ImageVector? = null,
    val iconDrawable: DrawableResource? = null,
    val tintIcon: Boolean = true,
    val tags: Set<WorksWithTag> = setOf(WorksWithTag.ANDROID),
    val available: (PlatformConfiguration) -> Boolean = { true },
    val requiresStore: AppStore? = null,
    val requiresStoreDescription: StringResource? = null,
    val showPricingIfUnsubscribed: Boolean = false,
    val openMcpSettingsOnDesktop: Boolean = false,
)


data class WorksWithGroup(
    val title: StringResource,
    val entries: List<WorksWithEntry>,
    val available: (PlatformConfiguration) -> Boolean = { true },
)

val worksWithCatalog = listOf(
    WorksWithGroup(
        title = Res.string.works_with_mobile,
        entries = listOf(
            WorksWithEntry(
                title = Res.string.works_with_android,
                description = Res.string.works_with_android_description,
                iconDrawable = Res.drawable.ic_round_icon,
                tintIcon = false,
                tags = setOf(WorksWithTag.ANDROID, WorksWithTag.OFFICIAL),
                available = { !it.isAndroid },
                links = WorksWithLinks(web = TasksUrls.DOWNLOAD),
            ),
            WorksWithEntry(
                title = Res.string.works_with_ios,
                description = Res.string.works_with_coming_soon,
                iconDrawable = Res.drawable.ic_round_icon,
                tintIcon = false,
                tags = setOf(WorksWithTag.IOS, WorksWithTag.OFFICIAL),
            ),
            WorksWithEntry(
                title = Res.string.apple_reminders,
                description = Res.string.works_with_apple_reminders_description,
                icon = Icons.Outlined.Checklist,
                tags = setOf(WorksWithTag.IOS),
                links = WorksWithLinks(
                    web = "https://support.apple.com/guide/reminders/welcome/mac",
                ),
            ),
        ),
    ),
    WorksWithGroup(
        title = Res.string.works_with_desktop_apps,
        available = { it.isAndroid },
        entries = listOf(
            WorksWithEntry(
                title = Res.string.works_with_desktop,
                description = Res.string.works_with_desktop_description,
                icon = Icons.Outlined.Laptop,
                tags = setOf(WorksWithTag.DESKTOP, WorksWithTag.OFFICIAL),
                available = { it.isAndroid },
                links = WorksWithLinks(web = TasksUrls.DOWNLOAD),
            ),
            WorksWithEntry(
                title = Res.string.thunderbird,
                description = Res.string.works_with_thunderbird_description,
                icon = Icons.Outlined.Email,
                tags = setOf(WorksWithTag.DESKTOP),
                links = WorksWithLinks(web = "https://www.thunderbird.net/"),
            ),
            WorksWithEntry(
                title = Res.string.apple_reminders,
                description = Res.string.works_with_apple_reminders_description,
                icon = Icons.Outlined.Checklist,
                tags = setOf(WorksWithTag.DESKTOP),
                links = WorksWithLinks(
                    web = "https://support.apple.com/guide/reminders/welcome/mac",
                ),
            ),
        ),
    ),
    WorksWithGroup(
        title = Res.string.works_with_wearables,
        entries = listOf(
            WorksWithEntry(
                title = Res.string.wear_os,
                description = Res.string.works_with_wear_description,
                icon = Icons.Outlined.Watch,
                tags = setOf(WorksWithTag.ANDROID, WorksWithTag.OFFICIAL),
                requiresStore = AppStore.GOOGLE_PLAY,
                requiresStoreDescription = Res.string.works_with_wear_requires_googleplay,
                links = WorksWithLinks(
                    web = TasksUrls.GOOGLE_PLAY_LISTING,
                    play = "org.tasks",
                ),
            ),
            WorksWithEntry(
                title = Res.string.pebble,
                description = Res.string.works_with_pebble_description,
                icon = Icons.Outlined.DevicesOther,
                tags = setOf(WorksWithTag.ANDROID, WorksWithTag.OFFICIAL),
                links = WorksWithLinks(web = TasksUrls.PEBBLE_LISTING),
            ),
        ),
    ),
    WorksWithGroup(
        title = Res.string.works_with_services,
        entries = listOf(
            WorksWithEntry(
                title = Res.string.tasks_org_account,
                description = Res.string.works_with_cloud_description,
                iconDrawable = Res.drawable.ic_round_icon,
                tintIcon = false,
                tags = setOf(WorksWithTag.SYNC, WorksWithTag.OFFICIAL),
                showPricingIfUnsubscribed = true,
                links = WorksWithLinks(web = TasksUrls.SYNC),
            ),
            WorksWithEntry(
                title = Res.string.gtasks_GPr_header,
                description = Res.string.google_tasks_selection_description,
                iconDrawable = Res.drawable.ic_google,
                tintIcon = false,
                tags = setOf(WorksWithTag.SYNC),
                available = { it.supportsGoogleTasks },
                links = WorksWithLinks(web = TasksUrls.GOOGLE_TASKS),
            ),
            WorksWithEntry(
                title = Res.string.microsoft,
                description = Res.string.microsoft_selection_description_googleplay,
                libreDescription = Res.string.microsoft_selection_description,
                iconDrawable = Res.drawable.ic_microsoft_tasks,
                tintIcon = false,
                tags = setOf(WorksWithTag.SYNC),
                available = { it.supportsMicrosoft },
                links = WorksWithLinks(web = TasksUrls.MICROSOFT),
            ),
        ),
    ),
    WorksWithGroup(
        title = Res.string.works_with_sync,
        entries = listOf(
            WorksWithEntry(
                title = Res.string.davx5,
                description = Res.string.davx5_selection_description,
                iconDrawable = Res.drawable.ic_davx5_icon_green_bg,
                tintIcon = false,
                tags = setOf(WorksWithTag.ANDROID, WorksWithTag.SYNC),
                links = WorksWithLinks(
                    web = TasksUrls.DAVX5,
                    play = "at.bitfire.davdroid",
                    fdroid = "at.bitfire.davdroid",
                ),
            ),
            WorksWithEntry(
                title = Res.string.decsync,
                description = Res.string.decsync_selection_description,
                iconDrawable = Res.drawable.ic_decsync,
                tintIcon = false,
                tags = setOf(WorksWithTag.ANDROID, WorksWithTag.SYNC),
                links = WorksWithLinks(
                    web = TasksUrls.DECSYNC,
                    fdroid = "org.decsync.cc",
                ),
            ),
            WorksWithEntry(
                title = Res.string.etesync,
                description = Res.string.etesync_selection_description,
                iconDrawable = Res.drawable.ic_etesync,
                tintIcon = false,
                tags = setOf(WorksWithTag.ANDROID, WorksWithTag.SYNC),
                links = WorksWithLinks(
                    web = TasksUrls.ETESYNC,
                    play = "com.etesync.syncadapter",
                    fdroid = "com.etesync.syncadapter",
                ),
            ),
            WorksWithEntry(
                title = Res.string.ksync,
                description = Res.string.works_with_ksync_description,
                iconDrawable = Res.drawable.ic_ksync,
                tintIcon = false,
                tags = setOf(WorksWithTag.ANDROID, WorksWithTag.SYNC),
                links = WorksWithLinks(
                    web = "https://play.google.com/store/apps/details?id=com.infomaniak.sync",
                    play = "com.infomaniak.sync",
                ),
            ),
        ),
    ),
    WorksWithGroup(
        title = Res.string.works_with_servers,
        entries = listOf(
            WorksWithEntry(
                title = Res.string.caldav_server_nextcloud,
                description = Res.string.works_with_nextcloud_description,
                icon = Icons.Outlined.Cloud,
                tags = setOf(WorksWithTag.SYNC),
                links = WorksWithLinks(web = "https://nextcloud.com/"),
            ),
            WorksWithEntry(
                title = Res.string.radicale,
                description = Res.string.works_with_radicale_description,
                icon = Icons.Outlined.Dns,
                tags = setOf(WorksWithTag.SYNC),
                links = WorksWithLinks(web = "https://radicale.org/"),
            ),
        ),
    ),
    WorksWithGroup(
        title = Res.string.works_with_launchers,
        entries = listOf(
            WorksWithEntry(
                title = Res.string.kvaesitso,
                description = Res.string.works_with_kvaesitso_description,
                icon = Icons.Outlined.Apps,
                tags = setOf(WorksWithTag.ANDROID),
                links = WorksWithLinks(web = "https://kvaesitso.mm20.de/"),
            ),
        ),
    ),
    WorksWithGroup(
        title = Res.string.works_with_calendars,
        entries = listOf(
            WorksWithEntry(
                title = Res.string.acalendar,
                description = Res.string.works_with_acalendar_description,
                icon = Icons.Outlined.CalendarMonth,
                tags = setOf(WorksWithTag.ANDROID),
                links = WorksWithLinks(
                    web = "https://play.google.com/store/apps/details?id=org.withouthat.acalendarplus",
                    play = "org.withouthat.acalendarplus",
                ),
            ),
            WorksWithEntry(
                title = Res.string.todoagenda,
                description = Res.string.works_with_todoagenda_description,
                icon = Icons.Outlined.Widgets,
                tags = setOf(WorksWithTag.ANDROID),
                links = WorksWithLinks(
                    web = "https://github.com/andstatus/todoagenda",
                    play = "org.andstatus.todoagenda",
                    fdroid = "org.andstatus.todoagenda",
                ),
            ),
        ),
    ),
    WorksWithGroup(
        title = Res.string.works_with_automation,
        entries = listOf(
            WorksWithEntry(
                title = Res.string.tasker,
                description = Res.string.works_with_tasker_description,
                icon = Icons.Outlined.Bolt,
                tags = setOf(WorksWithTag.ANDROID),
                links = WorksWithLinks(
                    web = TasksUrls.TASKER,
                    play = "net.dinglisch.android.taskerm",
                ),
            ),
            WorksWithEntry(
                title = Res.string.mcp_server,
                description = Res.string.works_with_mcp_description,
                icon = Icons.Outlined.SmartToy,
                tags = setOf(WorksWithTag.DESKTOP),
                openMcpSettingsOnDesktop = true,
                links = WorksWithLinks(web = TasksUrls.DOWNLOAD),
            ),
        ),
    ),
    WorksWithGroup(
        title = Res.string.works_with_developers,
        entries = listOf(
            WorksWithEntry(
                title = Res.string.works_with_content_provider,
                description = Res.string.works_with_content_provider_description,
                icon = Icons.Outlined.Api,
                tags = setOf(WorksWithTag.ANDROID),
                links = WorksWithLinks(web = TasksUrls.CONTENT_PROVIDER),
            ),
        ),
    ),
)

fun worksWithGroups(
    configuration: PlatformConfiguration,
    tag: WorksWithTag? = null,
): List<WorksWithGroup> = worksWithCatalog.mapNotNull { group ->
    if (!group.available(configuration)) return@mapNotNull null
    val entries = group.entries.filter { entry ->
        entry.available(configuration) && (tag == null || tag in entry.tags)
    }
    group.takeIf { entries.isNotEmpty() }?.copy(entries = entries)
}
