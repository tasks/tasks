package com.todoroo.astrid.api

import kotlinx.parcelize.Parcelize

@Parcelize
class EmptyFilter(override val sql: String? = "WHERE 0", override val title: String? = null) : Filter {
    override fun areItemsTheSame(other: FilterListItem): Boolean = false
}