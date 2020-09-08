package org.tasks.drive

import android.content.Context
import android.net.Uri
import com.google.api.client.http.HttpResponseException
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.GenericJson
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveRequest
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.gtasks.api.HttpCredentialsAdapter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.BuildConfig
import org.tasks.DebugNetworkInterceptor
import org.tasks.R
import org.tasks.files.FileHelper
import org.tasks.preferences.Preferences
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class DriveInvoker @Inject constructor(
        @param:ApplicationContext private val context: Context,
        private val preferences: Preferences,
        private val credentialsAdapter: HttpCredentialsAdapter,
        private val interceptor: DebugNetworkInterceptor) {
    private val service =
            Drive
                    .Builder(NetHttpTransport(), JacksonFactory(), credentialsAdapter)
                    .setApplicationName(String.format("Tasks/%s", BuildConfig.VERSION_NAME))
                    .build()

    @Throws(IOException::class)
    suspend fun getFile(folderId: String?): File? {
        return execute(service.files()[folderId].setFields("id, trashed"))
    }

    @Throws(IOException::class)
    suspend fun delete(file: File) {
        execute(service.files().delete(file.id))
    }

    @Throws(IOException::class)
    suspend fun getFilesByPrefix(folderId: String?, prefix: String?): List<File> {
        val query = String.format(
                "'%s' in parents and name contains '%s' and trashed = false and mimeType != '%s'",
                folderId, prefix, MIME_FOLDER)
        return execute(
                service
                        .files()
                        .list()
                        .setQ(query)
                        .setSpaces("drive")
                        .setFields("files(id, modifiedTime)"))
                ?.files
                ?: emptyList()
    }

    @Throws(IOException::class)
    suspend fun createFolder(name: String?): File? {
        val folder = File().setName(name).setMimeType("application/vnd.google-apps.folder")
        return execute(service.files().create(folder).setFields("id"))
    }

    @Throws(IOException::class)
    suspend fun createFile(folderId: String, uri: Uri?) {
        val mime = FileHelper.getMimeType(context, uri)
        val metadata = File()
                .setParents(listOf(folderId))
                .setMimeType(mime)
                .setName(FileHelper.getFilename(context, uri))
        val content = InputStreamContent(mime, context.contentResolver.openInputStream(uri!!))
        execute(service.files().create(metadata, content))
    }

    @Synchronized
    @Throws(IOException::class)
    private suspend fun <T> execute(request: DriveRequest<T>): T? {
        return execute(request, false)
    }

    @Synchronized
    @Throws(IOException::class)
    private suspend fun <T> execute(
            request: DriveRequest<T>,
            retry: Boolean
    ): T? = withContext(Dispatchers.IO) {
        val account = preferences.getStringValue(R.string.p_google_drive_backup_account)
        credentialsAdapter.checkToken(account, DriveScopes.DRIVE_FILE)
        Timber.d("%s request: %s", caller, request)
        val response: T?
        response = try {
            if (preferences.isFlipperEnabled) {
                val start = DateUtilities.now()
                val httpResponse = request.executeUnparsed()
                interceptor.report(httpResponse, request.responseClass, start, DateUtilities.now())
            } else {
                request.execute()
            }
        } catch (e: HttpResponseException) {
            return@withContext if (e.statusCode == 401 && !retry) {
                credentialsAdapter.invalidateToken()
                execute(request, true)
            } else {
                throw e
            }
        }
        Timber.d("%s response: %s", caller, prettyPrint(response))
        return@withContext response
    }

    @Throws(IOException::class)
    private fun <T> prettyPrint(`object`: T?): Any? {
        if (BuildConfig.DEBUG) {
            if (`object` is GenericJson) {
                return (`object` as GenericJson).toPrettyString()
            }
        }
        return `object`
    }

    private val caller: String
        get() {
            if (BuildConfig.DEBUG) {
                try {
                    return Thread.currentThread().stackTrace[4].methodName
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
            return ""
        }

    companion object {
        private const val MIME_FOLDER = "application/vnd.google-apps.folder"
    }
}