package org.tasks.makers

import com.natpryce.makeiteasy.Instantiator
import com.natpryce.makeiteasy.Property
import com.natpryce.makeiteasy.Property.newProperty
import com.natpryce.makeiteasy.PropertyValue
import com.todoroo.astrid.helper.UUIDHelper
import org.tasks.data.CaldavTask
import org.tasks.makers.Maker.make

object CaldavTaskMaker {
    val CALENDAR: Property<CaldavTask, String> = newProperty()
    val TASK: Property<CaldavTask, Long> = newProperty()
    val REMOTE_ID: Property<CaldavTask, String?> = newProperty()
    val REMOTE_PARENT: Property<CaldavTask, String?> = newProperty()
    val ETAG: Property<CaldavTask, String?> = newProperty()
    val OBJECT: Property<CaldavTask, String?> = newProperty()

    private val instantiator = Instantiator {
        val remoteId = it.valueOf(REMOTE_ID, UUIDHelper.newUUID())
        CaldavTask(
            task = it.valueOf(TASK, 1L),
            calendar = it.valueOf(CALENDAR, "calendar"),
            remoteId = remoteId,
            remoteParent = it.valueOf(REMOTE_PARENT, null as String?),
            etag = it.valueOf(ETAG, null as String?),
            `object` = it.valueOf(OBJECT, remoteId?.let { id -> "$id.ics" }),
        )
    }

    fun newCaldavTask(vararg properties: PropertyValue<in CaldavTask?, *>): CaldavTask {
        return make(instantiator, *properties)
    }
}