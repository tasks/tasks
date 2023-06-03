package org.tasks.caldav

import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavCalendar
import org.tasks.data.CaldavDao
import org.tasks.data.CaldavTask
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VtodoCache @Inject constructor(
    private val caldavDao: CaldavDao,
    private val fileStorage: FileStorage,
) {
    fun move(from: CaldavCalendar, to: CaldavCalendar, task: CaldavTask) {
        val source =
            fileStorage.getFile(from.account, from.uuid, task.`object`)
        if (source?.exists() != true) {
            return
        }
        val target =
            fileStorage.getFile(to.account, to.uuid)
                ?.apply { mkdirs() }
                ?.let { File(it, task.`object`!!) }
                ?: return
        source.copyTo(target, overwrite = true)
        source.delete()
    }

    suspend fun getVtodo(caldavTask: CaldavTask?): String? {
        if (caldavTask == null) {
            return null
        }
        val calendar = caldavDao.getCalendar(caldavTask.calendar!!) ?: return null
        return getVtodo(calendar, caldavTask)
    }

    fun getVtodo(calendar: CaldavCalendar?, caldavTask: CaldavTask?): String? {
        val file = fileStorage.getFile(
            calendar?.account,
            caldavTask?.calendar,
            caldavTask?.`object`
        )
        return fileStorage.read(file)
    }

    fun putVtodo(calendar: CaldavCalendar, caldavTask: CaldavTask, vtodo: String?) {
        val `object` = caldavTask.`object`?.takeIf { it.isNotBlank() } ?: return
        val directory =
            fileStorage
                .getFile(calendar.account, caldavTask.calendar)
                ?.apply { mkdirs() }
                ?: return
        fileStorage.write(File(directory, `object`), vtodo)
    }

    suspend fun delete(taskIds: List<Long>) {
        val tasks = caldavDao.getTasks(taskIds).groupBy { it.calendar!! }
        tasks.forEach { (c, t) ->
            val calendar = caldavDao.getCalendar(c) ?: return@forEach
            t.forEach { delete(calendar, it) }
        }
    }

    fun delete(calendar: CaldavCalendar, caldavTask: CaldavTask) {
        fileStorage
            .getFile(calendar.account!!, caldavTask.calendar!!, caldavTask.`object`!!)
            ?.delete()
    }

    fun delete(calendar: CaldavCalendar) =
        fileStorage.getFile(calendar.account!!, calendar.uuid!!)?.deleteRecursively()

    fun delete(account: CaldavAccount) =
        fileStorage.getFile(account.uuid!!)?.deleteRecursively()

    fun clear() =
        fileStorage.getFile()?.deleteRecursively()
}