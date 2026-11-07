package org.tasks.compose.pricing

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Test
import org.tasks.billing.BillingCatalog
import org.tasks.billing.BillingOption

@OptIn(ExperimentalTestApi::class)
class CloudTierCardTest {

    private val annual = BillingOption(
        priceId = "pri_30",
        sku = "annual_30",
        amountCents = 2999,
        entitles = true,
    )

    private val monthly = BillingOption(
        priceId = "pri_03",
        sku = "monthly_03",
        amountCents = 299,
        interval = BillingOption.INTERVAL_MONTH,
        entitles = true,
    )

    @Test
    fun thePriceAlreadyBilledCannotBeBoughtAgain() = runComposeUiTest {
        showPaywall(
            StoreBilling(
                catalog = catalog(annual, monthly),
                currentSku = "annual_30",
                onSelect = {},
            )
        )

        onNodeWithText("Subscribe for \$29.99/year").assertIsNotEnabled()
    }

    @Test
    fun aSubscriptionThatStoppedBillingCanBeBoughtAgain() = runComposeUiTest {
        showPaywall(
            StoreBilling(
                catalog = catalog(annual, monthly),
                currentSku = "annual_30",
                sellsCurrentSku = true,
                onSelect = {},
            )
        )

        onNodeWithText("Subscribe for \$29.99/year").assertIsEnabled()
    }

    private fun catalog(vararg prices: BillingOption) = BillingCatalog.from(prices.toList())
}
