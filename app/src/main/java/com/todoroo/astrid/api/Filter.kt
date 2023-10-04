package com.todoroo.astrid.api

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.MenuRes
import com.todoroo.andlib.sql.QueryTemplate

open class Filter : FilterListItem {
    val valuesForNewTasks: MutableMap<String, Any> = HashMap()
    var originalSqlQuery: String? = null
    @Deprecated("for astrid manual order") var filterOverride: String? = null
    var id = 0L
    var icon = -1
    var listingTitle: String? = null
    var tint = 0
    var count = -1
    var order = NO_ORDER

    constructor(listingTitle: String?, sqlQuery: QueryTemplate?) : this(
        listingTitle,
        sqlQuery,
        emptyMap<String, Any>()
    )

    /**
     * Utility constructor for creating a Filter object
     *
     * @param listingTitle Title of this item as displayed on the lists page, e.g. Inbox
     * @param sqlQuery SQL query for this list (see [.sqlQuery] for examples).
     */
    protected constructor(
        listingTitle: String?, sqlQuery: QueryTemplate?, valuesForNewTasks: Map<String, Any>?
    ) : this(listingTitle, sqlQuery?.toString(), valuesForNewTasks)

    /**
     * Utility constructor for creating a Filter object
     *
     * @param listingTitle Title of this item as displayed on the lists page, e.g. Inbox
     * @param sqlQuery SQL query for this list (see [.sqlQuery] for examples).
     */
    internal constructor(
        listingTitle: String?,
        sqlQuery: String?,
        valuesForNewTasks: Map<String, Any>?
    ) {
        this.listingTitle = listingTitle
        originalSqlQuery = sqlQuery
        filterOverride = null
        if (valuesForNewTasks != null) {
            this.valuesForNewTasks.putAll(valuesForNewTasks)
        }
    }

    protected constructor()

    fun getSqlQuery(): String {
        return filterOverride ?: originalSqlQuery!!
    }

    override val itemType = FilterListItem.Type.ITEM

    /** {@inheritDoc}  */
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeInt(icon)
        dest.writeString(listingTitle)
        dest.writeInt(tint)
        dest.writeInt(count)
        dest.writeInt(order)
        dest.writeString(originalSqlQuery)
        dest.writeMap(valuesForNewTasks)
    }

    open fun readFromParcel(source: Parcel) {
        id = source.readLong()
        icon = source.readInt()
        listingTitle = source.readString()
        tint = source.readInt()
        count = source.readInt()
        order = source.readInt()
        originalSqlQuery = source.readString()
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
        return other is Filter && id == other.id && originalSqlQuery == other.originalSqlQuery
    }

    override fun areContentsTheSame(other: FilterListItem): Boolean {
        return this == other
    }

    override fun toString(): String {
        return "Filter(valuesForNewTasks=$valuesForNewTasks, originalSqlQuery=$originalSqlQuery, filterOverride=$filterOverride, id=$id, icon=$icon, listingTitle=$listingTitle, tint=$tint, count=$count, order=$order)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Filter

        if (valuesForNewTasks != other.valuesForNewTasks) return false
        if (originalSqlQuery != other.originalSqlQuery) return false
        if (filterOverride != other.filterOverride) return false
        if (id != other.id) return false
        if (icon != other.icon) return false
        if (listingTitle != other.listingTitle) return false
        if (tint != other.tint) return false
        if (count != other.count) return false
        if (order != other.order) return false
        return isReadOnly == other.isReadOnly
    }

    override fun hashCode(): Int {
        var result = valuesForNewTasks.hashCode()
        result = 31 * result + (originalSqlQuery?.hashCode() ?: 0)
        result = 31 * result + (filterOverride?.hashCode() ?: 0)
        result = 31 * result + id.hashCode()
        result = 31 * result + icon
        result = 31 * result + (listingTitle?.hashCode() ?: 0)
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
