@file:OptIn(ExperimentalTestApi::class)

package org.tasks.compose.pricing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue

internal fun ComposeUiTest.showPaywall(
    store: StoreBilling?,
    mode: PricingMode = PricingMode.CLOUD_ONLY,
    supporterFeatures: SupporterFeatures = SupporterFeatures.APP_STORE,
    onRestorePurchases: () -> Unit = {},
    onSignIn: () -> Unit = {},
    showSignIn: Boolean = true,
    supporterFirst: Boolean = false,
    subscribedSku: String? = null,
    width: Dp = 480.dp,
) = setContent {
    Box(modifier = Modifier.size(width, 2000.dp)) {
        PricingScreen(
            mode = mode,
            onBack = {},
            onSignIn = onSignIn,
            onRestorePurchases = onRestorePurchases,
            supporterFeatures = supporterFeatures,
            showSignIn = showSignIn,
            supporterFirst = supporterFirst,
            subscribedSku = subscribedSku,
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
