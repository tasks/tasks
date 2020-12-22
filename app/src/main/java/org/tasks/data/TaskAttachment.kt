package org.tasks.data

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.todoroo.astrid.data.Task
import org.tasks.Strings
import java.io.File

@Entity(tableName = "task_attachments")
class TaskAttachment {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    @Transient
    var id: Long? = null

    @ColumnInfo(name = "remoteId")
    var remoteId: String? = Task.NO_UUID

    // -- Constants
    @ColumnInfo(name = "task_id")
    var taskId: String? = Task.NO_UUID

    @ColumnInfo(name = "name")
    var name: String? = ""

    @ColumnInfo(name = "path")
    var uri: String? = ""
        private set

    @ColumnInfo(name = "content_type")
    var contentType: String? = ""

    constructor()

    @Ignore
    constructor(taskUuid: String, uri: Uri?, fileName: String) {
        taskId = taskUuid
        name = fileName
        setUri(uri?.toString())
    }

    fun setUri(uri: String?) {
        this.uri = uri
    }

    fun convertPathUri() {
        setUri(Uri.fromFile(File(uri!!)).toString())
    }

    fun parseUri(): Uri? = if (Strings.isNullOrEmpty(uri)) null else Uri.parse(uri)

    companion object {
        const val KEY = "attachment"

        /** default directory for files on external storage  */
        const val FILES_DIRECTORY_DEFAULT = "attachments" // $NON-NLS-1$
    }
}