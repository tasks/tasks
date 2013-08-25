/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * CustomFilterCriteria allow users to build a custom filter by chaining
 * together criteria
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
abstract public class CustomFilterCriterion implements Parcelable {

    /**
     * Criteria Identifier. This identifier allows saved filters to be reloaded.
     * <p>
     * e.g "duedate"
     */
    public String identifier;

    /**
     * Criteria Title. If the title contains ?, this is replaced by the entry
     * label string selected.
     * <p>
     * e.g "Due: ?"
     */
    public String text;

    /**
     * Criterion SQL. This query should return task id's. If this contains
     * ?, it will be replaced by the entry value
     * <p>
     * Examples:
     * <ul>
     * <li><code>SELECT _id FROM tasks WHERE dueDate <= ?</code>
     * <li><code>SELECT task FROM metadata WHERE value = '?'</code>
     * </ul>
     */
    public String sql;

    /**
     * Values to apply to a task when quick-adding a task from a filter
     * created from this criterion. ? will be replaced with the entry value.
     * For example, when a user views tasks tagged 'ABC', the
     * tasks they create should also be tagged 'ABC'. If set to null, no
     * additional values will be stored for a task.
     */
    public ContentValues valuesForNewTasks = null;

    /**
     * Icon for this criteria. Can be null for no bitmap
     */
    public Bitmap icon;

    /**
     * Criteria name. This is displayed when users are selecting a criteria
     */
    public String name;

    // --- parcelable utilities

    /**
     * Utility method to write to parcel
     */
    public void writeToParcel(Parcel dest) {
        dest.writeString(identifier);
        dest.writeString(text);
        dest.writeString(sql);
        dest.writeParcelable(valuesForNewTasks, 0);
        dest.writeParcelable(icon, 0);
        dest.writeString(name);
    }

    /**
     * Utility method to read from parcel
     */
    public void readFromParcel(Parcel source) {
        identifier = source.readString();
        text = source.readString();
        sql = source.readString();
        valuesForNewTasks = (ContentValues)source.readParcelable(ContentValues.class.getClassLoader());
        icon = (Bitmap)source.readParcelable(Bitmap.class.getClassLoader());
        name = source.readString();
    }
}
