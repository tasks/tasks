package org.tasks.data.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_LOCAL

object CommonMigrations {
    val MIGRATION_92_93 = object : Migration(92, 93) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("CREATE TABLE IF NOT EXISTS `task_dirty` (`caldav_task_id` INTEGER NOT NULL PRIMARY KEY, `dirty_version` INTEGER NOT NULL DEFAULT 0, `synced_version` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`caldav_task_id`) REFERENCES `caldav_tasks`(`cd_id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_task_dirty_dirty_version_synced_version` ON `task_dirty` (`dirty_version`, `synced_version`)")
            // The CASE below is the SQL mirror of TaskDirtyVersion.reconstruct(lastSync, modified) —
            // keep the two in sync. The trigger that seeds new rows lives in Database.TASK_DIRTY_TRIGGER.
            connection.execSQL("""
                INSERT INTO `task_dirty` (`caldav_task_id`, `dirty_version`, `synced_version`)
                SELECT ct.`cd_id`,
                  CASE WHEN ct.`cd_last_sync` > 0 AND t.`modified` <= ct.`cd_last_sync` THEN 1
                       WHEN ct.`cd_last_sync` > 0 THEN 2
                       ELSE 1 END,
                  CASE WHEN ct.`cd_last_sync` > 0 THEN 1 ELSE 0 END
                FROM `caldav_tasks` ct
                INNER JOIN `tasks` t ON t.`_id` = ct.`cd_task`
                INNER JOIN `caldav_lists` ON `cdl_uuid` = ct.`cd_calendar`
                INNER JOIN `caldav_accounts` ON `cda_uuid` = `cdl_account`
                WHERE ct.`cd_deleted` = 0 AND `cda_account_type` != $TYPE_LOCAL
            """.trimIndent())
        }
    }

    val all: Array<Migration> = arrayOf(
        MIGRATION_92_93,
    )
}
