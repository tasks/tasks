package com.todoroo.astrid.dao

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.todoroo.astrid.data.Task
import org.tasks.data.Alarm
import org.tasks.data.AlarmDao
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavCalendar
import org.tasks.data.CaldavDao
import org.tasks.data.CaldavTask
import org.tasks.data.ContentProviderDao
import org.tasks.data.DeletionDao
import org.tasks.data.Filter
import org.tasks.data.FilterDao
import org.tasks.data.Geofence
import org.tasks.data.GoogleTask
import org.tasks.data.GoogleTaskAccount
import org.tasks.data.GoogleTaskDao
import org.tasks.data.GoogleTaskList
import org.tasks.data.GoogleTaskListDao
import org.tasks.data.LocationDao
import org.tasks.data.Place
import org.tasks.data.Principal
import org.tasks.data.PrincipalAccess
import org.tasks.data.PrincipalDao
import org.tasks.data.Tag
import org.tasks.data.TagDao
import org.tasks.data.TagData
import org.tasks.data.TagDataDao
import org.tasks.data.TaskAttachment
import org.tasks.data.TaskAttachmentDao
import org.tasks.data.TaskDao
import org.tasks.data.TaskListMetadata
import org.tasks.data.TaskListMetadataDao
import org.tasks.data.UpgraderDao
import org.tasks.data.UserActivity
import org.tasks.data.UserActivityDao
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
        GoogleTaskAccount::class,
        Principal::class,
        PrincipalAccess::class
    ],
    autoMigrations = [
        AutoMigration(from = 82, to = 83, spec = Migrations.AutoMigrate82to83::class),
    ],
    version = 83
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