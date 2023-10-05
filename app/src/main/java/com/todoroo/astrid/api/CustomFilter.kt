package com.todoroo.astrid.api

import android.os.Parcel
import android.os.Parcelable
import com.todoroo.andlib.utility.AndroidUtilities
import org.tasks.R
import org.tasks.themes.CustomIcons

class CustomFilter : Filter {
    var criterion: String? = null
        private set

    constructor(filter: org.tasks.data.Filter) : super(
        filter.title,
        filter.sql!!,
        AndroidUtilities.mapFromSerializedString(filter.values)
    ) {
        id = filter.id
        criterion = filter.criterion
        tint = filter.color ?: 0
        icon = filter.icon ?: CustomIcons.FILTER
        order = filter.order
    }

    private constructor(parcel: Parcel) {
        readFromParcel(parcel)
    }

    /** {@inheritDoc}  */
    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeString(criterion)
    }

    override fun readFromParcel(source: Parcel) {
        super.readFromParcel(source)
        criterion = source.readString()
    }

    override val menu: Int
        get() = if (id > 0) R.menu.menu_custom_filter else 0

    override fun areContentsTheSame(other: FilterListItem): Boolean {
        return super.areContentsTheSame(other) && criterion == (other as CustomFilter).criterion
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<CustomFilter> = object : Parcelable.Creator<CustomFilter> {
            override fun createFromParcel(source: Parcel): CustomFilter? {
                return CustomFilter(source)
            }

            override fun newArray(size: Int): Array<CustomFilter?> {
                return arrayOfNulls(size)
            }
        }
    }
}
