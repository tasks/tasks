package org.tasks.makers

import com.natpryce.makeiteasy.Instantiator
import com.natpryce.makeiteasy.Property
import com.natpryce.makeiteasy.Property.newProperty
import com.natpryce.makeiteasy.PropertyLookup
import com.natpryce.makeiteasy.PropertyValue
import com.todoroo.astrid.data.Task
import org.tasks.data.Tag
import org.tasks.data.TagData
import org.tasks.makers.Maker.make

object TagMaker {
    @JvmField val TAGDATA: Property<Tag, TagData?> = newProperty()
    @JvmField val TASK: Property<Tag, Task?> = newProperty()
    @JvmField val TAGUID: Property<Tag, String?> = newProperty()

    private val instantiator = Instantiator { lookup: PropertyLookup<Tag> ->
        val tag = Tag()
        val task = lookup.valueOf(TASK, null as Task?)!!
        tag.task = task.getId()
        tag.taskUid = task.uuid
        tag.tagUid = lookup.valueOf(TAGUID, null as String?)
        val tagData = lookup.valueOf(TAGDATA, null as TagData?)
        if (tagData != null) {
            tag.tagUid = tagData.remoteId
        }
        assert(tag.tagUid != null)
        tag
    }

    @JvmStatic
    fun newTag(vararg properties: PropertyValue<in Tag?, *>): Tag {
        return make(instantiator, *properties)
    }
}