package org.tasks.caldav

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.XmlUtils
import at.bitfire.dav4jvm.XmlUtils.insertTag
import at.bitfire.dav4jvm.XmlUtils.propertyName
import at.bitfire.dav4jvm.ktor.PropStatParser
import at.bitfire.dav4jvm.property.webdav.WebDAV
import at.bitfire.dav4jvm.property.webdav.WebDAV.NS_WEBDAV
import io.ktor.http.isSuccess
import nl.adaptivity.xmlutil.EventType

internal fun propstatFailureCode(body: String?): Int? {
    if (body.isNullOrBlank()) return null
    return try {
        val parser = XmlUtils.newReader(body)
        var event = parser.eventType
        while (event != EventType.END_DOCUMENT) {
            if (event == EventType.START_ELEMENT && parser.propertyName() == WebDAV.PropStat) {
                val propStat = PropStatParser.parse(parser)
                if (!propStat.status.isSuccess()) return propStat.status.value
            }
            event = parser.next()
        }
        null
    } catch (_: Exception) {
        null
    }
}

internal fun proppatchBody(set: List<Pair<Property.Name, String>>, remove: List<Property.Name>): String {
    val namespaces = listOfNotNull(
        "d" to NS_WEBDAV,
        (set.firstOrNull()?.first ?: remove.firstOrNull())?.let { "t" to it.namespace },
    )
    return XmlUtils.buildDocument(namespaces, WebDAV.PropertyUpdate) {
        if (set.isNotEmpty()) {
            insertTag(WebDAV.Set) {
                insertTag(WebDAV.Prop) {
                    set.forEach { (name, value) ->
                        insertTag(name) { text(value) }
                    }
                }
            }
        }
        if (remove.isNotEmpty()) {
            insertTag(WebDAV.Remove) {
                insertTag(WebDAV.Prop) {
                    remove.forEach { insertTag(it) }
                }
            }
        }
    }
}
