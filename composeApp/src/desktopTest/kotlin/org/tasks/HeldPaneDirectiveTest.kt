package org.tasks

import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalTestApi::class, ExperimentalMaterial3AdaptiveApi::class)
class HeldPaneDirectiveTest {

    @Test
    fun theListComesBackAfterTheWindowWidensWhileSettingsCoversIt() = runComposeUiTest {
        val app = paneCountChangeUnderSettings(hold = true)

        app.onNodeWithText(TASK_LIST).assertIsDisplayed()
        app.onNodeWithText(PLACEHOLDER).assertIsDisplayed()
    }

    @Test
    fun withoutTheHoldTheListIsReplacedByThePlaceholder() = runComposeUiTest {
        val app = paneCountChangeUnderSettings(hold = false)

        assertEquals(
            "list panes showing",
            0,
            app.onAllNodes(hasText(TASK_LIST)).fetchSemanticsNodes().size,
        )
        app.onNodeWithText(PLACEHOLDER).assertIsDisplayed()
    }

    @Test
    fun aResizeWhileTheListIsShowingStillTakesEffect() = runComposeUiTest {
        val backStack = mutableStateListOf<NavKey>(TaskListDestination)
        var twoPaneLayout by mutableStateOf(false)
        setContent { ListDetailNavDisplay(backStack, twoPaneLayout, hold = true) }

        onNodeWithText(TASK_LIST).assertIsDisplayed()
        assertEquals(
            "detail panes showing in one-pane mode",
            0,
            onAllNodes(hasText(PLACEHOLDER)).fetchSemanticsNodes().size,
        )

        runOnIdle { twoPaneLayout = true }

        onNodeWithText(TASK_LIST).assertIsDisplayed()
        onNodeWithText(PLACEHOLDER).assertIsDisplayed()
    }

    private fun ComposeUiTest.paneCountChangeUnderSettings(hold: Boolean): ComposeUiTest {
        val backStack = mutableStateListOf<NavKey>(TaskListDestination)
        var twoPaneLayout by mutableStateOf(false)
        setContent { ListDetailNavDisplay(backStack, twoPaneLayout, hold) }

        onNodeWithText(TASK_LIST).assertIsDisplayed()

        runOnIdle { backStack.add(SettingsDestination) }
        onNodeWithText(SETTINGS).assertIsDisplayed()

        runOnIdle { twoPaneLayout = true }
        onNodeWithText(SETTINGS).assertIsDisplayed()

        runOnIdle { backStack.removeAt(backStack.lastIndex) }
        waitForIdle()
        return this
    }

    @androidx.compose.runtime.Composable
    private fun ListDetailNavDisplay(
        backStack: MutableList<NavKey>,
        twoPaneLayout: Boolean,
        hold: Boolean,
    ) {
        val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
        val windowPaneDirective = remember(windowAdaptiveInfo, twoPaneLayout) {
            val directive = calculatePaneScaffoldDirective(windowAdaptiveInfo)
            if (twoPaneLayout) directive else directive.copy(maxHorizontalPartitions = 1)
        }
        val paneDirective = if (hold) {
            rememberHeldPaneDirective(
                directive = windowPaneDirective,
                showing = backStack.lastOrNull() is TaskListDestination,
            )
        } else {
            windowPaneDirective
        }
        val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = paneDirective)
        NavDisplay(
            backStack = backStack,
            sceneStrategies = listOf(listDetailStrategy),
            entryProvider = entryProvider {
                entry<TaskListDestination>(
                    metadata = ListDetailSceneStrategy.listPane(
                        detailPlaceholder = { Text(PLACEHOLDER) },
                    ),
                ) {
                    Text(TASK_LIST)
                }
                entry<SettingsDestination> { Text(SETTINGS) }
            },
        )
    }

    companion object {
        private const val TASK_LIST = "task list"
        private const val SETTINGS = "settings"
        private const val PLACEHOLDER = "no task selected"
    }
}
