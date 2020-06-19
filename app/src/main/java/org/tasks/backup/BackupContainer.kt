package org.tasks.backup

import com.todoroo.astrid.data.Task
import org.tasks.backup.TasksJsonImporter.LegacyLocation
import org.tasks.data.*

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
        val intPrefs: Map<String, Int>?,
        val longPrefs: Map<String, Long>?,
        val stringPrefs: Map<String, String>?,
        val boolPrefs: Map<String, Boolean>?) {

    class TaskBackup(
            val task: Task,
            val alarms: List<Alarm>,
            val geofences: List<Geofence>?,
            val tags: List<Tag>,
            val google: List<GoogleTask>,
            val comments: List<UserActivity>,
            val attachments: List<TaskAttachment>?,
            val caldavTasks: List<CaldavTask>?) {

        val locations: List<LegacyLocation> = emptyList()
    }
}