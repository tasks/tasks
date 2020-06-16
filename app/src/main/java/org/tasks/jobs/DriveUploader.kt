package org.tasks.jobs

import android.content.Context
import android.net.Uri
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.Data
import androidx.work.WorkerParameters
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.drive.model.File
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.analytics.Firebase
import org.tasks.drive.DriveInvoker
import org.tasks.injection.InjectingWorker
import org.tasks.preferences.Preferences
import timber.log.Timber
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.*
import javax.net.ssl.SSLException

class DriveUploader @WorkerInject constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        firebase: Firebase,
        private val drive: DriveInvoker,
        private val preferences: Preferences) : InjectingWorker(context, workerParams, firebase) {

    override fun run(): Result {
        val inputData = inputData
        val uri = Uri.parse(inputData.getString(EXTRA_URI))
        return try {
            val folder = folder
            preferences.setString(R.string.p_google_drive_backup_folder, folder.id)
            drive.createFile(folder.id, uri)
            if (inputData.getBoolean(EXTRA_PURGE, false)) {
                val files = drive.getFilesByPrefix(folder.id, "auto.")
                for (file in getDeleteList(files)) {
                    try {
                        drive.delete(file)
                    } catch (e: GoogleJsonResponseException) {
                        if (e.statusCode == 404) {
                            Timber.e(e)
                        } else {
                            throw e
                        }
                    }
                }
            }
            Result.success()
        } catch (e: SocketTimeoutException) {
            Timber.e(e)
            Result.retry()
        } catch (e: SSLException) {
            Timber.e(e)
            Result.retry()
        } catch (e: ConnectException) {
            Timber.e(e)
            Result.retry()
        } catch (e: UnknownHostException) {
            Timber.e(e)
            Result.retry()
        } catch (e: IOException) {
            firebase.reportException(e)
            Result.failure()
        }
    }

    @get:Throws(IOException::class)
    private val folder: File
        get() {
            val folderId = preferences.getStringValue(R.string.p_google_drive_backup_folder)
            var file: File? = null
            if (!isNullOrEmpty(folderId)) {
                try {
                    file = drive.getFile(folderId)
                } catch (e: GoogleJsonResponseException) {
                    if (e.statusCode != 404) {
                        throw e
                    }
                }
            }
            return if (file == null || file.trashed) drive.createFolder(FOLDER_NAME) else file
        }

    companion object {
        private const val FOLDER_NAME = "Tasks Backups"
        private const val EXTRA_URI = "extra_uri"
        private const val EXTRA_PURGE = "extra_purge"
        private val DRIVE_FILE_COMPARATOR = Comparator<File> { f1, f2 ->
            f2.modifiedTime.value.compareTo(f1.modifiedTime.value)
        }

        fun getInputData(uri: Uri, purge: Boolean) =
                Data.Builder()
                    .putString(EXTRA_URI, uri.toString())
                    .putBoolean(EXTRA_PURGE, purge)
                    .build()

        private fun getDeleteList(files: List<File>) =
                files.sortedWith(DRIVE_FILE_COMPARATOR).drop(BackupWork.DAYS_TO_KEEP_BACKUP)
    }
}