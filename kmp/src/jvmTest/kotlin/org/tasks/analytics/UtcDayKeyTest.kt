package org.tasks.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class UtcDayKeyTest {
    @Test
    fun anEventDedupedByItselfKeysOnItsNameAlone() {
        assertEquals("last_utc_day_mcp_tool_used", utcDayKey("mcp_tool_used", "mcp_tool_used"))
    }

    @Test
    fun anEventDedupedBySomethingElseKeysOnBoth() {
        assertEquals("last_utc_day_mcp_tool_used:read_only", utcDayKey("mcp_tool_used", "read_only"))
    }

    @Test
    fun aPlainNameIsPrefixed() {
        assertEquals("last_utc_day_daily_heartbeat", utcDayKey("daily_heartbeat"))
    }
}
