package com.todoroo.astrid.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import com.todoroo.astrid.data.Task
import org.tasks.data.*
import org.tasks.notifications.Notification
import org.tasks.notifications.NotificationDaoBlocking

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
            GoogleTaskAccount::class],
        version = 76)
abstract class Database : RoomDatabase() {
    abstract fun notificationDao(): NotificationDaoBlocking
    abstract val tagDataDao: TagDataDaoBlocking
    abstract val userActivityDao: UserActivityDaoBlocking
    abstract val taskAttachmentDao: TaskAttachmentDaoBlocking
    abstract val taskListMetadataDao: TaskListMetadataDaoBlocking
    abstract val alarmDao: AlarmDaoBlocking
    abstract val locationDao: LocationDaoBlocking
    abstract val tagDao: TagDaoBlocking
    abstract val googleTaskDao: GoogleTaskDaoBlocking
    abstract val filterDao: FilterDaoBlocking
    abstract val googleTaskListDao: GoogleTaskListDaoBlocking
    abstract val taskDao: TaskDaoBlocking
    abstract val caldavDao: CaldavDaoBlocking
    abstract val deletionDao: DeletionDaoBlocking
    abstract val contentProviderDao: ContentProviderDaoBlocking

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