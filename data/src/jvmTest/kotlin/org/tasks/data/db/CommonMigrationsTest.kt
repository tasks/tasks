package org.tasks.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Exercises [CommonMigrations.MIGRATION_94_95] with Room's [MigrationTestHelper]: the database is
 * created at v94 from the exported `94.json` schema, the migration runs, and
 * [MigrationTestHelper.runMigrationsAndValidate] validates the result against `95.json` — so any
 * drift between the hand-written DDL and the Room entity fails the test automatically. The
 * assertions below then cover the data-mutating parts Room can't validate: the `normalized_name`
 * backfill, the case-collision merge, and join-row repointing/de-duping.
 *
 * This is the template for future migration tests — copy the [migrate] helper and point it at the
 * relevant versions.
 */
class CommonMigrationsTest {
    private val tempDir: Path = Files.createTempDirectory("room-migration-test")

    @get:Rule
    val helper = MigrationTestHelper(
        schemaDirectoryPath = Paths.get(System.getProperty("tasks.schemaDir") ?: "schemas"),
        databasePath = tempDir.resolve("migration-test.db"),
        driver = BundledSQLiteDriver(),
        databaseClass = Database::class,
    )

    /**
     * Seeds a fresh v94 database via [seed], runs 94→95, validates the schema against `95.json`,
     * and returns the post-migration connection for assertions (caller closes it).
     */
    private fun migrate(seed: SQLiteConnection.() -> Unit): SQLiteConnection {
        helper.createDatabase(94).use { db ->
            // Seeding orphan `tags` rows (no backing task) would trip the tags->tasks foreign key.
            db.execSQL("PRAGMA foreign_keys = OFF")
            db.seed()
        }
        return helper.runMigrationsAndValidate(95, listOf(CommonMigrations.MIGRATION_94_95))
    }

    @Test
    fun backfillsNormalizedNameLocaleInvariant() {
        migrate {
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
        migrate {
            insertTag(1, "uuid-plain", "work")
            insertTag(2, "uuid-decorated", "Work", color = -12345)
        }.use {
            // Only the decorated row survives, keeping its display casing.
            assertEquals(listOf("uuid-decorated" to "Work"), it.remoteIdAndName())
        }
    }

    @Test
    fun mergeTieBreaksByLowestIdWhenUndecorated() {
        migrate {
            insertTag(5, "uuid-high", "FOO")
            insertTag(3, "uuid-low", "foo")
        }.use {
            assertEquals(listOf("uuid-low" to "foo"), it.remoteIdAndName())
        }
    }

    @Test
    fun repointsAndDedupesJoinRows() {
        migrate {
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
        migrate {
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
        migrate9596 {
            insertTask(100)
            insertJoin(1, task = 100, tagUid = "uuid-live", name = "Work")
            insertJoin(2, task = 999, tagUid = "uuid-orphan", name = "Gone")
        }.use { db ->
            assertEquals(listOf(Triple(100L, "uuid-live", "Work")), db.joinRows())
            assertEquals(0, db.rowCount("metadata_sync_state"))
            assertEquals(0, db.rowCount("metadata_tombstone"))
        }
    }

    private fun migrate9596(seed: SQLiteConnection.() -> Unit): SQLiteConnection {
        helper.createDatabase(95).use { db ->
            db.execSQL("PRAGMA foreign_keys = OFF")
            db.seed()
        }
        return helper.runMigrationsAndValidate(96, listOf(CommonMigrations.MIGRATION_95_96))
    }

    private fun SQLiteConnection.insertTask(id: Long) {
        execSQL(
            "INSERT INTO `tasks` (`_id`, `importance`, `dueDate`, `hideUntil`, `created`, `modified`, `completed`, `deleted`, `estimatedSeconds`, `elapsedSeconds`, `timerStart`, `notificationFlags`, `lastNotified`, `collapsed`, `parent`) " +
                "VALUES ($id, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)"
        )
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
