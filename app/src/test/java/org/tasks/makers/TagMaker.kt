package org.tasks.makers

import com.natpryce.makeiteasy.Instantiator
import com.natpryce.makeiteasy.Property
import com.natpryce.makeiteasy.Property.newProperty
import com.natpryce.makeiteasy.PropertyLookup
import com.natpryce.makeiteasy.PropertyValue
import org.tasks.data.entity.Task
import org.tasks.data.entity.Tag
import org.tasks.data.entity.TagData
import org.tasks.makers.Maker.make

object TagMaker {
    val TAGDATA: Property<Tag, TagData?> = newProperty()
    val TASK: Property<Tag, Task?> = newProperty()
    val TAGUID: Property<Tag, String?> = newProperty()

    private val instantiator = Instantiator { lookup: PropertyLookup<Tag> ->
        val tag = Tag()
        val task = lookup.valueOf(TASK, null as Task?)!!
        tag.task = task.id
        tag.setTaskUid(task.uuid)
        tag.tagUid = lookup.valueOf(TAGUID, null as String?)
        val tagData = lookup.valueOf(TAGDATA, null as TagData?)
        if (tagData != null) {
            tag.tagUid = tagData.remoteId
        }
        assert(tag.tagUid != null)
        tag
    }

    fun newTag(vararg properties: PropertyValue<in Tag?, *>): Tag {
        return make(instantiator, *properties)
    }
}