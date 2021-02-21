package org.tasks.caldav.property

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.XmlUtils
import java.io.StringReader

object PropertyUtils {
    fun <T: Property> String.toProperty(): T =
            toProperties()
                    .apply { if (this.size != 1) throw IllegalStateException("${this.size} items") }
                    .first()
                    .let {
                        @Suppress("UNCHECKED_CAST")
                        it as T
                    }

    fun String.toProperties(): List<Property> =
            XmlUtils.newPullParser()
                    .apply {
                        setInput(
                                StringReader("""
                                    <test xmlns:d="DAV:">
                                        ${this@toProperties}
                                    </test>
                                    """.trimIndent()
                                )
                        )
                        nextTag()
                    }
                    .let { Property.parse(it) }
}