package org.tasks.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.Astrid2ContentProviderDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.CompletionDao
import org.tasks.data.dao.DeletionDao
import org.tasks.data.dao.DirtyDao
import org.tasks.data.dao.FilterDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.NotificationDao
import org.tasks.data.dao.PrincipalDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskAttachmentDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.dao.TaskListMetadataDao
import org.tasks.data.dao.UpgraderDao
import org.tasks.data.dao.UserActivityDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Attachment
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Filter
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.Notification
import org.tasks.data.entity.Place
import org.tasks.data.entity.Principal
import org.tasks.data.entity.PrincipalAccess
import org.tasks.data.entity.Tag
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.data.entity.TaskAttachment
import org.tasks.data.entity.TaskDirtyVersion
import org.tasks.data.entity.TaskListMetadata
import org.tasks.data.entity.UserActivity

@Database(
    entities = [
        Notification::class,
        TagData::class,
        UserActivity::class,
        TaskAttachment::class,
        TaskListMetadata::class,
        Task::class,
        Alarm::class,
        Place::class,
        Geofence::class,
        Tag::class,
        Filter::class,
        CaldavCalendar::class,
        CaldavTask::class,
        CaldavAccount::class,
        Principal::class,
        PrincipalAccess::class,
        Attachment::class,
        TaskDirtyVersion::class,
    ],
    autoMigrations = [
        AutoMigration(from = 83, to = 84, spec = AutoMigrate83to84::class),
        AutoMigration(from = 88, to = 89, spec = AutoMigrate88to89::class),
        AutoMigration(from = 91, to = 92),
        AutoMigration(from = 93, to = 94, spec = AutoMigrate93to94::class),
    ],
    version = 95
)
abstract class Database : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun tagDataDao(): TagDataDao
    abstract fun userActivityDao(): UserActivityDao
    abstract fun taskAttachmentDao(): TaskAttachmentDao
    abstract fun taskListMetadataDao(): TaskListMetadataDao
    abstract fun alarmDao(): AlarmDao
    abstract fun locationDao(): LocationDao
    abstract fun tagDao(): TagDao
    abstract fun googleTaskDao(): GoogleTaskDao
    abstract fun filterDao(): FilterDao
    abstract fun taskDao(): TaskDao
    abstract fun dirtyDao(): DirtyDao
    abstract fun caldavDao(): CaldavDao
    abstract fun deletionDao(): DeletionDao
    abstract fun contentProviderDao(): Astrid2ContentProviderDao
    abstract fun upgraderDao(): UpgraderDao
    abstract fun principalDao(): PrincipalDao
    abstract fun completionDao(): CompletionDao

    /** @return human-readable database name for debugging
     */
    override fun toString(): String {
        return "DB:$name"
    }

    val name: String
        get() = NAME

    companion object {
        const val NAME = "database"

        // Seeds a task_dirty row for every new caldav_tasks row so freshly created tasks are
        // marked dirty (pending push). Re-created idempotently on every database open (CREATE
        // TRIGGER IF NOT EXISTS), so it survives caldav_tasks table rebuilds from future
        // destructive auto-migrations (a rebuild drops triggers bound to the table, and Room does
        // not recreate app-defined triggers). Local-account rows are excluded: they never sync, so
        // seeding them dirty would pin hasDirtyTasks() to true forever. The WHEN uses `IS NOT` so an
        // unresolved account (NULL) still seeds — better to over-sync than to silently drop a push.
        val TASK_DIRTY_TRIGGER = """
            CREATE TRIGGER IF NOT EXISTS task_dirty_after_insert
            AFTER INSERT ON caldav_tasks
            WHEN (
              SELECT cda_account_type
              FROM caldav_lists
              INNER JOIN caldav_accounts ON cda_uuid = cdl_account
              WHERE cdl_uuid = NEW.cd_calendar
            ) IS NOT ${CaldavAccount.TYPE_LOCAL}
            BEGIN
              INSERT OR IGNORE INTO task_dirty (caldav_task_id, dirty_version, synced_version)
              VALUES (NEW.cd_id, 1, 0);
            END
        """.trimIndent()

        val CALLBACK = object : RoomDatabase.Callback() {
            override fun onOpen(connection: SQLiteConnection) {
                connection.execSQL(TASK_DIRTY_TRIGGER)
            }
        }
    }
}
