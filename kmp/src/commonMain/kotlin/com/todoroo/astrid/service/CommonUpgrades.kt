package com.todoroo.astrid.service

import org.tasks.caldav.VtodoCache
import org.tasks.data.db.Database
import org.tasks.security.Encryption
import org.tasks.service.Upgrade
import org.tasks.service.UpgradeStep

object CommonUpgrades {
    fun all(
        database: Database,
        encryption: Encryption,
        vtodoCache: VtodoCache,
        openTasksStatuses: suspend () -> Unit = {},
        msalSubjects: suspend () -> Map<String, String> = { emptyMap() },
    ): List<UpgradeStep> = listOf(
        UpgradeStep(Upgrade_15_11.VERSION) {
            Upgrade_15_11(database.upgraderDao(), database.dirtyDao())
        },
    ) + platformUpgrades(database) + listOf(
        UpgradeStep(Upgrade_15_14_1.VERSION) {
            Upgrade_15_14_1(database.caldavDao())
        },
        UpgradeStep(Upgrade_15_15.VERSION) {
            Upgrade {
                Upgrade_15_15(database.caldavDao(), encryption, msalSubjects).run()
                Upgrade_15_15_Status(
                    database.upgraderDao(),
                    database.caldavDao(),
                    vtodoCache,
                    openTasksStatuses,
                ).run()
            }
        },
    )
}

internal expect fun platformUpgrades(database: Database): List<UpgradeStep>
