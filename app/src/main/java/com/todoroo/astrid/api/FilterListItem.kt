package com.todoroo.astrid.api

import androidx.annotation.LayoutRes
import org.tasks.R

interface FilterListItem {
    val itemType: Type

    fun areItemsTheSame(other: FilterListItem): Boolean

    enum class Type(@param:LayoutRes val layout: Int) {
        ITEM(R.layout.filter_adapter_row),
        SUBHEADER(R.layout.filter_adapter_subheader),
    }
}
