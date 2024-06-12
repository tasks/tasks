package org.tasks.filters

import android.content.Context
import kotlinx.parcelize.Parcelize
import org.tasks.data.NO_COUNT
import org.tasks.data.dao.TaskDao.TaskCriteria.activeAndVisible
import org.tasks.data.displayName
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.Place
import org.tasks.data.entity.Task
import org.tasks.data.open
import org.tasks.data.sql.Criterion.Companion.and
import org.tasks.data.sql.Field.Companion.field
import org.tasks.data.sql.Join.Companion.inner
import org.tasks.data.sql.QueryTemplate
import org.tasks.themes.CustomIcons

@Parcelize
data class PlaceFilter(
    val place: Place,
    override val count: Int = NO_COUNT,
) : Filter {
    override val valuesForNewTasks: String
        get() = mapToSerializedString(mapOf(Place.KEY to place.uid!!))
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
