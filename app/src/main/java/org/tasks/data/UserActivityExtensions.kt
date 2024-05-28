package org.tasks.data

import android.net.Uri
import org.json.JSONException
import org.json.JSONObject
import org.tasks.data.entity.UserActivity
import timber.log.Timber
import java.io.File

fun UserActivity.setPicture(uri: Uri?) {
    picture = uri?.toString()
}

val UserActivity.pictureUri: Uri?
    get() = if (picture.isNullOrBlank()) null else Uri.parse(picture)

fun UserActivity.convertPictureUri() {
    setPicture(getLegacyPictureUri(picture))
}

private fun getLegacyPictureUri(value: String?): Uri? {
    return try {
        if (value.isNullOrBlank()) {
            return null
        }
        if (value.contains("uri") || value.contains("path")) {
            val json = JSONObject(value)
            if (json.has("uri")) {
                return Uri.parse(json.getString("uri"))
            }
            if (json.has("path")) {
                val path = json.getString("path")
                return Uri.fromFile(File(path))
            }
        }
        null
    } catch (e: JSONException) {
        Timber.e(e)
        null
    }
}
