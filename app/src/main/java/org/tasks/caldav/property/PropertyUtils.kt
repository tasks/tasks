package org.tasks.caldav.property

import at.bitfire.dav4jvm.PropertyFactory
import at.bitfire.dav4jvm.PropertyRegistry

object PropertyUtils {
    const val NS_OWNCLOUD = "http://owncloud.org/ns"

    fun PropertyRegistry.register(vararg factories: PropertyFactory) = register(factories.toList())
}