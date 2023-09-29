package com.todoroo.astrid.api

import com.todoroo.andlib.sql.Criterion
import com.todoroo.andlib.sql.Join
import com.todoroo.andlib.sql.Query
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.astrid.data.Task
import kotlinx.parcelize.Parcelize
import org.tasks.data.CaldavCalendar
import org.tasks.data.CaldavTask
import org.tasks.data.Geofence
import org.tasks.data.Place
import org.tasks.data.Tag
import org.tasks.data.UserActivity

@Parcelize
data class SearchFilter(
    override val title: String,
    val query: String,
) : Filter {
    override val sql: String
        get() {
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
                                    .where(Tag.NAME.like(matcher))
                            ),
                            Task.UUID.`in`(
                                Query.select(UserActivity.TASK)
                                    .from(UserActivity.TABLE)
                                    .where(UserActivity.MESSAGE.like(matcher))
                            ),
                            Task.ID.`in`(
                                Query.select(Geofence.TASK)
                                    .from(Geofence.TABLE)
                                    .join(Join.inner(Place.TABLE, Place.UID.eq(Geofence.PLACE)))
                                    .where(
                                        Criterion.or(
                                            Place.NAME.like(matcher),
                                            Place.ADDRESS.like(matcher)
                                        )
                                    )
                            ),
                            Task.ID.`in`(
                                Query.select(CaldavTask.TASK)
                                    .from(CaldavTask.TABLE)
                                    .join(
                                        Join.inner(
                                            CaldavCalendar.TABLE,
                                            CaldavCalendar.UUID.eq(CaldavTask.CALENDAR)
                                        )
                                    )
                                    .where(CaldavCalendar.NAME.like(matcher))
                            ),
                        )
                    )
                )
                .toString()
        }

    override fun areItemsTheSame(other: FilterListItem): Boolean {
        return other is SearchFilter
    }

    override fun supportsHiddenTasks() = false
}