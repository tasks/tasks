/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;


import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.todoroo.andlib.sql.QueryTemplate;

public class FilterWithCustomIntent extends Filter {

    /**
     * Custom activity name
     */
    public ComponentName customTaskList = null;

    /**
     * Bundle with extras set. Can be null
     */
    public Bundle customExtras = null;

    protected FilterWithCustomIntent() {
        super();
    }

    public FilterWithCustomIntent(String listingTitle, String title,
            QueryTemplate sqlQuery, ContentValues valuesForNewTasks) {
        super(listingTitle, title, sqlQuery, valuesForNewTasks);
    }

    public FilterWithCustomIntent(String listingTitle, String title,
            String sqlQuery, ContentValues valuesForNewTasks) {
        super(listingTitle, title, sqlQuery, valuesForNewTasks);
    }

    public Intent getCustomIntent() {
        Intent intent = new Intent();
        intent.putExtra("filter", this); //$NON-NLS-1$
        intent.setComponent(new ComponentName(AstridApiConstants.ASTRID_PACKAGE, "com.todoroo.astrid.activity.TaskListActivity")); //$NON-NLS-1$
        if(customExtras != null)
            intent.putExtras(customExtras);

        return intent;
    }

    public void start(Activity activity, int resultCode) {
        activity.startActivityForResult(getCustomIntent(), resultCode);
    }

    // --- parcelable

    /**
     * {@inheritDoc}
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(customTaskList, 0);
        dest.writeParcelable(customExtras, 0);
    }

    @Override
    public void readFromParcel(Parcel source) {
        super.readFromParcel(source);
        customTaskList = source.readParcelable(ComponentName.class.getClassLoader());
        customExtras = source.readParcelable(Bundle.class.getClassLoader());
    }

    /**
     * Parcelable Creator Object
     */
    @SuppressWarnings("hiding")
    public static final Parcelable.Creator<FilterWithCustomIntent> CREATOR = new Parcelable.Creator<FilterWithCustomIntent>() {

        /**
         * {@inheritDoc}
         */
        public FilterWithCustomIntent createFromParcel(Parcel source) {
            FilterWithCustomIntent item = new FilterWithCustomIntent();
            item.readFromParcel(source);
            return item;
        }

        /**
         * {@inheritDoc}
         */
        public FilterWithCustomIntent[] newArray(int size) {
            return new FilterWithCustomIntent[size];
        }

    };

}
