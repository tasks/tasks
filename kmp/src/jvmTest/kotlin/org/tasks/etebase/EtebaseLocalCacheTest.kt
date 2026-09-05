package org.tasks.etebase

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Exercise the bundled native filesystem cache without server credentials or network access. */
class EtebaseLocalCacheTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun sameEmailAccountsKeepSeparateNativeCaches() = runBlocking {
        val username = "same@example.invalid"
        val root = folder.root.path
        val firstRoot = EtebaseCacheLocation.root(root, "account-one")
        val secondRoot = EtebaseCacheLocation.root(root, "account-two")
        val legacy = EtebaseLocalCache.getInstance(root, username)
        val first = EtebaseLocalCache.getInstance(firstRoot, username)
        val second = EtebaseLocalCache.getInstance(secondRoot, username)
        try {
            assertNotSame(legacy, first)
            assertNotSame(first, second)
            legacy.saveStoken("legacy-token")
            first.saveStoken("first-token")
            second.saveStoken("second-token")
            assertEquals("legacy-token", legacy.loadStoken())
            assertEquals("first-token", first.loadStoken())
            assertEquals("second-token", second.loadStoken())
            EtebaseLocalCache.clear(firstRoot, username)
            assertEquals("legacy-token", legacy.loadStoken())
            assertEquals("second-token", second.loadStoken())
            assertNotSame(first, EtebaseLocalCache.getInstance(firstRoot, username))
        } finally {
            EtebaseLocalCache.clear(root)
        }
    }

    @Test fun clearingOneApplicationRootDoesNotClearAnother() = runBlocking {
        val username = "same@example.invalid"
        val firstRoot = folder.newFolder("first").path
        val secondRoot = folder.newFolder("second").path
        val first = EtebaseLocalCache.getInstance(firstRoot, username)
        val second = EtebaseLocalCache.getInstance(secondRoot, username)
        try {
            first.saveStoken("first-token")
            second.saveStoken("second-token")
            EtebaseLocalCache.clear(firstRoot)
            assertEquals("second-token", second.loadStoken())
        } finally {
            EtebaseLocalCache.clear(firstRoot)
            EtebaseLocalCache.clear(secondRoot)
        }
    }
}
