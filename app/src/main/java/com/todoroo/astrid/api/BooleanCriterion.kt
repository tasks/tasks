package com.todoroo.astrid.api

import android.os.Parcel
import android.os.Parcelable

class BooleanCriterion() : CustomFilterCriterion(), Parcelable {

    constructor(identifier: String, title: String, sql: String): this() {
        this.identifier = identifier
        this.text = title
        this.sql = sql
        this.name = title
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        writeToParcel(dest)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<BooleanCriterion> = object : Parcelable.Creator<BooleanCriterion> {
            override fun createFromParcel(source: Parcel?): BooleanCriterion {
                val item = BooleanCriterion()
                item.readFromParcel(source)
                return item
            }

            override fun newArray(size: Int): Array<BooleanCriterion?> {
                return arrayOfNulls(size)
            }
        }
    }
}