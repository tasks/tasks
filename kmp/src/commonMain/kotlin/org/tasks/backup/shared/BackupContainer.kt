@file:Suppress("LongParameterList")
package org.tasks.backup.shared

import kotlinx.serialization.Serializable
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
)

@Serializable
class BackupData(
    val tasks: List<TaskBackup>,
    val places: List<Place>? = null,
    val tags: List<TagData>? = null,
    val filters: List<Filter>? = null,
    val caldavAccounts: List<CaldavAccount>? = null,
    val caldavCalendars: List<CaldavCalendar>? = null,
    val taskListMetadata: List<TaskListMetadata>? = null,
    val taskAttachments: List<TaskAttachment>? = null,
)
