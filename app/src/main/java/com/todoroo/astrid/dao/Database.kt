package com.todoroo.astrid.dao

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.todoroo.astrid.data.Task
import org.tasks.data.Alarm
import org.tasks.data.AlarmDao
import org.tasks.data.Attachment
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavCalendar
import org.tasks.data.CaldavDao
import org.tasks.data.CaldavTask
import org.tasks.data.ContentProviderDao
import org.tasks.data.DeletionDao
import org.tasks.data.Filter
import org.tasks.data.FilterDao
import org.tasks.data.Geofence
import org.tasks.data.GoogleTaskDao
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
        Filter::class,
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
    abstract fun tagDataDao(): TagDataDao
    abstract fun userActivityDao(): UserActivityDao
    abstract fun taskAttachmentDao(): TaskAttachmentDao
    abstract fun taskListMetadataDao(): TaskListMetadataDao
    abstract fun alarmDao(): AlarmDao
    abstract fun locationDao(): LocationDao
    abstract fun tagDao(): TagDao
    abstract fun googleTaskDao(): GoogleTaskDao
    abstract fun filterDao(): FilterDao
    abstract fun googleTaskListDao(): GoogleTaskListDao
    abstract fun taskDao(): TaskDao
    abstract fun caldavDao(): CaldavDao
    abstract fun deletionDao(): DeletionDao
    abstract fun contentProviderDao(): ContentProviderDao
    abstract fun upgraderDao(): UpgraderDao
    abstract fun principalDao(): PrincipalDao

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