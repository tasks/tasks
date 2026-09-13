package org.tasks.etebase

import com.etebase.client.FileSystemCache
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Exercise the bundled native filesystem cache without server credentials or network access. */
class EtebaseLocalCacheTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun globalClearRemovesPersistedScopesWithoutOpeningWrappers() {
        val root = folder.newFolder("app")
        val username = "same@example.invalid"
        val scopes = listOf("account-one", "account-two").map {
            File(EtebaseCacheLocation.root(root.path, it))
        }
        // Direct native writes leave the application wrapper registry empty, as after restart.
        scopes.forEach { scope ->
            val native = FileSystemCache.create(scope.path, username)
            native.saveStoken("persisted-token")
            assertEquals("persisted-token", native.loadStoken())
        }
        val legacy = FileSystemCache.create(root.path, username)
        legacy.saveStoken("unopened-legacy-token")
        val otherRoot = EtebaseCacheLocation.root(folder.newFolder("other-app").path, "account-one")
        val other = FileSystemCache.create(otherRoot, username)
        other.saveStoken("other-token")
        val neighbor = root.resolve("etebase-accounts-backup/keep").apply {
            parentFile.mkdirs()
            writeText("keep")
        }

        EtebaseLocalCache.clear(root.path)

        scopes.forEach { assertFalse("Persisted scoped cache survived global clear: $it", it.exists()) }
        assertEquals("unopened-legacy-token", legacy.loadStoken())
        assertEquals("other-token", other.loadStoken())
        assertEquals("keep", neighbor.readText())
    }

    @Test fun globalClearEvictsOpenedScopesAndRetainsLegacyClearing() = runBlocking {
        val root = folder.newFolder("app").path
        val username = "same@example.invalid"
        val scopedRoot = EtebaseCacheLocation.root(root, "account-one")
        val scoped = EtebaseLocalCache.getInstance(scopedRoot, username)
        val legacy = EtebaseLocalCache.getInstance(root, username)
        scoped.saveStoken("scoped-token")
        legacy.saveStoken("legacy-token")

        EtebaseLocalCache.clear(root)

        assertFalse(File(scopedRoot).exists())
        assertNull(legacy.loadStoken())
        val reopened = EtebaseLocalCache.getInstance(scopedRoot, username)
        assertNotSame(scoped, reopened)
        assertNull(reopened.loadStoken())
        assertNotSame(legacy, EtebaseLocalCache.getInstance(root, username))
        EtebaseLocalCache.clear(root)
    }

    @Test fun globalClearDoesNotFollowNamespaceSymlink() = runBlocking {
        val root = folder.newFolder("app")
        val outside = folder.newFolder("outside")
        val namespace = root.resolve("etebase-accounts").toPath()
        Files.createSymbolicLink(namespace, outside.toPath())
        val scopedRoot = EtebaseCacheLocation.root(root.path, "account-one")
        val scoped = EtebaseLocalCache.getInstance(scopedRoot, "same@example.invalid")
        scoped.saveStoken("outside-token")
        val outsideCache = FileSystemCache.create(
            outside.resolve(File(scopedRoot).name).path, "same@example.invalid"
        )

        EtebaseLocalCache.clear(root.path)

        assertEquals("outside-token", outsideCache.loadStoken())
        assertFalse(Files.isSymbolicLink(namespace))
    }

    @Test fun globalClearDoesNotFollowScopeOrNestedSymlinks() {
        val root = folder.newFolder("app")
        val outside = folder.newFolder("outside")
        val native = FileSystemCache.create(outside.path, "same@example.invalid")
        native.saveStoken("outside-token")
        val scopedRoot = File(EtebaseCacheLocation.root(root.path, "account-one"))
        FileSystemCache.create(scopedRoot.path, "same@example.invalid").saveStoken("scoped-token")
        Files.createSymbolicLink(scopedRoot.resolve("nested-link").toPath(), outside.toPath())
        Files.createSymbolicLink(
            File(EtebaseCacheLocation.root(root.path, "account-two")).toPath(), outside.toPath()
        )
        Files.createSymbolicLink(scopedRoot.resolve("dangling-link").toPath(), outside.resolve("missing").toPath())

        EtebaseLocalCache.clear(root.path)

        assertEquals("outside-token", native.loadStoken())
        assertFalse(root.resolve("etebase-accounts").exists())
    }

    @Test fun globalClearPreservesRegisteredRootWithTraversalOutsideNamespace() = runBlocking {
        val root = folder.newFolder("app")
        root.resolve("etebase-accounts").mkdirs()
        val outsideRoot = root.resolve("etebase-accounts/../unrelated").path
        val outside = EtebaseLocalCache.getInstance(outsideRoot, "same@example.invalid")
        outside.saveStoken("outside-token")

        EtebaseLocalCache.clear(root.path)

        // Use the normalized path since deletion also removes the empty namespace directory.
        val native = FileSystemCache.create(root.resolve("unrelated").path, "same@example.invalid")
        assertEquals("outside-token", native.loadStoken())
        assertSame(outside, EtebaseLocalCache.getInstance(outsideRoot, "same@example.invalid"))
    }

    @Test fun globalClearWithoutScopedCachesIsRepeatable() {
        val root = folder.newFolder("app")
        EtebaseLocalCache.clear(root.path)
        EtebaseLocalCache.clear(root.path)
        assertEquals(emptyList<String>(), root.list()!!.toList())
    }

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
