package org.tasks.billing

import androidx.compose.runtime.Composable
import org.tasks.compose.pricing.StoreBilling

interface StoreBillingProvider {
    @Composable
    fun storeBilling(suggestedPrice: Int?, onPurchased: () -> Unit): StoreBilling
}
