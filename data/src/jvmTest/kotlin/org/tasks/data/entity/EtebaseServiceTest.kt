package org.tasks.data.entity

import org.junit.Test
import org.junit.Assert.assertEquals

class EtebaseServiceTest {
    @Test fun unmarkedAccountsRemainEteSync() {
        for (marker in listOf(-1, 0, 99, 1234)) {
            assertEquals(EtebaseService.ETESYNC, EtebaseService.fromServerType(marker))
        }
    }
    @Test fun explicitSilentSuiteChoiceSurvivesCustomHosting() {
        val service = EtebaseService.fromServerType(EtebaseService.SILENTSUITE.serverType)
        assertEquals(EtebaseService.SILENTSUITE, service)
        assertEquals("https://server.silentsuite.io", service.effectiveUrl("", false))
        assertEquals("https://server.silentsuite.io", service.effectiveUrl("  ", true))
        assertEquals("https://self.example/etebase/", service.effectiveUrl(" https://self.example/etebase/ ", true))
    }
    @Test fun hiddenUrlUsesSelectedServiceDefault() {
        assertEquals("https://server.silentsuite.io", EtebaseService.SILENTSUITE.effectiveUrl("https://other.example", false))
        assertEquals("https://api.etebase.com/partner/tasksorg/", EtebaseService.ETESYNC.effectiveUrl("", false))
    }
}
