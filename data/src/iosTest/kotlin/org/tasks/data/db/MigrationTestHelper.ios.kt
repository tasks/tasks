package org.tasks.data.db

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID

internal actual fun migrationTestHelper(schemaDir: String) = MigrationTestHelper(
    schemaDirectoryPath = schemaDir,
    fileName = NSTemporaryDirectory() + "migration-test-" + NSUUID().UUIDString + ".db",
    driver = BundledSQLiteDriver(),
    databaseClass = Database::class,
)
