package com.todoroo.astrid.api

import kotlinx.parcelize.Parcelize
import org.tasks.filters.Filter
import org.tasks.filters.FilterListItem

@Parcelize
class EmptyFilter(
    override val sql: String? = "WHERE 0",
    override val title: String? = null
) : Filter {
    override fun areItemsTheSame(other: FilterListItem): Boolean = false
}