package org.tasks.filters

import org.tasks.CommonParcelize

@CommonParcelize
class EmptyFilter(
    override val sql: String? = "WHERE 0",
    override val title: String? = null
) : Filter {
    override fun areItemsTheSame(other: FilterListItem): Boolean = false
}