package org.tasks.compose.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.mcp.AccessMode
import org.tasks.mcp.McpServerState
import org.tasks.mcp.McpToolCatalog
import org.tasks.mcp.McpToolGroup

@OptIn(ExperimentalTestApi::class)
class McpToolSettingsTest {

    private val changes = mutableListOf<Pair<Set<String>, Boolean>>()
    private val modes = mutableListOf<AccessMode>()

    @Test
    fun thePresetModesShowNoToolList() = runComposeUiTest {
        setContent { Screen(AccessMode.ReadWrite) }
        awaitText("Read only")

        onAllNodes(hasText("list_places")).fetchSemanticsNodes().let {
            assertTrue("a preset mode must not show the tool list", it.isEmpty())
        }
    }

    @Test
    fun advancedListsEveryToolThereIs() = runComposeUiTest {
        setContent { Screen(AccessMode.Advanced) }
        awaitText("list_tasks")

        McpToolCatalog.names.forEach {
            assertTrue(
                "$it has no row, so it cannot be switched off",
                onAllNodes(hasText(it)).fetchSemanticsNodes().isNotEmpty(),
            )
        }
    }

    @Test
    fun tickingOneToolAsksForThatToolAlone() = runComposeUiTest {
        setContent { Screen(AccessMode.Advanced, enabled = McpToolCatalog.names) }
        awaitText("delete_task")

        onNodeWithText("delete_task").performScrollTo().performClick()

        assertEquals(listOf(setOf("delete_task") to false), changes)
    }

    @Test
    fun aGroupHeaderTogglesItsWholeGroupInOneGo() = runComposeUiTest {
        setContent { Screen(AccessMode.Advanced, enabled = McpToolCatalog.names) }
        awaitText("list_places")

        onNodeWithText("Places").performScrollTo().performClick()

        val places = McpToolCatalog.byGroup
            .single { it.first == McpToolGroup.Places }
            .second
            .map { it.name }
            .toSet()
        assertEquals(listOf(places to false), changes)
        assertEquals(setOf("list_places", "create_place", "update_place", "delete_place"), places)
    }

    @Test
    fun aGroupThatIsAlreadyOffSwitchesBackOn() = runComposeUiTest {
        setContent {
            Screen(AccessMode.Advanced, enabled = McpToolCatalog.names - "list_places")
        }
        awaitText("list_places")

        onNodeWithText("Places").performScrollTo().performClick()

        assertEquals(true, changes.single().second)
    }

    @Test
    fun choosingAdvancedAsksForAdvanced() = runComposeUiTest {
        setContent { Screen(AccessMode.ReadOnly) }
        awaitText("Advanced")

        onNodeWithText("Advanced").performScrollTo().performClick()

        assertEquals(listOf(AccessMode.Advanced), modes)
    }

    @androidx.compose.runtime.Composable
    private fun Screen(mode: AccessMode, enabled: Set<String> = emptySet()) {
        McpServerContent(
            state = McpServerState(enabled = true, mode = mode, enabledTools = enabled),
            onEnabledChange = {},
            onAccessModeChange = { modes += it },
            onToolsEnabledChange = { names, on -> changes += names to on },
            onRegenerateToken = {},
        )
    }

    private fun androidx.compose.ui.test.ComposeUiTest.awaitText(text: String) =
        waitUntil { onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
}
