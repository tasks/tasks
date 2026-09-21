package org.tasks.filters

import org.tasks.billing.PurchaseState
import org.tasks.themes.TasksIcons

fun Filter.getIcon(purchaseState: PurchaseState): String? {
    if (purchaseState.hasPro) {
        icon?.takeIf { it.isNotBlank() }?.let { return it }
    }
    return when (this) {
        is TagFilter -> TasksIcons.LABEL
        is CaldavFilter -> TasksIcons.LIST
        is CustomFilter -> TasksIcons.FILTER_LIST
        is PlaceFilter -> TasksIcons.PLACE
        else -> icon
    }
}
