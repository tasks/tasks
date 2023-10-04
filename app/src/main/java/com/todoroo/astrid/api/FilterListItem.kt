package com.todoroo.astrid.api

import android.os.Parcelable
import androidx.annotation.LayoutRes
import org.tasks.R

interface FilterListItem : Parcelable {
    val itemType: Type

    fun areItemsTheSame(other: FilterListItem): Boolean
    fun areContentsTheSame(other: FilterListItem): Boolean

    enum class Type(@param:LayoutRes val layout: Int) {
        ITEM(R.layout.filter_adapter_row),
        ACTION(R.layout.filter_adapter_action),
        SUBHEADER(R.layout.filter_adapter_subheader),
        SEPARATOR(R.layout.filter_adapter_separator)
    }
}
