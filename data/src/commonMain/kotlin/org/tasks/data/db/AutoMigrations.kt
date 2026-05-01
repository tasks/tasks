package org.tasks.data.db

import androidx.room3.DeleteColumn
import androidx.room3.RenameColumn
import androidx.room3.migration.AutoMigrationSpec

@DeleteColumn(tableName = "caldav_accounts", columnName = "cda_encryption_key")
@DeleteColumn(tableName = "caldav_accounts", columnName = "cda_repeat")
class AutoMigrate83to84: AutoMigrationSpec

@RenameColumn(tableName = "tagdata", fromColumnName = "td_icon", toColumnName = "td_icon")
@RenameColumn(tableName = "places", fromColumnName = "place_icon", toColumnName = "place_icon")
@RenameColumn(tableName = "filters", fromColumnName = "f_icon", toColumnName = "f_icon")
@RenameColumn(tableName = "caldav_lists", fromColumnName = "cdl_icon", toColumnName = "cdl_icon")
class AutoMigrate88to89: AutoMigrationSpec
