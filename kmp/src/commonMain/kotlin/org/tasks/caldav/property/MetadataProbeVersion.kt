package org.tasks.caldav.property

import at.bitfire.dav4jvm.Property

data class MetadataProbeVersion(
    val version: String,
) : Property {
    companion object {
        val NAME = Property.Name(PropertyUtils.NS_TASKS, "metadata-probe-version")
        val Factory = textDeadPropertyFactory(NAME) { MetadataProbeVersion(it) }
    }
}
