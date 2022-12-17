package org.tasks.makers

import com.natpryce.makeiteasy.Instantiator
import com.natpryce.makeiteasy.Property
import com.natpryce.makeiteasy.Property.newProperty
import com.natpryce.makeiteasy.PropertyLookup
import com.natpryce.makeiteasy.PropertyValue
import com.todoroo.astrid.api.FilterListItem.NO_ORDER
import org.tasks.data.GoogleTaskList
import org.tasks.makers.Maker.make

object GtaskListMaker {
    val ID: Property<GoogleTaskList, Long> = newProperty()
    val ACCOUNT: Property<GoogleTaskList, String> = newProperty()
    val REMOTE_ID: Property<GoogleTaskList, String> = newProperty()
    val LAST_SYNC: Property<GoogleTaskList, Long> = newProperty()
    val NAME: Property<GoogleTaskList, String> = newProperty()
    private val ORDER: Property<GoogleTaskList, Int> = newProperty()
    private val COLOR: Property<GoogleTaskList, Int> = newProperty()

    private val instantiator = Instantiator { lookup: PropertyLookup<GoogleTaskList> ->
        val list = GoogleTaskList()
        list.id = lookup.valueOf(ID, 0L)
        list.account = lookup.valueOf(ACCOUNT, "account")
        list.uuid = lookup.valueOf(REMOTE_ID, "1")
        list.name = lookup.valueOf(NAME, "Default")
        list.order = lookup.valueOf(ORDER, NO_ORDER)
        list.lastSync = lookup.valueOf(LAST_SYNC, 0L)
        list.setColor(lookup.valueOf(COLOR, 0))
        list
    }

    fun newGtaskList(vararg properties: PropertyValue<in GoogleTaskList?, *>): GoogleTaskList {
        return make(instantiator, *properties)
    }
}