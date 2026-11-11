package org.tasks.compose.sort

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.todoroo.astrid.core.SortHelper
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The shared sort sheet's "My order" option: enabled and wired up for filters that support
 * manual sorting (CalDAV lists), disabled for the rest.
 */
@OptIn(ExperimentalTestApi::class)
class SortSheetManualSortTest {
    private val myOrderLabel = "My order"

    @Test
    fun myOrderOptionIsAvailableForCaldavFilters() = runComposeUiTest {
        val manualSortCalls = mutableListOf<Boolean>()
        setContent {
            MaterialTheme {
                SortSheetContent(
                    manualSortSelected = false,
                    manualSortEnabled = true,
                    astridSortEnabled = false,
                    selected = SortHelper.SORT_DUE,
                    setManualSort = { manualSortCalls.add(it) },
                    setAstridSort = {},
                    onSelected = {},
                )
            }
        }
        waitForIdle()

        onNodeWithText(myOrderLabel).assertIsDisplayed()
        onNodeWithText(myOrderLabel).performClick()
        assertEquals(listOf(true), manualSortCalls)
    }

    @Test
    fun myOrderOptionIsDisabledForOtherFilters() = runComposeUiTest {
        val manualSortCalls = mutableListOf<Boolean>()
        setContent {
            MaterialTheme {
                SortSheetContent(
                    manualSortSelected = false,
                    manualSortEnabled = false,
                    astridSortEnabled = false,
                    selected = SortHelper.SORT_DUE,
                    setManualSort = { manualSortCalls.add(it) },
                    setAstridSort = {},
                    onSelected = {},
                )
            }
        }
        waitForIdle()

        onNodeWithText(myOrderLabel).assertIsDisplayed()
        onNodeWithText("Not available for tags, filters, or places").assertIsDisplayed()
        onNodeWithText(myOrderLabel).performClick()
        assertEquals(emptyList<Boolean>(), manualSortCalls)
    }
}
