package com.todoroo.astrid.api

import com.todoroo.andlib.sql.Criterion.Companion.and
import com.todoroo.andlib.sql.Join.Companion.inner
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.astrid.api.Filter.Companion.NO_COUNT
import com.todoroo.astrid.data.Task
import kotlinx.parcelize.Parcelize
import org.tasks.data.Tag
import org.tasks.data.TagData
import org.tasks.data.TaskDao.TaskCriteria.activeAndVisible

@Parcelize
data class TagFilter(
    val tagData: TagData,
    override val count: Int = NO_COUNT,
    override var filterOverride: String? = null,
) : AstridOrderingFilter {
    override val title: String?
        get() = tagData.name
    override val sql: String
        get() = QueryTemplate()
            .join(inner(Tag.TABLE, Task.ID.eq(Tag.TASK)))
            .where(and(Tag.TAG_UID.eq(uuid), activeAndVisible()))
            .toString()

    override val order: Int
        get() = tagData.order

    override val valuesForNewTasks: String
        get() = AndroidUtilities.mapToSerializedString(mapOf(Tag.KEY to tagData.name!!))

    override val icon: Int
        get() = tagData.getIcon()!!

    override val tint: Int
        get() = tagData.getColor()!!

    val uuid: String
        get() = tagData.remoteId!!

    override fun areItemsTheSame(other: FilterListItem): Boolean {
        return other is TagFilter && tagData.id!! == other.tagData.id
    }
}
