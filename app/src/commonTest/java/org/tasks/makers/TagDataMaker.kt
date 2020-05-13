package org.tasks.makers

import com.natpryce.makeiteasy.Instantiator
import com.natpryce.makeiteasy.Property
import com.natpryce.makeiteasy.Property.newProperty
import com.natpryce.makeiteasy.PropertyLookup
import com.natpryce.makeiteasy.PropertyValue
import org.tasks.data.TagData
import org.tasks.makers.Maker.make

object TagDataMaker {
    val NAME: Property<TagData, String> = newProperty()
    val UID: Property<TagData, String?> = newProperty()

    private val instantiator = Instantiator { lookup: PropertyLookup<TagData> ->
        val tagData = TagData()
        tagData.name = lookup.valueOf(NAME, "tag")
        tagData.remoteId = lookup.valueOf(UID, null as String?)
        tagData
    }

    fun newTagData(vararg properties: PropertyValue<in TagData?, *>): TagData {
        return make(instantiator, *properties)
    }
}