package com.todoroo.astrid.api

import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.ParcelCompat
import com.todoroo.andlib.sql.Criterion.Companion.and
import com.todoroo.andlib.sql.Join.Companion.inner
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.astrid.data.Task
import org.tasks.R
import org.tasks.data.Tag
import org.tasks.data.TagData
import org.tasks.data.TaskDao.TaskCriteria.activeAndVisible

class TagFilter(
    val tagData: TagData
) : Filter(tagData.name, queryTemplate(tagData.remoteId), getValuesForNewTask(tagData)) {

    init {
        id = tagData.id!!
        tint = tagData.getColor()!!
        icon = tagData.getIcon()!!
        order = tagData.order
    }

    val uuid: String
        get() = tagData.remoteId!!

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(tagData, 0)
    }

    override fun supportsAstridSorting() = true

    override val menu = R.menu.menu_tag_view_fragment

    override fun areContentsTheSame(other: FilterListItem): Boolean {
        return tagData == (other as TagFilter).tagData
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<TagFilter> = object : Parcelable.Creator<TagFilter> {
            override fun createFromParcel(source: Parcel): TagFilter {
                return TagFilter(
                    ParcelCompat.readParcelable(source, javaClass.classLoader, TagData::class.java)!!
                )
            }

            override fun newArray(size: Int): Array<TagFilter?> {
                return arrayOfNulls(size)
            }
        }

        private fun queryTemplate(uuid: String?): QueryTemplate {
            return QueryTemplate()
                .join(inner(Tag.TABLE, Task.ID.eq(Tag.TASK)))
                .where(and(Tag.TAG_UID.eq(uuid), activeAndVisible()))
        }

        private fun getValuesForNewTask(tagData: TagData): Map<String, Any> {
            val values: MutableMap<String, Any> = HashMap()
            values[Tag.KEY] = tagData.name!!
            return values
        }
    }
}
