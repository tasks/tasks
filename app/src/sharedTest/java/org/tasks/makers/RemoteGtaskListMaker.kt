package org.tasks.makers

import com.google.api.client.util.DateTime
import com.google.api.services.tasks.model.TaskList
import com.natpryce.makeiteasy.Instantiator
import com.natpryce.makeiteasy.Property
import com.natpryce.makeiteasy.Property.newProperty
import com.natpryce.makeiteasy.PropertyLookup
import com.natpryce.makeiteasy.PropertyValue
import org.tasks.makers.Maker.make
import org.tasks.time.DateTimeUtils2.currentTimeMillis

object RemoteGtaskListMaker {
    val REMOTE_ID: Property<TaskList, String> = newProperty()
    val NAME: Property<TaskList, String> = newProperty()

    private val instantiator = Instantiator { lookup: PropertyLookup<TaskList> ->
        TaskList()
                .setId(lookup.valueOf(REMOTE_ID, "1"))
                .setTitle(lookup.valueOf(NAME, "Default"))
                .setUpdated(DateTime(currentTimeMillis()).toStringRfc3339())
    }

    fun newRemoteList(vararg properties: PropertyValue<in TaskList?, *>): TaskList {
        return make(instantiator, *properties)
    }
}