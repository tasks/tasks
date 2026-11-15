package org.tasks.caldav

object LineSeparatorFixtures {
    const val NOTES =
        "Hardware store \u2028Seeds: 12/40 tomato, 8/40 basil\u2028Soil\u00a0bags\u2028" +
            "Watering can \u2028Gloves\u2028Fertilizer \u2028Twine"

    val VTODO = listOf(
        "BEGIN:VCALENDAR",
        "VERSION:2.0",
        "PRODID:-//Example Corp//Example Tasks//EN",
        "BEGIN:VTODO",
        "DTSTAMP:20260101T000000Z",
        "UID:7f3c1d2e-line-separator",
        "CREATED:20260101T000000Z",
        "LAST-MODIFIED:20260101T000000Z",
        "SUMMARY:Shopping list",
        "DESCRIPTION:Hardware store \u2028Seeds: 12/40 tomato\\, 8/40 basil\u2028Soil\u00a0bag",
        " s\u2028Watering can \u2028Gloves\u2028",
        " Fertilizer \u2028Twine",
        "PRIORITY:9",
        "STATUS:NEEDS-ACTION",
        "X-APPLE-SORT-ORDER:123456",
        "END:VTODO",
        "END:VCALENDAR",
    ).joinToString("\r\n", postfix = "\r\n")

    val DAMAGED = VTODO.replace("\r\n", "\n").replace('\u2028', '\n')

    const val CUT_OFF_NOTES = "Hardware store"

    fun multiget(href: String, vtodo: String = VTODO): String {
        val calendarData = vtodo.replace("\r\n", "\n").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<d:multistatus xmlns:d=\"DAV:\" xmlns:cal=\"urn:ietf:params:xml:ns:caldav\">\n" +
            " <d:response>\n" +
            "  <d:href>$href</d:href>\n" +
            "  <d:propstat>\n" +
            "   <d:prop>\n" +
            "    <d:getetag>&quot;etag-1&quot;</d:getetag>\n" +
            "    <cal:calendar-data>$calendarData</cal:calendar-data>\n" +
            "   </d:prop>\n" +
            "   <d:status>HTTP/1.1 200 OK</d:status>\n" +
            "  </d:propstat>\n" +
            " </d:response>\n" +
            "</d:multistatus>\n"
    }
}
