package org.tasks.billing

interface StoreSubscriptionProvider : SubscriptionProvider {
    val current: SubscriptionProvider.SubscriptionInfo?
}
