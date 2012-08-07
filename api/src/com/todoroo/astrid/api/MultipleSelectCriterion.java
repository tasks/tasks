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
public class MultipleSelectCriterion extends CustomFilterCriterion implements Parcelable {

    /**
     * Array of entries for user to select from
     */
    public String[] entryTitles;

    /**
     * Array of entry values corresponding to entries
     */
    public String[] entryValues;


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
    public MultipleSelectCriterion(String identifier, String title, String sql,
            ContentValues valuesForNewTasks, String[] entryTitles,
            String[] entryValues, Bitmap icon, String name) {
        this.identifier = identifier;
        this.text = title;
        this.sql = sql;
        this.valuesForNewTasks = valuesForNewTasks;
        this.entryTitles = entryTitles;
        this.entryValues = entryValues;
        this.icon = icon;
        this.name = name;
    }

    protected MultipleSelectCriterion() {
        // constructor for inflating from parceling
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
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(entryTitles);
        dest.writeStringArray(entryValues);
        super.writeToParcel(dest);
    }

    /**
     * Parcelable Creator Object
     */
    public static final Parcelable.Creator<MultipleSelectCriterion> CREATOR = new Parcelable.Creator<MultipleSelectCriterion>() {

        /**
         * {@inheritDoc}
         */
        public MultipleSelectCriterion createFromParcel(Parcel source) {
            MultipleSelectCriterion item = new MultipleSelectCriterion();
            item.entryTitles = source.createStringArray();
            item.entryValues = source.createStringArray();
            item.readFromParcel(source);
            return item;
        }

        /**
         * {@inheritDoc}
         */
        public MultipleSelectCriterion[] newArray(int size) {
            return new MultipleSelectCriterion[size];
        }

    };

}
