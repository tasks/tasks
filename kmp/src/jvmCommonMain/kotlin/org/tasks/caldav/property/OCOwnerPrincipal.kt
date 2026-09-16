package org.tasks.caldav.property

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.PropertyFactory
import at.bitfire.dav4jvm.readText
import nl.adaptivity.xmlutil.XmlReader

data class OCOwnerPrincipal(val owner: String?): Property {
    companion object {
        @JvmField
        val NAME = Property.Name(PropertyUtils.NS_OWNCLOUD, "owner-principal")
    }

    class Factory: PropertyFactory {
        override fun getName() = NAME
        override fun create(parser: XmlReader): OCOwnerPrincipal =
                OCOwnerPrincipal(parser.readText())
    }
}