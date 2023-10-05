package com.todoroo.astrid.api

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.MenuRes
import com.todoroo.andlib.sql.QueryTemplate

open class Filter : FilterListItem, Parcelable {
    var valuesForNewTasks: Map<String, Any> = HashMap()
    var sql: String? = null
    @Deprecated("for astrid manual order") var filterOverride: String? = null
    var id = 0L
    var icon = -1
    var title: String? = null
    var tint = 0
    var count = -1
    var order = NO_ORDER

    /**
     * Utility constructor for creating a Filter object
     *
     * @param title Title of this item as displayed on the lists page, e.g. Inbox
     * @param sql SQL query for this list (see [.sqlQuery] for examples).
     */
    constructor(
        title: String?,
        sql: QueryTemplate,
        valuesForNewTasks: Map<String, Any> = emptyMap(),
    ) : this(title, sql.toString(), valuesForNewTasks)

    /**
     * Utility constructor for creating a Filter object
     *
     * @param title Title of this item as displayed on the lists page, e.g. Inbox
     * @param sql SQL query for this list (see [.sqlQuery] for examples).
     */
    internal constructor(
        title: String?,
        sql: String,
        valuesForNewTasks: Map<String, Any>
    ) {
        this.title = title
        this.sql = sql
        this.valuesForNewTasks = valuesForNewTasks
    }

    protected constructor()

    fun getSqlQuery(): String {
        return filterOverride ?: sql!!
    }

    override val itemType = FilterListItem.Type.ITEM

    /** {@inheritDoc}  */
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeInt(icon)
        dest.writeString(title)
        dest.writeInt(tint)
        dest.writeInt(count)
        dest.writeInt(order)
        dest.writeString(sql)
        dest.writeMap(valuesForNewTasks)
    }

    open fun readFromParcel(source: Parcel) {
        id = source.readLong()
        icon = source.readInt()
        title = source.readString()
        tint = source.readInt()
        count = source.readInt()
        order = source.readInt()
        sql = source.readString()
        source.readMap(valuesForNewTasks, javaClass.classLoader)
    }

    open fun supportsAstridSorting() = false

    open fun supportsManualSort() = false

    open fun supportsHiddenTasks() = true

    open fun supportsSubtasks() = true

    open fun supportsSorting() = true

    open val isReadOnly: Boolean = false

    val isWritable: Boolean
        get() = !isReadOnly

    fun hasBeginningMenu(): Boolean {
        return beginningMenu != 0
    }

    @get:MenuRes
    open val beginningMenu: Int
        get() = 0

    fun hasMenu(): Boolean {
        return menu != 0
    }

    @get:MenuRes
    open val menu: Int
        get() = 0

    override fun describeContents() = 0

    override fun areItemsTheSame(other: FilterListItem): Boolean {
        return other is Filter && id == other.id && sql == other.sql
    }

    override fun areContentsTheSame(other: FilterListItem): Boolean {
        return this == other
    }

    override fun toString(): String {
        return "Filter(valuesForNewTasks=$valuesForNewTasks, sql=$sql, filterOverride=$filterOverride, id=$id, icon=$icon, title=$title, tint=$tint, count=$count, order=$order)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Filter

        if (valuesForNewTasks != other.valuesForNewTasks) return false
        if (sql != other.sql) return false
        if (filterOverride != other.filterOverride) return false
        if (id != other.id) return false
        if (icon != other.icon) return false
        if (title != other.title) return false
        if (tint != other.tint) return false
        if (count != other.count) return false
        if (order != other.order) return false
        return isReadOnly == other.isReadOnly
    }

    override fun hashCode(): Int {
        var result = valuesForNewTasks.hashCode()
        result = 31 * result + (sql?.hashCode() ?: 0)
        result = 31 * result + (filterOverride?.hashCode() ?: 0)
        result = 31 * result + id.hashCode()
        result = 31 * result + icon
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + tint
        result = 31 * result + count
        result = 31 * result + order
        result = 31 * result + isReadOnly.hashCode()
        return result
    }

    companion object {
        const val NO_ORDER = -1

        @JvmField
        val CREATOR: Parcelable.Creator<Filter> = object : Parcelable.Creator<Filter> {
            /** {@inheritDoc}  */
            override fun createFromParcel(source: Parcel): Filter {
                val item = Filter()
                item.readFromParcel(source)
                return item
            }

            /** {@inheritDoc}  */
            override fun newArray(size: Int): Array<Filter?> {
                return arrayOfNulls(size)
            }
        }
    }
}
