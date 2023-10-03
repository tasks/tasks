package org.tasks.filters

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.ParcelCompat
import com.todoroo.andlib.sql.Criterion.Companion.and
import com.todoroo.andlib.sql.Field.Companion.field
import com.todoroo.andlib.sql.Join.Companion.inner
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.FilterListItem
import com.todoroo.astrid.data.Task
import org.tasks.R
import org.tasks.data.Geofence
import org.tasks.data.Place
import org.tasks.data.TaskDao.TaskCriteria.activeAndVisible
import org.tasks.themes.CustomIcons

class PlaceFilter(
    val place: Place
) : Filter(place.displayName, queryTemplate(place), getValuesForNewTask(place)) {

    init {
        id = place.id
        tint = place.color
        icon = place.icon
        if (icon == -1) {
            icon = CustomIcons.PLACE
        }
        order = place.order
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(place, 0)
    }

    val uid: String
        get() = place.uid!!

    override val beginningMenu = R.menu.menu_location_actions

    override val menu = R.menu.menu_location_list_fragment

    override fun areContentsTheSame(other: FilterListItem): Boolean {
        return place == (other as PlaceFilter).place && count == other.count
    }

    fun openMap(context: Context?) {
        place.open(context)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PlaceFilter> = object : Parcelable.Creator<PlaceFilter> {
            override fun createFromParcel(source: Parcel): PlaceFilter {
                return PlaceFilter(
                    ParcelCompat.readParcelable(source, javaClass.classLoader, Place::class.java)!!
                )
            }

            /** {@inheritDoc}  */
            override fun newArray(size: Int): Array<PlaceFilter?> {
                return arrayOfNulls(size)
            }
        }
        private val G2 = Geofence.TABLE.`as`("G2")
        private val G2_PLACE = field("G2.place")
        private val G2_TASK = field("G2.task")
        private val P2 = Place.TABLE.`as`("P2")
        private val P2_UID = field("P2.uid")
        private fun queryTemplate(place: Place): QueryTemplate {
            return QueryTemplate()
                .join(inner(G2, Task.ID.eq(G2_TASK)))
                .join(inner(P2, P2_UID.eq(G2_PLACE)))
                .where(and(activeAndVisible(), G2_PLACE.eq(place.uid)))
        }

        private fun getValuesForNewTask(place: Place): Map<String, Any> {
            val result: MutableMap<String, Any> = HashMap()
            result[Place.KEY] = place.uid!!
            return result
        }
    }
}
