package org.tasks.files

import android.annotation.TargetApi
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import com.google.common.collect.Iterables
import com.google.common.io.ByteStreams
import com.google.common.io.Files
import com.todoroo.astrid.utility.Constants
import org.tasks.Strings.isNullOrEmpty
import org.tasks.extensions.Context.safeStartActivity
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*

object FileHelper {
    fun newFilePickerIntent(
        activity: Activity?,
        initial: Uri?,
        allowMultiple: Boolean = false,
        vararg mimeTypes: String?,
    ): Intent =
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                putExtra("android.content.extra.SHOW_ADVANCED", true)
                putExtra("android.content.extra.FANCY", true)
                putExtra("android.content.extra.SHOW_FILESIZE", true)
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)
                addCategory(Intent.CATEGORY_OPENABLE)
                setInitialUri(activity, this, initial)

                if (mimeTypes.size == 1) {
                    type = mimeTypes[0]
                } else {
                    type = "*/*"
                    if (mimeTypes.size > 1) {
                        putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                    }
                }
            }

    fun newDirectoryPicker(fragment: Fragment, rc: Int, initial: Uri?) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                            or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            or Intent.FLAG_GRANT_READ_URI_PERMISSION
                            or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
            putExtra("android.content.extra.SHOW_ADVANCED", true)
            putExtra("android.content.extra.FANCY", true)
            putExtra("android.content.extra.SHOW_FILESIZE", true)
            setInitialUri(fragment.context, this, initial)
        }
        fragment.startActivityForResult(intent, rc)
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun setInitialUri(context: Context?, intent: Intent, uri: Uri?) {
        if (uri == null || uri.scheme != ContentResolver.SCHEME_CONTENT) {
            return
        }
        try {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, DocumentFile.fromTreeUri(context!!, uri)!!.uri)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun delete(context: Context?, uri: Uri?) {
        if (uri == null) {
            return
        }
        when (uri.scheme) {
            "content" -> {
                val documentFile = DocumentFile.fromSingleUri(context!!, uri)
                documentFile!!.delete()
            }
            "file" -> delete(File(uri.path))
        }
    }

    private fun delete(vararg files: File) {
        for (file in files) {
            if (file.isDirectory) {
                file.listFiles()?.let { delete(*it) }
            } else {
                file.delete()
            }
        }
    }

    fun getFilename(context: Context, uri: Uri): String? {
        when (uri.scheme) {
            ContentResolver.SCHEME_FILE -> return uri.lastPathSegment
            ContentResolver.SCHEME_CONTENT -> {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    return cursor.use {
                        it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    }
                }
            }
        }
        return null
    }

    fun getExtension(context: Context, uri: Uri): String? {
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val mimeType = context.contentResolver.getType(uri)
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            if (!isNullOrEmpty(extension)) {
                return extension
            }
        }
        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.path)
        return if (!isNullOrEmpty(extension))
            extension
        else
            Files.getFileExtension(getFilename(context, uri)!!)
    }

    fun getMimeType(context: Context, uri: Uri): String? {
        val mimeType = context.contentResolver.getType(uri)
        if (!isNullOrEmpty(mimeType)) {
            return mimeType
        }
        val extension = getExtension(context, uri)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    fun startActionView(context: Activity, uri: Uri?) {
        var uri = uri ?: return
        val mimeType = getMimeType(context, uri)
        val intent = Intent(Intent.ACTION_VIEW)
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            uri = copyToUri(context, Uri.fromFile(context.externalCacheDir), uri)
        }
        val share = FileProvider.getUriForFile(context, Constants.FILE_PROVIDER_AUTHORITY, File(uri.path))
        intent.setDataAndType(share, mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.safeStartActivity(intent)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun newFile(
            context: Context, destination: Uri, mimeType: String?, baseName: String, extension: String?): Uri {
        val filename = getNonCollidingFileName(context, destination, baseName, extension)
        return when (destination.scheme) {
            "content" -> {
                val tree = DocumentFile.fromTreeUri(context, destination)
                val f1 = tree!!.createFile(mimeType!!, filename)
                        ?: throw FileNotFoundException("Failed to create $filename")
                f1.uri
            }
            "file" -> {
                val dir = File(destination.path)
                if (!dir.exists() && !dir.mkdirs()) {
                    throw IOException("Failed to create %s" + dir.absolutePath)
                }
                val f2 = File(dir.absolutePath + File.separator + filename)
                if (f2.createNewFile()) {
                    return Uri.fromFile(f2)
                }
                throw FileNotFoundException("Failed to create $filename")
            }
            else -> throw IllegalArgumentException("Unknown URI scheme: " + destination.scheme)
        }
    }

    fun copyToUri(context: Context, destination: Uri, input: Uri): Uri {
        val filename = getFilename(context, input)
        val basename = Files.getNameWithoutExtension(filename!!)
        try {
            val output = newFile(
                context,
                destination,
                getMimeType(context, input),
                basename,
                getExtension(context, input)
            )
            copyStream(context, input, output)
            return output
        } catch (e: IOException) {
            throw IllegalStateException(e)
        }
    }

    fun copyStream(context: Context, input: Uri?, output: Uri?) {
        val contentResolver = context.contentResolver
        try {
            val inputStream = contentResolver.openInputStream(input!!)
            val outputStream = contentResolver.openOutputStream(output!!)
            ByteStreams.copy(inputStream!!, outputStream!!)
            inputStream.close()
            outputStream.close()
        } catch (e: IOException) {
            throw IllegalStateException(e)
        }
    }

    private fun getNonCollidingFileName(
            context: Context, uri: Uri, baseName: String, extension: String?): String {
        var extension = extension
        var tries = 1
        if (!extension!!.startsWith(".")) {
            extension = ".$extension"
        }
        var tempName = baseName
        when (uri.scheme) {
            ContentResolver.SCHEME_CONTENT -> {
                val dir = DocumentFile.fromTreeUri(context, uri)
                val documentFiles = listOf(*dir!!.listFiles())
                while (true) {
                    val result = tempName + extension
                    if (Iterables.any(documentFiles) { f: DocumentFile? -> f!!.name == result }) {
                        tempName = "$baseName-$tries"
                        tries++
                    } else {
                        break
                    }
                }
            }
            ContentResolver.SCHEME_FILE -> {
                var f = File(uri.path, baseName + extension)
                while (f.exists()) {
                    tempName = "$baseName-$tries" // $NON-NLS-1$
                    f = File(uri.path, tempName + extension)
                    tries++
                }
            }
        }
        return tempName + extension
    }

    fun uri2String(uri: Uri?): String {
        if (uri == null) {
            return ""
        }
        return if (uri.scheme == ContentResolver.SCHEME_FILE)
            File(uri.path).absolutePath
        else
            uri.toString()
    }
}