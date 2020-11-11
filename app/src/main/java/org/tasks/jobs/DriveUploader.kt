package org.tasks.jobs

import android.content.Context
import android.net.Uri
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.Data
import androidx.work.WorkerParameters
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.drive.model.File
import com.todoroo.astrid.backup.BackupConstants
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.analytics.Firebase
import org.tasks.googleapis.InvokerFactory
import org.tasks.injection.BaseWorker
import org.tasks.preferences.Preferences
import timber.log.Timber
import java.io.FileNotFoundException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class DriveUploader @WorkerInject constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        firebase: Firebase,
        invokers: InvokerFactory,
        private val preferences: Preferences,
        private val localBroadcastManager: LocalBroadcastManager
) : BaseWorker(context, workerParams, firebase) {
    private val drive = invokers.getDriveInvoker()

    override suspend fun run(): Result {
        val inputData = inputData
        val uri = Uri.parse(inputData.getString(EXTRA_URI))
        return try {
            val folder = getFolder() ?: return Result.failure()
            preferences.setString(R.string.p_google_drive_backup_folder, folder.id)
            drive.createFile(folder.id, uri)
                    ?.let(BackupConstants::getTimestamp)
                    ?.let { preferences.setLong(R.string.p_backups_drive_last, it) }
            localBroadcastManager.broadcastPreferenceRefresh()
            if (inputData.getBoolean(EXTRA_PURGE, false)) {
                drive
                        .getFilesByPrefix(folder.id, "auto.")
                        .drop(BackupWork.DAYS_TO_KEEP_BACKUP)
                        .forEach {
                            try {
                                drive.delete(it)
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
        } catch (e: FileNotFoundException) {
            Timber.e(e)
            Result.failure()
        } catch (e: GoogleJsonResponseException) {
            when (e.statusCode) {
                401 -> {
                    Timber.e(e)
                    Result.failure()
                }
                503 -> {
                    Timber.e(e)
                    Result.retry()
                }
                else -> {
                    firebase.reportException(e)
                    Result.retry()
                }
            }
        } catch (e: IOException) {
            firebase.reportException(e)
            Result.failure()
        }
    }

    @Throws(IOException::class)
    private suspend fun getFolder(): File? {
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

        fun getInputData(uri: Uri, purge: Boolean) =
                Data.Builder()
                    .putString(EXTRA_URI, uri.toString())
                    .putBoolean(EXTRA_PURGE, purge)
                    .build()
    }
}