package org.tasks.caldav.property

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.XmlUtils
import at.bitfire.dav4jvm.property.webdav.WebDAV

object TestPropertyUtils {
    private val DAV = "d" to WebDAV.NS_WEBDAV

    fun <T: Property> String.toProperty(vararg ns: Pair<String, String>): T =
            toProperties(*ns)
                    .apply { if (this.size != 1) throw IllegalStateException("${this.size} items") }
                    .first()
                    .let {
                        @Suppress("UNCHECKED_CAST")
                        it as T
                    }

    fun String.toProperties(vararg ns: Pair<String, String>): List<Property> {
        val namespaces = ns.toList().plus(DAV).joinToString(" ") {
            """xmlns:${it.first}="${it.second}""""
        }
        return Property.parse(XmlUtils.newReader("<test $namespaces>$this</test>").apply { nextTag() })
    }
}