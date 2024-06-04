package org.tasks.backup

import kotlinx.serialization.Serializable
import org.tasks.backup.TasksJsonImporter.LegacyLocation
import org.tasks.data.GoogleTask
import org.tasks.data.GoogleTaskAccount
import org.tasks.data.GoogleTaskList
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Attachment
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Filter
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.Place
import org.tasks.data.entity.Tag
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.data.entity.TaskAttachment
import org.tasks.data.entity.TaskListMetadata
import org.tasks.data.entity.UserActivity

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
@Serializable
class BackupContainer(
    val tasks: List<TaskBackup>? = null,
    val places: List<Place>? = null,
    val tags: List<TagData>? = null,
    val filters: List<Filter>? = null,
    val caldavAccounts: List<CaldavAccount>? = null,
    val caldavCalendars: List<CaldavCalendar>? = null,
    val taskListMetadata: List<TaskListMetadata>? = null,
    val taskAttachments: List<TaskAttachment>? = null,
    val intPrefs: Map<String, Integer>? = null,
    val longPrefs: Map<String, java.lang.Long>? = null,
    val stringPrefs: Map<String, String>? = null,
    val boolPrefs: Map<String, java.lang.Boolean>? = null,
    val setPrefs: Map<String, java.util.Set<String>>? = null,
    val googleTaskAccounts: List<GoogleTaskAccount>? = null,
    val googleTaskLists: List<GoogleTaskList>? = null,
) {
    @Serializable
    class TaskBackup(
        val task: Task,
        val alarms: List<Alarm>? = null,
        val geofences: List<Geofence>? = null,
        val tags: List<Tag>? = null,
        val comments: List<UserActivity>? = null,
        val attachments: List<Attachment>? = null,
        val caldavTasks: List<CaldavTask>? = null,
        val vtodo: String? = null,
        val google: List<GoogleTask>? = null,
        val locations: List<LegacyLocation>? = null,
    )
}