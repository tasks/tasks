package org.tasks.caldav.property

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.PropertyFactory
import at.bitfire.dav4jvm.readText
import nl.adaptivity.xmlutil.XmlReader

internal fun textDeadPropertyFactory(name: Property.Name, wrap: (String) -> Property): PropertyFactory =
    object : PropertyFactory {
        override fun getName() = name
        override fun create(parser: XmlReader): Property =
            wrap(parser.readText()?.takeIf { it.isNotBlank() } ?: "")
    }
