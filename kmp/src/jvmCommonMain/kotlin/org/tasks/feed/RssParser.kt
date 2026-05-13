package org.tasks.feed

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

object RssParser {
    fun parse(xml: String): List<BlogPost> {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(xml.reader())

        val items = mutableListOf<BlogPost>()
        var inItem = false
        // Depth relative to <item>: 1 = <item>, 2 = direct child, 3+ = nested.
        var depth = 0
        var fieldTag: String? = null
        val text = StringBuilder()
        var title: String? = null
        var link: String? = null
        var guid: String? = null
        var description: String? = null
        var pubDate: String? = null
        val categories = mutableListOf<String>()

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    if (!inItem && parser.name == "item") {
                        inItem = true
                        depth = 1
                        title = null
                        link = null
                        guid = null
                        description = null
                        pubDate = null
                        categories.clear()
                    } else if (inItem) {
                        depth++
                        if (depth == 2) {
                            fieldTag = parser.name
                            text.clear()
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    // Accumulate text for the current direct-child field, including any
                    // text found inside nested elements (e.g. HTML in description).
                    if (inItem && depth >= 2) {
                        text.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (inItem) {
                        if (depth == 2) {
                            val value = text.toString().trim()
                            when (fieldTag) {
                                "title" -> title = value
                                "link" -> link = value
                                "guid" -> guid = value
                                "description" -> description = value
                                "pubDate" -> pubDate = value
                                "category" -> categories.add(value)
                            }
                            fieldTag = null
                        }
                        depth--
                        if (depth == 0) {
                            val currentTitle = title
                            val currentLink = link
                            if (currentTitle != null && currentLink != null) {
                                items.add(
                                    BlogPost(
                                        title = currentTitle,
                                        link = currentLink,
                                        guid = guid ?: currentLink,
                                        description = description ?: "",
                                        pubDate = pubDate ?: "",
                                        categories = categories.toList(),
                                    )
                                )
                            }
                            inItem = false
                        }
                    }
                }
            }
        }
        return items
    }
}
