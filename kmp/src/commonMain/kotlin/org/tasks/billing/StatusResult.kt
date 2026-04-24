package org.tasks.billing

data class StatusResult(
    val status: String,
    val jwt: String? = null,
    val refreshToken: String? = null,
    val sku: String? = null,
    val formattedPrice: String? = null,
)
