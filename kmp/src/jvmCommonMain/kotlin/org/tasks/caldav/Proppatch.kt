package org.tasks.caldav

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.XmlUtils
import at.bitfire.dav4jvm.XmlUtils.propertyName
import at.bitfire.dav4jvm.okhttp.PropStat
import at.bitfire.dav4jvm.property.webdav.WebDAV
import at.bitfire.dav4jvm.property.webdav.WebDAV.NS_WEBDAV
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.io.StringWriter

internal fun propstatFailureCode(body: String?): Int? {
    if (body.isNullOrBlank()) return null
    return try {
        val parser = XmlUtils.newPullParser()
        parser.setInput(StringReader(body))
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.propertyName() == WebDAV.PropStat) {
                val propStat = PropStat.parse(parser)
                if (!propStat.isSuccess()) return propStat.status.code
            }
            event = parser.next()
        }
        null
    } catch (_: Exception) {
        null
    }
}

internal fun proppatchBody(set: List<Pair<Property.Name, String>>, remove: List<Property.Name>): String {
    val serializer = XmlPullParserFactory.newInstance().newSerializer()
    val writer = StringWriter()
    with(serializer) {
        setOutput(writer)
        startDocument("UTF-8", null)
        setPrefix("d", NS_WEBDAV)
        if (set.isNotEmpty() || remove.isNotEmpty()) {
            setPrefix("t", (set.firstOrNull()?.first ?: remove.first()).namespace)
        }
        startTag(NS_WEBDAV, "propertyupdate")
        if (set.isNotEmpty()) {
            startTag(NS_WEBDAV, "set")
            startTag(NS_WEBDAV, "prop")
            set.forEach { (name, value) ->
                startTag(name.namespace, name.name)
                text(value)
                endTag(name.namespace, name.name)
            }
            endTag(NS_WEBDAV, "prop")
            endTag(NS_WEBDAV, "set")
        }
        if (remove.isNotEmpty()) {
            startTag(NS_WEBDAV, "remove")
            startTag(NS_WEBDAV, "prop")
            remove.forEach { name ->
                startTag(name.namespace, name.name)
                endTag(name.namespace, name.name)
            }
            endTag(NS_WEBDAV, "prop")
            endTag(NS_WEBDAV, "remove")
        }
        endTag(NS_WEBDAV, "propertyupdate")
        endDocument()
    }
    return writer.toString()
}
