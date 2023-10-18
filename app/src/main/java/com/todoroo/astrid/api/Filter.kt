package com.todoroo.astrid.api

import android.os.Parcelable
import com.todoroo.astrid.api.Filter.Companion.NO_COUNT
import kotlinx.parcelize.Parcelize

interface Filter : FilterListItem, Parcelable {
    val valuesForNewTasks: String?
        get() = null
    val sql: String?
    val icon: Int
        get() = -1
    val title: String?
    val tint: Int
        get() = 0
    @Deprecated("Remove this")
    var count: Int
    val order: Int
        get() = NO_ORDER
    override val itemType: FilterListItem.Type
        get() = FilterListItem.Type.ITEM
    val isReadOnly: Boolean
        get() = false
    val isWritable: Boolean
        get() = !isReadOnly

    fun supportsManualSort(): Boolean = false
    fun supportsHiddenTasks(): Boolean = true
    fun supportsSubtasks(): Boolean = true
    fun supportsSorting(): Boolean = true

    companion object {
        const val NO_ORDER = -1
        const val NO_COUNT = -1
    }
}

@Deprecated("Use manual ordering")
interface AstridOrderingFilter : Filter {
    var filterOverride: String?

    fun getSqlQuery(): String = filterOverride ?: sql!!
}

@Parcelize
data class FilterImpl(
    override val title: String? = null,
    override val sql: String? = null,
    override val valuesForNewTasks: String? = null,
    override val icon: Int = -1,
    override val tint: Int = 0,
    override var count: Int = NO_COUNT,
) : Filter {
    override fun areItemsTheSame(other: FilterListItem): Boolean {
        return other is Filter && sql == other.sql
    }
}
