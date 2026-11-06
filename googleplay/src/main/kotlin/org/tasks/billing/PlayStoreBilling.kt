package org.tasks.billing

import org.tasks.compose.pricing.StoreBilling

fun PlayCheckoutViewModel.ViewState.toStoreBilling(
    suggestedPrice: Int?,
    onSelect: (BillingOption) -> Unit,
): StoreBilling = StoreBilling(
    catalog = catalog,
    loadingPrices = loadingPrices,
    busySku = busySku,
    currentSku = currentSku,
    sellsCurrentSku = false,
    suggestedPrice = suggestedPrice,
    knownCustomer = false,
    coveredElsewhere = coveredElsewhere,
    coveredBy = coveredBy,
    onSelect = onSelect,
)
