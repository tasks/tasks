package org.tasks.billing

import kotlinx.serialization.Serializable

@Serializable
data class Sku(
    val productId: String,
    val price: String,
)