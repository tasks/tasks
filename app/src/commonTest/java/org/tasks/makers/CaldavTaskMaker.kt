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
    val VTODO: Property<CaldavTask, String?> = newProperty()

    private val instantiator = Instantiator<CaldavTask> {
        val task = CaldavTask(it.valueOf(TASK, 1L), it.valueOf(CALENDAR, "calendar"))
        task.remoteId = it.valueOf(REMOTE_ID, task.remoteId)
        task.remoteParent = it.valueOf(REMOTE_PARENT, null as String?)
        task.vtodo = it.valueOf(VTODO, null as String?)
        task
    }

    fun newCaldavTask(vararg properties: PropertyValue<in CaldavTask?, *>): CaldavTask {
        return make(instantiator, *properties)
    }
}