package org.tasks.data.db

import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL

fun <T : RoomDatabase> RoomDatabase.Builder<T>.setBundledDriver(): RoomDatabase.Builder<T> {
    val driver = BundledSQLiteDriver()
    return setDriver(
        object : SQLiteDriver by driver {
            override fun open(fileName: String): SQLiteConnection {
                return driver.open(fileName).also {
                    it.execSQL("PRAGMA busy_timeout = 60000")
                }
            }
        }
    )
}
