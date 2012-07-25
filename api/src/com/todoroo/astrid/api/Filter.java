/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.QueryTemplate;

/**
 * A <code>FilterListFilter</code> allows users to display tasks that have
 * something in common.
 * <p>
 * A plug-in can expose new <code>FilterListFilter</code>s to the system by
 * responding to the <code>com.todoroo.astrid.GET_FILTERS</code> broadcast
 * intent.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class Filter extends FilterListItem {

    // --- constants

    /** Constant for valuesForNewTasks to indicate the value should be replaced
     * with the current time as long */
    public static final long VALUE_NOW = Long.MIN_VALUE + 1;

    // --- instance variables

    /**
     * Expanded title of this filter. This is displayed at the top
     * of the screen when user is viewing this filter.
     * <p>
     * e.g "Tasks With Notes"
     */
    public String title;

    /**
     * {@link PermaSql} query for this filter. The query will be appended to the select
     * statement after "<code>SELECT fields FROM table %s</code>". It is
     * recommended that you use a {@link QueryTemplate} to construct your
     * query.
     * <p>
     * Examples:
     * <ul>
     * <li><code>"WHERE completionDate = 0"</code>
     * <li><code>"INNER JOIN " +
     *      Constants.TABLE_METADATA + " ON metadata.task = tasks.id WHERE
     *      metadata.namespace = " + NAMESPACE + " AND metadata.key = 'a' AND
     *      metadata.value = 'b' GROUP BY tasks.id ORDER BY tasks.title"</code>
     * </ul>
     */
    protected String sqlQuery;

    /**
     * Field for holding a modified sqlQuery based on sqlQuery. Useful for adjusting
     * query for sort/subtasks without breaking the equality checking based on sqlQuery.
     */
    protected String filterOverride;

    /**
     * Values to apply to a task when quick-adding a task from this filter.
     * For example, when a user views tasks tagged 'ABC', the
     * tasks they create should also be tagged 'ABC'. If set to null, no
     * additional values will be stored for a task. Can use {@link PermaSql}
     */
    public ContentValues valuesForNewTasks = null;

    /**
     * Utility constructor for creating a Filter object
     * @param listingTitle
     *            Title of this item as displayed on the lists page, e.g. Inbox
     * @param title
     *            Expanded title of this filter when user is viewing this
     *            filter, e.g. Inbox (20 tasks)
     * @param sqlQuery
     *            SQL query for this list (see {@link #sqlQuery} for examples).
     * @param valuesForNewTasks
     *            see {@link #sqlForNewTasks}
     */
    public Filter(String listingTitle, String title,
            QueryTemplate sqlQuery, ContentValues valuesForNewTasks) {
        this(listingTitle, title, sqlQuery == null ? null : sqlQuery.toString(),
                valuesForNewTasks);
    }

    /**
     * Utility constructor for creating a Filter object
     * @param listingTitle
     *            Title of this item as displayed on the lists page, e.g. Inbox
     * @param title
     *            Expanded title of this filter when user is viewing this
     *            filter, e.g. Inbox (20 tasks)
     * @param sqlQuery
     *            SQL query for this list (see {@link #sqlQuery} for examples).
     * @param valuesForNewTasks
     *            see {@link #sqlForNewTasks}
     */
    public Filter(String listingTitle, String title,
            String sqlQuery, ContentValues valuesForNewTasks) {
        this.listingTitle = listingTitle;
        this.title = title;
        this.sqlQuery = sqlQuery;
        this.filterOverride = null;
        this.valuesForNewTasks = valuesForNewTasks;
    }

    public String getSqlQuery() {
        if (filterOverride != null)
            return filterOverride;
        return sqlQuery;
    }

    public void setSqlQuery(String sqlQuery) {
        this.sqlQuery = sqlQuery;
    }

    public void setFilterQueryOverride(String filterOverride) {
        this.filterOverride = filterOverride;
    }

    /**
     * Utility constructor
     *
     * @param plugin
     *            {@link Addon} identifier that encompasses object
     */
    protected Filter() {
        // do nothing
    }

    // --- parcelable

    /**
     * {@inheritDoc}
     */
    public int describeContents() {
        return 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((sqlQuery == null) ? 0 : sqlQuery.hashCode());
        result = prime * result + ((title == null) ? 0 : title.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Filter other = (Filter) obj;
        if (sqlQuery == null) {
            if (other.sqlQuery != null)
                return false;
        } else if (!sqlQuery.equals(other.sqlQuery))
            return false;
        if (title == null) {
            if (other.title != null)
                return false;
        } else if (!title.equals(other.title))
            return false;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(title);
        dest.writeString(sqlQuery);
        dest.writeParcelable(valuesForNewTasks, 0);
    }

    @Override
    public void readFromParcel(Parcel source) {
        super.readFromParcel(source);
        title = source.readString();
        sqlQuery = source.readString();
        valuesForNewTasks = source.readParcelable(ContentValues.class.getClassLoader());
    }

    /**
     * Parcelable Creator Object
     */
    public static final Parcelable.Creator<Filter> CREATOR = new Parcelable.Creator<Filter>() {

        /**
         * {@inheritDoc}
         */
        public Filter createFromParcel(Parcel source) {
            Filter item = new Filter();
            item.readFromParcel(source);
            return item;
        }

        /**
         * {@inheritDoc}
         */
        public Filter[] newArray(int size) {
            return new Filter[size];
        }

    };

    /**
     * @param title
     * @return a filter that matches nothing
     */
    public static Filter emptyFilter(String title) {
        return new Filter(title, title,
                new QueryTemplate().where(Criterion.none), null);
    }
}
