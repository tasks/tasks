package org.tasks.caldav.property

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.PropertyFactory
import at.bitfire.dav4jvm.XmlReader
import org.xmlpull.v1.XmlPullParser

internal fun textDeadPropertyFactory(name: Property.Name, wrap: (String) -> Property): PropertyFactory =
    object : PropertyFactory {
        override fun getName() = name
        override fun create(parser: XmlPullParser): Property =
            wrap(XmlReader(parser).readText()?.takeIf { it.isNotBlank() } ?: "")
    }
