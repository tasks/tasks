package org.tasks

data class PlatformConfiguration(
    // Sync providers
    val supportsTasksOrg: Boolean = true,
    val supportsCaldav: Boolean = true,
    val supportsGoogleTasks: Boolean = false,
    val supportsMicrosoft: Boolean = false,
    val supportsOpenTasks: Boolean = false,
    val supportsEteSync: Boolean = false,

    // Platform features
    val supportsBackupImport: Boolean = true,
    val supportsGeofences: Boolean = false,
    val supportsCalendarEvents: Boolean = false,
    val supportsInAppPurchase: Boolean = false,
    val isLibre: Boolean = false,
)
