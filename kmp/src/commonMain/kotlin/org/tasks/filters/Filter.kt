package org.tasks.filters

import androidx.compose.runtime.Stable
import org.tasks.CommonParcelable
import org.tasks.data.NO_COUNT
import org.tasks.data.NO_ORDER

@Stable
abstract class Filter : FilterListItem, CommonParcelable {
    open val valuesForNewTasks: String?
        get() = null
    abstract val sql: String?
    open val icon: String?
        get() = null
    abstract val title: String?
    open val tint: Int
        get() = 0
    @Deprecated("Remove this")
    open val count: Int
        get() = NO_COUNT
    open val order: Int
        get() = NO_ORDER
    override val itemType: FilterListItem.Type
        get() = FilterListItem.Type.ITEM
    open val isReadOnly: Boolean
        get() = false
    val isWritable: Boolean
        get() = !isReadOnly

    open fun supportsManualSort(): Boolean = false
    open fun supportsHiddenTasks(): Boolean = true
    open fun supportsSubtasks(): Boolean = true
    open fun supportsSorting(): Boolean = true
    open fun disableHeaders(): Boolean = !supportsSorting()
}
