package com.todoroo.astrid.dao

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.todoroo.astrid.data.Task
import org.tasks.data.*
import org.tasks.data.TaskDao
import org.tasks.db.Migrations
import org.tasks.notifications.Notification
import org.tasks.notifications.NotificationDao

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
        GoogleTask::class,
        Filter::class,
        GoogleTaskList::class,
        CaldavCalendar::class,
        CaldavTask::class,
        CaldavAccount::class,
        Principal::class,
        PrincipalAccess::class,
        Attachment::class,
    ],
    autoMigrations = [
        AutoMigration(from = 83, to = 84, spec = Migrations.AutoMigrate83to84::class),
    ],
    version = 88
)
abstract class Database : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract val tagDataDao: TagDataDao
    abstract val userActivityDao: UserActivityDao
    abstract val taskAttachmentDao: TaskAttachmentDao
    abstract val taskListMetadataDao: TaskListMetadataDao
    abstract val alarmDao: AlarmDao
    abstract val locationDao: LocationDao
    abstract val tagDao: TagDao
    abstract val googleTaskDao: GoogleTaskDao
    abstract val filterDao: FilterDao
    abstract val googleTaskListDao: GoogleTaskListDao
    abstract val taskDao: TaskDao
    abstract val caldavDao: CaldavDao
    abstract val deletionDao: DeletionDao
    abstract val contentProviderDao: ContentProviderDao
    abstract val upgraderDao: UpgraderDao
    abstract val principalDao: PrincipalDao

    /** @return human-readable database name for debugging
     */
    override fun toString(): String {
        return "DB:$name"
    }

    val name: String
        get() = NAME

    companion object {
        const val NAME = "database"
    }
}