package com.todoroo.astrid.api

import android.os.Parcel
import android.os.Parcelable
import com.todoroo.andlib.sql.Criterion
import com.todoroo.andlib.sql.Join
import com.todoroo.andlib.sql.Query
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.astrid.data.Task
import org.tasks.data.*

class SearchFilter : Filter {
    private constructor()

    constructor(title: String?, query: String) : super(title, getQueryTemplate(query))

    override fun supportsHiddenTasks() = false

    companion object {
        /** Parcelable Creator Object  */
        @JvmField
        val CREATOR: Parcelable.Creator<SearchFilter> = object : Parcelable.Creator<SearchFilter> {
            /** {@inheritDoc}  */
            override fun createFromParcel(source: Parcel): SearchFilter? {
                val item = SearchFilter()
                item.readFromParcel(source)
                return item
            }

            /** {@inheritDoc}  */
            override fun newArray(size: Int): Array<SearchFilter?> {
                return arrayOfNulls(size)
            }
        }

        private fun getQueryTemplate(query: String): QueryTemplate {
            val matcher = "%$query%"
            return QueryTemplate()
                    .where(
                            Criterion.and(
                                    Task.DELETION_DATE.eq(0),
                                    Criterion.or(
                                            Task.NOTES.like(matcher),
                                            Task.TITLE.like(matcher),
                                            Task.ID.`in`(
                                                    Query.select(Tag.TASK)
                                                            .from(Tag.TABLE)
                                                            .where(Tag.NAME.like(matcher))),
                                            Task.UUID.`in`(
                                                    Query.select(UserActivity.TASK)
                                                            .from(UserActivity.TABLE)
                                                            .where(UserActivity.MESSAGE.like(matcher))),
                                            Task.ID.`in`(
                                                    Query.select(Geofence.TASK)
                                                            .from(Geofence.TABLE)
                                                            .join(Join.inner(Place.TABLE, Place.UID.eq(Geofence.PLACE)))
                                                            .where(Criterion.or(Place.NAME.like(matcher),
                                                                    Place.ADDRESS.like(matcher)))),
                                            Task.ID.`in`(
                                                    Query.select(CaldavTask.TASK)
                                                            .from(CaldavTask.TABLE)
                                                            .join(Join.inner(CaldavCalendar.TABLE, CaldavCalendar.UUID.eq(CaldavTask.CALENDAR)))
                                                            .where(CaldavCalendar.NAME.like(matcher))),
                                    )))
        }
    }
}