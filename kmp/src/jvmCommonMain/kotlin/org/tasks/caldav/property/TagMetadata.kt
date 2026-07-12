package org.tasks.caldav.property

import at.bitfire.dav4jvm.Property
import org.tasks.caldav.metadata.TAG_METADATA_CATEGORY

data class TagMetadata(
    val json: String,
) : Property {
    companion object {
        @JvmField
        val NAME = Property.Name(PropertyUtils.NS_TASKS, TAG_METADATA_CATEGORY)

        @JvmField
        val Factory = textDeadPropertyFactory(NAME) { TagMetadata(it) }
    }
}
