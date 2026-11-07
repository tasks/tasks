package org.tasks.billing

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import org.tasks.compose.pricing.StoreBilling

class HiltPlayStoreBillingProvider : StoreBillingProvider {
    @Composable
    override fun storeBilling(suggestedPrice: Int?, onPurchased: () -> Unit): StoreBilling =
        playStoreBilling(
            viewModel = hiltViewModel<PlayCheckoutHiltViewModel>(),
            suggestedPrice = suggestedPrice,
            onPurchased = onPurchased,
        )
}
