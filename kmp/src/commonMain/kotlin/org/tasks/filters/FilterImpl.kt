package org.tasks.filters

import org.tasks.CommonParcelize

@CommonParcelize
data class FilterImpl(
    override val title: String? = null,
    override val sql: String? = null,
    override val valuesForNewTasks: String? = null,
    override val icon: Int = -1,
    override val tint: Int = 0,
) : Filter {
    override fun areItemsTheSame(other: FilterListItem): Boolean {
        return other is Filter && sql == other.sql
    }
}
