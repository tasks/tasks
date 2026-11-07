package org.tasks.compose.pricing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.Flow
import org.tasks.billing.SubscriptionProvider

data class PaywallEntitlement(
    val pro: Boolean,
    val cloud: Boolean,
) {
    companion object {
        fun of(info: SubscriptionProvider.SubscriptionInfo?) = PaywallEntitlement(
            pro = info != null,
            cloud = info?.isTasksSubscription == true,
        )
    }
}

fun PricingMode.satisfiedBy(
    openedWith: PaywallEntitlement,
    now: PaywallEntitlement,
): Boolean = when (this) {
    PricingMode.NYP_ONLY -> now.pro
    PricingMode.CLOUD_ONLY -> now.cloud && !openedWith.cloud
    PricingMode.BOTH -> (now.pro && !openedWith.pro) || (now.cloud && !openedWith.cloud)
}

@Composable
fun rememberPaywallSatisfied(
    mode: PricingMode,
    subscription: Flow<SubscriptionProvider.SubscriptionInfo?>,
): Boolean {
    var openedWithPro by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var openedWithCloud by rememberSaveable { mutableStateOf(false) }
    var satisfied by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(mode) {
        subscription.collect { info ->
            val now = PaywallEntitlement.of(info)
            if (openedWithPro == null) {
                openedWithPro = now.pro
                openedWithCloud = now.cloud
            }
            val openedWith = PaywallEntitlement(
                pro = openedWithPro == true,
                cloud = openedWithCloud,
            )
            if (mode.satisfiedBy(openedWith, now)) {
                satisfied = true
            }
        }
    }
    return satisfied
}
