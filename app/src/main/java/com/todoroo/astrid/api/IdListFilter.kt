package com.todoroo.astrid.api

import android.os.Parcel
import android.os.Parcelable
import com.google.common.primitives.Longs
import com.todoroo.andlib.sql.Join.Companion.left
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.astrid.data.Task
import org.tasks.data.Tag

class IdListFilter : Filter {
    private var ids: List<Long?>? = null

    constructor(ids: List<Long?>) : super("", getQueryTemplate(ids)) {
        this.ids = ids
    }

    private constructor(source: Parcel) {
        readFromParcel(source)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeLongArray(Longs.toArray(ids!!))
    }

    override fun readFromParcel(source: Parcel) {
        super.readFromParcel(source)
        val ids = LongArray(source.readInt())
        source.setDataPosition(source.dataPosition() - 1)
        source.readLongArray(ids)
        this.ids = Longs.asList(*ids)
    }

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<IdListFilter> = object : Parcelable.Creator<IdListFilter> {
            override fun createFromParcel(source: Parcel) = IdListFilter(source)

            override fun newArray(size: Int): Array<IdListFilter?> = arrayOfNulls(size)
        }

        private fun getQueryTemplate(ids: List<Long?>): QueryTemplate {
            return QueryTemplate()
                .join(left(Tag.TABLE, Tag.TASK.eq(Task.ID)))
                .where(Task.ID.`in`(ids))
        }
    }
}