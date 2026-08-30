/**
 * WearDatabase.kt — Room database for the Wear OS app.
 *
 * ## Tables
 * | Table            | Entity               | Purpose                                       |
 * |------------------|-----------------------|-----------------------------------------------|
 * | `tasks`          | [TaskEntity]          | Local copy of tasks (synced with phone)        |
 * | `outbox_ops`     | [OutboxOpEntity]      | Queue of pending watch → phone operations      |
 * | `processed_ops`  | [ProcessedOpEntity]   | Idempotency guard for phone → watch operations |
 * | `settings`       | [SettingsEntity]      | Singleton row for user preferences             |
 *
 * Uses `fallbackToDestructiveMigration` so a schema bump wipes and
 * recreates — acceptable because the phone is the source of truth.
 *
 * ## Singleton
 * Accessed via [WearDatabase.getInstance].
 */
package org.tasks.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

/**
 * Type converters for Room database.
 */
class Converters {
    @TypeConverter
    fun fromOutboxOpType(value: OutboxOpType): String = value.name

    @TypeConverter
    fun toOutboxOpType(value: String): OutboxOpType = OutboxOpType.valueOf(value)

    @TypeConverter
    fun fromOutboxOpState(value: OutboxOpState): String = value.name

    @TypeConverter
    fun toOutboxOpState(value: String): OutboxOpState = OutboxOpState.valueOf(value)
}

/**
 * Room Database for the Wear app.
 * Contains local task storage, settings, and sync-related tables.
 */
@Database(
    entities = [
        TaskEntity::class,
        OutboxOpEntity::class,
        ProcessedOpEntity::class,
        SettingsEntity::class,
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class WearDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun outboxOpDao(): OutboxOpDao
    abstract fun processedOpDao(): ProcessedOpDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        private const val DATABASE_NAME = "wear.db"

        @Volatile
        private var INSTANCE: WearDatabase? = null

        /**
         * Get the singleton database instance.
         * Creates the database if it doesn't exist.
         */
        fun getInstance(context: Context): WearDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): WearDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                WearDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
    }
}
