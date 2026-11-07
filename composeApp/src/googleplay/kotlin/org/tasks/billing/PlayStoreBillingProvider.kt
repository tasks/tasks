package org.tasks.billing

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel
import org.tasks.compose.pricing.StoreBilling

class PlayStoreBillingProvider : StoreBillingProvider {
    @Composable
    override fun storeBilling(suggestedPrice: Int?, onPurchased: () -> Unit): StoreBilling =
        playStoreBilling(
            viewModel = koinViewModel(),
            suggestedPrice = suggestedPrice,
            onPurchased = onPurchased,
        )
}
