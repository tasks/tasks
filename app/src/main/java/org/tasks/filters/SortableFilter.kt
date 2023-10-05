package org.tasks.filters

import android.os.Parcel
import android.os.Parcelable
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.astrid.api.Filter

class SortableFilter : Filter {
    constructor(title: String?, sql: QueryTemplate) : super(title, sql)
    constructor(
        title: String?, sql: QueryTemplate, valuesForNewTasks: Map<String, Any>
    ) : super(title, sql, valuesForNewTasks)

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
