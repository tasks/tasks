package com.todoroo.astrid.service

import org.tasks.data.db.Database
import org.tasks.service.UpgradeStep

internal actual fun platformUpgrades(database: Database): List<UpgradeStep> = listOf(
    UpgradeStep(Upgrade_15_13.VERSION) {
        Upgrade_15_13(database.caldavDao())
    },
)
