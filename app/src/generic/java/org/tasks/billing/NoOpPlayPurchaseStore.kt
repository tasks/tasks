package org.tasks.billing

import javax.inject.Inject

class NoOpPlayPurchaseStore @Inject constructor() : PlayPurchaseStore {
    override suspend fun load(): List<PlayPurchase> = emptyList()

    override suspend fun save(purchases: List<PlayPurchase>) = Unit
}
