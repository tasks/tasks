package org.tasks.filters

import org.tasks.CommonParcelable
import org.tasks.data.NO_COUNT
import org.tasks.data.NO_ORDER

interface Filter : FilterListItem, CommonParcelable {
    val valuesForNewTasks: String?
        get() = null
    val sql: String?
    val icon: Int
        get() = -1
    val title: String?
    val tint: Int
        get() = 0
    @Deprecated("Remove this")
    val count: Int
        get() = NO_COUNT
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
    fun disableHeaders(): Boolean = !supportsSorting()
}
