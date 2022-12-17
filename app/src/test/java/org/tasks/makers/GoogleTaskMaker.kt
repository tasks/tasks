package org.tasks.makers

import com.natpryce.makeiteasy.Instantiator
import com.natpryce.makeiteasy.Property
import com.natpryce.makeiteasy.Property.newProperty
import com.natpryce.makeiteasy.PropertyValue
import com.todoroo.astrid.helper.UUIDHelper
import org.tasks.makers.Maker.make

object GoogleTaskMaker {
    val LIST: Property<GoogleTask, String> = newProperty()
    val ORDER: Property<GoogleTask, Long> = newProperty()
    val REMOTE_ID: Property<GoogleTask, String> = newProperty()
    val TASK: Property<GoogleTask, Long> = newProperty()
    val PARENT: Property<GoogleTask, Long> = newProperty()
    val REMOTE_PARENT: Property<GoogleTask, String?> = newProperty()

    private val instantiator = Instantiator<GoogleTask> {
        val task = GoogleTask()
        task.calendar = it.valueOf(LIST, "1")
        task.order = it.valueOf(ORDER, 0)
        task.remoteId = it.valueOf(REMOTE_ID, UUIDHelper.newUUID())
        task.task = it.valueOf(TASK, 1)
        task.parent = it.valueOf(PARENT, 0L)
        task.remoteParent = it.valueOf(REMOTE_PARENT, null as String?)
        task
    }

    fun newGoogleTask(vararg properties: PropertyValue<in GoogleTask?, *>): GoogleTask {
        return make(instantiator, *properties)
    }
}