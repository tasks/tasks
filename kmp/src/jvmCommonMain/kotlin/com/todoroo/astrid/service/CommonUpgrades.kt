package com.todoroo.astrid.service

import org.tasks.data.db.Database
import org.tasks.service.UpgradeStep

object CommonUpgrades {
    fun all(database: Database): List<UpgradeStep> = listOf(
        UpgradeStep(Upgrade_15_11.VERSION) {
            Upgrade_15_11(database.upgraderDao(), database.dirtyDao())
        },
    )
}
