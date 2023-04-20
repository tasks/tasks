package org.tasks.makers

import com.natpryce.makeiteasy.Instantiator
import com.natpryce.makeiteasy.MakeItEasy.with
import com.natpryce.makeiteasy.Property
import com.natpryce.makeiteasy.Property.newProperty
import com.natpryce.makeiteasy.PropertyLookup
import com.natpryce.makeiteasy.PropertyValue
import com.todoroo.astrid.data.Task.Companion.NO_ID
import org.tasks.data.TaskContainer
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.makers.Maker.make
import org.tasks.makers.TaskMaker.newTask
import org.tasks.time.DateTime

object TaskContainerMaker {
    val ID: Property<TaskContainer, Long> = newProperty()
    val PARENT: Property<TaskContainer, TaskContainer> = newProperty()
    val CREATED: Property<TaskContainer, DateTime> = newProperty()

    private val instantiator = Instantiator { lookup: PropertyLookup<TaskContainer> ->
        val taskId = lookup.valueOf(ID, NO_ID)
        val created = lookup.valueOf(CREATED, newDateTime())
        val parent = lookup.valueOf(PARENT, null as TaskContainer?)
        TaskContainer(
            task = newTask(
                with(TaskMaker.ID, taskId),
                with(TaskMaker.CREATION_TIME, created),
                with(TaskMaker.PARENT, parent?.id ?: 0L)
            ),
            indent = parent?.indent?.plus(1) ?: 0,
        )
    }

    fun newTaskContainer(vararg properties: PropertyValue<in TaskContainer?, *>): TaskContainer {
        return make(instantiator, *properties)
    }
}