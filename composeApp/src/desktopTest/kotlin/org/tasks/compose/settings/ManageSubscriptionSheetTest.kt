package org.tasks.compose.settings

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ManageSubscriptionSheetTest {

    @Test
    fun aCardSubscriptionIsChangedRatherThanManagedElsewhere() = runComposeUiTest {
        showSheet(modifyTitle = "Change plan")

        assertShowing("Change plan")
    }

    @Test
    fun anythingElseKeepsTheGenericTitle() = runComposeUiTest {
        showSheet(modifyTitle = null)

        assertShowing("Modify subscription")
    }

    @Test
    fun aSubscriptionWithNothingToChangeOffersNoRow() = runComposeUiTest {
        showSheet(modifyTitle = "Change plan", showModify = false)

        assertTrue(
            "expected no modify row",
            onAllNodes(hasText("Change plan")).fetchSemanticsNodes().isEmpty(),
        )
    }

    private fun ComposeUiTest.showSheet(
        modifyTitle: String?,
        showModify: Boolean = true,
    ) {
        setContent {
            ManageSubscriptionSheetContent(
                onUpgrade = {},
                onModify = {},
                onCancel = {},
                showModify = showModify,
                modifyTitle = modifyTitle,
            )
        }
    }

    private fun ComposeUiTest.assertShowing(text: String) = assertTrue(
        "expected to be showing \"$text\"",
        onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty(),
    )
}
