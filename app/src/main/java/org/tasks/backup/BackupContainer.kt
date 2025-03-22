package org.tasks.backup

import kotlinx.serialization.Serializable
import org.tasks.backup.TasksJsonImporter.LegacyLocation
import org.tasks.data.GoogleTask
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Attachment
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.Tag
import org.tasks.data.entity.Task
import org.tasks.data.entity.UserActivity

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