package org.tasks.data

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec

@DeleteColumn.Entries(
    DeleteColumn(tableName = "caldav_accounts", columnName = "cda_encryption_key"),
    DeleteColumn(tableName = "caldav_accounts", columnName = "cda_repeat"),
)
class AutoMigrate83to84: AutoMigrationSpec
