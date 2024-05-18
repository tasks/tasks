package com.todoroo.astrid.api

import com.todoroo.andlib.utility.AndroidUtilities
import kotlinx.parcelize.Parcelize
import org.tasks.data.NO_COUNT
import org.tasks.data.dao.TaskDao.TaskCriteria.activeAndVisible
import org.tasks.data.entity.Tag
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.data.sql.Criterion.Companion.and
import org.tasks.data.sql.Join.Companion.inner
import org.tasks.data.sql.QueryTemplate

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
        get() = tagData.color ?: 0

    val uuid: String
        get() = tagData.remoteId!!

    override fun areItemsTheSame(other: FilterListItem): Boolean {
        return other is TagFilter && tagData.id!! == other.tagData.id
    }
}
