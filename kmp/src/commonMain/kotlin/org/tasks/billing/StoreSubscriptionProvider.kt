package org.tasks.billing

interface StoreSubscriptionProvider : SubscriptionProvider {
    override val current: SubscriptionProvider.SubscriptionInfo?
}
