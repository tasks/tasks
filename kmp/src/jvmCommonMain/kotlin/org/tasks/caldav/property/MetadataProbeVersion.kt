package org.tasks.caldav.property

import at.bitfire.dav4jvm.Property

data class MetadataProbeVersion(
    val version: String,
) : Property {
    companion object {
        @JvmField
        val NAME = Property.Name(PropertyUtils.NS_TASKS, "metadata-probe-version")

        @JvmField
        val Factory = textDeadPropertyFactory(NAME) { MetadataProbeVersion(it) }
    }
}
