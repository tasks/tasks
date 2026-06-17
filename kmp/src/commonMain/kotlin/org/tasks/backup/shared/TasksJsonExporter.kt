@file:Suppress("LongParameterList")

package org.tasks.backup.shared

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.tasks.TasksBuildConfig
import org.tasks.caldav.VtodoCache
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.FilterDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskAttachmentDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.dao.TaskListMetadataDao
import org.tasks.data.dao.UserActivityDao
import org.tasks.time.DateTimeUtils2
import java.io.OutputStream

class TasksJsonExporter(
    private val taskDao: TaskDao,
    private val alarmDao: AlarmDao,
    private val locationDao: LocationDao,
    private val tagDao: TagDao,
    private val userActivityDao: UserActivityDao,
    private val taskAttachmentDao: TaskAttachmentDao,
    private val caldavDao: CaldavDao,
    private val tagDataDao: TagDataDao,
    private val filterDao: FilterDao,
    private val taskListMetadataDao: TaskListMetadataDao,
    private val vtodoCache: VtodoCache,
    private val json: Json,
) {
    var exportCount: Int = 0
        private set

    suspend fun exportTasks(os: OutputStream) = withContext(Dispatchers.IO) {
        val taskIds = taskDao.getAllTaskIds()
        val writer = os.bufferedWriter(Charsets.UTF_8)
        writer.write("""{"version":${TasksBuildConfig.VERSION_CODE},"timestamp":${DateTimeUtils2.currentTimeMillis()},"data":{""")
        writer.write(""""tasks":[""")
        taskIds.forEachIndexed { index, id ->
            val task = taskDao.fetch(id)!!
            writer.write("{")
            writer.write("\"task\":${json.encodeToString(task)}")
            writer.write(",\"alarms\":${json.encodeToString(alarmDao.getAlarms(id))}")
            writer.write(",\"geofences\":${json.encodeToString(locationDao.getGeofencesForTask(id))}")
            writer.write(",\"tags\":${json.encodeToString(tagDao.getTagsForTask(id))}")
            writer.write(",\"comments\":${json.encodeToString(userActivityDao.getComments(id))}")
            writer.write(",\"attachments\":${json.encodeToString(taskAttachmentDao.getAttachmentsForTask(id))}")
            val caldavTasks = caldavDao.getTasks(id)
            writer.write(",\"caldavTasks\":${json.encodeToString(caldavTasks)}")
            vtodoCache
                .getVtodo(caldavTasks.firstOrNull { !it.isDeleted() })
                ?.let { writer.write(",\"vtodo\":${json.encodeToString(it)}") }
            writer.write("}")
            if (index < taskIds.size - 1) writer.write(",")
        }
        writer.write("],")
        writer.write("\"places\":${json.encodeToString(locationDao.getPlaces())}")
        writer.write(",\"tags\":${json.encodeToString(tagDataDao.getAll())}")
        writer.write(",\"filters\":${json.encodeToString(filterDao.getFilters())}")
        writer.write(",\"caldavAccounts\":${json.encodeToString(caldavDao.getAccounts())}")
        writer.write(",\"caldavCalendars\":${json.encodeToString(caldavDao.getCalendars())}")
        writer.write(",\"taskListMetadata\":${json.encodeToString(taskListMetadataDao.getAll())}")
        writer.write(",\"taskAttachments\":${json.encodeToString(taskAttachmentDao.getAttachments())}")
        writer.write("}}")
        writer.close()
        os.close()
        exportCount = taskIds.size
    }
}
