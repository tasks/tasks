package org.tasks.makers

import com.natpryce.makeiteasy.Instantiator
import com.natpryce.makeiteasy.Property
import com.natpryce.makeiteasy.Property.newProperty
import com.natpryce.makeiteasy.PropertyValue
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
        val task = CaldavTask(it.valueOf(TASK, 1L), it.valueOf(CALENDAR, "calendar"))
        task.remoteId = it.valueOf(REMOTE_ID, task.remoteId)
        task.remoteParent = it.valueOf(REMOTE_PARENT, null as String?)
        task.etag = it.valueOf(ETAG, null as String?)
        task.`object` = it.valueOf(OBJECT, task.remoteId?.let { id -> "$id.ics" })
        task
    }

    fun newCaldavTask(vararg properties: PropertyValue<in CaldavTask?, *>): CaldavTask {
        return make(instantiator, *properties)
    }
}