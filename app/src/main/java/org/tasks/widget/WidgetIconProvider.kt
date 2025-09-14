package org.tasks.widget

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.sizeDp
import org.tasks.BuildConfig
import org.tasks.icons.OutlinedGoogleMaterial
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class WidgetIconProvider : ContentProvider() {

    override fun onCreate() = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        if (mode != "r") {
            throw SecurityException("Only read access allowed")
        }

        return try {
            val segments = uri.pathSegments
            if (segments.size != 2) return null

            val iconName = segments[1]

            if (!iconName.matches(Regex("^[a-zA-Z0-9_]+$"))) return null

            val cacheFile = getCacheFile(iconName)

            if (!cacheFile.exists()) {
                generateIcon(cacheFile, iconName)
            }

            if (cacheFile.exists()) {
                ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to open icon file for URI: $uri")
            null
        }
    }

    private fun generateIcon(file: File, iconName: String) {
        try {
            val icon = OutlinedGoogleMaterial.getIcon("gmo_$iconName")
            val context = context ?: return

            val drawable = IconicsDrawable(context, icon).apply {
                this.sizeDp = 24
            }

            val bitmap = createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(1),
                drawable.intrinsicHeight.coerceAtLeast(1)
            )

            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            file.parentFile?.mkdirs()
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate icon: $iconName")
            file.delete()
        }
    }

    private fun getCacheFile(iconName: String): File {
        val context = context ?: throw IllegalStateException("Context is null")
        val cacheDir = File(context.cacheDir, "widget_icons")
        cacheDir.mkdirs()
        return File(cacheDir, "${iconName}.png")
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun getType(uri: Uri): String = "image/png"

    companion object {
        const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.widgeticons"

        fun getIconUri(iconName: String): Uri {
            return "content://$AUTHORITY/icon/$iconName".toUri()
        }
    }
}
