/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import android.net.Uri;

/**
 * Constants for interfacing with Astrid's Content Providers.
 *
 * @author Tim Su <tim@todoroo.com>
 */
@SuppressWarnings("nls")
public class AstridContentProvider {

    /**
     * Content Provider
     */
    public static final String PROVIDER = "com.todoroo.astrid.provider";

    // --- methods for generating URI's for accessing Astrid data

    /**
     * URI for:
     * <ul>
     * <li>Queries on multiple tasks
     * <li>Inserting new tasks
     * <li>Deleting multiple tasks at a time
     * <li>Updating multiple tasks at a time
     * </ul>
     * If your selection clause contains metadata columns, you need to use
     * <code>allItemsWithMetadataUri</code> instead of this one.
     */
    public static Uri allItemsUri() {
        return Uri.parse("content://" + PROVIDER + "/items");
    }

    /**
     * URI for:
     * <ul>
     * <li>Querying on tasks with metadata columns in selection
     * <li>Deleting multiple tasks with metadata columns in selection
     * <li>Updating multiple tasks with metadata columns in selection
     * </ul>
     * If, for example, you have defined metadata key 'tag' and wish to delete
     * all tasks where 'tag' = 'deleteme', you would use this URI. For querying
     * or insertion, use <code>allItemsUri</code>.
     * <p>
     * For queries, <code>allItemsUri</code> will be more efficient, but will
     * not work if your selection clause contains columns not mentioned in your
     * projection
     *
     * @param metadata
     *            array of metadata columns you wish to select using
     *
     */
    public static Uri allItemsWithMetadataUri(String[] metadata) {
        if(metadata == null || metadata.length == 0)
            throw new IllegalArgumentException("You must provide metadata");
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < metadata.length; i++)
            builder.append(escapeUriSubComponent(metadata[i])).append(
                    SUB_COMPONENT_SEPARATOR);

        return Uri.parse("content://" + PROVIDER + "/itemsWith/" +
                escapeUriComponent(builder.toString()));
    }

    /**
     * URI for:
     * <ul>
     * <li>Queries on a single task
     * <li>Updating fields for a single task
     * <li>Deleting a single task
     * </ul>
     *
     * @param id
     *            id of task to fetch
     */
    public static Uri singleItemUri(long id) {
        return Uri.parse("content://" + PROVIDER + "/" + id);
    }

    /**
     * URI for:
     * <ul>
     * <li>Queries on multiple tasks, grouped by column
     * </ul>
     * @param groupBy
     *            column name to group by
     */
    public static Uri groupByUri(String groupBy) {
        groupBy = escapeUriComponent(groupBy);
        return Uri.parse("content://" + PROVIDER + "/groupby/" + groupBy);
    }

    // --- task built-in columns and constnats

    /**
     * A task in Astrid represents a single item in a user's task list
     *
     * @author Tim Su <tim@todoroo.com>
     */
    public static class AstridTask {

        // --- columns

        /** long: Task id */
        public static final String ID = "_id";

        /** String: name of Task */
        public static final String TITLE = "title";

        /** int: Task Urgency setting (see <code>Task.URGENCY_*</code>) */
        public static final String URGENCY = "urgency";

        /** int: Task Importance setting (see <code>Task.IMPORTANCE_*</code>) */
        public static final String IMPORTANCE = "importance";

        /** int: unixtime Task is due, 0 if not set */
        public static final String DUE_DATE = "dueDate";

        /** int: unixtime Task should be hidden until, 0 if not set */
        public static final String HIDDEN_UNTIL = "hiddenUntil";

        /** int: unixtime Task was created */
        public static final String CREATION_DATE = "creationDate";

        /** int: unixtime Task was completed, 0 if task not completed */
        public static final String COMPLETION_DATE = "completionDate";

        /** int: unixtime Task was deleted, 0 if task not deleted */
        public static final String DELETION_DATE = "deletionDate";

        /** int: unixtime Task was modified */
        public static final String MODIFICATION_DATE = "modificationDate";

        // --- urgency settings

        public static final int URGENCY_NONE = 0;
        public static final int URGENCY_TODAY = 1;
        public static final int URGENCY_THIS_WEEK = 2;
        public static final int URGENCY_THIS_MONTH = 3;
        public static final int URGENCY_WITHIN_THREE_MONTHS = 4;
        public static final int URGENCY_WITHIN_SIX_MONTHS = 5;
        public static final int URGENCY_WITHIN_A_YEAR = 6;
        public static final int URGENCY_SPECIFIC_DAY = 7;
        public static final int URGENCY_SPECIFIC_DAY_TIME = 8;

        // --- importance settings

        public static final int IMPORTANCE_DO_OR_DIE = 0;
        public static final int IMPORTANCE_MUST_DO = 1;
        public static final int IMPORTANCE_SHOULD_DO = 2;
        public static final int IMPORTANCE_NONE = 3;


    }

    // --- internal methods

    /**
     * Escapes a string for use in a URI. Used internally to pass extra data
     * to the content provider.
     * @param component
     * @return
     */
    private static String escapeUriComponent(String component) {
        return component.replace("%", "%o").replace("/", "%s");
    }

    private static final String SUB_COMPONENT_SEPARATOR = "|";

    /**
     * Escapes a string for use as part of a URI string. Used internally to pass extra data
     * to the content provider.
     * @param component
     * @return
     */
    private static String escapeUriSubComponent(String component) {
        return component.replace("$", "$o").replace(SUB_COMPONENT_SEPARATOR, "$s");
    }

}
