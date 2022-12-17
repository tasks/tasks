package org.tasks.makers

import com.natpryce.makeiteasy.Instantiator
import com.natpryce.makeiteasy.Property
import com.natpryce.makeiteasy.PropertyLookup
import com.natpryce.makeiteasy.PropertyValue
import org.tasks.data.GoogleTaskList

object GoogleTaskListMaker {
    val REMOTE_ID: Property<GoogleTaskList, String> = Property.newProperty()
    val ACCOUNT: Property<GoogleTaskList, String?> = Property.newProperty()

    private val instantiator = Instantiator { lookup: PropertyLookup<GoogleTaskList> ->
        val list = GoogleTaskList()
        list.uuid = lookup.valueOf(REMOTE_ID, "1234")
        list.account = lookup.valueOf(ACCOUNT, null as String?)
        list.setColor(0)
        list
    }

    fun newGoogleTaskList(vararg properties: PropertyValue<in GoogleTaskList?, *>): GoogleTaskList {
        return Maker.make(instantiator, *properties)
    }
}