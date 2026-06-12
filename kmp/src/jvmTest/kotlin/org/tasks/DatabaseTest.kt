package org.tasks

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.junit.After
import org.tasks.data.db.Database

abstract class DatabaseTest {
    protected val db: Database = Room.inMemoryDatabaseBuilder<Database>()
        .setDriver(BundledSQLiteDriver())
        .addCallback(Database.CALLBACK)
        .build()

    @After
    fun closeDb() {
        db.close()
    }
}
