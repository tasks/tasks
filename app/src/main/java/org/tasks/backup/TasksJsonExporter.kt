package org.tasks.backup

import android.app.Activity
import android.app.ProgressDialog
import android.app.backup.BackupManager
import android.content.Context
import android.net.Uri
import android.os.Handler
import com.google.common.io.Files
import com.todoroo.andlib.utility.DialogUtilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.caldav.VtodoCache
import org.tasks.data.*
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
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.extensions.Context.toast
import org.tasks.files.FileHelper
import org.tasks.jobs.WorkManager
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.io.Writer
import java.util.Set
import javax.inject.Inject

class TasksJsonExporter @Inject constructor(
    private val tagDataDao: TagDataDao,
    private val taskDao: TaskDao,
    private val userActivityDao: UserActivityDao,
    private val preferences: Preferences,
    private val alarmDao: AlarmDao,
    private val locationDao: LocationDao,
    private val tagDao: TagDao,
    private val filterDao: FilterDao,
    private val taskAttachmentDao: TaskAttachmentDao,
    private val caldavDao: CaldavDao,
    private val workManager: WorkManager,
    private val taskListMetadataDao: TaskListMetadataDao,
    private val vtodoCache: VtodoCache,
) {
    private var context: Context? = null
    private var exportCount = 0
    private var progressDialog: ProgressDialog? = null
    private var handler: Handler? = null

    private fun post(runnable: () -> Unit) = handler?.post(runnable)

    private fun setProgress(taskNumber: Int, total: Int) = post {
        progressDialog?.max = total
        progressDialog?.progress = taskNumber
    }

    suspend fun exportTasks(context: Context?, exportType: ExportType, progressDialog: ProgressDialog?) {
        this.context = context
        exportCount = 0
        this.progressDialog = progressDialog
        if (exportType == ExportType.EXPORT_TYPE_MANUAL) {
            handler = Handler()
        }
        runBackup(exportType)
    }

    private suspend fun runBackup(exportType: ExportType) {
        try {
            val filename = getFileName(exportType)
            val tasks = taskDao.getAllTaskIds()
            val file = File(String.format("%s/%s", context!!.filesDir, BackupConstants.INTERNAL_BACKUP))
            file.delete()
            file.createNewFile()
            val internalStorageBackup = Uri.fromFile(file)
            val os = context!!.contentResolver.openOutputStream(internalStorageBackup)
            doTasksExport(os, tasks)
            os!!.close()
            val externalStorageBackup = FileHelper.newFile(
                    context!!,
                    preferences.backupDirectory!!,
                    MIME,
                    Files.getNameWithoutExtension(filename),
                    EXTENSION)
            FileHelper.copyStream(context!!, internalStorageBackup, externalStorageBackup)
            workManager.scheduleDriveUpload(externalStorageBackup, exportType == ExportType.EXPORT_TYPE_SERVICE)
            BackupManager(context).dataChanged()
            if (exportType == ExportType.EXPORT_TYPE_MANUAL) {
                onFinishExport(filename)
            }
        } catch (e: IOException) {
            Timber.e(e)
        } finally {
            post {
                if (progressDialog != null && progressDialog!!.isShowing
                        && context is Activity) {
                    DialogUtilities.dismissDialog(context as Activity?, progressDialog)
                }
            }
        }
    }

    suspend fun doSettingsExport(os: OutputStream?) = withContext(Dispatchers.IO) {
        val writer = os!!.bufferedWriter()
        with (JsonWriter(writer)) {
            write("{")
            write("version", BuildConfig.VERSION_CODE)
            write("timestamp", currentTimeMillis())
            write("\"data\":{")
            writePreferences()
            write("}")
            write("}")
        }
        writer.flush()
    }

    @Throws(IOException::class)
    private suspend fun doTasksExport(os: OutputStream?, taskIds: List<Long>) = withContext(Dispatchers.IO) {
        val writer = os!!.bufferedWriter()
        with (JsonWriter(writer)) {
            write("{")
            write("version", BuildConfig.VERSION_CODE)
            write("timestamp", currentTimeMillis())
            write("\"data\":{")
            write("\"tasks\":[")
            taskIds.forEachIndexed { index, id ->
                setProgress(index, taskIds.size)
                write("{")
                write("task", taskDao.fetch(id)!!)
                write("alarms", alarmDao.getAlarms(id))
                write("geofences", locationDao.getGeofencesForTask(id))
                write("tags", tagDao.getTagsForTask(id))
                write("comments", userActivityDao.getComments(id))
                write("attachments", taskAttachmentDao.getAttachmentsForTask(id))
                val caldavTasks = caldavDao.getTasks(id)
                vtodoCache
                    .getVtodo(caldavTasks.firstOrNull { !it.isDeleted() })
                    ?.let { write("vtodo", it) }
                write("caldavTasks", caldavTasks, lastItem = true)
                write("}")
                if (index < taskIds.size - 1) write(",")
            }
            write("],")
            write("places", locationDao.getPlaces())
            write("tags", tagDataDao.getAll())
            write("filters", filterDao.getFilters())
            write("caldavAccounts", caldavDao.getAccounts())
            write("caldavCalendars", caldavDao.getCalendars())
            write("taskListMetadata", taskListMetadataDao.getAll())
            write("taskAttachments", taskAttachmentDao.getAttachments())
            writePreferences()
            write("}")
            write("}")
        }
        writer.close()
        os.close()
        exportCount = taskIds.size
    }

    private fun JsonWriter.writePreferences() {
        write("intPrefs", preferences.getPrefs(Integer::class.java))
        write("longPrefs", preferences.getPrefs(java.lang.Long::class.java))
        write("stringPrefs", preferences.getPrefs(String::class.java))
        write("boolPrefs", preferences.getPrefs(java.lang.Boolean::class.java))
        write("setPrefs", preferences.getPrefs(Set::class.java) as Map<String, Set<String>>, lastItem = true)
    }

    private fun onFinishExport(outputFile: String) = post {
        context?.toast(
            R.string.export_toast,
            context!!
                .resources
                .getQuantityString(R.plurals.Ntasks, exportCount, exportCount),
            outputFile
        )
    }


    private fun getFileName(type: ExportType): String =
        when (type) {
            ExportType.EXPORT_TYPE_SERVICE -> String.format(BackupConstants.BACKUP_FILE_NAME, dateForExport)
            ExportType.EXPORT_TYPE_MANUAL -> String.format(BackupConstants.EXPORT_FILE_NAME, dateForExport)
        }

    enum class ExportType {
        EXPORT_TYPE_SERVICE, EXPORT_TYPE_MANUAL
    }

    companion object {
        private const val MIME = "application/json"
        private const val EXTENSION = ".json"
        private val dateForExport: String
            get() = newDateTime().toString("yyyyMMdd'T'HHmm")

        class JsonWriter(val writer: Writer, val json: Json = Json) {
            fun write(data: String) = writer.write(data)

            inline fun <reified T> write(key: String, value: @Serializable T, lastItem: Boolean = false) where T : Any =
                writer.write("\"$key\":${json.encodeToString(value)}${if (lastItem) "" else ","}")
        }
    }
}