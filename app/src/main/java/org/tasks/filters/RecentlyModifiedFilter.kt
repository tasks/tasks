package org.tasks.filters

import android.os.Parcel
import android.os.Parcelable
import com.todoroo.andlib.sql.Criterion.Companion.and
import com.todoroo.andlib.sql.Order.Companion.desc
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.data.Task
import org.tasks.time.DateTime

class RecentlyModifiedFilter : Filter {
    constructor(title: String?) : super(title, queryTemplate)
    private constructor()

    override fun supportsHiddenTasks() = false

    override fun supportsSubtasks() = false

    override fun supportsSorting() = false

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<RecentlyModifiedFilter> =
            object : Parcelable.Creator<RecentlyModifiedFilter> {
                override fun createFromParcel(source: Parcel): RecentlyModifiedFilter? {
                    val item = RecentlyModifiedFilter()
                    item.readFromParcel(source)
                    return item
                }

                override fun newArray(size: Int): Array<RecentlyModifiedFilter?> {
                    return arrayOfNulls(size)
                }
            }
        private val queryTemplate: QueryTemplate
            get() = QueryTemplate()
                .where(
                    and(
                        Task.DELETION_DATE.lte(0),
                        Task.MODIFICATION_DATE.gt(
                            DateTime().minusDays(1).startOfMinute().millis
                        )
                    )
                )
                .orderBy(desc(Task.MODIFICATION_DATE))
    }
}
