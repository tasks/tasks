package org.tasks.backup

import android.app.Activity
import android.app.ProgressDialog
import android.app.backup.BackupManager
import android.content.Context
import android.net.Uri
import android.os.Handler
import com.google.common.io.Files
import com.todoroo.andlib.utility.DialogUtilities
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.backup.BackupContainer.TaskBackup
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
import org.tasks.data.entity.Task
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
import java.io.OutputStreamWriter
import java.nio.charset.Charset
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
            val tasks = taskDao.getAll()
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

    @Throws(IOException::class)
    private suspend fun doTasksExport(os: OutputStream?, tasks: List<Task>) {
        val taskBackups: MutableList<TaskBackup> = ArrayList()
        for (task in tasks) {
            setProgress(taskBackups.size, tasks.size)
            val taskId = task.id
            val caldavTasks = caldavDao.getTasks(taskId)
            taskBackups.add(
                    TaskBackup(
                        task = task,
                        alarms = alarmDao.getAlarms(taskId),
                        geofences = locationDao.getGeofencesForTask(taskId),
                        tags = tagDao.getTagsForTask(taskId),
                        comments = userActivityDao.getComments(taskId),
                        attachments = taskAttachmentDao.getAttachmentsForTask(taskId),
                        caldavTasks = caldavTasks,
                        vtodo = vtodoCache.getVtodo(caldavTasks.firstOrNull { !it.isDeleted() })
                    )
            )
        }
        val data = JsonObject(
            mapOf(
                "version" to JsonPrimitive(BuildConfig.VERSION_CODE),
                "timestamp" to JsonPrimitive(currentTimeMillis()),
                "data" to Json.encodeToJsonElement(
                    BackupContainer(
                        taskBackups,
                        locationDao.getPlaces(),
                        tagDataDao.getAll(),
                        filterDao.getFilters(),
                        caldavDao.getAccounts(),
                        caldavDao.getCalendars(),
                        taskListMetadataDao.getAll(),
                        taskAttachmentDao.getAttachments(),
                        preferences.getPrefs(Integer::class.java),
                        preferences.getPrefs(java.lang.Long::class.java),
                        preferences.getPrefs(String::class.java),
                        preferences.getPrefs(java.lang.Boolean::class.java),
                        preferences.getPrefs(java.util.Set::class.java) as Map<String, java.util.Set<String>>,
                    )
                )
            )
        )
        val out = OutputStreamWriter(os, UTF_8)
        val json = if (BuildConfig.DEBUG) Json { prettyPrint = true } else Json
        out.write(json.encodeToString(data))
        out.close()
        exportCount = taskBackups.size
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
        val UTF_8: Charset = Charset.forName("UTF-8")
        private const val MIME = "application/json"
        private const val EXTENSION = ".json"
        private val dateForExport: String
            get() = newDateTime().toString("yyyyMMdd'T'HHmm")
    }
}