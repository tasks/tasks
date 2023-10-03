package org.tasks.filters

import android.os.Parcel
import android.os.Parcelable
import com.todoroo.andlib.sql.Criterion.Companion.and
import com.todoroo.andlib.sql.Join.Companion.inner
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.data.Task
import org.tasks.data.Alarm

class SnoozedFilter : Filter {
    constructor(listingTitle: String?) : super(listingTitle, queryTemplate)
    private constructor()

    override fun supportsHiddenTasks(): Boolean {
        return false
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<SnoozedFilter> =
            object : Parcelable.Creator<SnoozedFilter> {
                override fun createFromParcel(source: Parcel): SnoozedFilter {
                    val item = SnoozedFilter()
                    item.readFromParcel(source)
                    return item
                }

                override fun newArray(size: Int): Array<SnoozedFilter?> {
                    return arrayOfNulls(size)
                }
            }
        private val queryTemplate: QueryTemplate
            get() = QueryTemplate()
                .join(inner(Alarm.TABLE, Task.ID.eq(Alarm.TASK)))
                .where(and(Task.DELETION_DATE.lte(0), Alarm.TYPE.eq(Alarm.TYPE_SNOOZE)))
    }
}
