package org.tasks.data.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_LOCAL
import org.tasks.data.entity.TagData
import org.tasks.data.getTextOrNull

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

    val MIGRATION_94_95 = object : Migration(94, 95) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `tagdata` ADD COLUMN `normalized_name` TEXT NOT NULL DEFAULT ''")

            data class Row(
                val id: Long,
                val remoteId: String?,
                val name: String?,
                val color: Int?,
                val icon: String?,
            )

            val rows = mutableListOf<Row>()
            connection
                .prepare("SELECT `_id`, `remoteId`, `name`, `color`, `td_icon` FROM `tagdata`")
                .use { cursor ->
                    while (cursor.step()) {
                        rows.add(
                            Row(
                                id = cursor.getLong(0),
                                remoteId = cursor.getTextOrNull(1),
                                name = cursor.getTextOrNull(2),
                                color = if (cursor.isNull(3)) null else cursor.getLong(3).toInt(),
                                icon = cursor.getTextOrNull(4),
                            )
                        )
                    }
                }

            connection
                .prepare("UPDATE `tagdata` SET `normalized_name` = ? WHERE `_id` = ?")
                .use { statement ->
                    for (row in rows) {
                        statement.bindText(1, TagData.normalize(row.name))
                        statement.bindLong(2, row.id)
                        statement.step()
                        statement.reset()
                    }
                }

            // Merge case-collisions before the unique index can be created.
            val decorated: (Row) -> Boolean = { (it.color ?: 0) != 0 || !it.icon.isNullOrBlank() }
            rows
                .groupBy { TagData.normalize(it.name) }
                .filter { it.value.size > 1 }
                .forEach { (_, group) ->
                    val survivor = group
                        .sortedWith(compareByDescending(decorated).thenBy { it.id })
                        .first()
                    for (loser in group) {
                        if (loser.id == survivor.id) continue
                        if (loser.remoteId != null && survivor.remoteId != null) {
                            connection
                                .prepare("UPDATE `tags` SET `tag_uid` = ?, `name` = ? WHERE `tag_uid` = ?")
                                .use { statement ->
                                    statement.bindText(1, survivor.remoteId)
                                    if (survivor.name == null) {
                                        statement.bindNull(2)
                                    } else {
                                        statement.bindText(2, survivor.name)
                                    }
                                    statement.bindText(3, loser.remoteId)
                                    statement.step()
                                }
                        }
                        connection.execSQL("DELETE FROM `tagdata` WHERE `_id` = ${loser.id}")
                    }
                }

            // Repointing can leave a task tagged with both variants -> duplicate join rows.
            connection.execSQL(
                "DELETE FROM `tags` WHERE `_id` NOT IN (SELECT MIN(`_id`) FROM `tags` GROUP BY `task`, `tag_uid`)"
            )

            connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tagdata_normalized_name` ON `tagdata` (`normalized_name`)")
        }
    }

    val all: Array<Migration> = arrayOf(
        MIGRATION_92_93,
        MIGRATION_94_95,
    )
}
