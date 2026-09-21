package org.tasks.data.db

import androidx.room3.testing.MigrationTestHelper

internal expect fun migrationTestHelper(schemaDir: String): MigrationTestHelper
