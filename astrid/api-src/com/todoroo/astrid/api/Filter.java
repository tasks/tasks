/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import android.os.Parcel;
import android.os.Parcelable;

import com.todoroo.astrid.api.AstridContentProvider.AstridTask;

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

    /**
     * Expanded title of this filter. This is displayed at the top
     * of the screen when user is viewing this filter.
     * <p>
     * e.g "Tasks With Notes"
     */
    public String title;

    /**
     * SQL query for this filter. The query will be appended to the select
     * statement after "<code>SELECT fields FROM table %s</code>". Use
     * {@link AstridApiConstants.TASK_TABLE} and
     * {@link AstridApiConstants.METADATA_TABLE} as table names,
     * {@link AstridTask} for field names.
     * <p>
     * Examples:
     * <ul>
     * <li><code>" WHERE completionDate = 0"</code>
     * <li><code>" INNER JOIN " +
     *      Constants.TABLE_METADATA + " ON metadata.task = tasks.id WHERE
     *      metadata.namespace = " + NAMESPACE + " AND metadata.key = 'a' AND
     *      metadata.value = 'b' GROUP BY tasks.id ORDER BY tasks.title"</code>
     * </ul>
     */
    public String sqlQuery;

    /**
     * SQL query to execute on a task when quick-creating a new task while viewing
     * this filter. For example, when a user views tasks tagged 'ABC', the
     * tasks they create should also be tagged 'ABC'. If set to null, no
     * query will be executed. In this string, $ID will be replaced with the
     * task id.
     * <p>
     * Examples:
     * <ul>
     * <li><code>"INSERT INTO " + Constants.TABLE_METADATA + " (task,
     *      namespace, key, string) VALUES ($ID, " + ... + ")"</code>
     * <li><code>"UPDATE " + Constants.TABLE_TASK + " SET urgency = 0
     *      WHERE _id = $ID"</code>
     * </ul>
     */
    public String sqlForNewTasks = null;

    /**
     * Utility constructor for creating a TaskList object
     *
     * @param listingTitle
     *            Title of this item as displayed on the lists page, e.g. Inbox
     * @param title
     *            Expanded title of this filter when user is viewing this
     *            filter, e.g. Inbox (20 tasks)
     * @param sqlQuery
     *            SQL query for this list (see {@link sqlQuery} for examples).
     * @param sqlForNewTasks
     *            see {@link sqlForNewTasks}
     */
    public Filter(String listingTitle,
            String title, String sqlQuery, String sqlForNewTasks) {
        this.listingTitle = listingTitle;
        this.title = title;
        this.sqlQuery = sqlQuery;
        this.sqlForNewTasks = sqlForNewTasks;
    }

    /**
     * Blank constructor
     */
    public Filter() {
        //
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
        super.writeToParcel(dest, flags);
        dest.writeString(title);
        dest.writeString(sqlQuery);
        dest.writeString(sqlForNewTasks);
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
            item.title = source.readString();
            item.sqlQuery = source.readString();
            item.sqlForNewTasks = source.readString();
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
