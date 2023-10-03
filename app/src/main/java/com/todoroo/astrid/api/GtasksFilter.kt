package com.todoroo.astrid.api

import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.ParcelCompat
import com.todoroo.andlib.sql.Criterion.Companion.and
import com.todoroo.andlib.sql.Join.Companion.left
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.astrid.data.Task
import org.tasks.R
import org.tasks.data.CaldavCalendar
import org.tasks.data.CaldavTask
import org.tasks.data.GoogleTask
import org.tasks.data.TaskDao.TaskCriteria.activeAndVisible

class GtasksFilter(
    val list: CaldavCalendar
) : Filter(list.name, getQueryTemplate(list), getValuesForNewTasks(list)) {

    init {
        id = list.id
        tint = list.color
        icon = list.getIcon()!!
        order = list.order
    }

    val account: String
        get() = list.account!!

    override fun supportsManualSort() = true

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(list, 0)
    }

    val remoteId: String
        get() = list.uuid!!

    override val menu = R.menu.menu_gtasks_list_fragment

    override fun areContentsTheSame(other: FilterListItem): Boolean {
        return super.areContentsTheSame(other) && list == (other as GtasksFilter).list
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<GtasksFilter> = object : Parcelable.Creator<GtasksFilter> {
            override fun createFromParcel(source: Parcel): GtasksFilter {
                return GtasksFilter(
                    ParcelCompat.readParcelable(source, javaClass.classLoader, CaldavCalendar::class.java)!!
                )
            }

            override fun newArray(size: Int): Array<GtasksFilter?> {
                return arrayOfNulls(size)
            }
        }

        private fun getQueryTemplate(list: CaldavCalendar): QueryTemplate {
            return QueryTemplate()
                .join(left(CaldavTask.TABLE, Task.ID.eq(CaldavTask.TASK)))
                .where(
                    and(
                        activeAndVisible(),
                        CaldavTask.DELETED.eq(0),
                        CaldavTask.CALENDAR.eq(list.uuid)
                    )
                )
        }

        private fun getValuesForNewTasks(list: CaldavCalendar): Map<String, Any> {
            val values: MutableMap<String, Any> = HashMap()
            values[GoogleTask.KEY] = list.uuid!!
            return values
        }
    }
}
