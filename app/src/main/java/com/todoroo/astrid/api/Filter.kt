package com.todoroo.astrid.api

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.MenuRes
import com.todoroo.andlib.sql.QueryTemplate

open class Filter : FilterListItem {
    /**
     * Values to apply to a task when quick-adding a task from this filter. For example, when a user
     * views tasks tagged 'ABC', the tasks they create should also be tagged 'ABC'. If set to null, no
     * additional values will be stored for a task. Can use [PermaSql]
     */
    val valuesForNewTasks: MutableMap<String, Any> = HashMap()

    /**
     * [PermaSql] query for this filter. The query will be appended to the select statement
     * after "`SELECT fields FROM table %s`". It is recommended that you use a [ ] to construct your query.
     *
     *
     * Examples:
     *
     *
     *  * `"WHERE completionDate = 0"`
     *  * `"INNER JOIN " +
     * Constants.TABLE_METADATA + " ON metadata.task = tasks.id WHERE
     * metadata.namespace = " + NAMESPACE + " AND metadata.key = 'a' AND
     * metadata.value = 'b' GROUP BY tasks.id ORDER BY tasks.title"`
     *
     */
    var originalSqlQuery: String? = null

    /**
     * Field for holding a modified sqlQuery based on sqlQuery. Useful for adjusting query for
     * sort/subtasks without breaking the equality checking based on sqlQuery.
     */
    private var filterOverride: String? = null

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

    // --- parcelable
    fun setFilterQueryOverride(filterOverride: String?) {
        this.filterOverride = filterOverride
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + if (originalSqlQuery == null) 0 else originalSqlQuery.hashCode()
        result = prime * result + if (listingTitle == null) 0 else listingTitle.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }
        val filter = other as Filter
        if (originalSqlQuery == null) {
            if (filter.originalSqlQuery != null) {
                return false
            }
        } else if (originalSqlQuery != filter.originalSqlQuery) {
            return false
        }
        return if (listingTitle == null) {
            filter.listingTitle == null
        } else listingTitle == filter.listingTitle
    }

    override fun getItemType(): Type {
        return Type.ITEM
    }

    /** {@inheritDoc}  */
    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeString("") // old title
        dest.writeString(originalSqlQuery)
        dest.writeMap(valuesForNewTasks)
    }

    override fun readFromParcel(source: Parcel) {
        super.readFromParcel(source)
        source.readString() // old title
        originalSqlQuery = source.readString()
        source.readMap(valuesForNewTasks, javaClass.classLoader)
    }

    open fun supportsAstridSorting(): Boolean {
        return false
    }

    open fun supportsManualSort(): Boolean {
        return false
    }

    open fun supportsHiddenTasks(): Boolean {
        return true
    }

    open fun supportsSubtasks(): Boolean {
        return true
    }

    open fun supportsSorting(): Boolean {
        return true
    }

    val isWritable: Boolean
        get() = !isReadOnly
    open val isReadOnly: Boolean
        get() = false

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

    override fun toString(): String {
        return ("Filter{"
                + "sqlQuery='"
                + originalSqlQuery
                + '\''
                + ", filterOverride='"
                + filterOverride
                + '\''
                + ", valuesForNewTasks="
                + valuesForNewTasks
                + '}')
    }

    override fun areItemsTheSame(other: FilterListItem): Boolean {
        return other is Filter && originalSqlQuery == other.originalSqlQuery
    }

    override fun areContentsTheSame(other: FilterListItem): Boolean {
        return super.areContentsTheSame(other) && originalSqlQuery == (other as Filter).originalSqlQuery
    }

    companion object {
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
