package org.tasks.compose.pricing

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.tasks.billing.BillingCatalog
import org.tasks.billing.BillingOption
import org.tasks.billing.SubscriptionSkus
import org.tasks.billing.UnlockSource
import org.tasks.previews.PREVIEW_NIGHT_MODE
import org.tasks.themes.TasksTheme

internal data class PaywallPreviewState(
    val name: String,
    val mode: PricingMode = PricingMode.BOTH,
    val showSignIn: Boolean = true,
    val supporterFeatures: SupporterFeatures = SupporterFeatures.APP_STORE,
    val supporterFirst: Boolean = false,
    val store: StoreBilling? = previewStore(),
)

private val previewCatalog = BillingCatalog.from(
    listOf(
        BillingOption(
            priceId = "pri_cloud_year",
            sku = SubscriptionSkus.CLOUD_ANNUAL,
            amountCents = 2999,
            entitles = true,
            trialDays = 7,
        ),
        BillingOption(
            priceId = "pri_cloud_month",
            sku = SubscriptionSkus.CLOUD_MONTHLY,
            amountCents = 299,
            interval = BillingOption.INTERVAL_MONTH,
            entitles = true,
            trialDays = 7,
        ),
    ) + listOf(1, 2, 3, 5, 10, 15, 20, 25).map { dollars ->
        BillingOption(
            priceId = "pri_support_year_$dollars",
            sku = SubscriptionSkus.sku(dollars),
            amountCents = dollars * 100 - 1,
            trialDays = 7,
        )
    } + listOf(1, 2).map { dollars ->
        BillingOption(
            priceId = "pri_support_month_$dollars",
            sku = SubscriptionSkus.sku(dollars, monthly = true),
            amountCents = dollars * 100 - 1,
            interval = BillingOption.INTERVAL_MONTH,
            trialDays = 7,
        )
    }
)

private fun previewStore(
    catalog: BillingCatalog = previewCatalog,
    loadingPrices: Boolean = false,
    busySku: String? = null,
    currentSku: String? = null,
    sellsCurrentSku: Boolean = false,
    suggestedPrice: Int? = null,
    knownCustomer: Boolean = false,
    coveredElsewhere: Boolean = false,
    coveredBy: UnlockSource? = null,
) = StoreBilling(
    catalog = catalog,
    loadingPrices = loadingPrices,
    busySku = busySku,
    currentSku = currentSku,
    sellsCurrentSku = sellsCurrentSku,
    suggestedPrice = suggestedPrice,
    knownCustomer = knownCustomer,
    coveredElsewhere = coveredElsewhere,
    coveredBy = coveredBy,
    onSelect = {},
)

internal val paywallPreviewStates = listOf(
    PaywallPreviewState("New customer"),
    PaywallPreviewState("F-Droid features", supporterFeatures = SupporterFeatures.ANDROID_CARD),
    PaywallPreviewState("Desktop features", supporterFeatures = SupporterFeatures.DESKTOP),
    PaywallPreviewState("Opened from DAVx5", supporterFirst = true),
    PaywallPreviewState("Cloud only", mode = PricingMode.CLOUD_ONLY),
    PaywallPreviewState("Supporter only", mode = PricingMode.NYP_ONLY),
    PaywallPreviewState(
        "Cloud subscriber",
        store = previewStore(currentSku = SubscriptionSkus.CLOUD_ANNUAL, knownCustomer = true),
        showSignIn = false,
    ),
    PaywallPreviewState(
        "Supporter at \$2/year",
        store = previewStore(
            currentSku = SubscriptionSkus.sku(2),
            suggestedPrice = 2,
            knownCustomer = true,
        ),
    ),
    PaywallPreviewState(
        "Supporter at \$2/year, slider at \$5",
        store = previewStore(
            currentSku = SubscriptionSkus.sku(2),
            suggestedPrice = 5,
            knownCustomer = true,
        ),
    ),
    PaywallPreviewState(
        "Cancelled cloud subscription",
        store = previewStore(
            currentSku = SubscriptionSkus.CLOUD_ANNUAL,
            sellsCurrentSku = true,
            knownCustomer = true,
        ),
        showSignIn = false,
    ),
    PaywallPreviewState("Returning customer", store = previewStore(knownCustomer = true)),
    PaywallPreviewState("Signed in", showSignIn = false),
    PaywallPreviewState(
        "Paid on Google Play",
        store = previewStore(coveredElsewhere = true, coveredBy = UnlockSource.GOOGLE_PLAY),
    ),
    PaywallPreviewState(
        "GitHub sponsor",
        store = previewStore(coveredElsewhere = true, coveredBy = UnlockSource.GITHUB),
    ),
    PaywallPreviewState(
        "Purchase in progress",
        store = previewStore(busySku = SubscriptionSkus.CLOUD_ANNUAL),
    ),
    PaywallPreviewState(
        "Loading prices",
        store = previewStore(catalog = BillingCatalog(), loadingPrices = true),
    ),
    PaywallPreviewState("Billing unavailable", store = previewStore(catalog = BillingCatalog())),
    PaywallPreviewState("No store", store = null),
)

internal class PaywallPreviewStates : PreviewParameterProvider<PaywallPreviewState> {
    override val values = paywallPreviewStates.asSequence()

    override fun getDisplayName(index: Int): String = paywallPreviewStates[index].name
}

@Composable
internal fun PaywallPreviewContent(state: PaywallPreviewState) {
    PricingScreen(
        mode = state.mode,
        onBack = {},
        onSignIn = {},
        onRestorePurchases = {},
        showSignIn = state.showSignIn,
        supporterFeatures = state.supporterFeatures,
        supporterFirst = state.supporterFirst,
        store = state.store,
    )
}

@Preview(showBackground = true, widthDp = 1000, heightDp = 900)
@Composable
private fun PaywallPreview(
    @PreviewParameter(PaywallPreviewStates::class) state: PaywallPreviewState,
) {
    TasksTheme {
        PaywallPreviewContent(state)
    }
}

@Preview(showBackground = true, name = "Dark", uiMode = PREVIEW_NIGHT_MODE, widthDp = 1000, heightDp = 900)
@Composable
private fun PaywallDarkPreview() {
    TasksTheme {
        PaywallPreviewContent(paywallPreviewStates.first())
    }
}
