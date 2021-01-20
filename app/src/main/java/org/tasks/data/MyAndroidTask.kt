package org.tasks.data

import android.database.Cursor
import android.net.Uri
import at.bitfire.ical4android.AndroidTask
import at.bitfire.ical4android.BatchOperation
import at.bitfire.ical4android.BatchOperation.CpoBuilder.Companion.newInsert
import at.bitfire.ical4android.BatchOperation.CpoBuilder.Companion.newUpdate
import at.bitfire.ical4android.MiscUtils.CursorHelper.toValues
import at.bitfire.ical4android.Task
import org.dmfs.tasks.contract.TaskContract

class MyAndroidTask() : AndroidTask(null) {

    constructor(cursor: Cursor) : this() {
        val values = cursor.toValues()
        task = Task()
        populateTask(values)
        populateRelatedTo(values)
        if (values.containsKey(TaskContract.Properties.PROPERTY_ID)) {
            // process the first property, which is combined with the task row
            populateProperty(values)

            while (cursor.moveToNext()) {
                // process the other properties
                populateProperty(cursor.toValues(true))
            }
        }
    }

    constructor(task: Task) : this() {
        this.task = task
    }

    fun toBuilder(uri: Uri, isNew: Boolean): BatchOperation.CpoBuilder {
        val builder = if (isNew) newInsert(uri) else newUpdate(uri)
        buildTask(builder, true)
        if (!isNew) {
            builder.remove(TaskContract.Tasks._UID)
        }
        return builder
                .remove(TaskContract.Tasks.CREATED)
                .remove(TaskContract.Tasks.LAST_MODIFIED)
                .remove(TaskContract.Tasks._DIRTY)
                .remove(TaskContract.Tasks.SYNC_VERSION)
    }
}