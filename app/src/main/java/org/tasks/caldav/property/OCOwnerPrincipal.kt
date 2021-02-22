package org.tasks.caldav.property

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.PropertyFactory
import at.bitfire.dav4jvm.XmlUtils
import org.xmlpull.v1.XmlPullParser

data class OCOwnerPrincipal(val owner: String): Property {
    companion object {
        @JvmField
        val NAME = Property.Name(PropertyUtils.NS_OWNCLOUD, "owner-principal")
    }

    class Factory: PropertyFactory {
        override fun getName() = NAME
        override fun create(parser: XmlPullParser): OCOwnerPrincipal? =
                XmlUtils.readText(parser)?.let { OCOwnerPrincipal(it) }
    }
}