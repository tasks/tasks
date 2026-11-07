package org.tasks.compose.settings

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.billing.ModifySubscription

@OptIn(ExperimentalTestApi::class)
class ManageSubscriptionSheetTest {

    private val routes = mutableListOf<String>()

    @Test
    fun aPlanChangeStaysInTheApp() = runComposeUiTest {
        showSheet(ModifySubscription.CHANGE_PLAN)

        clickModify()

        assertEquals(listOf("change_plan"), routes)
    }

    @Test
    fun aSponsorshipIsManagedOnGitHub() = runComposeUiTest {
        showSheet(ModifySubscription.OPEN_SPONSOR_PAGE)

        clickModify()

        assertEquals(listOf("sponsor_page"), routes)
    }

    @Test
    fun bothRoutesShareTheGenericTitle() = runComposeUiTest {
        showSheet(ModifySubscription.OPEN_SPONSOR_PAGE)

        assertTrue(
            "expected the generic modify row",
            onAllNodes(hasText(MODIFY)).fetchSemanticsNodes().isNotEmpty(),
        )
    }

    @Test
    fun aSubscriptionWithNothingToChangeOffersNoRow() = runComposeUiTest {
        showSheet(ModifySubscription.NONE)

        assertTrue(
            "expected no modify row",
            onAllNodes(hasText(MODIFY)).fetchSemanticsNodes().isEmpty(),
        )
    }

    private fun ComposeUiTest.showSheet(modifyAction: ModifySubscription) {
        setContent {
            ManageSubscriptionSheetContent(
                onUpgrade = {},
                onChangePlan = { routes.add("change_plan") },
                onOpenSponsorPage = { routes.add("sponsor_page") },
                onCancel = {},
                modifyAction = modifyAction,
            )
        }
    }

    private fun ComposeUiTest.clickModify() =
        onAllNodes(hasText(MODIFY)).onFirst().performClick()

    companion object {
        private const val MODIFY = "Modify subscription"
    }
}
