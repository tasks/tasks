/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;

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
public final class Filter extends FilterListItem {

    /**
     * Plug-in Identifier
     */
    public final String plugin;

    /**
     * Expanded title of this filter. This is displayed at the top
     * of the screen when user is viewing this filter.
     * <p>
     * e.g "Tasks With Notes"
     */
    public String title;

    /**
     * SQL query for this filter. The query will be appended to the select
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
    public String sqlQuery;

    /**
     * Values to apply to a task when quick-adding a task from this filter.
     * For example, when a user views tasks tagged 'ABC', the
     * tasks they create should also be tagged 'ABC'. If set to null, no
     * additional values will be stored for a task.
     */
    public ContentValues valuesForNewTasks = null;

    /**
     * Utility constructor for creating a TaskList object
     *
     * @param plugin
     *            {@link Addon} identifier that encompasses object
     * @param listingTitle
     *            Title of this item as displayed on the lists page, e.g. Inbox
     * @param title
     *            Expanded title of this filter when user is viewing this
     *            filter, e.g. Inbox (20 tasks)
     * @param sqlQuery
     *            SQL query for this list (see {@link sqlQuery} for examples).
     * @param valuesForNewTasks
     *            see {@link sqlForNewTasks}
     */
    public Filter(String plugin, String listingTitle,
            String title, QueryTemplate sqlQuery, ContentValues valuesForNewTasks) {
        this.plugin = plugin;
        this.listingTitle = listingTitle;
        this.title = title;
        this.sqlQuery = sqlQuery.toString();
        this.valuesForNewTasks = valuesForNewTasks;
    }

    /**
     * Utility constructor
     *
     * @param plugin
     *            {@link Addon} identifier that encompasses object
     */
    protected Filter(String plugin) {
        this.plugin = plugin;
    }

    // --- parcelable

    /**
     * {@inheritDoc}
     */
    public int describeContents() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(plugin);
        super.writeToParcel(dest, flags);
        dest.writeString(title);
        dest.writeString(sqlQuery);
        dest.writeParcelable(valuesForNewTasks, 0);
    }

    /**
     * Parcelable Creator Object
     */
    public static final Parcelable.Creator<Filter> CREATOR = new Parcelable.Creator<Filter>() {

        /**
         * {@inheritDoc}
         */
        public Filter createFromParcel(Parcel source) {
            Filter item = new Filter(source.readString());
            item.readFromParcel(source);
            item.title = source.readString();
            item.sqlQuery = source.readString();
            item.valuesForNewTasks = source.readParcelable(ContentValues.class.getClassLoader());
            return item;
        }

        /**
         * {@inheritDoc}
         */
        public Filter[] newArray(int size) {
            return new Filter[size];
        }

    };
}
