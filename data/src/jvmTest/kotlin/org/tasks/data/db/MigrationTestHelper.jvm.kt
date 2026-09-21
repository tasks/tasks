package org.tasks.data.db

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.nio.file.Files
import java.nio.file.Paths

internal actual fun migrationTestHelper(schemaDir: String) = MigrationTestHelper(
    schemaDirectoryPath = Paths.get(schemaDir),
    databasePath = Files.createTempDirectory("room-migration-test").resolve("migration-test.db"),
    driver = BundledSQLiteDriver(),
    databaseClass = Database::class,
)
