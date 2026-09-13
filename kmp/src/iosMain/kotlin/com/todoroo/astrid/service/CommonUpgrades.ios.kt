package com.todoroo.astrid.service

import org.tasks.data.db.Database
import org.tasks.service.UpgradeStep

internal actual fun platformUpgrades(database: Database): List<UpgradeStep> = emptyList()
