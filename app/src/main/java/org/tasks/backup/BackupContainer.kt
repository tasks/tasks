package org.tasks.backup

import org.tasks.data.entity.Task
import org.tasks.backup.TasksJsonImporter.LegacyLocation
import org.tasks.data.*
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
import org.tasks.data.entity.TaskAttachment
import org.tasks.data.entity.TaskListMetadata
import org.tasks.data.entity.UserActivity

class BackupContainer(
    val tasks: List<TaskBackup>?,
    val places: List<Place>?,
    val tags: List<TagData>?,
    val filters: List<Filter>?,
    val caldavAccounts: List<CaldavAccount>?,
    val caldavCalendars: List<CaldavCalendar>?,
    val taskListMetadata: List<TaskListMetadata>?,
    val taskAttachments: List<TaskAttachment>?,
    val intPrefs: Map<String, Integer>?,
    val longPrefs: Map<String, java.lang.Long>?,
    val stringPrefs: Map<String, String>?,
    val boolPrefs: Map<String, java.lang.Boolean>?,
    val setPrefs: Map<String, java.util.Set<*>>?,
    val googleTaskAccounts: List<GoogleTaskAccount>? = emptyList(),
    val googleTaskLists: List<GoogleTaskList>? = emptyList(),
) {
    class TaskBackup(
        val task: Task,
        val alarms: List<Alarm>,
        val geofences: List<Geofence>?,
        val tags: List<Tag>,
        val comments: List<UserActivity>,
        val attachments: List<Attachment>?,
        val caldavTasks: List<CaldavTask>?,
        val vtodo: String?,
        val google: List<GoogleTask> = emptyList(),
    ) {
        val locations: List<LegacyLocation> = emptyList()
    }
}