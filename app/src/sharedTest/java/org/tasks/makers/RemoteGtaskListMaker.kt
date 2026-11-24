package org.tasks.makers

import com.natpryce.makeiteasy.Instantiator
import com.natpryce.makeiteasy.Property
import com.natpryce.makeiteasy.Property.newProperty
import com.natpryce.makeiteasy.PropertyLookup
import com.natpryce.makeiteasy.PropertyValue
import org.tasks.googleapis.GtasksDates
import org.tasks.googleapis.GtasksTaskList
import org.tasks.makers.Maker.make
import org.tasks.time.DateTimeUtils2.currentTimeMillis

object RemoteGtaskListMaker {
    val REMOTE_ID: Property<GtasksTaskList, String> = newProperty()
    val NAME: Property<GtasksTaskList, String> = newProperty()

    private val instantiator = Instantiator { lookup: PropertyLookup<GtasksTaskList> ->
        GtasksTaskList(
                id = lookup.valueOf(REMOTE_ID, "1"),
                title = lookup.valueOf(NAME, "Default"),
                updated = GtasksDates.unixTimeToGtasksCompletionTime(currentTimeMillis()),
        )
    }

    fun newRemoteList(vararg properties: PropertyValue<in GtasksTaskList?, *>): GtasksTaskList {
        return make(instantiator, *properties)
    }
}