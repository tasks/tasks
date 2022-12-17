package org.tasks.backup

import com.todoroo.astrid.data.Task
import org.tasks.backup.TasksJsonImporter.LegacyLocation
import org.tasks.data.*

class BackupContainer(
        val tasks: List<TaskBackup>?,
        val places: List<Place>?,
        val tags: List<TagData>?,
        val filters: List<Filter>?,
        val googleTaskLists: List<GoogleTaskList>?,
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
) {
    class TaskBackup(
            val task: Task,
            val alarms: List<Alarm>,
            val geofences: List<Geofence>?,
            val tags: List<Tag>,
            val google: List<GoogleTask>,
            val comments: List<UserActivity>,
            val attachments: List<Attachment>?,
            val caldavTasks: List<CaldavTask>?,
            val vtodo: String?,
    ) {
        val locations: List<LegacyLocation> = emptyList()
    }
}