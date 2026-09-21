package org.tasks.caldav

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask

private const val TAG_METADATA_FILE = "tag-metadata.json"

class VtodoCache(
    private val caldavDao: CaldavDao,
    private val fileStorage: FileStorage,
) {
    suspend fun getVtodo(caldavTask: CaldavTask?): String? {
        if (caldavTask == null) {
            return null
        }
        val calendar = caldavDao.getCalendar(caldavTask.calendar!!) ?: return null
        return getVtodo(calendar, caldavTask)
    }

    suspend fun getVtodo(calendar: CaldavCalendar?, caldavTask: CaldavTask?): String? {
        val file = fileStorage.getFile(
            calendar?.account,
            caldavTask?.calendar,
            caldavTask?.obj
        )
        return fileStorage.read(file)
    }

    suspend fun putVtodo(calendar: CaldavCalendar, caldavTask: CaldavTask, vtodo: String?) {
        val `object` = caldavTask.obj?.takeIf { it.isNotBlank() } ?: return
        withContext(Dispatchers.IO) {
            val directory =
                fileStorage
                    .getFile(calendar.account, caldavTask.calendar)
                    ?.let { fileStorage.mkdirs(it) }
                    ?: return@withContext
            fileStorage.write(directory / `object`, vtodo)
        }
    }

    suspend fun delete(tasks: List<Long>) {
        val calendars = caldavDao.getCalendars(tasks).associateBy { it.uuid }
        caldavDao.getTasks(tasks).forEach { caldavTask ->
            calendars[caldavTask.calendar]?.let { delete(it, caldavTask) }
        }
    }

    suspend fun delete(calendar: CaldavCalendar, caldavTask: CaldavTask) = withContext(Dispatchers.IO) {
        fileStorage.getFile(calendar.account, caldavTask.calendar, caldavTask.obj)?.let {
            val deleted = fileStorage.delete(it)
            Logger.d("VtodoCache") { "Deleting $it [success=$deleted]" }
        }
    }

    suspend fun delete(calendar: CaldavCalendar) = withContext(Dispatchers.IO) {
        fileStorage.getFile(calendar.account, calendar.uuid)?.let {
            val deleted = fileStorage.deleteRecursively(it)
            Logger.d("VtodoCache") { "Deleting $it [success=$deleted]" }
        }
    }

    suspend fun delete(account: CaldavAccount) = withContext(Dispatchers.IO) {
        fileStorage.getFile(account.uuid)?.let {
            val deleted = fileStorage.deleteRecursively(it)
            Logger.d("VtodoCache") { "Deleting $it [success=$deleted]" }
        }
    }

    suspend fun getTagMetadata(account: CaldavAccount): String? =
        fileStorage.read(fileStorage.getFile(account.uuid, TAG_METADATA_FILE))

    suspend fun putTagMetadata(account: CaldavAccount, data: String?) = withContext(Dispatchers.IO) {
        val directory = fileStorage.getFile(account.uuid)?.let { fileStorage.mkdirs(it) } ?: return@withContext
        fileStorage.write(directory / TAG_METADATA_FILE, data)
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        fileStorage.getFile()?.let {
            val deleted = fileStorage.deleteRecursively(it)
            Logger.d("VtodoCache") { "Deleting $it [success=$deleted]" }
        }
    }
}
