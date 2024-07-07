package org.tasks.compose.drawer

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

    val localListsEnabled: Boolean
        get() = true
}