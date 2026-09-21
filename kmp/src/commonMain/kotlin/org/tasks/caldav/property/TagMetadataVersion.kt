package org.tasks.caldav.property

import at.bitfire.dav4jvm.Property

data class TagMetadataVersion(
    val version: String,
) : Property {
    companion object {
        val NAME = Property.Name(PropertyUtils.NS_TASKS, "tag-metadata-version")
        val Factory = textDeadPropertyFactory(NAME) { TagMetadataVersion(it) }
    }
}
