package org.tasks.drive

import android.content.Context
import android.net.Uri
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.todoroo.astrid.gtasks.api.HttpCredentialsAdapter
import com.todoroo.astrid.gtasks.api.HttpNotFoundException
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.DebugNetworkInterceptor
import org.tasks.backup.BackupConstants
import org.tasks.files.FileHelper
import org.tasks.googleapis.BaseInvoker
import org.tasks.preferences.Preferences
import java.io.IOException

class DriveInvoker(
        @param:ApplicationContext private val context: Context,
        preferences: Preferences,
        credentialsAdapter: HttpCredentialsAdapter,
        interceptor: DebugNetworkInterceptor
) : BaseInvoker(credentialsAdapter, preferences, interceptor) {
    private val service =
            Drive.Builder(NetHttpTransport(), GsonFactory(), credentialsAdapter)
                    .setApplicationName(APP_NAME)
                    .build()

    @Throws(IOException::class)
    suspend fun getFile(folderId: String?): File? = try {
        execute(service.files()[folderId].setFields("id, trashed"))
    } catch (ignored: HttpNotFoundException) {
        null
    }

    @Throws(IOException::class)
    suspend fun delete(file: File) {
        try {
            execute(service.files().delete(file.id))
        } catch (ignored: HttpNotFoundException) {}
    }

    @Throws(IOException::class)
    suspend fun getFilesByPrefix(folderId: String?, vararg prefix: String?): List<File> {
        val namePredicate = prefix.joinToString(" or ") { "name contains '$it'" }
        val query = String.format(
                "'%s' in parents and ($namePredicate) and trashed = false and mimeType != '%s'",
                folderId, prefix, MIME_FOLDER)
        return execute(
                service
                        .files()
                        .list()
                        .setQ(query)
                        .setSpaces("drive")
                        .setFields("files(id, name, modifiedTime)"))
                ?.files
                ?.filter { BackupConstants.isBackupFile(it.name) }
                ?.sortedWith(DRIVE_FILE_COMPARATOR)
                ?: emptyList()
    }

    @Throws(IOException::class)
    suspend fun createFolder(name: String?): File? {
        val folder = File().setName(name).setMimeType("application/vnd.google-apps.folder")
        return execute(service.files().create(folder).setFields("id"))
    }

    @Throws(IOException::class)
    suspend fun createFile(folderId: String, uri: Uri?): File? {
        val mime = FileHelper.getMimeType(context, uri!!)
        val metadata = File()
                .setParents(listOf(folderId))
                .setMimeType(mime)
                .setName(FileHelper.getFilename(context, uri))
        val content = InputStreamContent(mime, context.contentResolver.openInputStream(uri))
        return execute(service.files().create(metadata, content))
    }

    companion object {
        private const val MIME_FOLDER = "application/vnd.google-apps.folder"
        private val DRIVE_FILE_COMPARATOR = Comparator<File> { f1, f2 ->
            BackupConstants.getTimestamp(f2)!!.compareTo(BackupConstants.getTimestamp(f1)!!)
        }
    }
}