package org.tasks.caldav.property

import at.bitfire.dav4jvm.Property

data class MetadataProbe(
    val json: String,
) : Property {
    companion object {
        val NAME = Property.Name(PropertyUtils.NS_TASKS, "metadata-probe")
        val Factory = textDeadPropertyFactory(NAME) { MetadataProbe(it) }
    }
}
