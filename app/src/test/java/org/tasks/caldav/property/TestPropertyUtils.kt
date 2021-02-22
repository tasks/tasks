package org.tasks.caldav.property

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.XmlUtils
import java.io.StringReader

object TestPropertyUtils {
    private val DAV = "d" to XmlUtils.NS_WEBDAV

    fun <T: Property> String.toProperty(vararg ns: Pair<String, String>): T =
            toProperties(*ns)
                    .apply { if (this.size != 1) throw IllegalStateException("${this.size} items") }
                    .first()
                    .let {
                        @Suppress("UNCHECKED_CAST")
                        it as T
                    }

    fun String.toProperties(vararg ns: Pair<String, String>): List<Property> =
            XmlUtils.newPullParser()
                    .apply {
                        val namespaces = ns.toList().plus(DAV).joinToString(" ") {
                            """xmlns:${it.first}="${it.second}""""
                        }
                        setInput(
                                StringReader("""
                                    <test $namespaces>
                                        ${this@toProperties}
                                    </test>
                                    """.trimIndent()
                                )
                        )
                        nextTag()
                    }
                    .let { Property.parse(it) }
}