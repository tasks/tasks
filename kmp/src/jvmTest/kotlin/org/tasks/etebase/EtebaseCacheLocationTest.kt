package org.tasks.etebase

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows

class EtebaseCacheLocationTest {
    @Test fun legacyCachePathIsNeverMovedOrClearedOnUpgrade() {
        assertEquals("/files", EtebaseCacheLocation.root("/files", null))
    }
    @Test fun sameEmailAccountsAndFilesRootsAreIsolated() {
        val first = EtebaseCacheLocation.root("/files", "account-a")
        val second = EtebaseCacheLocation.root("/files", "account-b")
        assertNotEquals(first, second)
        assertNotEquals("/files", first)
        assertNotEquals(EtebaseCacheLocation.key(first, "same@example.com"), EtebaseCacheLocation.key(second, "same@example.com"))
        assertNotEquals(EtebaseCacheLocation.key("/one", "same@example.com"), EtebaseCacheLocation.key("/two", "same@example.com"))
        assertEquals(EtebaseCacheLocation.key(first, "same@example.com"), EtebaseCacheLocation.key(first, "same@example.com"))
    }
    @Test fun scopeCannotEscapeCacheRoot() {
        assertThrows(IllegalArgumentException::class.java) { EtebaseCacheLocation.root("/files", "") }
        val root = EtebaseCacheLocation.root("/files", "../../other")
        assertEquals("/files/etebase-accounts/", root.substringBeforeLast('/') + "/")
    }
}
