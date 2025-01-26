package org.tasks.caldav

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VtodoCache @Inject constructor(
    private val caldavDao: CaldavDao,
    private val fileStorage: FileStorage,
) {
    suspend fun move(from: CaldavCalendar, to: CaldavCalendar, task: CaldavTask) =
        withContext(Dispatchers.IO) {
            val source =
                fileStorage.getFile(from.account, from.uuid, task.obj)
            if (source?.exists() != true) {
                return@withContext
            }
            val target =
                fileStorage.getFile(to.account, to.uuid)
                    ?.apply { mkdirs() }
                    ?.let { File(it, task.obj!!) }
                    ?: return@withContext
            source.copyTo(target, overwrite = true)
            val deleted = source.delete()
            Timber.d("Moved $source to $target [success=${deleted}]")
        }

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
                    ?.apply { mkdirs() }
                    ?: return@withContext
            fileStorage.write(File(directory, `object`), vtodo)
        }
    }

    suspend fun delete(calendar: CaldavCalendar, tasks: List<CaldavTask>) {
        tasks.forEach { delete(calendar, it) }
    }

    suspend fun delete(calendar: CaldavCalendar, caldavTask: CaldavTask) = withContext(Dispatchers.IO) {
        fileStorage.getFile(calendar.account, caldavTask.calendar, caldavTask.obj)?.let {
            val deleted = it.delete()
            Timber.d("Deleting $it [success=$deleted]")
        }
    }

    suspend fun delete(calendar: CaldavCalendar) = withContext(Dispatchers.IO) {
        fileStorage.getFile(calendar.account, calendar.uuid)?.let {
            val deleted = it.deleteRecursively()
            Timber.d("Deleting $it [success=$deleted]")
        }
    }

    suspend fun delete(account: CaldavAccount) = withContext(Dispatchers.IO) {
        fileStorage.getFile(account.uuid)?.let {
            val deleted = it.deleteRecursively()
            Timber.d("Deleting $it [success=$deleted]")
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        fileStorage.getFile()?.let {
            val deleted = it.deleteRecursively()
            Timber.d("Deleting $it [success=$deleted]")
        }
    }
}
