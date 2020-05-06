package org.tasks.makers

import com.natpryce.makeiteasy.Instantiator
import com.natpryce.makeiteasy.Property
import com.natpryce.makeiteasy.Property.newProperty
import com.natpryce.makeiteasy.PropertyValue
import org.tasks.data.CaldavTask
import org.tasks.makers.Maker.make

object CaldavTaskMaker {
    @JvmField val CALENDAR: Property<CaldavTask, String> = newProperty()
    @JvmField val TASK: Property<CaldavTask, Long> = newProperty()
    @JvmField val REMOTE_ID: Property<CaldavTask, String?> = newProperty()
    @JvmField val REMOTE_PARENT: Property<CaldavTask, String?> = newProperty()

    private val instantiator = Instantiator<CaldavTask> {
        val task = CaldavTask(it.valueOf(TASK, 1L), it.valueOf(CALENDAR, "calendar"))
        task.remoteId = it.valueOf(REMOTE_ID, task.remoteId)
        task.remoteParent = it.valueOf(REMOTE_PARENT, null as String?)
        task
    }

    @JvmStatic
    fun newCaldavTask(vararg properties: PropertyValue<in CaldavTask?, *>): CaldavTask {
        return make(instantiator, *properties)
    }
}