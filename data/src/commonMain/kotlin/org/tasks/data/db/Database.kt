package org.tasks.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import org.tasks.data.entity.Task
import org.tasks.data.entity.Alarm
import org.tasks.data.dao.AlarmDao
import org.tasks.data.entity.Attachment
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavTask
import org.tasks.data.dao.Astrid2ContentProviderDao
import org.tasks.data.dao.DeletionDao
import org.tasks.data.entity.Filter
import org.tasks.data.dao.FilterDao
import org.tasks.data.entity.Geofence
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.GoogleTaskListDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.entity.Notification
import org.tasks.data.dao.NotificationDao
import org.tasks.data.entity.Place
import org.tasks.data.entity.Principal
import org.tasks.data.entity.PrincipalAccess
import org.tasks.data.dao.PrincipalDao
import org.tasks.data.entity.Tag
import org.tasks.data.dao.TagDao
import org.tasks.data.entity.TagData
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.TaskAttachment
import org.tasks.data.dao.TaskAttachmentDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.TaskListMetadata
import org.tasks.data.dao.TaskListMetadataDao
import org.tasks.data.dao.UpgraderDao
import org.tasks.data.entity.UserActivity
import org.tasks.data.dao.UserActivityDao

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
        AutoMigration(from = 83, to = 84, spec = AutoMigrate83to84::class),
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
    abstract fun contentProviderDao(): Astrid2ContentProviderDao
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