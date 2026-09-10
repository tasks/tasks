package org.tasks.mcp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolPermissionsTest {

    @Test
    fun readOnlyAllowsNothingThatWrites() {
        val permissions = ToolPermissions.of(AccessMode.ReadOnly)

        assertFalse(permissions.isWrite)
        McpToolCatalog.tools.filter { it.write }.forEach {
            assertFalse("${it.name} must not be allowed", permissions.allows(it.name))
        }
        assertTrue(permissions.allows("list_tasks"))
    }

    @Test
    fun readWriteAllowsEverythingThereIs() {
        val permissions = ToolPermissions.of(AccessMode.ReadWrite)

        assertEquals(McpToolCatalog.names, permissions.enabled)
        assertTrue(permissions.isWrite)
        assertFalse("nothing is switched off", permissions.isPartial)
    }

    @Test
    fun advancedAllowsExactlyWhatWasTicked() {
        val permissions = ToolPermissions.of(AccessMode.Advanced, setOf("list_tasks", "delete_task"))

        assertEquals(setOf("list_tasks", "delete_task"), permissions.enabled)
        assertTrue(permissions.allows("delete_task"))
        assertFalse(permissions.allows("list_places"))
        assertTrue(permissions.isPartial)
    }

    @Test
    fun aStoredNameFromAnotherReleaseIsIgnored() {
        val permissions = ToolPermissions.of(
            AccessMode.Advanced,
            setOf("list_tasks", "delete_everything", "list_tasks_v2"),
        )

        assertEquals(setOf("list_tasks"), permissions.enabled)
        assertFalse(permissions.allows("delete_everything"))
    }

    @Test
    fun advancedIsWriteOnlyWhenAWriteToolIsTicked() {
        assertFalse(ToolPermissions.of(AccessMode.Advanced, McpToolCatalog.readTools).isWrite)
        assertTrue(ToolPermissions.of(AccessMode.Advanced, setOf("delete_task")).isWrite)
    }

    @Test
    fun anEmptyAdvancedSelectionExposesNothing() {
        val permissions = ToolPermissions.of(AccessMode.Advanced)

        assertEquals(emptySet<String>(), permissions.enabled)
        assertFalse(permissions.isWrite)
    }

    @Test
    fun anUnknownStoredModeFallsBackToTheSafeEnd() {
        assertEquals(AccessMode.ReadOnly, AccessMode.of(null))
        assertEquals(AccessMode.ReadOnly, AccessMode.of(""))
        assertEquals(AccessMode.ReadOnly, AccessMode.of("ReadOnlyIsh"))
        assertEquals(AccessMode.Advanced, AccessMode.of("Advanced"))
    }

    @Test
    fun theCatalogueGroupsEveryToolExactlyOnce() {
        val grouped = McpToolCatalog.byGroup.flatMap { (_, tools) -> tools.map { it.name } }

        assertEquals(McpToolCatalog.tools.size, grouped.size)
        assertEquals(McpToolCatalog.names, grouped.toSet())
    }
}
