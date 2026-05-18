package org.tasks.compose.drawer

import org.tasks.data.entity.CaldavAccount

interface DrawerConfiguration {
    val filtersEnabled: Boolean
        get() = true

    val placesEnabled: Boolean
        get() = true

    val hideUnusedPlaces: Boolean
        get() = false

    val tagsEnabled: Boolean
        get() = true

    val hideUnusedTags: Boolean
        get() = false

    val todayFilter: Boolean
        get() = true

    val recentlyModifiedFilter: Boolean
        get() = true

    val canCreateFilters: Boolean
        get() = true

    val canCreateTags: Boolean
        get() = true

    val canCreatePlaces: Boolean
        get() = true

    fun canCreateLists(account: CaldavAccount): Boolean = true

    fun canEditList(account: CaldavAccount): Boolean = true
}
