/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * CustomFilterCriteria allow users to build a custom filter by chaining
 * together criteria
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class CustomFilterCriterion extends FilterListItem {

    // --- constants

    /** value to be replaced with the current time as long */
    public static final String VALUE_NOW = "NOW()"; //$NON-NLS-1$

    /** value to be replaced by end of day as long */
    public static final String VALUE_EOD = "EOD()"; //$NON-NLS-1$

    /** value to be replaced by end of day yesterday as long */
    public static final String VALUE_EOD_YESTERDAY = "EODY()"; //$NON-NLS-1$

    /** value to be replaced by end of day tomorrow as long */
    public static final String VALUE_EOD_TOMORROW = "EODT()"; //$NON-NLS-1$

    /** value to be replaced by end of day day after tomorrow as long */
    public static final String VALUE_EOD_DAY_AFTER = "EODTT()"; //$NON-NLS-1$

    /** value to be replaced by end of day next week as long */
    public static final String VALUE_EOD_NEXT_WEEK = "EODW()"; //$NON-NLS-1$

    // --- instance variables

    /**
     * Criteria Title. If the title contains %s, this is replaced by the entry
     * label string selected.
     * <p>
     * e.g "Due: %s"
     */
    @CheckForNull
    public String text;

    /**
     * Criterion SQL. This query should return task id's. If this contains
     * %s, it will be replaced by the entry value
     * <p>
     * Examples:
     * <ul>
     * <li><code>SELECT _id FROM tasks WHERE dueDate <= %s</code>
     * <li><code>SELECT task FROM metadata WHERE value = '%s'</code>
     * </ul>
     */
    @CheckForNull
    public String sql;

    /**
     * Values to apply to a task when quick-adding a task from a filter
     * created from this criterion. %s will be replaced with the entry value.
     * For example, when a user views tasks tagged 'ABC', the
     * tasks they create should also be tagged 'ABC'. If set to null, no
     * additional values will be stored for a task.
     */
    @CheckForNull
    public ContentValues valuesForNewTasks = null;

    /**
     * Array of entries for user to select from
     */
    @CheckForNull
    public String[] entryTitles;

    /**
     * Array of entry values corresponding to entries
     */
    @CheckForNull
    public String[] entryValues;

    /**
     * Icon for this criteria. Can be null for no bitmap
     */
    @CheckForNull
    public Bitmap icon;

    /**
     * Criteria name. This is displayed when users are selecting a criteria
     */
    @CheckForNull
    public String name;

    /**
     * Create a new CustomFilterCriteria object
     *
     * @param title
     * @param sql
     * @param valuesForNewTasks
     * @param entryTitles
     * @param entryValues
     * @param icon
     * @param name
     */
    public CustomFilterCriterion(String title, String sql,
            ContentValues valuesForNewTasks, String[] entryTitles,
            String[] entryValues, Bitmap icon, String name) {
        this.text = title;
        this.sql = sql;
        this.valuesForNewTasks = valuesForNewTasks;
        this.entryTitles = entryTitles;
        this.entryValues = entryValues;
        this.icon = icon;
        this.name = name;
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
        dest.writeString(text);
        dest.writeString(sql);
        dest.writeParcelable(valuesForNewTasks, 0);
        dest.writeStringArray(entryTitles);
        dest.writeStringArray(entryValues);
        dest.writeParcelable(icon, 0);
        dest.writeString(name);
    }

    /**
     * Parcelable Creator Object
     */
    public static final Parcelable.Creator<CustomFilterCriterion> CREATOR = new Parcelable.Creator<CustomFilterCriterion>() {

        /**
         * {@inheritDoc}
         */
        public CustomFilterCriterion createFromParcel(Parcel source) {
            CustomFilterCriterion item = new CustomFilterCriterion(
                    source.readString(), source.readString(),
                    (ContentValues)source.readParcelable(ContentValues.class.getClassLoader()),
                    source.createStringArray(), source.createStringArray(),
                    (Bitmap)source.readParcelable(Bitmap.class.getClassLoader()),
                    source.readString());
            return item;
        }

        /**
         * {@inheritDoc}
         */
        public CustomFilterCriterion[] newArray(int size) {
            return new CustomFilterCriterion[size];
        }

    };
}
