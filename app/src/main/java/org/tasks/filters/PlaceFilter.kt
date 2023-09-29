package org.tasks.filters

import android.content.Context
import com.todoroo.andlib.sql.Criterion.Companion.and
import com.todoroo.andlib.sql.Field.Companion.field
import com.todoroo.andlib.sql.Join.Companion.inner
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.Filter.Companion.NO_COUNT
import com.todoroo.astrid.api.FilterListItem
import com.todoroo.astrid.data.Task
import kotlinx.parcelize.Parcelize
import org.tasks.data.Geofence
import org.tasks.data.Place
import org.tasks.data.TaskDao.TaskCriteria.activeAndVisible
import org.tasks.themes.CustomIcons

@Parcelize
data class PlaceFilter(
    val place: Place,
    override val count: Int = NO_COUNT,
) : Filter {
    override val valuesForNewTasks: String
        get() = AndroidUtilities.mapToSerializedString(mapOf(Place.KEY to place.uid!!))
    override val sql: String
        get() = QueryTemplate()
            .join(inner(G2, Task.ID.eq(G2_TASK)))
            .join(inner(P2, P2_UID.eq(G2_PLACE)))
            .where(and(activeAndVisible(), G2_PLACE.eq(place.uid)))
            .toString()

    override val order: Int
        get() = place.order

    override val icon: Int
        get() = place.icon.takeIf { it != -1 } ?: CustomIcons.PLACE
    override val title: String
        get() = place.displayName
    override val tint: Int
        get() = place.color

    val uid: String
        get() = place.uid!!

    override fun areItemsTheSame(other: FilterListItem): Boolean {
        return other is PlaceFilter && place.id == other.place.id
    }

    fun openMap(context: Context?) {
        place.open(context)
    }

    companion object {
        private val G2 = Geofence.TABLE.`as`("G2")
        private val G2_PLACE = field("G2.place")
        private val G2_TASK = field("G2.task")
        private val P2 = Place.TABLE.`as`("P2")
        private val P2_UID = field("P2.uid")
    }
}
