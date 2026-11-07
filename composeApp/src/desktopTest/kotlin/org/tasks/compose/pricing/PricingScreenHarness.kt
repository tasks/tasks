@file:OptIn(ExperimentalTestApi::class)

package org.tasks.compose.pricing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue

internal fun ComposeUiTest.showPaywall(
    store: StoreBilling?,
    mode: PricingMode = PricingMode.CLOUD_ONLY,
    hasSupporterSubscription: Boolean = false,
    onRestorePurchases: () -> Unit = {},
) = setContent {
    Box(modifier = Modifier.size(480.dp, 2000.dp)) {
        PricingScreen(
            mode = mode,
            onBack = {},
            onSignIn = {},
            onRestorePurchases = onRestorePurchases,
            hasSupporterSubscription = hasSupporterSubscription,
            store = store,
        )
    }
}

internal fun ComposeUiTest.assertShowing(text: String) = assertTrue(
    "expected to be showing \"$text\"",
    onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty(),
)

internal fun ComposeUiTest.assertNotShowing(text: String) = assertTrue(
    "expected not to be showing \"$text\"",
    onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isEmpty(),
)
