package org.tasks.backup

import com.todoroo.astrid.data.Task
import org.tasks.backup.TasksJsonImporter.LegacyLocation
import org.tasks.data.Alarm
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavCalendar
import org.tasks.data.CaldavTask
import org.tasks.data.Filter
import org.tasks.data.Geofence
import org.tasks.data.GoogleTask
import org.tasks.data.GoogleTaskAccount
import org.tasks.data.GoogleTaskList
import org.tasks.data.Place
import org.tasks.data.Tag
import org.tasks.data.TagData
import org.tasks.data.TaskAttachment
import org.tasks.data.TaskListMetadata
import org.tasks.data.UserActivity

class BackupContainer(
        val tasks: List<TaskBackup>?,
        val places: List<Place>?,
        val tags: List<TagData>?,
        val filters: List<Filter>?,
        val googleTaskAccounts: List<GoogleTaskAccount>?,
        val googleTaskLists: List<GoogleTaskList>?,
        val caldavAccounts: List<CaldavAccount>?,
        val caldavCalendars: List<CaldavCalendar>?,
        val taskListMetadata: List<TaskListMetadata>?,
        val intPrefs: Map<String, Integer>?,
        val longPrefs: Map<String, java.lang.Long>?,
        val stringPrefs: Map<String, String>?,
        val boolPrefs: Map<String, java.lang.Boolean>?,
        val setPrefs: Map<String, java.util.Set<*>>?,
) {
    class TaskBackup(
            val task: Task,
            val alarms: List<Alarm>,
            val geofences: List<Geofence>?,
            val tags: List<Tag>,
            val google: List<GoogleTask>,
            val comments: List<UserActivity>,
            val attachments: List<TaskAttachment>?,
            val caldavTasks: List<CaldavTask>?,
            val vtodo: String?,
    ) {
        val locations: List<LegacyLocation> = emptyList()
    }
}