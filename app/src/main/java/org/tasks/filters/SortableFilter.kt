package org.tasks.filters

import android.os.Parcel
import android.os.Parcelable
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.astrid.api.Filter

class SortableFilter : Filter {
    constructor(listingTitle: String?, sqlQuery: QueryTemplate?) : super(listingTitle, sqlQuery)
    constructor(
        listingTitle: String?, sqlQuery: QueryTemplate?, valuesForNewTasks: Map<String, Any>?
    ) : super(listingTitle, sqlQuery, valuesForNewTasks)

    private constructor()

    override fun supportsAstridSorting(): Boolean {
        return true
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<SortableFilter> =
            object : Parcelable.Creator<SortableFilter> {
                /** {@inheritDoc}  */
                override fun createFromParcel(source: Parcel): SortableFilter {
                    val item = SortableFilter()
                    item.readFromParcel(source)
                    return item
                }

                /** {@inheritDoc}  */
                override fun newArray(size: Int): Array<SortableFilter?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
