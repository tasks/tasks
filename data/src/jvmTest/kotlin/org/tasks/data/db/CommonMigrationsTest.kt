package org.tasks.data.db

import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_LOCAL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class CommonMigrationsTest {
    private val tempDir: Path = Files.createTempDirectory("room-migration-test")

    @get:Rule
    val helper = MigrationTestHelper(
        schemaDirectoryPath = Paths.get(System.getProperty("tasks.schemaDir") ?: "schemas"),
        databasePath = tempDir.resolve("migration-test.db"),
        driver = BundledSQLiteDriver(),
        databaseClass = Database::class,
    )

    private fun migrate(
        from: Int,
        to: Int,
        migration: Migration,
        seed: SQLiteConnection.() -> Unit,
    ): SQLiteConnection {
        helper.createDatabase(from).use { db ->
            db.execSQL("PRAGMA foreign_keys = OFF")
            db.seed()
        }
        return helper.runMigrationsAndValidate(to, listOf(migration))
    }

    @Test
    fun backfillsNormalizedNameLocaleInvariant() {
        migrate(94, 95, CommonMigrations.MIGRATION_94_95) {
            insertTag(1, "uuid-1", "Work")
            insertTag(2, "uuid-2", "Mom's") // apostrophe exercises the bound-parameter backfill
            insertTag(3, "uuid-3", "ÉCOLE")
            insertTag(4, "uuid-4", "  Spaced  ") // leading/trailing whitespace is trimmed
        }.use {
            assertEquals(
                listOf("Work" to "work", "Mom's" to "mom's", "ÉCOLE" to "école", "  Spaced  " to "spaced"),
                it.nameAndNormalized(),
            )
        }
    }

    @Test
    fun mergesCaseCollisionPreferringDecoratedRow() {
        migrate(94, 95, CommonMigrations.MIGRATION_94_95) {
            insertTag(1, "uuid-plain", "work")
            insertTag(2, "uuid-decorated", "Work", color = -12345)
        }.use {
            // Only the decorated row survives, keeping its display casing.
            assertEquals(listOf("uuid-decorated" to "Work"), it.remoteIdAndName())
        }
    }

    @Test
    fun mergeTieBreaksByLowestIdWhenUndecorated() {
        migrate(94, 95, CommonMigrations.MIGRATION_94_95) {
            insertTag(5, "uuid-high", "FOO")
            insertTag(3, "uuid-low", "foo")
        }.use {
            assertEquals(listOf("uuid-low" to "foo"), it.remoteIdAndName())
        }
    }

    @Test
    fun repointsAndDedupesJoinRows() {
        migrate(94, 95, CommonMigrations.MIGRATION_94_95) {
            insertTag(1, "uuid-survivor", "Work", color = -12345)
            insertTag(2, "uuid-loser", "work")
            insertJoin(1, task = 100, tagUid = "uuid-survivor", name = "Work")
            insertJoin(2, task = 100, tagUid = "uuid-loser", name = "work") // same task, both variants
            insertJoin(3, task = 200, tagUid = "uuid-loser", name = "work")
        }.use {
            // Loser folded into survivor (tag_uid + denormalized name); duplicate (100, survivor)
            // join row is de-duped.
            assertEquals(
                listOf(
                    Triple(100L, "uuid-survivor", "Work"),
                    Triple(200L, "uuid-survivor", "Work"),
                ),
                it.joinRows(),
            )
        }
    }

    @Test
    fun uniqueIndexRejectsCaseDuplicatesAtRuntime() {
        migrate(94, 95, CommonMigrations.MIGRATION_94_95) {
            insertTag(1, "uuid-1", "Work")
        }.use { db ->
            var threw = false
            try {
                db.execSQL("INSERT INTO `tagdata` (`remoteId`, `name`, `tagOrdering`, `td_order`, `normalized_name`) VALUES ('uuid-2', 'work', '[]', -1, 'work')")
            } catch (e: Exception) {
                threw = true
            }
            assertTrue("unique index should reject a second normalized 'work'", threw)
        }
    }

    @Test
    fun deletesOrphanedJoinRowsAndAddsMetadataTables() {
        migrate(95, 96, CommonMigrations.MIGRATION_95_96) {
            insertTask(100)
            insertJoin(1, task = 100, tagUid = "uuid-live", name = "Work")
            insertJoin(2, task = 999, tagUid = "uuid-orphan", name = "Gone")
        }.use { db ->
            assertEquals(listOf(Triple(100L, "uuid-live", "Work")), db.joinRows())
            assertEquals(0, db.rowCount("metadata_sync_state"))
            assertEquals(0, db.rowCount("metadata_tombstone"))
        }
    }

    @Test
    fun seedsOneTaskDirtyRowDespiteDuplicateList() {
        migrate(92, 93, CommonMigrations.MIGRATION_92_93) {
            insertTask(100)
            insertAccount(1, uuid = "acct", type = TYPE_CALDAV)
            insertList(1, uuid = "list", account = "acct")
            insertList(2, uuid = "list", account = "acct")
            insertCaldavTask(10, task = 100, calendar = "list", lastSync = 0)
        }.use {
            assertEquals(listOf(Triple(10L, 1L, 0L)), it.taskDirtyRows())
        }
    }

    @Test
    fun computesVersionsAndExcludesLocalAndDeleted() {
        migrate(92, 93, CommonMigrations.MIGRATION_92_93) {
            insertAccount(1, uuid = "remote", type = TYPE_CALDAV)
            insertAccount(2, uuid = "local", type = TYPE_LOCAL)
            insertList(1, uuid = "rlist", account = "remote")
            insertList(2, uuid = "llist", account = "local")
            insertTask(100, modified = 50)
            insertTask(200, modified = 50)
            insertTask(300, modified = 200)
            insertCaldavTask(10, task = 100, calendar = "rlist", lastSync = 100)
            insertCaldavTask(20, task = 300, calendar = "rlist", lastSync = 100)
            insertCaldavTask(30, task = 200, calendar = "rlist", lastSync = 0)
            insertCaldavTask(40, task = 100, calendar = "llist", lastSync = 100)
            insertCaldavTask(50, task = 100, calendar = "rlist", lastSync = 100, deleted = 1)
            insertTask(400, modified = 100)
            insertCaldavTask(60, task = 400, calendar = "rlist", lastSync = 100)
        }.use {
            assertEquals(
                listOf(
                    Triple(10L, 1L, 1L),
                    Triple(20L, 2L, 1L),
                    Triple(30L, 1L, 0L),
                    Triple(60L, 1L, 1L),
                ),
                it.taskDirtyRows(),
            )
        }
    }

    @Test
    fun seedsOneTaskDirtyRowDespiteDuplicateAccount() {
        migrate(92, 93, CommonMigrations.MIGRATION_92_93) {
            insertTask(100)
            insertAccount(1, uuid = "acct", type = TYPE_CALDAV)
            insertAccount(2, uuid = "acct", type = TYPE_CALDAV)
            insertList(1, uuid = "list", account = "acct")
            insertCaldavTask(10, task = 100, calendar = "list", lastSync = 0)
        }.use {
            assertEquals(listOf(Triple(10L, 1L, 0L)), it.taskDirtyRows())
        }
    }

    @Test
    fun seedsUnresolvedCalendarLikeTheTrigger() {
        migrate(92, 93, CommonMigrations.MIGRATION_92_93) {
            insertTask(100)
            insertCaldavTask(10, task = 100, calendar = "missing", lastSync = 0)
        }.use {
            assertEquals(listOf(Triple(10L, 1L, 0L)), it.taskDirtyRows())
        }
    }

    private fun SQLiteConnection.insertTask(id: Long, modified: Long = 0) {
        execSQL(
            "INSERT INTO `tasks` (`_id`, `importance`, `dueDate`, `hideUntil`, `created`, `modified`, `completed`, `deleted`, `estimatedSeconds`, `elapsedSeconds`, `timerStart`, `notificationFlags`, `lastNotified`, `collapsed`, `parent`) " +
                "VALUES ($id, 0, 0, 0, 0, $modified, 0, 0, 0, 0, 0, 0, 0, 0, 0)"
        )
    }

    private fun SQLiteConnection.insertAccount(id: Long, uuid: String, type: Int) {
        prepare("INSERT INTO `caldav_accounts` (`cda_id`, `cda_uuid`, `cda_account_type`, `cda_collapsed`, `cda_server_type`) VALUES (?, ?, ?, 0, 0)")
            .use {
                it.bindLong(1, id)
                it.bindText(2, uuid)
                it.bindLong(3, type.toLong())
                it.step()
            }
    }

    private fun SQLiteConnection.insertList(id: Long, uuid: String, account: String) {
        prepare("INSERT INTO `caldav_lists` (`cdl_id`, `cdl_account`, `cdl_uuid`, `cdl_color`, `cdl_order`, `cdl_access`, `cdl_last_sync`) VALUES (?, ?, ?, 0, 0, 0, 0)")
            .use {
                it.bindLong(1, id)
                it.bindText(2, account)
                it.bindText(3, uuid)
                it.step()
            }
    }

    private fun SQLiteConnection.insertCaldavTask(
        id: Long,
        task: Long,
        calendar: String,
        lastSync: Long,
        deleted: Int = 0,
    ) {
        prepare("INSERT INTO `caldav_tasks` (`cd_id`, `cd_task`, `cd_calendar`, `cd_last_sync`, `cd_deleted`, `gt_moved`, `gt_remote_order`) VALUES (?, ?, ?, ?, ?, 0, 0)")
            .use {
                it.bindLong(1, id)
                it.bindLong(2, task)
                it.bindText(3, calendar)
                it.bindLong(4, lastSync)
                it.bindLong(5, deleted.toLong())
                it.step()
            }
    }

    private fun SQLiteConnection.taskDirtyRows(): List<Triple<Long, Long, Long>> = buildList {
        prepare("SELECT `caldav_task_id`, `dirty_version`, `synced_version` FROM `task_dirty` ORDER BY `caldav_task_id`").use {
            while (it.step()) {
                add(Triple(it.getLong(0), it.getLong(1), it.getLong(2)))
            }
        }
    }

    private fun SQLiteConnection.rowCount(table: String): Int {
        prepare("SELECT COUNT(*) FROM `$table`").use { return if (it.step()) it.getLong(0).toInt() else 0 }
    }

    private fun SQLiteConnection.insertTag(
        id: Long,
        remoteId: String,
        name: String?,
        color: Int? = null,
        icon: String? = null,
    ) {
        prepare("INSERT INTO `tagdata` (`_id`, `remoteId`, `name`, `color`, `tagOrdering`, `td_icon`, `td_order`) VALUES (?, ?, ?, ?, '[]', ?, -1)")
            .use {
                it.bindLong(1, id)
                it.bindText(2, remoteId)
                if (name == null) it.bindNull(3) else it.bindText(3, name)
                if (color == null) it.bindNull(4) else it.bindLong(4, color.toLong())
                if (icon == null) it.bindNull(5) else it.bindText(5, icon)
                it.step()
            }
    }

    private fun SQLiteConnection.insertJoin(id: Long, task: Long, tagUid: String, name: String? = null) {
        prepare("INSERT INTO `tags` (`_id`, `task`, `tag_uid`, `name`) VALUES (?, ?, ?, ?)")
            .use {
                it.bindLong(1, id)
                it.bindLong(2, task)
                it.bindText(3, tagUid)
                if (name == null) it.bindNull(4) else it.bindText(4, name)
                it.step()
            }
    }

    private fun SQLiteConnection.nameAndNormalized(): List<Pair<String, String>> = buildList {
        prepare("SELECT `name`, `normalized_name` FROM `tagdata` ORDER BY `_id`").use {
            while (it.step()) {
                add((if (it.isNull(0)) "" else it.getText(0)) to it.getText(1))
            }
        }
    }

    private fun SQLiteConnection.remoteIdAndName(): List<Pair<String, String>> = buildList {
        prepare("SELECT `remoteId`, `name` FROM `tagdata` ORDER BY `_id`").use {
            while (it.step()) {
                add(it.getText(0) to it.getText(1))
            }
        }
    }

    private fun SQLiteConnection.joinRows(): List<Triple<Long, String, String?>> = buildList {
        prepare("SELECT `task`, `tag_uid`, `name` FROM `tags` ORDER BY `_id`").use {
            while (it.step()) {
                add(Triple(it.getLong(0), it.getText(1), if (it.isNull(2)) null else it.getText(2)))
            }
        }
    }
}
