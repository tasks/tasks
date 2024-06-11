package org.tasks.filters

interface FilterListItem {
    val itemType: Type

    fun areItemsTheSame(other: FilterListItem): Boolean

    enum class Type {
        ITEM,
        SUBHEADER,
    }
}
