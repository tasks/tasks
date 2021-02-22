package org.tasks.caldav.property

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.XmlUtils
import java.io.StringReader

object TestPropertyUtils {
    fun <T: Property> String.toProperty(ns: String = """d="DAV:""""): T =
            toProperties(ns)
                    .apply { if (this.size != 1) throw IllegalStateException("${this.size} items") }
                    .first()
                    .let {
                        @Suppress("UNCHECKED_CAST")
                        it as T
                    }

    fun String.toProperties(ns: String = """d="DAV:""""): List<Property> =
            XmlUtils.newPullParser()
                    .apply {
                        setInput(
                                StringReader("""
                                    <test xmlns:$ns>
                                        ${this@toProperties}
                                    </test>
                                    """.trimIndent()
                                )
                        )
                        nextTag()
                    }
                    .let { Property.parse(it) }
}