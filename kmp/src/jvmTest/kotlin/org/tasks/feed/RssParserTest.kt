package org.tasks.feed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RssParserTest {

    private fun loadXml(name: String): String =
        javaClass.classLoader!!.getResource("feed/$name")!!.readText()

    @Test
    fun parsesNoAnnouncementsFeed() {
        val posts = RssParser.parse(loadXml("no_announcements.xml"))

        assertEquals(4, posts.size)
        assertEquals("Release v11.8", posts[0].title)
        assertEquals("Release v11.7", posts[1].title)
        assertEquals("Release v11.6", posts[2].title)
        assertEquals("Shared lists are coming!", posts[3].title)
    }

    @Test
    fun noAnnouncementPostsHaveNoCategories() {
        val posts = RssParser.parse(loadXml("no_announcements.xml"))

        assertTrue(posts.all { it.categories.isEmpty() })
    }

    @Test
    fun parsesMultipleAnnouncementsFeed() {
        val posts = RssParser.parse(loadXml("multiple_announcements.xml"))

        assertEquals(5, posts.size)
        assertEquals("New Terms of Service", posts[0].title)
        assertEquals("New Privacy Policy", posts[1].title)
        assertEquals("Release v11.8", posts[2].title)
        assertEquals("Release v11.7", posts[3].title)
        assertEquals("Shared lists are coming!", posts[4].title)
    }

    @Test
    fun announcementCategoryIsParsed() {
        val posts = RssParser.parse(loadXml("multiple_announcements.xml"))
        val announcements = posts.filter { "announcement" in it.categories }

        assertEquals(3, announcements.size)
        assertEquals("New Terms of Service", announcements[0].title)
        assertEquals("New Privacy Policy", announcements[1].title)
        assertEquals("Shared lists are coming!", announcements[2].title)
    }

    @Test
    fun guidIsParsed() {
        val posts = RssParser.parse(loadXml("multiple_announcements.xml"))

        assertEquals("https://tasks.org/blog/tos", posts[0].guid)
        assertEquals("https://tasks.org/blog/privacy", posts[1].guid)
    }

    @Test
    fun pubDateIsParsed() {
        val posts = RssParser.parse(loadXml("multiple_announcements.xml"))

        assertEquals("Mon, 19 Jan 2026 00:00:00 GMT", posts[0].pubDate)
        assertEquals("Tue, 30 Dec 2025 00:00:00 GMT", posts[1].pubDate)
    }

    @Test
    fun descriptionIsParsed() {
        val posts = RssParser.parse(loadXml("multiple_announcements.xml"))

        assertEquals("Updated terms", posts[0].description)
    }

    @Test
    fun linkIsParsed() {
        val posts = RssParser.parse(loadXml("multiple_announcements.xml"))

        assertEquals("https://tasks.org/blog/tos", posts[0].link)
        assertEquals("https://tasks.org/blog/privacy", posts[1].link)
    }

    @Test
    fun nestedHtmlInDescriptionDoesNotClobberOtherFields() {
        val posts = RssParser.parse(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <rss version="2.0">
                <channel>
                    <item>
                        <title>Nested</title>
                        <link>https://example.com/nested</link>
                        <description>hello <b>bold</b> world</description>
                        <pubDate>Mon, 19 Jan 2026 00:00:00 GMT</pubDate>
                    </item>
                </channel>
            </rss>
            """.trimIndent()
        )

        assertEquals(1, posts.size)
        assertEquals("Nested", posts[0].title)
        assertEquals("https://example.com/nested", posts[0].link)
        assertEquals("hello bold world", posts[0].description)
        assertEquals("Mon, 19 Jan 2026 00:00:00 GMT", posts[0].pubDate)
    }

    @Test
    fun cdataDescriptionIsPreserved() {
        val posts = RssParser.parse(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <rss version="2.0">
                <channel>
                    <item>
                        <title>CDATA</title>
                        <link>https://example.com/cdata</link>
                        <description><![CDATA[<p>raw html</p>]]></description>
                    </item>
                </channel>
            </rss>
            """.trimIndent()
        )

        assertEquals(1, posts.size)
        assertEquals("<p>raw html</p>", posts[0].description)
    }

    @Test
    fun itemMissingTitleIsSkipped() {
        val posts = RssParser.parse(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <rss version="2.0">
                <channel>
                    <item>
                        <link>https://example.com/no-title</link>
                    </item>
                    <item>
                        <title>Has title</title>
                        <link>https://example.com/has-title</link>
                    </item>
                </channel>
            </rss>
            """.trimIndent()
        )

        assertEquals(1, posts.size)
        assertEquals("Has title", posts[0].title)
    }
}
