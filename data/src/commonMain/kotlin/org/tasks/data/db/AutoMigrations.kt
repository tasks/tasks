package org.tasks.data.db

import androidx.room.DeleteColumn
import androidx.room.RenameColumn
import androidx.room.migration.AutoMigrationSpec

@DeleteColumn.Entries(
    DeleteColumn(tableName = "caldav_accounts", columnName = "cda_encryption_key"),
    DeleteColumn(tableName = "caldav_accounts", columnName = "cda_repeat"),
)
class AutoMigrate83to84: AutoMigrationSpec

@RenameColumn.Entries(
    RenameColumn(tableName = "tagdata", fromColumnName = "td_icon", toColumnName = "td_icon"),
    RenameColumn(tableName = "places", fromColumnName = "place_icon", toColumnName = "place_icon"),
    RenameColumn(tableName = "filters", fromColumnName = "f_icon", toColumnName = "f_icon"),
    RenameColumn(tableName = "caldav_lists", fromColumnName = "cdl_icon", toColumnName = "cdl_icon"),
)
class AutoMigrate88to89: AutoMigrationSpec

