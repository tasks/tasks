package org.tasks.billing

interface PurchaseState {
    val hasPro: Boolean
    fun purchasedThemes(): Boolean = hasPro
}
