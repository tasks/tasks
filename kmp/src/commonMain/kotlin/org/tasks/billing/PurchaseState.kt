package org.tasks.billing

interface PurchaseState {
    val hasTasksAccount: Boolean
        get() = false
    val hasPro: Boolean
    fun purchasedThemes(): Boolean = hasPro
}
