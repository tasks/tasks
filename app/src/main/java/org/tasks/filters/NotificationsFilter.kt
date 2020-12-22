package org.tasks.filters

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.todoroo.andlib.sql.Join
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.data.Task
import org.tasks.R
import org.tasks.notifications.Notification

class NotificationsFilter : Filter {
    constructor(context: Context) : super(context.getString(R.string.notifications), queryTemplate)

    private constructor()

    override fun supportsHiddenTasks(): Boolean = false

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<NotificationsFilter> = object : Parcelable.Creator<NotificationsFilter> {
            /** {@inheritDoc}  */
            override fun createFromParcel(source: Parcel): NotificationsFilter =
                    NotificationsFilter().apply {
                        readFromParcel(source)
                    }

            /** {@inheritDoc}  */
            override fun newArray(size: Int): Array<NotificationsFilter?> = arrayOfNulls(size)
        }

        private val queryTemplate: QueryTemplate
            get() = QueryTemplate()
                    .join(Join.inner(Notification.TABLE, Task.ID.eq(Notification.TASK)))
    }
}