package com.todoroo.astrid.service

import org.tasks.data.db.Database
import org.tasks.service.UpgradeStep

object CommonUpgrades {
    fun all(database: Database): List<UpgradeStep> = listOf(
        UpgradeStep(Upgrade_15_11.VERSION) {
            Upgrade_15_11(database.upgraderDao(), database.dirtyDao())
        },
    ) + platformUpgrades(database) + listOf(
        UpgradeStep(Upgrade_15_14_1.VERSION) {
            Upgrade_15_14_1(database.caldavDao())
        },
        UpgradeStep(Upgrade_15_14_2.VERSION) {
            Upgrade_15_14_2(database.caldavDao())
        },
    )
}

internal expect fun platformUpgrades(database: Database): List<UpgradeStep>
