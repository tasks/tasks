package com.todoroo.astrid.api

import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.ParcelCompat
import com.todoroo.andlib.sql.Criterion
import com.todoroo.andlib.sql.Criterion.Companion.and
import com.todoroo.andlib.sql.Join.Companion.left
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.astrid.data.Task
import org.tasks.R
import org.tasks.data.CaldavCalendar
import org.tasks.data.CaldavTask
import org.tasks.data.TaskDao.TaskCriteria.activeAndVisible

class CaldavFilter(
    val calendar: CaldavCalendar
) : Filter(calendar.name, queryTemplate(calendar), getValuesForNewTask(calendar)) {

    init {
        id = calendar.id
        tint = calendar.color
        icon = calendar.getIcon()!!
        order = calendar.order
    }

    val uuid: String
        get() = calendar.uuid!!
    val account: String
        get() = calendar.account!!

    override val isReadOnly: Boolean
        get() = calendar.access == CaldavCalendar.ACCESS_READ_ONLY

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(calendar, 0)
    }

    override fun supportsManualSort() = true

    override val menu = R.menu.menu_caldav_list_fragment

    override fun areContentsTheSame(other: FilterListItem): Boolean {
        return super.areContentsTheSame(other) && calendar == (other as CaldavFilter).calendar
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<CaldavFilter> {
            override fun createFromParcel(source: Parcel): CaldavFilter {
                return CaldavFilter(
                    ParcelCompat.readParcelable(source, javaClass.classLoader, CaldavCalendar::class.java)!!
                )
            }

            override fun newArray(size: Int): Array<CaldavFilter?> {
                return arrayOfNulls(size)
            }
        }

        private fun queryTemplate(caldavCalendar: CaldavCalendar): QueryTemplate {
            return QueryTemplate()
                .join(left(CaldavTask.TABLE, Task.ID.eq(CaldavTask.TASK)))
                .where(getCriterion(caldavCalendar))
        }

        private fun getCriterion(caldavCalendar: CaldavCalendar): Criterion {
            return and(
                activeAndVisible(),
                CaldavTask.DELETED.eq(0),
                CaldavTask.CALENDAR.eq(caldavCalendar.uuid)
            )
        }

        private fun getValuesForNewTask(caldavCalendar: CaldavCalendar): Map<String, Any> {
            val result: MutableMap<String, Any> = HashMap()
            result[CaldavTask.KEY] = caldavCalendar.uuid!!
            return result
        }
    }
}
