package org.tasks

data class PlatformConfiguration(
    val versionCode: Int = 0,

    // Sync providers
    val supportsTasksOrg: Boolean = true,
    val supportsCaldav: Boolean = false,
    val supportsGoogleTasks: Boolean = false,
    val supportsMicrosoft: Boolean = false,
    val supportsOpenTasks: Boolean = false,
    val supportsEteSync: Boolean = false,

    // Platform features
    val supportsBackupImport: Boolean = false,
    val supportsGeofences: Boolean = false,
    val supportsCalendarEvents: Boolean = false,
    val supportsInAppPurchase: Boolean = false,
    val isLibre: Boolean = false,
)
