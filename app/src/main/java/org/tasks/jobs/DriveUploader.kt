package org.tasks.jobs

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.drive.model.File
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.analytics.Firebase
import org.tasks.backup.BackupConstants
import org.tasks.googleapis.InvokerFactory
import org.tasks.injection.BaseWorker
import org.tasks.preferences.Preferences
import timber.log.Timber
import java.io.FileNotFoundException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

@HiltWorker
class DriveUploader @AssistedInject constructor(
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
                        .forEach { drive.delete(it) }
            }
            Result.success()
        } catch (e: FileNotFoundException) {
            fail(e)
        } catch (e: SocketException) {
            retry(e)
        } catch (e: SocketTimeoutException) {
            retry(e)
        } catch (e: SSLException) {
            retry(e)
        } catch (e: ConnectException) {
            retry(e)
        } catch (e: UnknownHostException) {
            retry(e)
        } catch (e: GoogleJsonResponseException) {
            when (e.statusCode) {
                401, 403 -> fail(e)
                503 -> retry(e)
                else -> retry(e, report = true)
            }
        } catch (e: IOException) {
            fail(e, report = true)
        }
    }

    private fun fail(e: Throwable, report: Boolean = false): Result {
        if (report) {
            firebase.reportException(e)
        } else {
            Timber.e(e)
        }
        return Result.failure()
    }

    private fun retry(e: Throwable, report: Boolean = false): Result {
        if (report) {
            firebase.reportException(e)
        } else {
            Timber.e(e)
        }
        return Result.retry()
    }

    @Throws(IOException::class)
    private suspend fun getFolder(): File? {
        val folderId = preferences.getStringValue(R.string.p_google_drive_backup_folder)
        var file: File? = null
        if (!isNullOrEmpty(folderId)) {
            file = drive.getFile(folderId)
        }
        return if (file == null || file.trashed) drive.createFolder(FOLDER_NAME) else file
    }

    companion object {
        private const val FOLDER_NAME = "Tasks Backups"
        const val EXTRA_URI = "extra_uri"
        const val EXTRA_PURGE = "extra_purge"
    }
}