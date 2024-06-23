package org.tasks.filters

import com.todoroo.astrid.api.CustomFilter
import org.tasks.billing.Inventory
import org.tasks.themes.TasksIcons

fun Filter.getIcon(inventory: Inventory): String {
    if (inventory.hasPro) {
        icon?.takeIf { it.isNotBlank() }?.let { return it }
    }
    return when (this) {
        is TagFilter -> TasksIcons.LABEL
        is GtasksFilter,
        is CaldavFilter -> TasksIcons.LIST

        is CustomFilter -> TasksIcons.FILTER_LIST
        is PlaceFilter -> TasksIcons.PLACE
        else -> icon!!
    }
}
