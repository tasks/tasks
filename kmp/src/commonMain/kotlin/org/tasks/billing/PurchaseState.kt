package org.tasks.billing

interface PurchaseState {
    val hasTasksAccount: Boolean
    val hasPro: Boolean
    val hasTasksSubscription: Boolean
    fun purchasedThemes(): Boolean = hasPro
}
