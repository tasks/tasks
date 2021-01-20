package org.tasks.data

import android.database.Cursor
import at.bitfire.ical4android.AndroidTask
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
}