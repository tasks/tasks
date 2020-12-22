package org.tasks.data

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.todoroo.andlib.data.Table
import com.todoroo.astrid.data.Task
import org.json.JSONException
import org.json.JSONObject
import org.tasks.Strings
import org.tasks.backup.XmlReader
import timber.log.Timber
import java.io.File

@Entity(tableName = "userActivity")
class UserActivity : Parcelable {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    @Transient
    var id: Long? = null

    @ColumnInfo(name = "remoteId")
    var remoteId: String? = Task.NO_UUID

    @ColumnInfo(name = "message")
    var message: String? = ""

    @ColumnInfo(name = "picture")
    var picture: String? = ""

    @ColumnInfo(name = "target_id")
    @Transient
    var targetId: String? = Task.NO_UUID

    @ColumnInfo(name = "created_at")
    var created: Long? = 0L

    constructor()

    @Ignore
    constructor(reader: XmlReader) {
        reader.readString("remoteId") { remoteId: String? -> this.remoteId = remoteId }
        reader.readString("message") { message: String? -> this.message = message }
        reader.readString("picture") { p: String? ->
            picture = p
            convertPictureUri()
        }
        reader.readString("target_id") { targetId: String? -> this.targetId = targetId }
        reader.readLong("created_at") { created: Long -> this.created = created }
    }

    @Ignore
    private constructor(parcel: Parcel) {
        with(parcel) {
            id = readLong()
            remoteId = readString()
            message = readString()
            picture = readString()
            targetId = readString()
            created = readLong()
        }
    }

    fun setPicture(uri: Uri?) {
        picture = uri?.toString()
    }

    val pictureUri: Uri?
        get() = if (Strings.isNullOrEmpty(picture)) null else Uri.parse(picture)

    fun convertPictureUri() {
        setPicture(getLegacyPictureUri(picture))
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        with(dest) {
            writeLong(id!!)
            writeString(remoteId)
            writeString(message)
            writeString(picture)
            writeString(targetId)
            writeLong(created!!)
        }
    }

    companion object {
        @JvmField val TABLE = Table("userActivity")
        @JvmField val TASK = TABLE.column("target_id")
        @JvmField val MESSAGE = TABLE.column("message")
        @JvmField val CREATOR: Parcelable.Creator<UserActivity> = object : Parcelable.Creator<UserActivity> {
            override fun createFromParcel(source: Parcel): UserActivity = UserActivity(source)

            override fun newArray(size: Int): Array<UserActivity?> = arrayOfNulls(size)
        }

        private fun getLegacyPictureUri(value: String?): Uri? {
            return try {
                if (Strings.isNullOrEmpty(value)) {
                    return null
                }
                if (value!!.contains("uri") || value.contains("path")) {
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
    }
}