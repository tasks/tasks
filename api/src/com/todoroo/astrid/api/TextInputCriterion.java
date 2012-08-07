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
public class TextInputCriterion extends CustomFilterCriterion implements Parcelable {

    /**
     * Text area prompt
     */
    public String prompt;

    /**
     * Text area hint
     */
    public String hint;


    /**
     * Create a new CustomFilterCriteria object
     *
     * @param identifier
     * @param title
     * @param sql
     * @param valuesForNewTasks
     * @param prompt
     * @param hint
     * @param icon
     * @param name
     */
    public TextInputCriterion(String identifier, String title, String sql,
            ContentValues valuesForNewTasks, String prompt, String hint,
            Bitmap icon, String name) {
        this.identifier = identifier;
        this.text = title;
        this.sql = sql;
        this.valuesForNewTasks = valuesForNewTasks;
        this.prompt = prompt;
        this.hint = hint;
        this.icon = icon;
        this.name = name;
    }

    protected TextInputCriterion() {
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
        dest.writeString(prompt);
        dest.writeString(hint);
        super.writeToParcel(dest);
    }

    /**
     * Parcelable Creator Object
     */
    public static final Parcelable.Creator<TextInputCriterion> CREATOR = new Parcelable.Creator<TextInputCriterion>() {

        /**
         * {@inheritDoc}
         */
        public TextInputCriterion createFromParcel(Parcel source) {
            TextInputCriterion item = new TextInputCriterion();
            item.prompt = source.readString();
            item.hint = source.readString();
            item.readFromParcel(source);
            return item;
        }

        /**
         * {@inheritDoc}
         */
        public TextInputCriterion[] newArray(int size) {
            return new TextInputCriterion[size];
        }

    };

}
